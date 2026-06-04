package com.salob.proto.events;

import java.util.UUID;

public record VoteEvent(
    UUID eventId,
    UUID voterId,
    UUID idOfEntryVotedOn,
    UUID submitterIdOfEntryVotedOn,
    VoteType voteType
) {}
