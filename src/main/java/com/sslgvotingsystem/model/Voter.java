package com.sslgvotingsystem.model;

public record Voter(
        int id,
        int userId,
        boolean hasVoted
) {
    public Voter(int userId) {
        this(0, userId, false);
    }
}