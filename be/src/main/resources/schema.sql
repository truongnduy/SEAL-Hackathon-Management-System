-- ============================================================
-- SEAL HACKATHON MANAGEMENT SYSTEM — COMPLETE DATABASE SCHEMA (MySQL 8.0+)
-- Phiên bản: 3.0 — Đồng bộ Operational Workflow v5.0
-- Kiến trúc: Hackathon → Round → Track
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;

-- Drop views if exist
DROP VIEW IF EXISTS v_rbl_anonymized;
DROP VIEW IF EXISTS v_team_track_assignment;
DROP VIEW IF EXISTS v_active_team_members;
DROP VIEW IF EXISTS v_scoring_progress;
DROP VIEW IF EXISTS v_judge_score_variance;
DROP VIEW IF EXISTS v_round_leaderboard;

-- Drop tables if exist
DROP TABLE IF EXISTS export_jobs;
DROP TABLE IF EXISTS audit_logs;
DROP TABLE IF EXISTS notification_templates;
DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS individual_rankings;
DROP TABLE IF EXISTS chapter_rankings;
DROP TABLE IF EXISTS prizes;
DROP TABLE IF EXISTS wildcard_reviews;
DROP TABLE IF EXISTS tiebreak_evaluations;
DROP TABLE IF EXISTS scores;
DROP TABLE IF EXISTS calibration_sessions;
DROP TABLE IF EXISTS submissions;
DROP TABLE IF EXISTS events;
DROP TABLE IF EXISTS mentor_assignments;
DROP TABLE IF EXISTS judge_assignments;
DROP TABLE IF EXISTS team_round_tracks;
DROP TABLE IF EXISTS team_members;
DROP TABLE IF EXISTS teams;
DROP TABLE IF EXISTS criteria;
DROP TABLE IF EXISTS tracks;
DROP TABLE IF EXISTS rounds;
DROP TABLE IF EXISTS hackathons;
DROP TABLE IF EXISTS invitations;
DROP TABLE IF EXISTS user_sessions;
DROP TABLE IF EXISTS oauth_accounts;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS chapters;

-- ============================================================
-- NHÓM 1: NGƯỜI DÙNG & PHÂN QUYỀN
-- ============================================================

