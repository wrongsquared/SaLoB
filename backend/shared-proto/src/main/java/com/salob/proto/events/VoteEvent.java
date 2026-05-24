package com.salob.proto.events;

public record VoteEvent(
    String eventId,
    String voterId,
    String submitterIdOfEntryVotedOn,
    VoteType voteType
) {}
