package com.pyjudge.model;

/** Writes exercises and lessons (subject to admin approval), grades submissions, mentors students. */
public class Instructor extends User {

    @Override
    public Role getRole() {
        return Role.INSTRUCTOR;
    }

    @Override
    public String getDashboardPath() {
        return "/instructor";
    }

    @Override
    public boolean canAuthor() {
        return true;
    }
}
