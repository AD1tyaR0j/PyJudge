package com.pyjudge.model;

/** A learner: takes lessons, solves exercises, joins challenges. */
public class Student extends User {

    @Override
    public Role getRole() {
        return Role.STUDENT;
    }

    @Override
    public String getDashboardPath() {
        return "/student";
    }

    /** Students may only message staff (instructors and admins), never other students. */
    @Override
    public boolean canMessage(User other) {
        return super.canMessage(other) && !other.isStudent();
    }
}
