package org.practise.service;

import org.junit.jupiter.api.Test;
import org.practise.model.CourseOffering;
import org.practise.model.DropResult;
import org.practise.model.EnrollmentResult;
import org.practise.model.Learner;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class CourseWaitlistServiceTest {
    private static final String PREREQUISITE = "INTRO";

    private CourseWaitlistService service(int capacity) {
        return new CourseWaitlistService(new CourseOffering("ADVANCED", PREREQUISITE, capacity));
    }

    private Learner eligible(String id) { return new Learner(id, Set.of(PREREQUISITE)); }
    private Learner ineligible(String id) { return new Learner(id, Set.of()); }

    @Test void constructorsRejectNullOrBlankIdsAndNonPositiveCapacity() {
        assertThrows(NullPointerException.class, () -> new Learner(null, Set.of()));
        assertThrows(IllegalArgumentException.class, () -> new Learner("  ", Set.of()));
        assertThrows(NullPointerException.class, () -> new Learner("a", null));
        assertThrows(IllegalArgumentException.class, () -> new Learner("a", Set.of(" ")));
        assertThrows(NullPointerException.class, () -> new CourseOffering(null, PREREQUISITE, 1));
        assertThrows(IllegalArgumentException.class, () -> new CourseOffering(" ", PREREQUISITE, 1));
        assertThrows(NullPointerException.class, () -> new CourseOffering("ADVANCED", null, 1));
        assertThrows(IllegalArgumentException.class, () -> new CourseOffering("ADVANCED", " ", 1));
    }

    @Test void rejectsZeroCapacity() {
        assertThrows(IllegalArgumentException.class,
                () -> new CourseOffering("ADVANCED", PREREQUISITE, 0));
    }

    @Test void dropResultRejectsNullFields() {
        assertThrows(NullPointerException.class,
                () -> new DropResult(null, java.util.Optional.empty()));
        assertThrows(NullPointerException.class,
                () -> new DropResult(DropResult.Status.DROPPED, null));
    }

    @Test void enrollsWhenCapacityIsAvailable() {
        CourseWaitlistService service = service(1);
        assertEquals(EnrollmentResult.ENROLLED, service.enroll(eligible("a")));
        assertEquals(List.of("a"), service.enrolledLearnerIds());
        assertEquals(0, service.waitlistCount());
    }

    @Test void waitlistsWhenCourseIsFull() {
        CourseWaitlistService service = service(1);
        service.enroll(eligible("a"));
        assertEquals(EnrollmentResult.WAITLISTED, service.enroll(eligible("b")));
        assertEquals(List.of("b"), service.waitlistedLearnerIds());
    }

    @Test void duplicateRequestsHaveConsistentResults() {
        CourseWaitlistService service = service(1);
        Learner a = eligible("a");
        assertEquals(EnrollmentResult.ENROLLED, service.enroll(a));
        assertEquals(EnrollmentResult.ALREADY_ENROLLED, service.enroll(a));
        service.enroll(eligible("b"));
        assertEquals(EnrollmentResult.ALREADY_WAITLISTED, service.enroll(eligible("b")));
        assertEquals(1, service.enrolledCount());
        assertEquals(1, service.waitlistCount());
    }

    @Test void dropPromotesFifoLearner() {
        CourseWaitlistService service = service(1);
        service.enroll(eligible("a")); service.enroll(eligible("b")); service.enroll(eligible("c"));
        assertEquals(new DropResult(DropResult.Status.DROPPED, java.util.Optional.of("b")), service.drop("a"));
        assertEquals(List.of("b"), service.enrolledLearnerIds());
        assertEquals(List.of("c"), service.waitlistedLearnerIds());
    }

    @Test void rejectsLearnerWithoutPrerequisite() {
        CourseWaitlistService service = service(1);
        assertEquals(EnrollmentResult.PREREQUISITE_NOT_MET, service.enroll(ineligible("x")));
        assertEquals(0, service.enrolledCount());
        assertEquals(0, service.waitlistCount());
    }

    @Test void skipsAndRemovesWaitlistedLearnerWhosePrerequisiteWasRevoked() {
        CourseWaitlistService service = service(1);
        service.enroll(eligible("a"));
        Learner first = eligible("b");
        service.enroll(first);
        service.enroll(eligible("c"));
        service.setCourseCompleted(first.id(), PREREQUISITE, false);

        assertEquals(java.util.Optional.of("c"), service.drop("a").promotedLearnerId());
        assertEquals(List.of("c"), service.enrolledLearnerIds());
        assertEquals(List.of(), service.waitlistedLearnerIds());
    }

    @Test void skipsAndRemovesMultipleIneligibleLearnersBeforePromotion() {
        CourseWaitlistService service = service(1);
        service.enroll(eligible("a"));
        service.enroll(eligible("b"));
        service.enroll(eligible("c"));
        service.enroll(eligible("d"));
        service.setCourseCompleted("b", PREREQUISITE, false);
        service.setCourseCompleted("c", PREREQUISITE, false);

        assertEquals(java.util.Optional.of("d"), service.drop("a").promotedLearnerId());
        assertEquals(List.of("d"), service.enrolledLearnerIds());
        assertEquals(List.of(), service.waitlistedLearnerIds());
        assertEquals(1, service.enrolledCount());
    }

    @Test void invalidAndRepeatedDropsDoNotChangeState() {
        CourseWaitlistService service = service(1);
        service.enroll(eligible("a"));
        assertEquals(DropResult.Status.NOT_ENROLLED, service.drop("missing").status());
        assertEquals(DropResult.Status.DROPPED, service.drop("a").status());
        assertEquals(DropResult.Status.NOT_ENROLLED, service.drop("a").status());
        assertEquals(0, service.enrolledCount());
        assertEquals(0, service.waitlistCount());
    }

    @Test void blankDropIdIsRejectedWithoutChangingState() {
        CourseWaitlistService service = service(1);
        service.enroll(eligible("a"));

        assertThrows(IllegalArgumentException.class, () -> service.drop(" "));
        assertEquals(List.of("a"), service.enrolledLearnerIds());
        assertEquals(0, service.waitlistCount());
    }

    @Test void cannotRevokeRequiredPrerequisiteWhileLearnerIsEnrolled() {
        CourseWaitlistService service = service(1);
        service.enroll(eligible("a"));

        assertThrows(IllegalStateException.class,
                () -> service.setCourseCompleted("a", PREREQUISITE, false));
        assertEquals(List.of("a"), service.enrolledLearnerIds());
    }

    @Test void dropWithEmptyWaitlistLeavesSeatAvailableAndAllowsReenrollment() {
        CourseWaitlistService service = service(1);
        Learner a = eligible("a");
        service.enroll(a);
        assertTrue(service.drop("a").promotedLearnerId().isEmpty());
        assertEquals(EnrollmentResult.ENROLLED, service.enroll(a));
        assertEquals(List.of("a"), service.enrolledLearnerIds());
    }

    @Test void preservesCapacityUniquenessAndDisjointMembership() {
        CourseWaitlistService service = service(2);
        for (String id : List.of("a", "b", "c", "d")) service.enroll(eligible(id));
        List<String> enrolled = service.enrolledLearnerIds();
        List<String> waiting = service.waitlistedLearnerIds();
        assertTrue(enrolled.size() <= 2);
        assertEquals(enrolled.size(), new HashSet<>(enrolled).size());
        assertEquals(waiting.size(), new HashSet<>(waiting).size());
        assertTrue(java.util.Collections.disjoint(enrolled, waiting));
        assertEquals(2, enrolled.size());
        assertEquals(2, waiting.size());
    }

    @Test void concurrentEnrollmentNeverExceedsCapacityOrDuplicates() throws Exception {
        int learners = 40;
        CourseWaitlistService service = service(7);
        CountDownLatch ready = new CountDownLatch(8);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new java.util.ArrayList<>();
        try (var pool = Executors.newFixedThreadPool(8)) {
            for (int i = 0; i < learners; i++) {
                String id = "learner-" + i;
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    try { start.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
                    service.enroll(eligible(id));
                }));
            }
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            pool.shutdown();
            assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
            for (Future<?> future : futures) future.get();
        }
        List<String> enrolled = service.enrolledLearnerIds();
        List<String> waiting = service.waitlistedLearnerIds();
        assertEquals(7, enrolled.size());
        assertEquals(learners - 7, waiting.size());
        assertEquals(learners, new HashSet<>(enrolled).size() + new HashSet<>(waiting).size());
        assertTrue(java.util.Collections.disjoint(enrolled, waiting));
    }
}
