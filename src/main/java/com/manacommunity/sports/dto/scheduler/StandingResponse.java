package com.manacommunity.sports.dto.scheduler;

public record StandingResponse(
    int     position,
    Long    teamId,
    String  teamName,
    String  teamColor,
    int     played,
    int     won,
    int     lost,
    int     drawn,
    int     points,
    double  netRunRate,
    boolean qualified
) {}
