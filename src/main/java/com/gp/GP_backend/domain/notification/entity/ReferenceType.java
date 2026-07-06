package com.gp.GP_backend.domain.notification.entity;

public enum ReferenceType {
    // Someone posted in a space you're a member of
    NEW_POST,

    // New material was shared in your space
    NEW_MATERIAL,

    // Your answer or post got upvoted 
    UPVOTE,

    // Your post was marked as a Good Question 
    GOOD_QUESTION,

    // Someone answered your question 
    NEW_ANSWER,

    // Your answer was accepted as the solution
    ANSWER_ACCEPTED
}
