package org.practise;

import org.practise.model.CourseOffering;
import org.practise.model.EnrollmentResult;
import org.practise.model.Learner;
import org.practise.service.CourseWaitlistService;

import java.util.Set;

/** Small console demonstration of enrollment, waitlisting, and promotion. */
public final class Main {
    private Main() { }

    public static void main(String[] args) {
        CourseWaitlistService service = new CourseWaitlistService(
                new CourseOffering("ADVANCED", "INTRO", 1));
        Learner maya = new Learner("maya", Set.of("INTRO"));
        Learner noah = new Learner("noah", Set.of("INTRO"));

        System.out.println("Maya: " + service.enroll(maya));
        System.out.println("Noah: " + service.enroll(noah));
        System.out.println("Drop Maya: " + service.drop("maya"));
        System.out.println("Enrolled now: " + service.enrolledLearnerIds());
        System.out.println("Waiting now: " + service.waitlistedLearnerIds());
    }
}
