package org.practise.model;

import java.util.Optional;
import java.util.Objects;

/** Result of dropping an enrolled learner and any resulting promotion. */
public record DropResult(Status status, Optional<String> promotedLearnerId) {
    public enum Status { DROPPED, NOT_ENROLLED }

    public DropResult {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(promotedLearnerId, "promotedLearnerId");
        if (status == Status.NOT_ENROLLED && promotedLearnerId.isPresent()) throw new IllegalArgumentException("Cannot promote on an invalid drop");
    }

    public static DropResult notEnrolled() { return new DropResult(Status.NOT_ENROLLED, Optional.empty()); }
    public static DropResult dropped(Optional<String> promoted) { return new DropResult(Status.DROPPED, promoted); }
}
