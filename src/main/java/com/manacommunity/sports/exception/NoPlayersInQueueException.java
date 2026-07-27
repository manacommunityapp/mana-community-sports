package com.manacommunity.sports.exception;

import org.springframework.http.HttpStatus;

/** Thrown when the auction has no players left in the queue. */
public class NoPlayersInQueueException extends ManaCommunityException {

    public NoPlayersInQueueException(Long auctionId) {
        super("No players remaining in the queue for auction id: " + auctionId + ".",
                HttpStatus.NOT_FOUND, "NO_PLAYERS_IN_QUEUE");
    }
}
