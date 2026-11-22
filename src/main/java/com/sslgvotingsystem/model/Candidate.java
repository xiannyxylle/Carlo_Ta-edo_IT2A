package com.sslgvotingsystem.model;

public record Candidate(
        int id,
        int userId,
        int positionId,
        String partyList,
        String platform,
        String candidateName,
        String positionName
) {
    // Constructor for inserting a new candidate into the database
    public Candidate(int userId, int positionId, String partyList, String platform) {
        this(0, userId, positionId, partyList, platform, null, null);
    }

    // Constructor for updating an existing candidate
    public Candidate(int id, int userId, int positionId, String partyList, String platform) {
        this(id, userId, positionId, partyList, platform, null, null);
    }
}