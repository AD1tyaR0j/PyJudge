package com.pyjudge.model;

/** Content that belongs to the user who created it (exercises, lessons, challenges). */
public interface Authored extends Identifiable {
    /** The author's user id, or null if built in / the author was deleted. */
    Long getCreatedBy();
}
