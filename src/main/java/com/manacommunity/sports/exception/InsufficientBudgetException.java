package com.manacommunity.sports.exception;

import org.springframework.http.HttpStatus;

/** Thrown when an auction team does not have enough budget to place a bid. */
public class InsufficientBudgetException extends ManaCommunityException {

    public InsufficientBudgetException(String teamName, int bidAmount, int remainingBudget) {
        super("Team '" + teamName + "' has insufficient budget. "
                + "Bid amount: ₹" + bidAmount + ", Remaining budget: ₹" + remainingBudget + ".",
                HttpStatus.BAD_REQUEST, "INSUFFICIENT_BUDGET");
    }
}
