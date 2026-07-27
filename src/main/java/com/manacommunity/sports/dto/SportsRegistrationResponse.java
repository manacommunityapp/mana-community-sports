package com.manacommunity.sports.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SportsRegistrationResponse {
    private Long id;
    private EventRef event;
    private UserRef user;
    private CategoryRef category;
    private String matchType;
    private UserRef partner;
    private String status;

    private String playerName;
    private String email;
    private String relation;
    private String flatNumber;
    private Integer age;
    private String role;

    private Boolean captainNomination;
    private Boolean captainConfirmation;
    private String proposedTeamName;

    private LocalDateTime registeredAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    public static class EventRef {
        private Long id;
        private String name;
        private String eventDateStart;
        private String eventDateEnd;
        private String registrationStatus;
        private SportRef sport;
    }

    @Data
    @Builder
    public static class SportRef {
        private Long id;
        private String name;
    }

    @Data
    @Builder
    public static class UserRef {
        private Long id;
        private String fullName;
        private String name;
        private String email;
        private String phone;
        private String flatNo;
    }

    @Data
    @Builder
    public static class CategoryRef {
        private Long id;
        private String name;
        private String categoryType;
        private Integer minAge;
        private Integer maxAge;
        private String gender;
    }
}
