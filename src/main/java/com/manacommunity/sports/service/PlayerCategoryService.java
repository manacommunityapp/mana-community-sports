package com.manacommunity.sports.service;

import com.manacommunity.common.enums.*;
import com.manacommunity.sports.dto.PlayerCategoryRequest;
import com.manacommunity.sports.model.PlayerCategory;
import com.manacommunity.common.user.model.AppUser;
import java.util.List;

public interface PlayerCategoryService {
    List<PlayerCategory> getCategories(AppUser user);
    PlayerCategory createCategory(PlayerCategoryRequest req);
    PlayerCategory updateCategory(Long id, PlayerCategoryRequest req);
    void deleteCategory(Long id);
}


