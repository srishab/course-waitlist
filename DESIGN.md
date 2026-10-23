# Design

## Service boundary and architecture

`CourseWaitlistService` owns the state and operations for exactly one configured `CourseOffering`. Callers provide a `Learner` to enroll and an ID to drop. It returns explicit result values and exposes immutable ID snapshots for inspection. `Main` is a small console demonstration; there is no persistence or external interface.

## Domain and collections

`CourseOffering` contains the course ID, required prerequisite course ID, and positive capacity. `Learner` contains a stable ID and an immutable snapshot of completed course IDs. The service keeps its own history for known learners so corrections can be made under the service lock. `EnrollmentResult` distinguishes enrollment, waitlisting, duplicate requests, and prerequisite rejection. `DropResult` distinguishes an actual drop from a missing enrollment and reports an optional promoted ID.

The service stores enrolled learners in a `LinkedHashMap` keyed by ID and waitlisted learners in an `ArrayDeque`. The map prevents duplicate enrollment and preserves enrollment insertion order for snapshots; the deque provides deterministic FIFO order.

## Prerequisite rule

A learner is eligible exactly when their completed-course set contains the offering's required prerequisite course ID. Eligibility is checked before either immediate enrollment or waitlisting, and checked again when considering promotion. Completed-course history for a known learner can be corrected with `CourseWaitlistService.setCourseCompleted`; this operation shares the service lock, so it cannot race an enrollment or promotion check. Revoking the required prerequisite is rejected while the learner is enrolled, preserving the eligibility invariant. Once dropped, the history may be corrected; if the learner is waitlisted, a later promotion rechecks eligibility.

## Promotion algorithm

After removing an enrolled learner, the service removes candidates from the front of the deque one at a time. A candidate who currently meets the prerequisite is enrolled and promotion stops. An ineligible candidate is permanently removed from the active waitlist and the scan continues. If the queue is exhausted, enrollment remains below capacity and the seat is available for a later request.

## Invariants

- Enrolled count is at most offering capacity.
- Enrollment IDs and active waitlist IDs are unique.
- A learner ID is never in both collections.
- Ineligible learners cannot be enrolled or promoted.
- Eligible candidates are considered in FIFO order.
- Rejected and invalid operations do not modify membership.

The service lock protects every read and write of membership and known learner history. Enrollment, history corrections, and drop-plus-promotion each run as one critical section. Learner and offering constructors reject null or blank IDs, the offering rejects nonpositive capacity and a self-prerequisite, and blank drop IDs are rejected before state is changed.

## Decisions and assumptions

IDs are nonblank, case-sensitive strings; callers should use the same ID for the same person. The first submitted learner snapshot establishes the service's stored history for that ID, and that stored history is the source of truth for every later eligibility check. The service copies the snapshot; changing the original `Learner` object does not change service state. Later profile corrections must use `CourseWaitlistService.setCourseCompleted`, which updates the service-owned history under the service lock. A repeated enrollment request returns `ALREADY_ENROLLED` or `ALREADY_WAITLISTED` based on current membership, before checking current eligibility. Thus a queued learner with corrected history remains queued until promotion or drop; it cannot create a second queue entry. Ineligible initial requests are rejected rather than queued. An ineligible learner skipped during promotion must submit a new request if they later regain the prerequisite.

A learner dropped from enrollment can enroll again by calling `enroll` with that learner after the drop completes. Enrollment state is keyed by learner ID, not object identity. Capacity must be at least one, and a course cannot be its own prerequisite. The single lock favors simplicity and correctness for a small assessment over parallel throughput.
