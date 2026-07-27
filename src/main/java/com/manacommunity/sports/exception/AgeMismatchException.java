package com.manacommunity.sports.exception;

import org.springframework.http.HttpStatus;

/** Thrown when a user's age does not meet the sport's or category's age requirements. */
public class AgeMismatchException extends ManaCommunityException {

    public AgeMismatchException(int userAge, int minAge, int maxAge, String sportName) {
        super("Your age (" + userAge + " yrs) does not meet the eligibility for '"
                + sportName + "'. Required age: " + minAge + "–" + maxAge + " years.",
                HttpStatus.BAD_REQUEST, "AGE_MISMATCH");
    }
}
