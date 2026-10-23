# Limitations

- All enrollment and waitlist state is held in memory. Restarting the process loses it; multiple service instances do not share state.
- There is no persistence, user interface, REST API, authentication, or integration with a course catalog.
- One offering has one required prerequisite. There are no alternative, combined, or time-dependent prerequisite rules.
- Learner history is mutable through the service in memory. There is no audit trail or validation against a source of truth.
- The required prerequisite cannot be revoked while a learner is enrolled; callers must drop the learner before correcting that history.
- The service uses one lock for the whole offering. This gives simple atomic enrollment and promotion but serializes concurrent operations and is not a distributed concurrency mechanism.
- Waitlisted learners who become ineligible are removed only when considered for promotion. The queue is not proactively reconciled.
- There is no cancellation reason, notification, capacity change operation, or waitlist expiration policy.

Possible future improvements include durable storage with transactional constraints, richer prerequisite rules, audit logging, and notification hooks. These are outside this assessment prototype's scope.
