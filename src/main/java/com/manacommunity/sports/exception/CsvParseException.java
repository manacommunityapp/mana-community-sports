package com.manacommunity.sports.exception;

import com.manacommunity.common.exception.ManaCommunityException;
import com.manacommunity.common.enums.*;
import org.springframework.http.HttpStatus;

/** Thrown when a user-uploaded CSV cannot be parsed. */
public class CsvParseException extends ManaCommunityException {

    public CsvParseException(String message, Throwable cause) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY, "CSV_PARSE_ERROR", cause);
    }
}


