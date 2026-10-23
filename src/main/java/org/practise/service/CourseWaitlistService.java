package org.practise.service;

import org.practise.model.CourseOffering;
import org.practise.model.DropResult;
import org.practise.model.EnrollmentResult;
import org.practise.model.Learner;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

/** Thread-safe in-memory enrollment service for exactly one course offering. */
public final class CourseWaitlistService {
    private final CourseOffering offering;
    private final ReentrantLock lock = new ReentrantLock();
    private final Map<String, Learner> enrolled = new LinkedHashMap<>();
    private final ArrayDeque<Learner> waitlist = new ArrayDeque<>();
    private final Map<String, Set<String>> completedCoursesByLearner = new HashMap<>();

    public CourseWaitlistService(CourseOffering offering) { this.offering = Objects.requireNonNull(offering, "offering"); }

    public EnrollmentResult enroll(Learner learner) {
        Objects.requireNonNull(learner, "learner");
        lock.lock();
        try {
            String id = learner.id();
            if (enrolled.containsKey(id)) return EnrollmentResult.ALREADY_ENROLLED;
            if (containsWaitlisted(id)) return EnrollmentResult.ALREADY_WAITLISTED;
            Set<String> completedCourses = completedCoursesByLearner.computeIfAbsent(id,
                    ignored -> new HashSet<>(learner.completedCourses()));
            if (!isEligible(completedCourses)) return EnrollmentResult.PREREQUISITE_NOT_MET;
            if (enrolled.size() < offering.capacity()) {
                enrolled.put(id, learner);
                return EnrollmentResult.ENROLLED;
            }
            waitlist.addLast(learner);
            return EnrollmentResult.WAITLISTED;
        } finally { lock.unlock(); }
    }

    public DropResult drop(String learnerId) {
        Objects.requireNonNull(learnerId, "learnerId");
        if (learnerId.isBlank()) throw new IllegalArgumentException("learnerId must not be blank");
        lock.lock();
        try {
            if (enrolled.remove(learnerId) == null) return DropResult.notEnrolled();
            while (!waitlist.isEmpty()) {
                Learner next = waitlist.removeFirst();
                if (isEligible(completedCoursesByLearner.get(next.id()))) {
                    enrolled.put(next.id(), next);
                    return DropResult.dropped(java.util.Optional.of(next.id()));
                }
            }
            return DropResult.dropped(java.util.Optional.empty());
        } finally { lock.unlock(); }
    }

    /** Updates a known learner's course history atomically with respect to enrollment and promotion. */
    public void setCourseCompleted(String learnerId, String courseId, boolean completed) {
        Objects.requireNonNull(learnerId, "learnerId");
        Objects.requireNonNull(courseId, "courseId");
        if (learnerId.isBlank() || courseId.isBlank()) throw new IllegalArgumentException("IDs must not be blank");
        lock.lock();
        try {
            Set<String> completedCourses = completedCoursesByLearner.get(learnerId);
            if (completedCourses == null) throw new IllegalArgumentException("Unknown learner: " + learnerId);
            if (!completed && courseId.equals(offering.requiredPrerequisiteCourseId())
                    && enrolled.containsKey(learnerId)) {
                throw new IllegalStateException("Cannot revoke the required prerequisite while the learner is enrolled");
            }
            if (completed) completedCourses.add(courseId);
            else completedCourses.remove(courseId);
        } finally { lock.unlock(); }
    }

    public int enrolledCount() {
        lock.lock(); try { return enrolled.size(); } finally { lock.unlock(); }
    }

    public int waitlistCount() {
        lock.lock(); try { return waitlist.size(); } finally { lock.unlock(); }
    }

    public List<String> enrolledLearnerIds() {
        lock.lock(); try { return List.copyOf(enrolled.keySet()); } finally { lock.unlock(); }
    }

    public List<String> waitlistedLearnerIds() {
        lock.lock(); try { return waitlist.stream().map(Learner::id).toList(); } finally { lock.unlock(); }
    }

    private boolean isEligible(Set<String> completedCourses) {
        return completedCourses != null && completedCourses.contains(offering.requiredPrerequisiteCourseId());
    }
    private boolean containsWaitlisted(String id) { return waitlist.stream().anyMatch(learner -> learner.id().equals(id)); }
}
