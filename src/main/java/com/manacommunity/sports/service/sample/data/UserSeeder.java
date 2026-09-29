package com.manacommunity.sports.service.sample.data;

import com.manacommunity.common.user.model.AppUser;
import com.manacommunity.common.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserSeeder {

    private final AppUserRepository appUserRepository;

    public AppUser getSandeep() {
        return appUserRepository.findByEmail("sandeep@manacommunity.com")
                .orElseGet(() -> appUserRepository.findAll().stream().findFirst().orElse(null));
    }

    public AppUser getSuperAdmin() {
        return getSandeep();
    }

    public AppUser getSunil() {
        return appUserRepository.findByEmail("sunil@manacommunity.com")
                .orElseGet(() -> appUserRepository.findAll().stream().findFirst().orElse(null));
    }

    public AppUser getRamesh() {
        return appUserRepository.findByEmail("ramesh@manacommunity.com")
                .orElseGet(() -> appUserRepository.findAll().stream().findFirst().orElse(null));
    }

    public AppUser getUser1() {
        return appUserRepository.findByEmail("user1@manacommunity.com")
                .orElseGet(() -> appUserRepository.findAll().stream().findFirst().orElse(null));
    }

    public AppUser getMady() {
        return appUserRepository.findByEmail("mady@manacommunity.com")
                .orElseGet(() -> appUserRepository.findAll().stream().findFirst().orElse(null));
    }

    public AppUser getUserByEmail(String email) {
        return appUserRepository.findByEmail(email)
                .orElseGet(() -> appUserRepository.findAll().stream().findFirst().orElse(null));
    }
}
