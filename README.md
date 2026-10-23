# Course Waitlist

## Overview

A small Java 21 and Maven prototype for managing enrollment in one course offering. It uses an in-memory service, a configurable capacity, a single course prerequisite, and a FIFO waitlist.

## Technology used

- **Java 21** for the application source and target compatibility.
- **Maven** for dependency management and build tasks.
- **JUnit 5.11.4** for automated tests.
- **Java standard library** collections and `ReentrantLock` for in-memory state and thread-safe operations. No database or application framework is used.

## Project structure

```text
course-waitlist/
├── pom.xml                         # Maven configuration and dependencies
├── README.md                       # Setup, run, and project guide
├── DESIGN.md                       # Architecture and design decisions
├── AI_USAGE.md                     # AI prompts, decisions, and verification notes
├── LIMITATIONS.md                  # Current limitations and possible improvements
└── src/
    ├── main/java/org/practise/
    │   ├── Main.java               # Console demonstration entry point
    │   ├── model/
    │   │   ├── CourseOffering.java  # Course ID, prerequisite, and capacity
    │   │   ├── Learner.java         # Learner ID and completed-course snapshot
    │   │   ├── EnrollmentResult.java
    │   │   └── DropResult.java
    │   └── service/
    │       └── CourseWaitlistService.java # Enrollment, drops, and promotion
    └── test/java/org/practise/service/
        └── CourseWaitlistServiceTest.java # JUnit 5 behavior and invariant tests
```

## Prerequisites

- IntelliJ IDEA with a Java 21 JDK configured as the Project SDK.
- Maven 3.9 or later (or IntelliJ's bundled Maven support).

## Open and run in IntelliJ

Open the project root (`course-waitlist`) in IntelliJ. Set Project SDK and language level to Java 21, then let IntelliJ load `pom.xml` as a Maven project. Open `src/main/java/org/practise/Main.java` and click the gutter run icon to run a short console demonstration of enrollment, waitlisting, and promotion.

## Run tests in IntelliJ

Open `src/test/java/org/practise/service/CourseWaitlistServiceTest.java` and click the gutter run icon next to the class or a test. Alternatively, open the Maven tool window and run `Lifecycle > test`.

## Maven commands

From the project root:

```shell
mvn clean test
mvn package
```

## Behavior

Eligible learners take an available seat; otherwise they join the end of the waitlist. A repeated request returns a distinct duplicate result. Dropping an enrolled learner frees the seat and checks waitlisted learners in FIFO order, removing any who no longer meet the prerequisite before considering the next. Invalid drops do not change state. A dropped learner may enroll again by submitting another enrollment request, subject to current eligibility and capacity. `Main` is only a demonstration; the automated tests provide broader verification.

The prerequisite is a required course ID present in a learner's completed-course history. For a learner already known to the service, `CourseWaitlistService.setCourseCompleted` allows history corrections under the same lock used for enrollment and promotion. Revoking the required prerequisite while enrolled is rejected so an ineligible learner cannot remain enrolled.
