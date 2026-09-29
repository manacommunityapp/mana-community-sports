package com.manacommunity.sports.user.service;

import com.manacommunity.common.enums.*;
import com.manacommunity.sports.user.dto.UserProfileRequest;
import com.manacommunity.sports.user.dto.UserProfileResponse;
import com.manacommunity.common.user.model.AppUser;

public interface UserProfileService {
    UserProfileResponse getProfile(AppUser user);
    UserProfileResponse updateProfile(AppUser user, UserProfileRequest request);
}