CREATE TABLE chapters (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    code            VARCHAR(20)  NOT NULL UNIQUE,       -- VD: "FPT-HCM", "HUST"
    university      VARCHAR(300),
    city            VARCHAR(100),
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
                        CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE users (
    id                      INT AUTO_INCREMENT PRIMARY KEY,
    full_name               VARCHAR(200) NOT NULL,
    email                   VARCHAR(320) NOT NULL UNIQUE,
    password_hash           VARCHAR(255),                       -- NULL nếu đăng nhập qua OAuth
    role                    VARCHAR(20)  NOT NULL
                                CHECK (role IN ('COORDINATOR', 'JUDGE', 'MENTOR', 'STUDENT')),
    user_type               VARCHAR(20)  NOT NULL
                                CHECK (user_type IN ('INTERNAL', 'EXTERNAL')),
    student_code            VARCHAR(50),                        -- Mã SV FPT hoặc trường ngoài
    is_temp_account         BOOLEAN      NOT NULL DEFAULT FALSE, -- TRUE = Judge EXTERNAL tài khoản tạm
    is_dept_head            BOOLEAN      NOT NULL DEFAULT FALSE, 
    status                  VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
                                CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    rejection_reason        TEXT,
    chapter_id              INT REFERENCES chapters(id) ON DELETE SET NULL,
    phone                   VARCHAR(30),
    avatar_url              TEXT,
    institution             VARCHAR(300),                       -- Trường/công ty với EXTERNAL
    email_verified_at       DATETIME,
    last_login_at           DATETIME,
    student_card_data       LONGBLOB,
    student_card_content_type VARCHAR(100),
    created_at              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE oauth_accounts (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    user_id         INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider        VARCHAR(30)  NOT NULL,                      -- 'google' | 'github'
    provider_uid    VARCHAR(255) NOT NULL,
    access_token    TEXT,
    refresh_token   TEXT,
    expires_at      DATETIME,
    UNIQUE (provider, provider_uid)
);

CREATE TABLE user_sessions (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    user_id         INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash      VARCHAR(255) NOT NULL UNIQUE,
    ip_address      VARCHAR(45),
    user_agent      TEXT,
    expires_at      DATETIME NOT NULL,
    revoked_at      DATETIME,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE invitations (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    email           VARCHAR(320) NOT NULL,
    role            VARCHAR(20)  NOT NULL,
    hackathon_id    INT,                                        -- NULL = mời toàn hệ thống
    invited_by      INT REFERENCES users(id),
    token           VARCHAR(128) NOT NULL UNIQUE,
    expires_at      DATETIME    NOT NULL,
    accepted_at     DATETIME,
    created_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- NHÓM 2: CẤU TRÚC CUỘC THI — HACKATHON → ROUND → TRACK
-- ============================================================

CREATE TABLE hackathons (
    id                          INT AUTO_INCREMENT PRIMARY KEY,
    name                        VARCHAR(300) NOT NULL,
    slug                        VARCHAR(150) NOT NULL UNIQUE,   -- VD: "seal-spring-2026"
    season                      VARCHAR(20)  NOT NULL
                                    CHECK (season IN ('Spring', 'Summer', 'Fall', 'Winter')),
    year                        INT          NOT NULL,
    status                      VARCHAR(20)  NOT NULL DEFAULT 'DRAFT'
                                    CHECK (status IN ('DRAFT', 'ONGOING', 'PENDING_CONFIRM', 'FINISHED')),
    description                 TEXT,
    rules                       TEXT,
    banner_url                  TEXT,
    registration_start          DATE,
    registration_end            DATE,
    event_start                 DATE,
    event_end                   DATE,
    wildcard_enabled            BOOLEAN      NOT NULL DEFAULT FALSE,
    individual_ranking_enabled  BOOLEAN      NOT NULL DEFAULT FALSE,
    chapter_scoring_formula     TEXT,                           -- JSON công thức tính điểm Chapter
    created_by                  INT REFERENCES users(id),
    created_at                  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE (name, season, year)
);

CREATE TABLE rounds (
    id                      INT AUTO_INCREMENT PRIMARY KEY,
    hackathon_id            INT          NOT NULL REFERENCES hackathons(id) ON DELETE CASCADE,
    name                    VARCHAR(100) NOT NULL,              -- VD: "Vòng Sơ loại", "Vòng Chung kết"
    sequence_order          INT          NOT NULL,              -- 1=Sơ loại, 2=Chung kết
    is_final                BOOLEAN      NOT NULL DEFAULT FALSE,
    round_type              VARCHAR(20)  NOT NULL DEFAULT 'PRELIMINARY'
                                CHECK (round_type IN ('PRELIMINARY', 'SEMIFINAL', 'FINAL')),
    coding_duration_hours   INT,                                -- Thực tế 2 mùa = 7 giờ
    submission_open         DATETIME,
    submission_deadline     DATETIME    NOT NULL,
    late_submission_policy  VARCHAR(20)  NOT NULL DEFAULT 'ALLOW_LATE_PENDING'
                                CHECK (late_submission_policy IN ('ALLOW_LATE_PENDING', 'HARD_LOCK')),
    problem_statement_url   TEXT,
    problem_released_at     DATETIME,
    top_n_advance           INT,
    min_teams_final         INT,
    wildcard_enabled        BOOLEAN      NOT NULL DEFAULT FALSE,
    tiebreak_rule           VARCHAR(50)  DEFAULT 'PENALTY_SCORE'
                                CHECK (tiebreak_rule IN (
                                    'PENALTY_SCORE',
                                    'SUBMISSION_TIME',
                                    'COORDINATOR_DECISION'
                                )),
    is_active               BOOLEAN      NOT NULL DEFAULT FALSE,
    scoring_locked          BOOLEAN      NOT NULL DEFAULT FALSE,
    scoring_locked_at       DATETIME,
    scoring_locked_by       INT REFERENCES users(id),
    force_locked            BOOLEAN      NOT NULL DEFAULT FALSE,
    force_lock_reason       TEXT,
    created_at              DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (hackathon_id, sequence_order),
    CONSTRAINT chk_round_type_final_consistent
        CHECK (
            (is_final = TRUE  AND round_type = 'FINAL')
            OR
            (is_final = FALSE AND round_type IN ('PRELIMINARY', 'SEMIFINAL'))
        )
);

CREATE TABLE tracks (
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    round_id            INT          NOT NULL REFERENCES rounds(id) ON DELETE CASCADE,
    name                VARCHAR(200) NOT NULL,                  -- VD: "Bảng A", "Track AI RAG"
    description         TEXT,
    topic               VARCHAR(300),
    max_teams           INT,
    max_teams_per_group INT,
    min_team_size       INT          NOT NULL DEFAULT 3,
    max_team_size       INT          NOT NULL DEFAULT 5,
    status              VARCHAR(20)  NOT NULL DEFAULT 'OPEN'
                            CHECK (status IN ('OPEN', 'CLOSED', 'CANCELLED')),
    sequence_order      INT          NOT NULL DEFAULT 1,
    UNIQUE (round_id, sequence_order)
);

CREATE TABLE criteria (
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    track_id            INT REFERENCES tracks(id) ON DELETE CASCADE,
    round_id            INT REFERENCES rounds(id) ON DELETE CASCADE,
    source_criteria_id  INT REFERENCES criteria(id) ON DELETE SET NULL,
    name                VARCHAR(200) NOT NULL,
    type                VARCHAR(20)  NOT NULL
                            CHECK (type IN ('TECHNICAL', 'SOFT_SKILL', 'PENALTY')),
    weight              FLOAT        NOT NULL CHECK (weight > 0 AND weight <= 1),
    max_score           INT          NOT NULL DEFAULT 10,
    description         TEXT,
    rubric_url          TEXT,
    display_order       INT          NOT NULL DEFAULT 0,
    CONSTRAINT chk_criteria_xor_fk
        CHECK (
            (track_id IS NOT NULL AND round_id IS NULL)
            OR
            (track_id IS NULL AND round_id IS NOT NULL)
        )
);

-- ============================================================
-- NHÓM 3: ĐỘI THI & THÀNH VIÊN
-- ============================================================

CREATE TABLE teams (
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    hackathon_id        INT          NOT NULL REFERENCES hackathons(id) ON DELETE CASCADE,
    team_name           VARCHAR(200) NOT NULL UNIQUE,
    leader_id           INT          NOT NULL REFERENCES users(id),
    chapter_id          INT REFERENCES chapters(id) ON DELETE SET NULL,
    status              VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
                            CHECK (status IN ('PENDING', 'ACTIVE', 'ELIMINATED', 'REJECTED')),
    rejection_reason    TEXT,
    is_locked           BOOLEAN      NOT NULL DEFAULT FALSE,
    locked_at           DATETIME,
    eliminated_at       DATETIME,
    elimination_reason  TEXT,
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE team_members (
    team_id         INT     NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    user_id         INT     NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_in_team    VARCHAR(20) NOT NULL DEFAULT 'MEMBER'
                        CHECK (role_in_team IN ('LEADER', 'MEMBER')),
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                        CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'LEFT')),
    joined_at       DATETIME,
    left_at         DATETIME,
    PRIMARY KEY (team_id, user_id)
);

CREATE TABLE team_round_tracks (
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    team_id             INT          NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    track_id            INT          NOT NULL REFERENCES tracks(id) ON DELETE CASCADE,
    assigned_group      VARCHAR(50),
    registration_type   VARCHAR(20)  NOT NULL DEFAULT 'ASSIGNED'
                            CHECK (registration_type IN ('PREFERRED', 'ASSIGNED')),
    assigned_at         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_by         INT REFERENCES users(id),
    UNIQUE (team_id, track_id)
);

-- ============================================================
-- NHÓM 4: PHÂN CÔNG JUDGE & MENTOR
-- ============================================================

CREATE TABLE judge_assignments (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    judge_id        INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    track_id        INT REFERENCES tracks(id) ON DELETE CASCADE,
    round_id        INT REFERENCES rounds(id) ON DELETE CASCADE,
    assignment_type VARCHAR(20) NOT NULL DEFAULT 'NORMAL'
                        CHECK (assignment_type IN (
                            'NORMAL',
                            'HEAD',
                            'CALIBRATION',
                            'FINAL_EXTERNAL'
                        )),
    assigned_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_by     INT REFERENCES users(id),
    CONSTRAINT chk_judge_assignment_xor_fk
        CHECK (
            (track_id IS NOT NULL AND round_id IS NULL)
            OR
            (track_id IS NULL AND round_id IS NOT NULL)
        ),
    CONSTRAINT chk_final_external_requires_round
        CHECK (
            assignment_type != 'FINAL_EXTERNAL'
            OR (assignment_type = 'FINAL_EXTERNAL' && track_id IS NULL && round_id IS NOT NULL)
        )
);

CREATE TABLE mentor_assignments (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    mentor_id       INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    track_id        INT NOT NULL REFERENCES tracks(id) ON DELETE CASCADE,
    assigned_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_by     INT REFERENCES users(id),
    UNIQUE (mentor_id, track_id)
);

-- ============================================================
-- NHÓM 5: SỰ KIỆN & LỊCH TRÌNH
-- ============================================================

CREATE TABLE events (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    hackathon_id    INT NOT NULL REFERENCES hackathons(id) ON DELETE CASCADE,
    title           VARCHAR(300) NOT NULL,
    type            VARCHAR(30)  NOT NULL
                        CHECK (type IN ('KICKOFF', 'WORKSHOP', 'PRESENTATION', 'AWARDS', 'OTHER')),
    description     TEXT,
    location        VARCHAR(300),
    meet_url        TEXT,
    starts_at       DATETIME NOT NULL,
    ends_at         DATETIME,
    is_public       BOOLEAN NOT NULL DEFAULT TRUE,
    created_by      INT REFERENCES users(id)
);

-- ============================================================
-- NHÓM 6: NỘP BÀI & CHẤM ĐIỂM
-- ============================================================

CREATE TABLE submissions (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    team_id         INT NOT NULL REFERENCES teams(id),
    track_id        INT REFERENCES tracks(id),
    round_id        INT REFERENCES rounds(id),
    repo_url        TEXT,
    demo_url        TEXT,
    report_url      TEXT,
    slide_url       TEXT,
    status          VARCHAR(20)  NOT NULL DEFAULT 'SUBMITTED'
                        CHECK (status IN (
                            'SUBMITTED',
                            'LATE',
                            'LATE_PENDING',
                            'LATE_APPROVED',
                            'REJECTED',
                            'ACCEPTED'
                        )),
    is_late         BOOLEAN      NOT NULL DEFAULT FALSE,
    late_reason     TEXT,
    reviewed_by     INT REFERENCES users(id),
    reviewed_at     DATETIME,
    review_note     TEXT,
    submitted_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_submission_xor_fk
        CHECK (
            (track_id IS NOT NULL AND round_id IS NULL)
            OR
            (track_id IS NULL AND round_id IS NOT NULL)
        )
);

CREATE TABLE calibration_sessions (
    id                      INT AUTO_INCREMENT PRIMARY KEY,
    round_id                INT NOT NULL REFERENCES rounds(id) ON DELETE CASCADE,
    sample_submission_id    INT REFERENCES submissions(id),
    status                  VARCHAR(20) NOT NULL DEFAULT 'OPEN'
                                CHECK (status IN ('OPEN', 'CLOSED')),
    target_score            FLOAT,
    instructions            TEXT,
    started_at              DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at                DATETIME,
    created_by              INT REFERENCES users(id)
);

CREATE TABLE scores (
    id                      INT AUTO_INCREMENT PRIMARY KEY,
    submission_id           INT     NOT NULL REFERENCES submissions(id) ON DELETE CASCADE,
    judge_id                INT     NOT NULL REFERENCES users(id),
    criterion_id            INT     NOT NULL REFERENCES criteria(id),
    score_value             FLOAT   NOT NULL CHECK (score_value >= 0),
    comment                 TEXT,
    score_type              VARCHAR(20) NOT NULL DEFAULT 'NORMAL'
                                CHECK (score_type IN ('NORMAL', 'CALIBRATION', 'PENALTY')),
    is_final                BOOLEAN NOT NULL DEFAULT FALSE,
    calibration_session_id  INT REFERENCES calibration_sessions(id) ON DELETE SET NULL,
    scored_at               DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version                 INT     NOT NULL DEFAULT 0,
    UNIQUE (submission_id, judge_id, criterion_id, score_type)
);

CREATE TABLE tiebreak_evaluations (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    round_id        INT     NOT NULL REFERENCES rounds(id),
    team_id         INT     NOT NULL REFERENCES teams(id),
    judge_id        INT     NOT NULL REFERENCES users(id),
    penalty_score   FLOAT   NOT NULL DEFAULT 0,
    notes           TEXT,
    evaluated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (round_id, team_id, judge_id)
);

CREATE TABLE wildcard_reviews (
    id                      INT AUTO_INCREMENT PRIMARY KEY,
    round_id                INT     NOT NULL REFERENCES rounds(id),
    team_id                 INT     NOT NULL REFERENCES teams(id),
    track_id                INT REFERENCES tracks(id) ON DELETE SET NULL,
    avg_score               FLOAT,
    coordinator_approved    BOOLEAN,
    coordinator_note        TEXT,
    reviewed_by             INT REFERENCES users(id),
    reviewed_at             DATETIME,
    UNIQUE (round_id, team_id)
);

-- ============================================================
-- NHÓM 7: KẾT QUẢ — GIẢI THƯỞNG & XẾP HẠNG
-- ============================================================

CREATE TABLE prizes (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    track_id        INT REFERENCES tracks(id),
    round_id        INT NOT NULL REFERENCES rounds(id),
    team_id         INT NOT NULL REFERENCES teams(id),
    prize_name      VARCHAR(200) NOT NULL,
    prize_rank      VARCHAR(50)
                        CHECK (prize_rank IN ('FIRST', 'SECOND', 'THIRD', 'HONORABLE', 'SPECIAL', NULL)),
    prize_value     VARCHAR(300),
    description     TEXT,
    awarded_at      DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    awarded_by      INT REFERENCES users(id)
);

CREATE TABLE chapter_rankings (
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    hackathon_id        INT     NOT NULL REFERENCES hackathons(id),
    chapter_id          INT     NOT NULL REFERENCES chapters(id),
    best_team_score     FLOAT   NOT NULL DEFAULT 0,
    total_score         FLOAT   NOT NULL DEFAULT 0,
    `rank`                INT,
    teams_participated  INT     NOT NULL DEFAULT 0,
    prizes_won          INT     NOT NULL DEFAULT 0,
    formula_snapshot    TEXT,
    calculated_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (hackathon_id, chapter_id)
);

CREATE TABLE individual_rankings (
    id                      INT AUTO_INCREMENT PRIMARY KEY,
    hackathon_id            INT     NOT NULL REFERENCES hackathons(id),
    user_id                 INT     NOT NULL REFERENCES users(id),
    score_this_hackathon    FLOAT   NOT NULL DEFAULT 0,
    cumulative_score        FLOAT   NOT NULL DEFAULT 0,
    `rank`                    INT,
    is_enabled              BOOLEAN NOT NULL DEFAULT TRUE,
    calculated_at           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (hackathon_id, user_id)
);

-- ============================================================
-- NHÓM 8: THÔNG BÁO & TRUYỀN THÔNG
-- ============================================================

CREATE TABLE notifications (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    user_id         INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type            VARCHAR(50) NOT NULL,
    title           VARCHAR(300) NOT NULL,
    body            TEXT,
    reference_type  VARCHAR(100),
    reference_id    INT,
    is_read         BOOLEAN     NOT NULL DEFAULT FALSE,
    sent_at         DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at         DATETIME
);

CREATE TABLE notification_templates (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    code            VARCHAR(100) NOT NULL UNIQUE,
    title           VARCHAR(300) NOT NULL,
    body_template   TEXT NOT NULL,
    channel         VARCHAR(20)  NOT NULL DEFAULT 'IN_APP'
                        CHECK (channel IN ('IN_APP', 'EMAIL', 'ALL')),
    created_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- NHÓM 9: NHẬT KÝ HỆ THỐNG
-- ============================================================

CREATE TABLE audit_logs (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    user_id         INT REFERENCES users(id) ON DELETE SET NULL,
    action          VARCHAR(100) NOT NULL,
    target_table    VARCHAR(100),
    target_id       INT,
    detail          JSON,
    ip_address      VARCHAR(45),
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE export_jobs (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    hackathon_id    INT NOT NULL REFERENCES hackathons(id),
    type            VARCHAR(50) NOT NULL
                        CHECK (type IN ('CSV_SCORES', 'CSV_RANKINGS', 'ANONYMIZED_RBL', 'FULL_REPORT')),
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                        CHECK (status IN ('PENDING', 'PROCESSING', 'DONE', 'FAILED')),
    file_url        TEXT,
    error_message   TEXT,
    requested_by    INT REFERENCES users(id),
    started_at      DATETIME,
    finished_at     DATETIME,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- INDEXES
-- ============================================================

CREATE INDEX idx_users_email            ON users(email);
CREATE INDEX idx_users_role_status      ON users(role, status);
CREATE INDEX idx_users_chapter          ON users(chapter_id);
CREATE INDEX idx_users_student_code     ON users(student_code);

CREATE INDEX idx_hackathons_status      ON hackathons(status);
CREATE INDEX idx_hackathons_year        ON hackathons(year);

CREATE INDEX idx_rounds_hackathon       ON rounds(hackathon_id);
CREATE INDEX idx_rounds_active          ON rounds(is_active);
CREATE INDEX idx_rounds_final           ON rounds(is_final);
CREATE INDEX idx_rounds_type            ON rounds(round_type);

CREATE UNIQUE INDEX idx_rounds_one_final_per_hackathon
    ON rounds(hackathon_id, is_final); -- Spring Boot standard instead of partial index

CREATE INDEX idx_tracks_round           ON tracks(round_id);
CREATE INDEX idx_tracks_status          ON tracks(status);

CREATE INDEX idx_criteria_track         ON criteria(track_id);
CREATE INDEX idx_criteria_round_final   ON criteria(round_id);

CREATE INDEX idx_trt_team               ON team_round_tracks(team_id);
CREATE INDEX idx_trt_track              ON team_round_tracks(track_id);
CREATE INDEX idx_trt_assigned_group     ON team_round_tracks(track_id, assigned_group);

CREATE INDEX idx_teams_hackathon        ON teams(hackathon_id);
CREATE INDEX idx_teams_status           ON teams(status);
CREATE INDEX idx_teams_leader           ON teams(leader_id);
CREATE INDEX idx_teams_locked           ON teams(is_locked);
CREATE INDEX idx_team_members_user      ON team_members(user_id);

CREATE INDEX idx_submissions_team       ON submissions(team_id);
CREATE INDEX idx_submissions_track      ON submissions(track_id);
CREATE INDEX idx_submissions_round_final ON submissions(round_id);
CREATE INDEX idx_submissions_status     ON submissions(status);

CREATE INDEX idx_judge_assign_judge     ON judge_assignments(judge_id);
CREATE INDEX idx_judge_assign_track     ON judge_assignments(track_id);
CREATE INDEX idx_judge_assign_round     ON judge_assignments(round_id);
CREATE INDEX idx_judge_assign_type      ON judge_assignments(assignment_type);

CREATE UNIQUE INDEX idx_judge_assignment_track_unique
    ON judge_assignments(judge_id, track_id);

CREATE UNIQUE INDEX idx_judge_assignment_final_round_unique
    ON judge_assignments(judge_id, round_id);

CREATE INDEX idx_mentor_assign_mentor   ON mentor_assignments(mentor_id);
CREATE INDEX idx_mentor_assign_track    ON mentor_assignments(track_id);

CREATE INDEX idx_scores_submission      ON scores(submission_id);
CREATE INDEX idx_scores_judge           ON scores(judge_id);
CREATE INDEX idx_scores_criterion       ON scores(criterion_id);
CREATE INDEX idx_scores_type_final      ON scores(score_type, is_final);
CREATE INDEX idx_scores_final           ON scores(is_final);

CREATE INDEX idx_chapter_rank_hackathon     ON chapter_rankings(hackathon_id);
CREATE INDEX idx_individual_rank_user       ON individual_rankings(user_id);

CREATE INDEX idx_audit_user             ON audit_logs(user_id);
CREATE INDEX idx_audit_table_id         ON audit_logs(target_table, target_id);
CREATE INDEX idx_audit_created          ON audit_logs(created_at DESC);

CREATE INDEX idx_notif_user_unread      ON notifications(user_id, is_read);

-- ============================================================
-- VIEWS (MySQL syntax)
-- ============================================================

CREATE OR REPLACE VIEW v_round_leaderboard AS
SELECT
    COALESCE(tr.round_id, s.round_id)           AS round_id,
    s.track_id,
    s.team_id,
    t.team_name,
    trt.assigned_group,
    COUNT(DISTINCT sc.judge_id)                 AS judge_count,
    ROUND(
        SUM(sc.score_value * c.weight) / NULLIF(COUNT(DISTINCT sc.judge_id), 0),
    4)                                          AS weighted_avg_score,
    MAX(s.submitted_at)                         AS submitted_at
FROM submissions s
JOIN teams t                ON t.id   = s.team_id
JOIN scores sc              ON sc.submission_id = s.id
JOIN criteria c             ON c.id   = sc.criterion_id
LEFT JOIN tracks tr         ON tr.id  = s.track_id
LEFT JOIN team_round_tracks trt
                            ON trt.team_id = s.team_id
                           AND trt.track_id = s.track_id
WHERE sc.is_final   = TRUE
  AND sc.score_type = 'NORMAL'
GROUP BY
    round_id,
    s.track_id,
    s.team_id,
    t.team_name,
    trt.assigned_group;

CREATE OR REPLACE VIEW v_judge_score_variance AS
SELECT
    COALESCE(tr.round_id, s.round_id)           AS round_id,
    s.track_id,
    sc.criterion_id,
    c.name                                      AS criterion_name,
    c.type                                      AS criterion_type,
    u.user_type                                 AS judge_type,
    COUNT(DISTINCT sc.judge_id)                 AS judge_count,
    ROUND(AVG(sc.score_value),   3)             AS mean_score,
    ROUND(STDDEV(sc.score_value), 3)            AS std_dev,
    MIN(sc.score_value)                         AS min_score,
    MAX(sc.score_value)                         AS max_score
FROM scores sc
JOIN submissions s       ON s.id  = sc.submission_id
JOIN criteria c          ON c.id  = sc.criterion_id
JOIN users u             ON u.id  = sc.judge_id
LEFT JOIN tracks tr      ON tr.id = s.track_id
WHERE sc.score_type = 'NORMAL'
GROUP BY
    round_id,
    s.track_id,
    sc.criterion_id,
    c.name,
    c.type,
    u.user_type;

CREATE OR REPLACE VIEW v_scoring_progress AS
SELECT
    r.id                                        AS round_id,
    r.name                                      AS round_name,
    r.is_final,
    COUNT(DISTINCT ja.judge_id)                 AS total_judges,
    COUNT(DISTINCT sc.judge_id)                 AS judges_scored,
    COUNT(DISTINCT s.id)                        AS total_submissions,
    COUNT(DISTINCT CASE WHEN sc.judge_id IS NOT NULL THEN s.id END)
                                                AS scored_submissions,
    ROUND(
        100.0 * COUNT(DISTINCT sc.judge_id) / NULLIF(COUNT(DISTINCT ja.judge_id), 0),
    1)                                          AS completion_pct
FROM rounds r
LEFT JOIN tracks tr             ON tr.round_id = r.id
LEFT JOIN judge_assignments ja  ON (
    (r.is_final = FALSE AND ja.track_id = tr.id)
    OR
    (r.is_final = TRUE  AND ja.round_id = r.id)
)
LEFT JOIN submissions s         ON (
    (r.is_final = FALSE AND s.track_id = tr.id)
    OR
    (r.is_final = TRUE  AND s.round_id = r.id)
)
LEFT JOIN scores sc             ON sc.submission_id = s.id
                               AND sc.score_type = 'NORMAL'
GROUP BY r.id, r.name, r.is_final;

CREATE OR REPLACE VIEW v_active_team_members AS
SELECT
    tm.team_id,
    t.team_name,
    t.hackathon_id,
    t.is_locked,
    tm.user_id,
    u.full_name,
    u.email,
    u.user_type,
    u.student_code,
    tm.role_in_team
FROM team_members tm
JOIN teams t    ON t.id = tm.team_id
JOIN users u    ON u.id = tm.user_id
WHERE tm.status  = 'ACCEPTED'
  AND tm.left_at IS NULL;

CREATE OR REPLACE VIEW v_team_track_assignment AS
SELECT
    trt.team_id,
    t.team_name,
    t.hackathon_id,
    trt.track_id,
    tr.name                     AS track_name,
    tr.topic                    AS track_topic,
    tr.round_id,
    r.name                      AS round_name,
    r.round_type,
    r.sequence_order            AS round_sequence,
    trt.assigned_group,
    trt.registration_type,
    trt.assigned_at
FROM team_round_tracks trt
JOIN teams t    ON t.id    = trt.team_id
JOIN tracks tr  ON tr.id   = trt.track_id
JOIN rounds r   ON r.id    = tr.round_id;

CREATE OR REPLACE VIEW v_rbl_anonymized AS
SELECT
    COALESCE(tr.round_id, s.round_id)   AS round_id,
    s.track_id,
    sc.criterion_id,
    c.name                              AS criterion_name,
    c.type                              AS criterion_type,
    u.user_type                         AS judge_type,
    sc.score_value,
    sc.scored_at
FROM scores sc
JOIN submissions s   ON s.id  = sc.submission_id
JOIN criteria c      ON c.id  = sc.criterion_id
JOIN users u         ON u.id  = sc.judge_id
LEFT JOIN tracks tr  ON tr.id = s.track_id
WHERE sc.score_type = 'NORMAL'
  AND sc.is_final   = TRUE;

-- ============================================================
-- DỮ LIỆU MẪU — SEAL Spring 2026 (BCrypt password: 'password')
-- ============================================================

INSERT INTO chapters (name, code, university, city) VALUES
    ('FPT University Ho Chi Minh City', 'FPT-HCM', 'FPT University', 'Ho Chi Minh City'),
    ('FPT University Hanoi',            'FPT-HN',  'FPT University', 'Hanoi'),
    ('External Participants',           'EXT',      NULL,             NULL);

INSERT INTO users (
    full_name, email, password_hash, role, user_type,
    student_code, is_temp_account, is_dept_head, status, chapter_id
) VALUES
    ('Nguyễn Văn Coordinator', 'coord@fpt.edu.vn',     '$2a$10$X5wFBtLrL/kHcmrOGGTrGufsBX8CJ0WpQpF3pgeuxBB/H73BK1DW6', 'COORDINATOR', 'INTERNAL', NULL,        FALSE, FALSE, 'APPROVED', 1),
    ('Trần Thị Judge Internal','judge1@fpt.edu.vn',    '$2a$10$X5wFBtLrL/kHcmrOGGTrGufsBX8CJ0WpQpF3pgeuxBB/H73BK1DW6', 'JUDGE',       'INTERNAL', NULL,        FALSE, FALSE, 'APPROVED', 1),
    ('Lê Văn Judge External',  'guestjudge@gmail.com', '$2a$10$X5wFBtLrL/kHcmrOGGTrGufsBX8CJ0WpQpF3pgeuxBB/H73BK1DW6', 'JUDGE',       'EXTERNAL', NULL,        TRUE,  FALSE, 'APPROVED', 3),
    ('Phạm Minh Mentor',       'mentor@fpt.edu.vn',    '$2a$10$X5wFBtLrL/kHcmrOGGTrGufsBX8CJ0WpQpF3pgeuxBB/H73BK1DW6', 'MENTOR',      'INTERNAL', NULL,        FALSE, FALSE, 'APPROVED', 1),
    ('Team A Leader',          'teama@fpt.edu.vn',     '$2a$10$X5wFBtLrL/kHcmrOGGTrGufsBX8CJ0WpQpF3pgeuxBB/H73BK1DW6', 'STUDENT',     'INTERNAL', 'FPT0001',   FALSE, FALSE, 'APPROVED', 1),
    ('Team A Member 1',        'teama1@fpt.edu.vn',    '$2a$10$X5wFBtLrL/kHcmrOGGTrGufsBX8CJ0WpQpF3pgeuxBB/H73BK1DW6', 'STUDENT',     'INTERNAL', 'FPT0002',   FALSE, FALSE, 'APPROVED', 1),
    ('Team A Member 2',        'teama2@fpt.edu.vn',    '$2a$10$X5wFBtLrL/kHcmrOGGTrGufsBX8CJ0WpQpF3pgeuxBB/H73BK1DW6', 'STUDENT',     'INTERNAL', 'FPT0003',   FALSE, FALSE, 'APPROVED', 1),
    ('Team B Leader',          'teamb@gmail.com',      '$2a$10$X5wFBtLrL/kHcmrOGGTrGufsBX8CJ0WpQpF3pgeuxBB/H73BK1DW6', 'STUDENT',     'EXTERNAL', 'HUST-2001', FALSE, FALSE, 'APPROVED', 3),
    ('Team B Member 1',        'teamb1@gmail.com',     '$2a$10$X5wFBtLrL/kHcmrOGGTrGufsBX8CJ0WpQpF3pgeuxBB/H73BK1DW6', 'STUDENT',     'EXTERNAL', 'HUST-2002', FALSE, FALSE, 'APPROVED', 3),
    ('Team C Leader',          'teamc@hust.edu.vn',    '$2a$10$X5wFBtLrL/kHcmrOGGTrGufsBX8CJ0WpQpF3pgeuxBB/H73BK1DW6', 'STUDENT',     'INTERNAL', 'HUST0001',  FALSE, FALSE, 'APPROVED', 1),
    ('Team C Member 1',        'teamc1@hust.edu.vn',   '$2a$10$X5wFBtLrL/kHcmrOGGTrGufsBX8CJ0WpQpF3pgeuxBB/H73BK1DW6', 'STUDENT',     'INTERNAL', 'HUST0002',  FALSE, FALSE, 'APPROVED', 1);

INSERT INTO hackathons (
    name, slug, season, year, status, description,
    registration_start, registration_end, event_start, event_end,
    wildcard_enabled, individual_ranking_enabled, created_by
) VALUES (
    'SEAL Spring 2026', 'seal-spring-2026', 'Spring', 2026, 'ONGOING',
    'Cuộc thi lập trình SEAL — Kỳ Spring 2026',
    '2026-01-01', '2026-01-20', '2026-02-01', '2026-03-15',
    TRUE, FALSE, 1
);

INSERT INTO rounds (
    hackathon_id, name, sequence_order,
    is_final, round_type,
    submission_deadline, coding_duration_hours,
    late_submission_policy, top_n_advance,
    wildcard_enabled, min_teams_final,
    tiebreak_rule, is_active
) VALUES
    (1, 'Vòng Sơ loại', 1,
     FALSE, 'PRELIMINARY',
     '2026-02-15 23:59:00', 7,
     'ALLOW_LATE_PENDING', 2,
     TRUE, 6,
     'PENALTY_SCORE', TRUE),
    (1, 'Vòng Chung kết', 2,
     TRUE, 'FINAL',
     '2026-03-01 23:59:00', NULL,
     'HARD_LOCK', NULL,
     FALSE, NULL,
     'PENALTY_SCORE', FALSE);

INSERT INTO tracks (
    round_id, name, description, topic,
    max_teams, max_teams_per_group,
    min_team_size, max_team_size,
    sequence_order
) VALUES
    (1, 'Track 1 — RAG Pipeline',
     'Xây dựng hệ thống RAG cho nghiệp vụ doanh nghiệp',
     'Business Analysis App',
     8, 8, 3, 5, 1),
    (1, 'Track 2 — AI Agent',
     'Thiết kế AI Agent tự động hóa quy trình',
     'Process Automation Agent',
     8, 8, 3, 5, 2);

INSERT INTO criteria (track_id, round_id, name, type, weight, max_score, display_order) VALUES
    (1, NULL, 'Domain Accuracy',          'TECHNICAL',  0.30, 10, 1),
    (1, NULL, 'Kiến trúc RAG',            'TECHNICAL',  0.30, 10, 2),
    (1, NULL, 'Ý tưởng & Thuyết trình',   'SOFT_SKILL', 0.15, 10, 3),
    (1, NULL, 'Thực thi & Sáng tạo',      'TECHNICAL',  0.15, 10, 4),
    (1, NULL, 'UX & Giao diện',           'SOFT_SKILL', 0.10, 10, 5),
    (2, NULL, 'Domain Accuracy',          'TECHNICAL',  0.30, 10, 1),
    (2, NULL, 'Kiến trúc RAG',            'TECHNICAL',  0.30, 10, 2),
    (2, NULL, 'Ý tưởng & Thuyết trình',   'SOFT_SKILL', 0.15, 10, 3),
    (2, NULL, 'Thực thi & Sáng tạo',      'TECHNICAL',  0.15, 10, 4),
    (2, NULL, 'UX & Giao diện',           'SOFT_SKILL', 0.10, 10, 5),
    (NULL, 2, 'Xử lý & Truy xuất',        'TECHNICAL',  0.30, 10, 1),
    (NULL, 2, 'Độ tin cậy',               'TECHNICAL',  0.20, 10, 2),
    (NULL, 2, 'Tư duy Agent',             'TECHNICAL',  0.20, 10, 3),
    (NULL, 2, 'Thực tế & Triển khai',     'TECHNICAL',  0.20, 10, 4),
    (NULL, 2, 'Mở rộng & Scale',          'SOFT_SKILL', 0.10, 10, 5);

INSERT INTO teams (
    hackathon_id, team_name, leader_id, chapter_id, status
) VALUES
    (1, 'FPT AI Warriors', 5, 1, 'ACTIVE'),
    (1, 'External Builders', 8, 3, 'ACTIVE'),
    (1, 'HUST Innovators', 10, 1, 'ACTIVE');

INSERT INTO team_members (team_id, user_id, role_in_team, status, joined_at) VALUES
    (1, 5, 'LEADER', 'ACCEPTED', NOW()),
    (1, 6, 'MEMBER', 'ACCEPTED', NOW()),
    (1, 7, 'MEMBER', 'ACCEPTED', NOW()),
    (2, 8, 'LEADER', 'ACCEPTED', NOW()),
    (2, 9, 'MEMBER', 'ACCEPTED', NOW()),
    (3, 10, 'LEADER', 'ACCEPTED', NOW()),
    (3, 11, 'MEMBER', 'ACCEPTED', NOW());

INSERT INTO team_round_tracks (
    team_id, track_id, assigned_group, registration_type, assigned_by
) VALUES
    (1, 1, NULL, 'ASSIGNED', 1),
    (2, 2, NULL, 'ASSIGNED', 1),
    (3, 1, NULL, 'ASSIGNED', 1);

INSERT INTO judge_assignments (
    judge_id, track_id, round_id, assignment_type, assigned_by
) VALUES
    (2, 1, NULL, 'NORMAL', 1),
    (3, 1, NULL, 'NORMAL', 1),
    (2, 2, NULL, 'NORMAL', 1),
    (3, 2, NULL, 'NORMAL', 1);

INSERT INTO mentor_assignments (mentor_id, track_id, assigned_by) VALUES
    (4, 1, 1),
    (4, 2, 1);

INSERT INTO submissions (
    team_id, track_id, round_id,
    repo_url, demo_url, slide_url,
    status, is_late, late_reason, submitted_at
) VALUES
    (1, 1, NULL,
     'https://github.com/fpt-aiwarriors/seal2026',
     'https://fpt-aiwarriors.vercel.app',
     'https://slides.fpt-aiwarriors.com',
     'SUBMITTED', FALSE, NULL, '2026-02-10 18:30:00'),
    (2, 2, NULL,
     'https://github.com/ext-builders/seal-agent',
     'https://ext-builders.netlify.app',
     'https://slides.ext-builders.com',
     'SUBMITTED', FALSE, NULL, '2026-02-12 20:00:00'),
    (3, 1, NULL,
     'https://github.com/hust-innovators/seal-rag',
     'https://hust-innovators.vercel.app',
     'https://slides.hust-innovators.com',
     'LATE', TRUE, 'Mạng bị trục trặc kỹ thuật những phút cuối trước deadline.', '2026-02-16 00:15:00');

INSERT INTO scores (
    submission_id, judge_id, criterion_id,
    score_value, comment, score_type, is_final
) VALUES
    (1, 2, 1,  9.0, 'Excellent domain coverage',          'NORMAL', FALSE),
    (1, 2, 2,  8.5, 'Clean RAG pipeline design',          'NORMAL', FALSE),
    (1, 2, 3,  8.8, 'Confident delivery',                 'NORMAL', FALSE),
    (1, 2, 4,  9.2, 'Creative agent implementation',      'NORMAL', FALSE),
    (1, 2, 5,  8.0, 'Good UI/UX design',                  'NORMAL', FALSE),
    (1, 3, 1,  8.7, 'Good domain but minor gaps',         'NORMAL', FALSE),
    (1, 3, 2,  8.2, 'Solid architecture',                 'NORMAL', FALSE),
    (1, 3, 3,  8.5, 'Clear presentation',                 'NORMAL', FALSE),
    (1, 3, 4,  8.8, 'Innovative approach',                'NORMAL', FALSE),
    (1, 3, 5,  7.8, 'UX could be more intuitive',         'NORMAL', FALSE),
    (2, 2, 6,  7.5, 'Domain knowledge acceptable',        'NORMAL', FALSE),
    (2, 2, 7,  7.8, 'RAG pipeline functional',            'NORMAL', FALSE),
    (2, 2, 8,  7.0, 'Needs more practice',                'NORMAL', FALSE),
    (2, 2, 9,  7.5, 'Decent execution',                   'NORMAL', FALSE),
    (2, 2, 10, 7.2, 'Basic interface',                    'NORMAL', FALSE),
    (2, 3, 6,  7.8, 'Good domain understanding',          'NORMAL', FALSE),
    (2, 3, 7,  7.5, 'Architecture is workable',           'NORMAL', FALSE),
    (2, 3, 8,  7.3, 'Presentation was okay',              'NORMAL', FALSE),
    (2, 3, 9,  7.0, 'Standard implementation',            'NORMAL', FALSE),
    (2, 3, 10, 7.5, 'Clean but minimal UI',               'NORMAL', FALSE),
    (3, 2, 1,  7.5, 'Acceptable RAG accuracy',             'NORMAL', FALSE),
    (3, 2, 2,  7.0, 'Workable pipeline structure',         'NORMAL', FALSE),
    (3, 2, 3,  8.0, 'Engaging presentation skills',        'NORMAL', FALSE),
    (3, 2, 4,  7.5, 'Basic feature completeness',          'NORMAL', FALSE),
    (3, 2, 5,  7.2, 'Fairly clean UI, need minor polish',   'NORMAL', FALSE),
    (3, 3, 1,  7.8, 'Fair domain coverage',                'NORMAL', FALSE),
    (3, 3, 2,  7.2, 'Pipeline could be optimized',         'NORMAL', FALSE),
    (3, 3, 3,  8.2, 'Confident slides and pitch',          'NORMAL', FALSE),
    (3, 3, 4,  7.0, 'Expected features are complete',      'NORMAL', FALSE),
    (3, 3, 5,  7.4, 'Decent layout design',                'NORMAL', FALSE);

INSERT INTO events (hackathon_id, title, type, location, starts_at, ends_at) VALUES
    (1, 'Workshop: RAG & AI Agent Fundamentals',    'WORKSHOP',     'Online (Teams)', '2026-02-05 20:00:00', '2026-02-05 21:30:00'),
    (1, 'Lễ Khai mạc & Bốc thăm chia Track',        'KICKOFF',      'FPT HCM — Hội trường A', '2026-02-10 14:00:00', '2026-02-10 17:00:00'),
    (1, 'Ngày thi Sơ loại & Thuyết trình',           'PRESENTATION', 'FPT HCM — Hội trường B', '2026-02-16 06:00:00', '2026-02-16 19:00:00'),
    (1, 'Vòng Chung kết & Trao giải',                'AWARDS',       'FPT HCM — Hội trường A', '2026-03-10 08:00:00', '2026-03-10 18:00:00');

INSERT INTO calibration_sessions (
    round_id, sample_submission_id, status, target_score, instructions, started_at, ended_at, created_by
) VALUES
    (1, 1, 'CLOSED', 8.5, 'Align on RAG criteria and domain accuracy metrics.', '2026-02-11 09:00:00', '2026-02-11 11:00:00', 1),
    (1, 2, 'OPEN', NULL, 'Evaluate AI Agent autonomy and UI/UX design.', '2026-02-13 14:00:00', NULL, 1);

INSERT INTO scores (
    submission_id, judge_id, criterion_id, score_value, comment, score_type, is_final, calibration_session_id, scored_at
) VALUES
    (1, 2, 1, 8.5, 'Calibration rating for Domain Accuracy', 'CALIBRATION', FALSE, 1, '2026-02-11 10:00:00'),
    (1, 3, 1, 8.2, 'Calibration rating for Domain Accuracy', 'CALIBRATION', FALSE, 1, '2026-02-11 10:15:00');

INSERT INTO wildcard_reviews (
    round_id, team_id, track_id, avg_score, coordinator_approved, coordinator_note, reviewed_by, reviewed_at
) VALUES
    (1, 2, 2, 7.42, TRUE, 'Team showed high potential in Agent autonomy. Approved for wild card.', 1, '2026-02-20 16:00:00');

INSERT INTO prizes (
    track_id, round_id, team_id, prize_name, prize_rank, prize_value, description, awarded_by
) VALUES
    (1, 1, 1, 'Best Technical Implementation', 'FIRST', '10,000,000 VND', 'Awarded to the team with the most robust RAG pipeline.', 1);

INSERT INTO chapter_rankings (
    hackathon_id, chapter_id, best_team_score, total_score, `rank`, teams_participated, prizes_won, formula_snapshot
) VALUES
    (1, 1, 8.85, 8.85, 1, 1, 1, '{"weight_best_team": 0.6, "weight_avg_team": 0.4}'),
    (1, 3, 7.42, 7.42, 2, 1, 0, '{"weight_best_team": 0.6, "weight_avg_team": 0.4}');

INSERT INTO individual_rankings (
    hackathon_id, user_id, score_this_hackathon, cumulative_score, `rank`
) VALUES
    (1, 5, 8.85, 8.85, 1),
    (1, 6, 8.85, 8.85, 2),
    (1, 7, 8.85, 8.85, 3),
    (1, 8, 7.42, 7.42, 4),
    (1, 9, 7.42, 7.42, 5);

INSERT INTO notifications (
    user_id, type, title, body, reference_type, reference_id, is_read, sent_at
) VALUES
    (5, 'TEAM_INVITATION', 'New Team Invitation', 'You have been invited to join Team FPT AI Warriors.', 'TEAM', 1, FALSE, '2026-02-02 10:00:00'),
    (6, 'ROUND_START', 'Round 1 Started', 'Vòng Sơ loại of SEAL Spring 2026 has officially started.', 'ROUND', 1, TRUE, '2026-02-08 08:00:00'),
    (1, 'SUBMISSION_ALERT', 'Submission Received', 'Team FPT AI Warriors has submitted their project for Track 1.', 'SUBMISSION', 1, FALSE, '2026-02-10 18:30:10');

INSERT INTO notification_templates (
    code, title, body_template, channel
) VALUES
    ('TEAM_INVITE', 'Invitation to join {teamName}', 'Hi {userName}, you have been invited to join team {teamName} by {inviterName}. Please respond to it.', 'ALL'),
    ('SUBMISSION_CONFIRM', 'Submission Confirmation: {roundName}', 'Your submission for {roundName} has been received successfully on {submitTime}.', 'IN_APP'),
    ('CALIBRATION_ASSIGN', 'New Calibration Session Assigned', 'You have been assigned to calibration session for round {roundName}. Target score is {targetScore}.', 'EMAIL');

INSERT INTO tiebreak_evaluations (
    round_id, team_id, judge_id, penalty_score, notes
) VALUES
    (1, 2, 2, 0.5, 'Slight penalty due to minor submission guideline deviation.');

SET FOREIGN_KEY_CHECKS = 1;
