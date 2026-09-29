package com.manacommunity.sports.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OtpService {

    public void assertEmailVerified(String email) {
        log.debug("Checking email verification for {}", email);
    }
}
