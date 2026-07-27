package com.manacommunity.sports.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "contact")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Contact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 100)
    private String title;

    @Column(nullable = false, name = "contact_number", length = 20)
    private String number;

    @Column(nullable = false, length = 100)
    private String email;
}
