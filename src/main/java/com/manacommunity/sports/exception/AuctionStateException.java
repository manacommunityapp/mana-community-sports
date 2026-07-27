package com.manacommunity.sports.exception;

import org.springframework.http.HttpStatus;

/** Thrown when an auction operation is invalid for the current auction state. */
public class AuctionStateException extends ManaCommunityException {

    public AuctionStateException(String message) {
        super(message, HttpStatus.CONFLICT, "AUCTION_STATE_ERROR");
    }
}
