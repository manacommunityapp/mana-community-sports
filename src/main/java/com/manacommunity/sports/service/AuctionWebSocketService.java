package com.manacommunity.sports.service;

import com.manacommunity.sports.dto.AuctionBidResponse;
import com.manacommunity.sports.dto.AuctionStatsResponse;
import com.manacommunity.sports.dto.PlayerWithBidResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionWebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public record AuctionEvent(String type, Object payload, LocalDateTime timestamp) {
        public AuctionEvent(String type, Object payload) {
            this(type, payload, LocalDateTime.now());
        }
    }

    public void broadcastBid(Long configId, AuctionBidResponse bid) {
        send(configId, new AuctionEvent("BID_PLACED", bid));
    }

    public void broadcastPlayerPicked(Long configId, PlayerWithBidResponse player) {
        send(configId, new AuctionEvent("PLAYER_PICKED", player));
    }

    public void broadcastPlayerSold(Long configId, PlayerSoldPayload payload) {
        send(configId, new AuctionEvent("PLAYER_SOLD", payload));
    }

    public void broadcastPlayerPassed(Long configId, PlayerPassedPayload payload) {
        send(configId, new AuctionEvent("PLAYER_PASSED", payload));
    }

    public void broadcastStatusChange(Long configId, String oldStatus, String newStatus) {
        send(configId, new AuctionEvent("STATUS_CHANGED",
                new StatusChangePayload(configId, oldStatus, newStatus)));
    }

    private void send(Long configId, AuctionEvent event) {
        String destination = "/topic/auction/" + configId;
        try {
            messagingTemplate.convertAndSend(destination, event);
            log.debug("STOMP broadcast to {}: type={}", destination, event.type());
        } catch (Exception ex) {
            log.error("STOMP broadcast failed to {}: type={}, error={}", destination, event.type(), ex.getMessage(), ex);
        }
    }

    public record PlayerSoldPayload(Long playerId, String playerName, Long teamId,
                                     String teamName, Long soldPrice) {}

    public record PlayerPassedPayload(Long playerId, String playerName, String newStatus,
                                       int queueOrder) {}

    public record StatusChangePayload(Long configId, String oldStatus, String newStatus) {}
}
