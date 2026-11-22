package com.sslgvotingsystem.model;

public record Position(
        int id,
        String positionName,
        int maxWinners,
        int sortOrder
) {
    // Convenience constructor when adding a new position before SQLite generates an ID
    public Position(String positionName, int maxWinners, int sortOrder) {
        this(0, positionName, maxWinners, sortOrder);
    }
}