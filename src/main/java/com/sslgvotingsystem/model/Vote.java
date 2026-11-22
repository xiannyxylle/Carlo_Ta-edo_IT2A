package com.sslgvotingsystem.model;

public record Vote(
        int id,
        int voterId,
        int candidateId,
        int positionId,
        String timestamp
) {
    // Convenience constructor when casting a new vote
    public Vote(int voterId, int candidateId, int positionId) {
        this(0, voterId, candidateId, positionId, null);
    }
}