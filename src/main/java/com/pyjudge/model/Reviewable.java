package com.pyjudge.model;

/** Content that instructors submit and admins approve or reject before students can see it. */
public interface Reviewable extends Authored {

    ContentStatus getStatus();

    default boolean isApproved() {
        return getStatus() == ContentStatus.APPROVED;
    }

    /** Approved content is public; anything else is visible only to people who can edit it. */
    default boolean isVisibleTo(User user) {
        return isApproved() || (user != null && user.canEdit(this));
    }
}
