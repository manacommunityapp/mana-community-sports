package com.manacommunity.sports.service.impl;

import com.manacommunity.common.enums.*;
import com.manacommunity.sports.model.AuctionPlayer;
import com.manacommunity.sports.repository.AuctionPlayerRepository;
import com.manacommunity.sports.service.AuctionPlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuctionPlayerServiceImpl implements AuctionPlayerService {

    private final AuctionPlayerRepository auctionPlayerRepository;

    @Override
    public AuctionPlayer savePlayer(AuctionPlayer player) {
        return auctionPlayerRepository.save(player);
    }
}
