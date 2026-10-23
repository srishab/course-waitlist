# AI usage

## Prompts and guidance used

This project was produced in this conversation. The actual user requests included:

1. The initial request to build the complete “Focused AI-Assisted Engineering Assessment: Course Waitlist” from scratch, with Java 21, Maven, JUnit 5, the specified enrollment/drop/prerequisite/invariant behaviors, four root documentation files, and a test/build attempt.
2. Follow-up review guidance to validate IDs and capacity, test multiple ineligible waitlisted learners, inspect concurrency-task failures, document the service-owned prerequisite history, and run tests.
3. request to make `DropResult` reject null fields and add a zero-capacity test.
4. The final audit request to inspect actual files, produce an evidence-based audit, fix genuine gaps, verify deliverables and scope, and rerun tests.

These are concise descriptions of the prompts, not fabricated transcripts. The suggested checks in items 3 and 4 came from the user and were implemented. AI design choices and review findings are summarized below.

## Accepted, adapted, and rejected choices

- Kept the requested one-offering in-memory service, standard collections, JUnit 5, explicit result types, and a `ReentrantLock` around enrollment, drops, promotion, and prerequisite updates.
- Used one required prerequisite course ID in a learner's completed-course snapshot. Adapted mutable learner progress into service-owned history so changes use the service lock. The service copy is the eligibility source of truth after first submission.
- Added a guard that rejects revoking the required prerequisite while a learner is enrolled. This closes the invariant gap found during final review; callers can drop the learner before correcting their history.
- Added a minimal console `Main` because the user asked how to run the application. No frontend, REST service, database, or unrelated dependency was added.
- No requested assessment-scope feature was rejected. Broader persistence, APIs, authentication, and notifications remain out of scope.

## Files and decisions affected

Production: `pom.xml`, `.idea/misc.xml`, `.idea/compiler.xml`, and `src/main/java/org/practise/` (including the demo, model, result types, and service). Tests: `src/test/java/org/practise/service/CourseWaitlistServiceTest.java`. Documentation: `README.md`, `DESIGN.md`, this file, and `LIMITATIONS.md`.

The tests added during review cover null/blank inputs, zero capacity, multiple ineligible waiters, concurrent worker failures via `Future.get`, invalid blank drops, and attempted prerequisite revocation for an enrolled learner.

## Verification evidence

The final audit ran IntelliJ's bundled Maven executable because `mvn` is not on the terminal `PATH`. `mvn clean test` completed with **16 tests, 0 failures, 0 errors, 0 skipped**. `mvn package` completed successfully. Maven compiled with `--release 21`. The runtime available in the environment reported Java 26.0.2; the IDE project language level and bytecode target are configured to 21. No claim is made that the tests were executed on a Java 21 runtime.
