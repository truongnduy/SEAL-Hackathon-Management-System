package hackthon.mangaement.hackathon.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseTriggerInitializer implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Initializing MySQL database triggers...");
        
        // 1. Drop existing triggers
        String[] dropTriggers = {
            "trg_lock_score_insert",
            "trg_lock_score_update",
            "trg_lock_member_insert",
            "trg_lock_member_update",
            "trg_audit_team_status",
            "trg_check_mentor_judge_conflict_insert",
            "trg_check_mentor_judge_conflict_update",
            "trg_check_submission_round_is_final_insert",
            "trg_check_submission_round_is_final_update",
            "trg_check_criteria_round_is_final_insert",
            "trg_check_criteria_round_is_final_update",
            "trg_check_team_track_same_hackathon_insert",
            "trg_check_team_track_same_hackathon_update",
            "trg_check_judge_mentor_conflict_insert",
            "trg_check_judge_mentor_conflict_update",
            "trg_prevent_track_in_final_round_insert",
            "trg_prevent_track_in_final_round_update"
        };
        
        for (String trigger : dropTriggers) {
            jdbcTemplate.execute("DROP TRIGGER IF EXISTS " + trigger);
        }
        
        // 2. Create triggers
        // trg_lock_score_insert
        jdbcTemplate.execute(
            "CREATE TRIGGER trg_lock_score_insert " +
            "BEFORE INSERT ON scores " +
            "FOR EACH ROW " +
            "BEGIN " +
            "    DECLARE v_locked BOOLEAN; " +
            "    SELECT r.scoring_locked INTO v_locked " +
            "    FROM submissions s " +
            "    LEFT JOIN tracks tr ON tr.id = s.track_id " +
            "    JOIN rounds r ON r.id = COALESCE(tr.round_id, s.round_id) " +
            "    WHERE s.id = NEW.submission_id; " +
            "    IF v_locked = TRUE THEN " +
            "        SIGNAL SQLSTATE '45000' " +
            "        SET MESSAGE_TEXT = 'Cannot score: round has locked scoring.'; " +
            "    END IF; " +
            "END"
        );
        
        // trg_lock_score_update
        jdbcTemplate.execute(
            "CREATE TRIGGER trg_lock_score_update " +
            "BEFORE UPDATE ON scores " +
            "FOR EACH ROW " +
            "BEGIN " +
            "    DECLARE v_locked BOOLEAN; " +
            "    SELECT r.scoring_locked INTO v_locked " +
            "    FROM submissions s " +
            "    LEFT JOIN tracks tr ON tr.id = s.track_id " +
            "    JOIN rounds r ON r.id = COALESCE(tr.round_id, s.round_id) " +
            "    WHERE s.id = NEW.submission_id; " +
            "    IF v_locked = TRUE THEN " +
            "        SIGNAL SQLSTATE '45000' " +
            "        SET MESSAGE_TEXT = 'Cannot score: round has locked scoring.'; " +
            "    END IF; " +
            "END"
        );
        
        // trg_lock_member_insert
        jdbcTemplate.execute(
            "CREATE TRIGGER trg_lock_member_insert " +
            "BEFORE INSERT ON team_members " +
            "FOR EACH ROW " +
            "BEGIN " +
            "    DECLARE v_locked BOOLEAN; " +
            "    SELECT is_locked INTO v_locked FROM teams WHERE id = NEW.team_id; " +
            "    IF v_locked = TRUE THEN " +
            "        INSERT INTO audit_logs (action, target_table, target_id, detail) " +
            "        VALUES ('MEMBER_CHANGE_DENIED', 'team_members', NEW.team_id, " +
            "                JSON_OBJECT('user_id', NEW.user_id, 'operation', 'INSERT')); " +
            "        SIGNAL SQLSTATE '45000' " +
            "        SET MESSAGE_TEXT = 'Cannot change members: team is locked after registration deadline.'; " +
            "    END IF; " +
            "END"
        );
        
        // trg_lock_member_update
        jdbcTemplate.execute(
            "CREATE TRIGGER trg_lock_member_update " +
            "BEFORE UPDATE ON team_members " +
            "FOR EACH ROW " +
            "BEGIN " +
            "    DECLARE v_locked BOOLEAN; " +
            "    SELECT is_locked INTO v_locked FROM teams WHERE id = NEW.team_id; " +
            "    IF v_locked = TRUE THEN " +
            "        INSERT INTO audit_logs (action, target_table, target_id, detail) " +
            "        VALUES ('MEMBER_CHANGE_DENIED', 'team_members', NEW.team_id, " +
            "                JSON_OBJECT('user_id', NEW.user_id, 'operation', 'UPDATE')); " +
            "        SIGNAL SQLSTATE '45000' " +
            "        SET MESSAGE_TEXT = 'Cannot change members: team is locked after registration deadline.'; " +
            "    END IF; " +
            "END"
        );
        
        // trg_audit_team_status
        jdbcTemplate.execute(
            "CREATE TRIGGER trg_audit_team_status " +
            "AFTER UPDATE ON teams " +
            "FOR EACH ROW " +
            "BEGIN " +
            "    IF OLD.status <> NEW.status THEN " +
            "        INSERT INTO audit_logs (action, target_table, target_id, detail) " +
            "        VALUES ( " +
            "            'TEAM_STATUS_CHANGE', 'teams', NEW.id, " +
            "            JSON_OBJECT( " +
            "                'from',   OLD.status, " +
            "                'to',     NEW.status, " +
            "                'reason', NEW.elimination_reason " +
            "            ) " +
            "        ); " +
            "    END IF; " +
            "    IF OLD.is_locked <> NEW.is_locked AND NEW.is_locked = TRUE THEN " +
            "        INSERT INTO audit_logs (action, target_table, target_id, detail) " +
            "        VALUES ( " +
            "            'TEAM_LOCKED', 'teams', NEW.id, " +
            "            JSON_OBJECT('locked_at', NEW.locked_at) " +
            "        ); " +
            "    END IF; " +
            "END"
        );
        
        // trg_check_mentor_judge_conflict_insert
        jdbcTemplate.execute(
            "CREATE TRIGGER trg_check_mentor_judge_conflict_insert " +
            "BEFORE INSERT ON judge_assignments " +
            "FOR EACH ROW " +
            "BEGIN " +
            "    DECLARE v_user_type VARCHAR(20); " +
            "    DECLARE v_is_dept_head BOOLEAN; " +
            "    DECLARE v_round_final BOOLEAN; " +
            "    IF NEW.track_id IS NOT NULL THEN " +
            "        IF EXISTS ( " +
            "            SELECT 1 FROM mentor_assignments " +
            "            WHERE mentor_id = NEW.judge_id AND track_id = NEW.track_id " +
            "        ) THEN " +
            "            SIGNAL SQLSTATE '45000' " +
            "            SET MESSAGE_TEXT = 'CONFLICT_SAME_TRACK: User is already a mentor of this track.'; " +
            "        END IF; " +
            "        IF NEW.assignment_type = 'FINAL_EXTERNAL' THEN " +
            "            SIGNAL SQLSTATE '45000' " +
            "            SET MESSAGE_TEXT = 'INVALID_ASSIGNMENT_TYPE: FINAL_EXTERNAL can only be assigned to Final Round (track_id must be NULL).'; " +
            "        END IF; " +
            "    END IF; " +
            "    IF NEW.track_id IS NULL THEN " +
            "        IF NEW.round_id IS NULL THEN " +
            "            SIGNAL SQLSTATE '45000' " +
            "            SET MESSAGE_TEXT = 'MISSING_ROUND_ID: When track_id is NULL, round_id must be provided.'; " +
            "        END IF; " +
            "        SELECT is_final INTO v_round_final FROM rounds WHERE id = NEW.round_id; " +
            "        IF v_round_final IS NOT TRUE THEN " +
            "            SIGNAL SQLSTATE '45000' " +
            "            SET MESSAGE_TEXT = 'INVALID_FINAL_ROUND: round_id does not refer to a Final round. Judges with track_id=NULL can only judge Final round.'; " +
            "        END IF; " +
            "        IF NEW.assignment_type <> 'FINAL_EXTERNAL' THEN " +
            "            SIGNAL SQLSTATE '45000' " +
            "            SET MESSAGE_TEXT = 'INVALID_ASSIGNMENT_TYPE: Final round judge assignment must have type FINAL_EXTERNAL.'; " +
            "        END IF; " +
            "        SELECT user_type, is_dept_head INTO v_user_type, v_is_dept_head FROM users WHERE id = NEW.judge_id; " +
            "        IF v_user_type = 'INTERNAL' THEN " +
            "            IF v_is_dept_head = TRUE AND NOT EXISTS ( " +
            "                SELECT 1 FROM mentor_assignments WHERE mentor_id = NEW.judge_id " +
            "            ) THEN " +
            "                INSERT INTO audit_logs (action, target_table, target_id, detail) " +
            "                VALUES ( " +
            "                    'DEPT_HEAD_FINAL_JUDGE_EXCEPTION', " +
            "                    'judge_assignments', " +
            "                    NEW.judge_id, " +
            "                    JSON_OBJECT( " +
            "                        'round_id',    NEW.round_id, " +
            "                        'assigned_by', NEW.assigned_by, " +
            "                        'note',        'Department Head final round judge exception' " +
            "                    ) " +
            "                ); " +
            "            ELSE " +
            "                IF EXISTS ( " +
            "                    SELECT 1 FROM mentor_assignments WHERE mentor_id = NEW.judge_id " +
            "                ) THEN " +
            "                    SIGNAL SQLSTATE '45000' " +
            "                    SET MESSAGE_TEXT = 'INTERNAL_MENTOR_NOT_ALLOWED_IN_FINAL: Internal user has mentored teams in this hackathon and cannot judge the Final round.'; " +
            "                END IF; " +
            "                SIGNAL SQLSTATE '45000' " +
            "                SET MESSAGE_TEXT = 'INTERNAL_JUDGE_NOT_ALLOWED_IN_FINAL: User is internal. Final round requires 100% external judges (except non-mentoring department heads).'; " +
            "            END IF; " +
            "        END IF; " +
            "    END IF; " +
            "END"
        );
        
        // trg_check_mentor_judge_conflict_update
        jdbcTemplate.execute(
            "CREATE TRIGGER trg_check_mentor_judge_conflict_update " +
            "BEFORE UPDATE ON judge_assignments " +
            "FOR EACH ROW " +
            "BEGIN " +
            "    DECLARE v_user_type VARCHAR(20); " +
            "    DECLARE v_is_dept_head BOOLEAN; " +
            "    DECLARE v_round_final BOOLEAN; " +
            "    IF NEW.track_id IS NOT NULL THEN " +
            "        IF EXISTS ( " +
            "            SELECT 1 FROM mentor_assignments " +
            "            WHERE mentor_id = NEW.judge_id AND track_id = NEW.track_id " +
            "        ) THEN " +
            "            SIGNAL SQLSTATE '45000' " +
            "            SET MESSAGE_TEXT = 'CONFLICT_SAME_TRACK: User is already a mentor of this track.'; " +
            "        END IF; " +
            "        IF NEW.assignment_type = 'FINAL_EXTERNAL' THEN " +
            "            SIGNAL SQLSTATE '45000' " +
            "            SET MESSAGE_TEXT = 'INVALID_ASSIGNMENT_TYPE: FINAL_EXTERNAL can only be assigned to Final Round (track_id must be NULL).'; " +
            "        END IF; " +
            "    END IF; " +
            "    IF NEW.track_id IS NULL THEN " +
            "        IF NEW.round_id IS NULL THEN " +
            "            SIGNAL SQLSTATE '45000' " +
            "            SET MESSAGE_TEXT = 'MISSING_ROUND_ID: When track_id is NULL, round_id must be provided.'; " +
            "        END IF; " +
            "        SELECT is_final INTO v_round_final FROM rounds WHERE id = NEW.round_id; " +
            "        IF v_round_final IS NOT TRUE THEN " +
            "            SIGNAL SQLSTATE '45000' " +
            "            SET MESSAGE_TEXT = 'INVALID_FINAL_ROUND: round_id does not refer to a Final round. Judges with track_id=NULL can only judge Final round.'; " +
            "        END IF; " +
            "        IF NEW.assignment_type <> 'FINAL_EXTERNAL' THEN " +
            "            SIGNAL SQLSTATE '45000' " +
            "            SET MESSAGE_TEXT = 'INVALID_ASSIGNMENT_TYPE: Final round judge assignment must have type FINAL_EXTERNAL.'; " +
            "        END IF; " +
            "        SELECT user_type, is_dept_head INTO v_user_type, v_is_dept_head FROM users WHERE id = NEW.judge_id; " +
            "        IF v_user_type = 'INTERNAL' THEN " +
            "            IF v_is_dept_head = TRUE AND NOT EXISTS ( " +
            "                SELECT 1 FROM mentor_assignments WHERE mentor_id = NEW.judge_id " +
            "            ) THEN " +
            "                INSERT INTO audit_logs (action, target_table, target_id, detail) " +
            "                VALUES ( " +
            "                    'DEPT_HEAD_FINAL_JUDGE_EXCEPTION', " +
            "                    'judge_assignments', " +
            "                    NEW.judge_id, " +
            "                    JSON_OBJECT( " +
            "                        'round_id',    NEW.round_id, " +
            "                        'assigned_by', NEW.assigned_by, " +
            "                        'note',        'Department Head final round judge exception' " +
            "                    ) " +
            "                ); " +
            "            ELSE " +
            "                IF EXISTS ( " +
            "                    SELECT 1 FROM mentor_assignments WHERE mentor_id = NEW.judge_id " +
            "                ) THEN " +
            "                    SIGNAL SQLSTATE '45000' " +
            "                    SET MESSAGE_TEXT = 'INTERNAL_MENTOR_NOT_ALLOWED_IN_FINAL: Internal user has mentored teams in this hackathon and cannot judge the Final round.'; " +
            "                END IF; " +
            "                SIGNAL SQLSTATE '45000' " +
            "                SET MESSAGE_TEXT = 'INTERNAL_JUDGE_NOT_ALLOWED_IN_FINAL: User is internal. Final round requires 100% external judges (except non-mentoring department heads).'; " +
            "            END IF; " +
            "        END IF; " +
            "    END IF; " +
            "END"
        );
        
        // trg_check_submission_round_is_final_insert
        jdbcTemplate.execute(
            "CREATE TRIGGER trg_check_submission_round_is_final_insert " +
            "BEFORE INSERT ON submissions " +
            "FOR EACH ROW " +
            "BEGIN " +
            "    DECLARE v_is_final BOOLEAN; " +
            "    DECLARE v_late_policy VARCHAR(20); " +
            "    IF NEW.track_id IS NULL THEN " +
            "        IF NEW.round_id IS NULL THEN " +
            "            SIGNAL SQLSTATE '45000' " +
            "            SET MESSAGE_TEXT = 'MISSING_ROUND_ID: Final submission must provide round_id.'; " +
            "        END IF; " +
            "        SELECT is_final, late_submission_policy INTO v_is_final, v_late_policy FROM rounds WHERE id = NEW.round_id; " +
            "        IF v_is_final IS NOT TRUE THEN " +
            "            SIGNAL SQLSTATE '45000' " +
            "            SET MESSAGE_TEXT = 'INVALID_ROUND_FOR_SUBMISSION: Submission with track_id=NULL is only valid in FINAL rounds.'; " +
            "        END IF; " +
            "        IF NEW.status IN ('LATE', 'LATE_PENDING') THEN " +
            "            SIGNAL SQLSTATE '45000' " +
            "            SET MESSAGE_TEXT = 'LATE_SUBMISSION_NOT_ALLOWED_IN_FINAL: Final round has HARD_LOCK late submission policy.'; " +
            "        END IF; " +
            "    END IF; " +
            "END"
        );
        
        // trg_check_submission_round_is_final_update
        jdbcTemplate.execute(
            "CREATE TRIGGER trg_check_submission_round_is_final_update " +
            "BEFORE UPDATE ON submissions " +
            "FOR EACH ROW " +
            "BEGIN " +
            "    DECLARE v_is_final BOOLEAN; " +
            "    DECLARE v_late_policy VARCHAR(20); " +
            "    IF NEW.track_id IS NULL THEN " +
            "        IF NEW.round_id IS NULL THEN " +
            "            SIGNAL SQLSTATE '45000' " +
            "            SET MESSAGE_TEXT = 'MISSING_ROUND_ID: Final submission must provide round_id.'; " +
            "        END IF; " +
            "        SELECT is_final, late_submission_policy INTO v_is_final, v_late_policy FROM rounds WHERE id = NEW.round_id; " +
            "        IF v_is_final IS NOT TRUE THEN " +
            "            SIGNAL SQLSTATE '45000' " +
            "            SET MESSAGE_TEXT = 'INVALID_ROUND_FOR_SUBMISSION: Submission with track_id=NULL is only valid in FINAL rounds.'; " +
            "        END IF; " +
            "        IF NEW.status IN ('LATE', 'LATE_PENDING') THEN " +
            "            SIGNAL SQLSTATE '45000' " +
            "            SET MESSAGE_TEXT = 'LATE_SUBMISSION_NOT_ALLOWED_IN_FINAL: Final round has HARD_LOCK late submission policy.'; " +
            "        END IF; " +
            "    END IF; " +
            "END"
        );
        
        // trg_check_criteria_round_is_final_insert
        jdbcTemplate.execute(
            "CREATE TRIGGER trg_check_criteria_round_is_final_insert " +
            "BEFORE INSERT ON criteria " +
            "FOR EACH ROW " +
            "BEGIN " +
            "    DECLARE v_is_final BOOLEAN; " +
            "    IF NEW.track_id IS NULL THEN " +
            "        IF NEW.round_id IS NULL THEN " +
            "            SIGNAL SQLSTATE '45000' " +
            "            SET MESSAGE_TEXT = 'MISSING_ROUND_ID: Final criteria must provide round_id.'; " +
            "        END IF; " +
            "        SELECT is_final INTO v_is_final FROM rounds WHERE id = NEW.round_id; " +
            "        IF v_is_final IS NOT TRUE THEN " +
            "            SIGNAL SQLSTATE '45000' " +
            "            SET MESSAGE_TEXT = 'INVALID_ROUND_FOR_CRITERIA: Criteria with track_id=NULL is only valid in FINAL rounds.'; " +
            "        END IF; " +
            "    END IF; " +
            "END"
        );
        
        // trg_check_criteria_round_is_final_update
        jdbcTemplate.execute(
            "CREATE TRIGGER trg_check_criteria_round_is_final_update " +
            "BEFORE UPDATE ON criteria " +
            "FOR EACH ROW " +
            "BEGIN " +
            "    DECLARE v_is_final BOOLEAN; " +
            "    IF NEW.track_id IS NULL THEN " +
            "        IF NEW.round_id IS NULL THEN " +
            "            SIGNAL SQLSTATE '45000' " +
            "            SET MESSAGE_TEXT = 'MISSING_ROUND_ID: Final criteria must provide round_id.'; " +
            "        END IF; " +
            "        SELECT is_final INTO v_is_final FROM rounds WHERE id = NEW.round_id; " +
            "        IF v_is_final IS NOT TRUE THEN " +
            "            SIGNAL SQLSTATE '45000' " +
            "            SET MESSAGE_TEXT = 'INVALID_ROUND_FOR_CRITERIA: Criteria with track_id=NULL is only valid in FINAL rounds.'; " +
            "        END IF; " +
            "    END IF; " +
            "END"
        );
        
        // trg_check_team_track_same_hackathon_insert
        jdbcTemplate.execute(
            "CREATE TRIGGER trg_check_team_track_same_hackathon_insert " +
            "BEFORE INSERT ON team_round_tracks " +
            "FOR EACH ROW " +
            "BEGIN " +
            "    DECLARE v_team_hackathon_id INT; " +
            "    DECLARE v_track_hackathon_id INT; " +
            "    DECLARE v_track_round_final BOOLEAN; " +
            "    SELECT hackathon_id INTO v_team_hackathon_id FROM teams WHERE id = NEW.team_id; " +
            "    SELECT r.hackathon_id, r.is_final INTO v_track_hackathon_id, v_track_round_final " +
            "    FROM tracks tr " +
            "    JOIN rounds r ON r.id = tr.round_id " +
            "    WHERE tr.id = NEW.track_id; " +
            "    IF v_team_hackathon_id <> v_track_hackathon_id THEN " +
            "        SIGNAL SQLSTATE '45000' " +
            "        SET MESSAGE_TEXT = 'CROSS_HACKATHON_VIOLATION: Team and Track must belong to the same Hackathon.'; " +
            "    END IF; " +
            "    IF v_track_round_final = TRUE THEN " +
            "        SIGNAL SQLSTATE '45000' " +
            "        SET MESSAGE_TEXT = 'DESIGN_VIOLATION: team_round_tracks is only valid for preliminary/semifinal tracks.'; " +
            "    END IF; " +
            "END"
        );
        
        // trg_check_team_track_same_hackathon_update
        jdbcTemplate.execute(
            "CREATE TRIGGER trg_check_team_track_same_hackathon_update " +
            "BEFORE UPDATE ON team_round_tracks " +
            "FOR EACH ROW " +
            "BEGIN " +
            "    DECLARE v_team_hackathon_id INT; " +
            "    DECLARE v_track_hackathon_id INT; " +
            "    DECLARE v_track_round_final BOOLEAN; " +
            "    SELECT hackathon_id INTO v_team_hackathon_id FROM teams WHERE id = NEW.team_id; " +
            "    SELECT r.hackathon_id, r.is_final INTO v_track_hackathon_id, v_track_round_final " +
            "    FROM tracks tr " +
            "    JOIN rounds r ON r.id = tr.round_id " +
            "    WHERE tr.id = NEW.track_id; " +
            "    IF v_team_hackathon_id <> v_track_hackathon_id THEN " +
            "        SIGNAL SQLSTATE '45000' " +
            "        SET MESSAGE_TEXT = 'CROSS_HACKATHON_VIOLATION: Team and Track must belong to the same Hackathon.'; " +
            "    END IF; " +
            "    IF v_track_round_final = TRUE THEN " +
            "        SIGNAL SQLSTATE '45000' " +
            "        SET MESSAGE_TEXT = 'DESIGN_VIOLATION: team_round_tracks is only valid for preliminary/semifinal tracks.'; " +
            "    END IF; " +
            "END"
        );
        
        // trg_check_judge_mentor_conflict_insert
        jdbcTemplate.execute(
            "CREATE TRIGGER trg_check_judge_mentor_conflict_insert " +
            "BEFORE INSERT ON mentor_assignments " +
            "FOR EACH ROW " +
            "BEGIN " +
            "    IF EXISTS ( " +
            "        SELECT 1 FROM judge_assignments " +
            "        WHERE judge_id = NEW.mentor_id AND track_id = NEW.track_id " +
            "    ) THEN " +
            "        SIGNAL SQLSTATE '45000' " +
            "        SET MESSAGE_TEXT = 'CONFLICT_SAME_TRACK: User is already a judge of this track.'; " +
            "    END IF; " +
            "    IF EXISTS ( " +
            "        SELECT 1 FROM judge_assignments ja " +
            "        JOIN rounds r ON r.id = ja.round_id " +
            "        WHERE ja.judge_id = NEW.mentor_id " +
            "          AND ja.assignment_type = 'FINAL_EXTERNAL' " +
            "          AND ja.track_id IS NULL " +
            "          AND r.hackathon_id = ( " +
            "              SELECT r2.hackathon_id " +
            "              FROM tracks tr2 " +
            "              JOIN rounds r2 ON r2.id = tr2.round_id " +
            "              WHERE tr2.id = NEW.track_id " +
            "          ) " +
            "    ) THEN " +
            "        SIGNAL SQLSTATE '45000' " +
            "        SET MESSAGE_TEXT = 'FINAL_JUDGE_CANNOT_BE_MENTOR: Final round judges cannot mentor teams in the same hackathon.'; " +
            "    END IF; " +
            "END"
        );
        
        // trg_check_judge_mentor_conflict_update
        jdbcTemplate.execute(
            "CREATE TRIGGER trg_check_judge_mentor_conflict_update " +
            "BEFORE UPDATE ON mentor_assignments " +
            "FOR EACH ROW " +
            "BEGIN " +
            "    IF EXISTS ( " +
            "        SELECT 1 FROM judge_assignments " +
            "        WHERE judge_id = NEW.mentor_id AND track_id = NEW.track_id " +
            "    ) THEN " +
            "        SIGNAL SQLSTATE '45000' " +
            "        SET MESSAGE_TEXT = 'CONFLICT_SAME_TRACK: User is already a judge of this track.'; " +
            "    END IF; " +
            "    IF EXISTS ( " +
            "        SELECT 1 FROM judge_assignments ja " +
            "        JOIN rounds r ON r.id = ja.round_id " +
            "        WHERE ja.judge_id = NEW.mentor_id " +
            "          AND ja.assignment_type = 'FINAL_EXTERNAL' " +
            "          AND ja.track_id IS NULL " +
            "          AND r.hackathon_id = ( " +
            "              SELECT r2.hackathon_id " +
            "              FROM tracks tr2 " +
            "              JOIN rounds r2 ON r2.id = tr2.round_id " +
            "              WHERE tr2.id = NEW.track_id " +
            "          ) " +
            "    ) THEN " +
            "        SIGNAL SQLSTATE '45000' " +
            "        SET MESSAGE_TEXT = 'FINAL_JUDGE_CANNOT_BE_MENTOR: Final round judges cannot mentor teams in the same hackathon.'; " +
            "    END IF; " +
            "END"
        );
        
        // trg_prevent_track_in_final_round_insert
        jdbcTemplate.execute(
            "CREATE TRIGGER trg_prevent_track_in_final_round_insert " +
            "BEFORE INSERT ON tracks " +
            "FOR EACH ROW " +
            "BEGIN " +
            "    DECLARE v_is_final BOOLEAN; " +
            "    SELECT is_final INTO v_is_final FROM rounds WHERE id = NEW.round_id; " +
            "    IF v_is_final = TRUE THEN " +
            "        SIGNAL SQLSTATE '45000' " +
            "        SET MESSAGE_TEXT = 'DESIGN_VIOLATION: Final round cannot have child tracks.'; " +
            "    END IF; " +
            "END"
        );
        
        // trg_prevent_track_in_final_round_update
        jdbcTemplate.execute(
            "CREATE TRIGGER trg_prevent_track_in_final_round_update " +
            "BEFORE UPDATE ON tracks " +
            "FOR EACH ROW " +
            "BEGIN " +
            "    DECLARE v_is_final BOOLEAN; " +
            "    SELECT is_final INTO v_is_final FROM rounds WHERE id = NEW.round_id; " +
            "    IF v_is_final = TRUE THEN " +
            "        SIGNAL SQLSTATE '45000' " +
            "        SET MESSAGE_TEXT = 'DESIGN_VIOLATION: Final round cannot have child tracks.'; " +
            "    END IF; " +
            "END"
        );
        
        try {
            jdbcTemplate.update(
                "UPDATE users SET password_hash = ? WHERE password_hash LIKE '%f01c77840134cd988f01c23a' " +
                "OR password_hash = '$2a$10$8.UnVuG9HHgffUDalk8Ur.d268297a9b9a695d51dc611b8b8098c1'",
                "$2a$10$X5wFBtLrL/kHcmrOGGTrGufsBX8CJ0WpQpF3pgeuxBB/H73BK1DW6"
            );
            System.out.println("Corrupted mock password hashes in database corrected successfully!");
        } catch (Exception e) {
            System.err.println("Failed to auto-correct corrupted password hashes: " + e.getMessage());
        }
        
        System.out.println("MySQL database triggers initialized successfully!");
    }
}
