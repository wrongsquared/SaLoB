-- Function to atomically update vote counts on food_entries
-- when a vote is inserted, updated, or deleted on food_entry_votes.
-- This replaces the manual application-side count management.
CREATE OR REPLACE FUNCTION update_vote_counts()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        IF NEW.is_upvote THEN
            UPDATE food_entries SET upvote_count = upvote_count + 1 WHERE id = NEW.food_entry_id;
        ELSE
            UPDATE food_entries SET downvote_count = downvote_count + 1 WHERE id = NEW.food_entry_id;
        END IF;
    ELSIF TG_OP = 'DELETE' THEN
        IF OLD.is_upvote THEN
            UPDATE food_entries SET upvote_count = upvote_count - 1 WHERE id = OLD.food_entry_id;
        ELSE
            UPDATE food_entries SET downvote_count = downvote_count - 1 WHERE id = OLD.food_entry_id;
        END IF;
    ELSIF TG_OP = 'UPDATE' THEN
        IF OLD.is_upvote AND NOT NEW.is_upvote THEN
            UPDATE food_entries SET upvote_count = upvote_count - 1, downvote_count = downvote_count + 1 WHERE id = NEW.food_entry_id;
        ELSIF NOT OLD.is_upvote AND NEW.is_upvote THEN
            UPDATE food_entries SET downvote_count = downvote_count - 1, upvote_count = upvote_count + 1 WHERE id = NEW.food_entry_id;
        END IF;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Apply trigger on food_entry_votes table
DROP TRIGGER IF EXISTS trg_update_vote_counts ON food_entry_votes;
CREATE TRIGGER trg_update_vote_counts
AFTER INSERT OR DELETE OR UPDATE ON food_entry_votes
FOR EACH ROW
EXECUTE FUNCTION update_vote_counts();
