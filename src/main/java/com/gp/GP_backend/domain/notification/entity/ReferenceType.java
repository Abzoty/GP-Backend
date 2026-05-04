package com.gp.GP_backend.domain.notification.entity;

public enum ReferenceType {
    // Someone posted in a space you're a member of (US-037)
    NEW_POST,

    // New material was shared in your space (US-038)
    NEW_MATERIAL,

    // Your answer or post got upvoted (US-039)
    UPVOTE,

    // Your post was marked as a Good Question (US-039)
    GOOD_QUESTION,

    // Someone answered your question (US-015 — notification sent to question author)
    NEW_ANSWER,

    // Your answer was accepted as the solution
    ANSWER_ACCEPTED
}
