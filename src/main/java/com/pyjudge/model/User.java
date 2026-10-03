package com.pyjudge.model;

import java.time.LocalDateTime;

/**
 * Base class for every account. Each role is a subclass that overrides what it may do,
 * so permission checks are polymorphic: {@code user.canAuthor()} rather than {@code if role == ...}.
 */
public abstract class User implements Identifiable {

    private long id;
    private String username;
    private String name;
    private String email;
    private String bio = "";
    private String passwordHash;
    private boolean blocked;
    private LocalDateTime createdAt;
    private LocalDateTime lastSeenAt;

    /** Which role this subclass represents. */
    public abstract Role getRole();

    /** Where this user lands after logging in. */
    public abstract String getDashboardPath();

    /** May create exercises, lessons, and challenges. */
    public boolean canAuthor() {
        return false;
    }

    /** May manage users, approve content, and change settings. */
    public boolean isAdmin() {
        return false;
    }

    public boolean isInstructor() {
        return getRole() == Role.INSTRUCTOR;
    }

    public boolean isStudent() {
        return getRole() == Role.STUDENT;
    }

    /** Admins edit anything; instructors edit only content they created. */
    public boolean canEdit(Authored content) {
        return isAdmin() || (canAuthor() && content.getCreatedBy() != null && content.getCreatedBy() == id);
    }

    /** May this user message {@code other}? Overridden for students. */
    public boolean canMessage(User other) {
        return other != null && other.id != id && !other.blocked;
    }

    public String getDisplayName() {
        return name == null || name.isBlank() ? username : name;
    }

    public String getInitial() {
        return getDisplayName().substring(0, 1).toUpperCase();
    }

    // JavaBean accessors (also used by JSP EL: ${user.displayName}, ${user.admin}, ...)
    @Override public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio == null ? "" : bio; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public boolean isBlocked() { return blocked; }
    public void setBlocked(boolean blocked) { this.blocked = blocked; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(LocalDateTime lastSeenAt) { this.lastSeenAt = lastSeenAt; }
    public boolean isCanAuthor() { return canAuthor(); }

    /** Factory: builds the right subclass for a role (used when loading rows from the database). */
    public static User forRole(Role role) {
        return switch (role) {
            case STUDENT -> new Student();
            case INSTRUCTOR -> new Instructor();
            case ADMIN -> new Admin();
        };
    }

    @Override
    public String toString() {
        return getRole().getLabel() + "(" + username + ")";
    }
}
