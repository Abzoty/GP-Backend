package com.gp.GP_backend.domain.space.entity;

/**
 * Valid categories for a {@link Space}.
 *
 * <p>
 * The category drives the duplicate / similarity check performed during space
 * creation ({@code force=0}):
 * <ul>
 * <li>{@link #COLLEGE_COURSE} – spaces are matched by {@code courseCode}; any
 * existing space sharing the same course code is returned as a conflict.</li>
 * <li>All other categories – spaces are matched by Jaccard text-similarity on
 * {@code name + description}; spaces with a score &gt; 0 are returned as
 * conflicts.</li>
 * </ul>
 */
public enum SpaceCategory {

    /** A space tied to a specific college course, matched by {@code courseCode}. */
    COLLEGE_COURSE,

    COMPUTER_SCIENCE,
    ENGINEERING,
    MATHEMATICS,
    PHYSICS,
    CHEMISTRY,
    BIOLOGY,
    ARTS,
    BUSINESS,
    GENERAL
}