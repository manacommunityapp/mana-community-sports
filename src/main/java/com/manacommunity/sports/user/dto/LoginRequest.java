package com.manacommunity.sports.user.dto;


import com.manacommunity.common.enums.*;
import lombok.Data;

@Data
public class LoginRequest {
    private String email;
    private String password;

}

