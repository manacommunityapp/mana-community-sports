package com.manacommunity.sports.service.impl;

import com.manacommunity.common.enums.*;
import com.manacommunity.common.model.Community;
import com.manacommunity.sports.dto.PlayerCategoryRequest;
import com.manacommunity.common.exception.ResourceNotFoundException;
import com.manacommunity.sports.model.PlayerCategory;
import com.manacommunity.sports.repository.CommunityRepository;
import com.manacommunity.sports.repository.PlayerCategoryRepository;
import com.manacommunity.sports.service.PlayerCategoryService;
import com.manacommunity.common.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

import static com.manacommunity.sports.constants.PermissionConstants.ROLE_SUPER_ADMIN;

@Service
@RequiredArgsConstructor
public class PlayerCategoryServiceImpl implements PlayerCategoryService {

    private final PlayerCategoryRepository categoryRepo;
    private final CommunityRepository communityRepo;

    @Override
    @Transactional(readOnly = true)
    public List<PlayerCategory> getCategories(AppUser user) {
        if (ROLE_SUPER_ADMIN.equals(user.getRole())) {
            return categoryRepo.findAll();
        }
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (communityId != null) {
            return categoryRepo.findDefaultAndCommunityCategories(communityId);
        }
        return Collections.emptyList();
    }

    @Override
    @Transactional
    public PlayerCategory createCategory(PlayerCategoryRequest req) {
        PlayerCategory cat = PlayerCategory.builder()
                .name(req.getName())
                .category_type(req.getCategoryType())
                .description(req.getDescription())
                .minAge(req.getMinAge())
                .maxAge(req.getMaxAge())
                .gender(req.getGender())
                .type(req.getType())
                .build();
        if (req.getCommunityId() != null) {
            cat.setCommunity(communityRepo.findById(req.getCommunityId())
                    .orElseThrow(() -> new ResourceNotFoundException("Community", req.getCommunityId())));
        }
        return categoryRepo.save(cat);
    }

    @Override
    @Transactional
    public PlayerCategory updateCategory(Long id, PlayerCategoryRequest req) {
        PlayerCategory cat = categoryRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PlayerCategory", id));
        cat.setName(req.getName());
        cat.setCategory_type(req.getCategoryType());
        cat.setDescription(req.getDescription());
        cat.setMinAge(req.getMinAge());
        cat.setMaxAge(req.getMaxAge());
        cat.setGender(req.getGender());
        if (req.getType() != null) {
            cat.setType(req.getType());
        }
        if (req.getCommunityId() != null) {
            cat.setCommunity(communityRepo.findById(req.getCommunityId())
                    .orElseThrow(() -> new ResourceNotFoundException("Community", req.getCommunityId())));
        } else {
            cat.setCommunity(null);
        }
        return categoryRepo.save(cat);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        categoryRepo.deleteById(id);
    }
}



