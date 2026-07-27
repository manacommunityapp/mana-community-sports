package com.manacommunity.sports.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MCTResponse {

    private int statusCode;
    private String description;
    private String message;
    private Object data;

    public MCTResponse(int statusCode, String description, String message) {
        this.statusCode = statusCode;
        this.description = description;
        this.message = message;
    }
}
