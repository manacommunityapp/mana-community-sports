package com.manacommunity.sports.exception;

import org.springframework.http.HttpStatus;

/** Thrown when a requested entity (user, event, team, etc.) does not exist. */
public class ResourceNotFoundException extends ManaCommunityException {

    public ResourceNotFoundException(String resource, Long id) {
        super(resource + " not found with id: " + id, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
    }

    public ResourceNotFoundException(String resource, String field, String value) {
        super(resource + " not found with the given " + field + ".", HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
    }
}
