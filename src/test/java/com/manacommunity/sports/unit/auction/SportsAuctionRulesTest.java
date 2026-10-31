package com.manacommunity.sports.unit.auction;

import com.manacommunity.common.exception.AuctionStateException;
import com.manacommunity.common.security.AuditService;
import com.manacommunity.common.user.model.AppUser;
import com.manacommunity.common.user.repository.AppUserRepository;
import com.manacommunity.sports.dto.BidRequest;
import com.manacommunity.sports.dto.SoldPlayerRequest;
import com.manacommunity.sports.model.*;
import com.manacommunity.sports.repository.*;
import com.manacommunity.sports.service.AuctionWebSocketService;
import com.manacommunity.sports.service.NotificationManagementService;
import com.manacommunity.sports.service.impl.AuctionServiceImpl;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SportsAuctionRulesTest {

    @Mock private AuctionConfigRepository configRepo;
    @Mock private AuctionPlayerRepository playerRepo;
    @Mock private AuctionBidRepository bidRepo;
    @Mock private AuctionTeamRepository teamRepo;
    @Mock private AuctionSessionLogRepository logRepo;
    @Mock private SportMetaRepository sportRepo;
    @Mock private AppUserRepository userRepo;
    @Mock private AuctionConfigCategoryRepository auctionConfigCategoryRepo;
    @Mock private AuctionDisputeCommitteeRepository committeeRepo;
    @Mock private SportsEventRegistrationRepository registrationRepo;
    @Mock private SportsEventRepository eventRepo;
    @Mock private AuditService auditService;
    @Mock private MeterRegistry meterRegistry;
    @Mock private NotificationManagementService notificationService;
    @Mock private AuctionWebSocketService auctionWs;
    @Mock private Counter counter;

    @InjectMocks
    private AuctionServiceImpl auctionService;

    private AuctionConfig liveConfig;
    private AuctionPlayer player;
    private AuctionTeam teamA;
    private AuctionTeam teamB;
    private AppUser bidder;

    @BeforeEach
    void setUp() {
        when(meterRegistry.counter(anyString(), any(String[].class))).thenReturn(counter);

        liveConfig = AuctionConfig.builder()
                .id(1L)
                .status(AuctionConfig.AuctionStatus.LIVE)
                .basePrice(100)
                .budgetPerTeam(1000L)
                .bidIncrementDefault(20)
                .bidIncrementThreshold(500L)
                .bidIncrementAbove(50)
                .build();

        player = AuctionPlayer.builder()
                .id(10L)
                .config(liveConfig)
                .playerName("Virat Kohli")
                .basePrice(100)
                .status(AuctionPlayer.PlayerStatus.QUEUED)
                .build();

        teamA = AuctionTeam.builder()
                .id(100L)
                .teamName("Royal Challengers")
                .remainingBudget(500L)
                .spent(0L)
                .build();

        teamB = AuctionTeam.builder()
                .id(200L)
                .teamName("Super Kings")
                .remainingBudget(200L)
                .spent(0L)
                .build();

        bidder = AppUser.builder()
                .id(5L)
                .email("bidder@community.com")
                .build();

        when(configRepo.findById(1L)).thenReturn(Optional.of(liveConfig));
        when(playerRepo.findByIdForUpdate(10L)).thenReturn(Optional.of(player));
        when(teamRepo.findByIdForUpdate(100L)).thenReturn(Optional.of(teamA));
        when(teamRepo.findByIdForUpdate(200L)).thenReturn(Optional.of(teamB));
        when(userRepo.getReferenceById(anyLong())).thenReturn(bidder);
    }

    @Test
    @DisplayName("Simultaneous Bidding: Incremental bids succeed while maintaining sequential minimum requirements")
    void testSimultaneousBidding_Increments() {
        // First bid by Team A: base price ₹100
        when(bidRepo.findMaxBidForPlayer(10L)).thenReturn(Optional.empty());
        when(bidRepo.save(any(AuctionBid.class))).thenAnswer(i -> {
            AuctionBid b = i.getArgument(0);
            b.setId(1001L);
            return b;
        });

        BidRequest bid1 = new BidRequest(1L, 10L, 100L, 100L, false);
        AuctionBid result1 = auctionService.placeBid(bid1, 5L);
        assertNotNull(result1);
        assertEquals(100L, result1.getBidAmount());

        // Subsequent bid by Team B: current max is ₹100, increment is ₹20 -> ₹120 required
        when(bidRepo.findMaxBidForPlayer(10L)).thenReturn(Optional.of(100L));
        BidRequest bid2 = new BidRequest(1L, 10L, 200L, 120L, false);
        AuctionBid result2 = auctionService.placeBid(bid2, 6L);
        assertNotNull(result2);
        assertEquals(120L, result2.getBidAmount());

        // Under-bid attempt (₹110 instead of ₹120) throws AuctionStateException
        BidRequest invalidBid = new BidRequest(1L, 10L, 100L, 110L, false);
        assertThrows(AuctionStateException.class, () -> auctionService.placeBid(invalidBid, 5L));
    }

    @Test
    @DisplayName("Budget Enforcement: Rejects bid exceeding team's remaining purse")
    void testBudgetEnforcement_ThrowsWhenInsufficientPurse() {
        // Team B has ₹200 remaining purse. Attempting to bid ₹250 must fail.
        when(bidRepo.findMaxBidForPlayer(10L)).thenReturn(Optional.of(150L));
        BidRequest overBudgetBid = new BidRequest(1L, 10L, 200L, 250L, false);

        AuctionStateException ex = assertThrows(AuctionStateException.class,
                () -> auctionService.placeBid(overBudgetBid, 6L));
        assertTrue(ex.getMessage().contains("budget insufficient"));
    }

    @Test
    @DisplayName("Duplicate Player Selection: Prevents selling an already SOLD player")
    void testDuplicatePlayerSelection_ThrowsWhenPlayerAlreadySold() {
        player.setStatus(AuctionPlayer.PlayerStatus.SOLD);
        player.setAssignedTeam(teamA);

        SoldPlayerRequest req = new SoldPlayerRequest(10L, 100L);
        AuctionStateException ex = assertThrows(AuctionStateException.class,
                () -> auctionService.soldPlayer(req, 1L));
        assertTrue(ex.getMessage().contains("already SOLD"));
    }

    @Test
    @DisplayName("Successful Sale: Deducts team budget and marks player SOLD")
    void testSuccessfulSale_DeductsPurseAndAssignsPlayer() {
        player.setStatus(AuctionPlayer.PlayerStatus.SELLING);
        when(bidRepo.findMaxBidForPlayer(10L)).thenReturn(Optional.of(300L));
        when(playerRepo.save(any(AuctionPlayer.class))).thenAnswer(i -> i.getArgument(0));

        SoldPlayerRequest req = new SoldPlayerRequest(10L, 100L);
        AuctionPlayer sold = auctionService.soldPlayer(req, 1L);

        assertEquals(AuctionPlayer.PlayerStatus.SOLD, sold.getStatus());
        assertEquals(300L, sold.getSoldPrice());
        assertEquals(teamA, sold.getAssignedTeam());
        assertEquals(200L, teamA.getRemainingBudget()); // 500 - 300 = 200
        assertEquals(300L, teamA.getSpent());
    }
}