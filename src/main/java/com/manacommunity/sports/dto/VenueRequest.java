package com.manacommunity.sports.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class VenueRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @Size(max = 255)
    private String address;

    @Size(max = 50)
    private String city;

    @Size(max = 50)
    private String area;

    @Size(max = 10)
    private String pinCode;

    @Size(max = 500)
    private String mapLink;

    private Integer capacity;

    @Size(max = 30)
    private String venueType;

    @Size(max = 50)
    private String venueCategory;

    @Size(max = 20)
    private String openingTime;

    @Size(max = 20)
    private String closingTime;

    @Size(max = 100)
    private String contactName;

    @Size(max = 15)
    private String contactNumber;

    @Size(max = 120)
    private String contactEmail;

    @Size(max = 100)
    private String contactTitle;

    private List<ContactDto> contacts;

    private List<CourtDto> courts;

    @Data
    public static class CourtDto {
        @NotBlank
        @Size(max = 50)
        private String name;

        @Size(max = 20)
        private String color;

        @Size(max = 20)
        private String openingTime;

        @Size(max = 20)
        private String closingTime;
    }
}
