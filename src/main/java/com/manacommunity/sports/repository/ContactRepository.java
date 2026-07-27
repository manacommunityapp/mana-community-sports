package com.manacommunity.sports.repository;

import com.manacommunity.sports.model.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {
    Optional<Contact> findByNameAndNumberAndEmail(String name, String number, String email);
}
