package org.practise.model;

import java.util.Objects;

/** Configuration for the single offering managed by a waitlist service. */
public record CourseOffering(String courseId, String requiredPrerequisiteCourseId, int capacity) {
    public CourseOffering {
        Objects.requireNonNull(courseId, "courseId");
        Objects.requireNonNull(requiredPrerequisiteCourseId, "requiredPrerequisiteCourseId");
        if (courseId.isBlank() || requiredPrerequisiteCourseId.isBlank()) throw new IllegalArgumentException("Course IDs must not be blank");
        if (courseId.equals(requiredPrerequisiteCourseId)) throw new IllegalArgumentException("Course cannot be its own prerequisite");
        if (capacity <= 0) throw new IllegalArgumentException("Course capacity must be greater than zero");
    }
}
