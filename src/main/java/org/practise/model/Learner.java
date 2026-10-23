package org.practise.model;

import java.util.Objects;
import java.util.Set;

/** An immutable learner identity and snapshot of completed-course history. */
public final class Learner {
    private final String id;
    private final Set<String> completedCourses;

    public Learner(String id, Set<String> completedCourses) {
        this.id = requireText(id, "id");
        Objects.requireNonNull(completedCourses, "completedCourses").forEach(course -> requireText(course, "completed course"));
        this.completedCourses = Set.copyOf(completedCourses);
    }

    public String id() { return id; }

    public Set<String> completedCourses() { return completedCourses; }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }
}
