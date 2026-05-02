/**
 * @param t - The number of upvotes it would take from users with a WTF of 
 * 			  50 (i.e no multiplicative effect) to reach full confidence
 * @returns A value between 0-100
 */
const finalConfidenceScore = (foodEntry, t): number => {
    const baseVoteValue = 100 / t
    const conf = 0
    for (const vote of foodEntry.votes) {
        const actualVoteValue = vote.isUpvote ? baseVoteValue : -baseVoteValue
        conf += actualVoteValue * computeMultiplier(vote.voter.WTFScore) // Lagrange
    }
    const decayedConf = decay(conf, foodEntry.ageInYears)
    return clamp(0, 100, decayedConf)
}
