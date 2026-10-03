package com.pyjudge.model;

/** Manages users, approves content, and controls system settings. Can do everything an instructor can. */
public class Admin extends Instructor {

    @Override
    public Role getRole() {
        return Role.ADMIN;
    }

    @Override
    public String getDashboardPath() {
        return "/admin";
    }

    @Override
    public boolean isAdmin() {
        return true;
    }
}
