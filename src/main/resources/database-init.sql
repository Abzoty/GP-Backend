/*
 * ============================================================
 * GP_db — MS SQL Server Schema
 * Version 2.0 — UUID Primary Keys
 * ============================================================
 *
 * MIGRATION NOTE:
 *   The users.id column has changed from BIGINT IDENTITY to UNIQUEIDENTIFIER.
 *   All foreign keys that reference users.id are also UNIQUEIDENTIFIER.
 *   This change is NOT automatically applied by Hibernate's ddl-auto=update —
 *   you must DROP the existing database and re-run this script on a fresh GP_db.
 *
 * WHY UUID?
 *   - Prevents enumeration attacks (sequential IDs leak record counts in URLs).
 *   - Simplifies future multi-node or distributed deployments.
 *   - Industry standard for user-facing primary keys in modern APIs.
 * ============================================================
 */

-- 1. Create Database
CREATE DATABASE GP_db;
GO

USE GP_db;
GO

-- ============================================================
-- 2. Tables (ordered by FK dependency)
-- ============================================================

-- ── USERS ────────────────────────────────────────────────────
-- Core user accounts; implements Spring Security's UserDetails.
CREATE TABLE users (
    id               UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID() CONSTRAINT PK_Users PRIMARY KEY,
    email            VARCHAR(255)     NOT NULL UNIQUE,
    password_hash    VARCHAR(255)     NOT NULL,
    full_name        VARCHAR(150)     NOT NULL,
    student_id       VARCHAR(50)      UNIQUE,
    academic_year    TINYINT,                          -- 1–5
    current_semester TINYINT,                          -- 1–10
    gpa              DECIMAL(4,2),
    department       VARCHAR(100),
    image_url        VARCHAR(512),
    bio              VARCHAR(500),
    is_active        BIT              DEFAULT 1,
    created_at       DATETIME2        DEFAULT GETUTCDATE(),
    updated_at       DATETIME2
);

-- ── REFRESH_TOKENS ───────────────────────────────────────────
-- Persistent refresh tokens for JWT rotation; supports multi-device login.
-- See RefreshTokenService for the token-family / reuse-detection logic.
CREATE TABLE refresh_tokens (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    token       VARCHAR(255)     NOT NULL UNIQUE,
    user_id     UNIQUEIDENTIFIER NOT NULL,              -- FK → users.id (UUID)
    family_id   VARCHAR(255)     NOT NULL,              -- groups tokens from the same login session
    revoked     BIT              NOT NULL DEFAULT 0,
    expiry_date DATETIMEOFFSET   NOT NULL,
    CONSTRAINT FK_RefreshToken_User FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ── GAMIFICATION_PROFILES ────────────────────────────────────
-- One-to-one XP/level tracking per user.
CREATE TABLE gamification_profiles (
    id                    BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id               UNIQUEIDENTIFIER NOT NULL UNIQUE, -- FK → users.id
    xp_points             INT              DEFAULT 0,
    level                 SMALLINT         DEFAULT 1,
    total_posts           INT              DEFAULT 0,
    total_answers         INT              DEFAULT 0,
    total_upvotes_received INT             DEFAULT 0,
    total_materials_shared INT             DEFAULT 0,
    current_streak_days   SMALLINT         DEFAULT 0,
    longest_streak_days   SMALLINT         DEFAULT 0,
    last_activity_date    DATE,
    updated_at            DATETIME2,
    CONSTRAINT FK_Gamification_User FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ── XP_TRANSACTIONS ──────────────────────────────────────────
-- Immutable audit log of every XP event (POST_CREATED, ANSWER_UPVOTED, etc.).
CREATE TABLE xp_transactions (
    id             BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id        UNIQUEIDENTIFIER NOT NULL,
    event_type     VARCHAR(50)      NOT NULL,
    xp_delta       INT              NOT NULL,
    reference_id   BIGINT,
    reference_type VARCHAR(50),                         -- POST / ANSWER / MATERIAL / LOGIN
    created_at     DATETIME2        DEFAULT GETUTCDATE(),
    CONSTRAINT FK_XP_User FOREIGN KEY (user_id) REFERENCES users(id)
);

-- ── COURSES_REGISTERED ───────────────────────────────────────
-- Courses a student has registered for (used by the recommendation engine).
CREATE TABLE courses_registered (
    id            BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id       UNIQUEIDENTIFIER NOT NULL,
    course_code   VARCHAR(30)      NOT NULL,
    course_name   VARCHAR(200)     NOT NULL,
    semester      TINYINT,
    academic_year TINYINT,
    grade         VARCHAR(5),
    is_current    BIT              DEFAULT 1,
    CONSTRAINT FK_Course_User FOREIGN KEY (user_id) REFERENCES users(id)
);

-- ── SPACES ───────────────────────────────────────────────────
-- Community spaces (e.g. a space per course or topic).
CREATE TABLE spaces (
    id           BIGINT IDENTITY(1,1) PRIMARY KEY,
    name         VARCHAR(100)     NOT NULL UNIQUE,
    slug         VARCHAR(120)     NOT NULL UNIQUE,      -- URL-friendly identifier
    description  VARCHAR(1000),
    category     VARCHAR(80),
    course_code  VARCHAR(30),
    created_by   UNIQUEIDENTIFIER,                      -- FK → users.id (nullable = system spaces)
    is_active    BIT              DEFAULT 1,
    member_count INT              DEFAULT 0,
    created_at   DATETIME2        DEFAULT GETUTCDATE(),
    CONSTRAINT FK_Space_Creator FOREIGN KEY (created_by) REFERENCES users(id)
);

-- ── SPACE_MEMBERSHIPS ────────────────────────────────────────
-- Join table: which users belong to which spaces and in what role.
CREATE TABLE space_memberships (
    id        BIGINT IDENTITY(1,1) PRIMARY KEY,
    space_id  BIGINT           NOT NULL,
    user_id   UNIQUEIDENTIFIER NOT NULL,
    role      VARCHAR(20)      DEFAULT 'MEMBER',        -- MEMBER / MODERATOR / OWNER
    joined_at DATETIME2        DEFAULT GETUTCDATE(),
    CONSTRAINT FK_Membership_Space FOREIGN KEY (space_id) REFERENCES spaces(id),
    CONSTRAINT FK_Membership_User  FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT UQ_Space_User       UNIQUE (space_id, user_id)
);

-- ── POSTS ────────────────────────────────────────────────────
-- Questions and discussions within a space.
CREATE TABLE posts (
    id                 BIGINT IDENTITY(1,1) PRIMARY KEY,
    space_id           BIGINT           NOT NULL,
    author_id          UNIQUEIDENTIFIER NOT NULL,
    title              VARCHAR(300)     NOT NULL,
    body               NVARCHAR(MAX)    NOT NULL,
    post_type          VARCHAR(20)      DEFAULT 'QUESTION', -- QUESTION / DISCUSSION
    is_solved          BIT              DEFAULT 0,
    accepted_answer_id BIGINT           NULL,           -- set after ANSWERS table is created (circular FK)
    view_count         INT              DEFAULT 0,
    good_question_count INT             DEFAULT 0,
    tags               VARCHAR(500),
    created_at         DATETIME2        DEFAULT GETUTCDATE(),
    updated_at         DATETIME2,
    CONSTRAINT FK_Post_Space  FOREIGN KEY (space_id)  REFERENCES spaces(id),
    CONSTRAINT FK_Post_Author FOREIGN KEY (author_id) REFERENCES users(id)
);

-- ── ANSWERS ──────────────────────────────────────────────────
-- Answers to posts; one can be marked as accepted (closes the question).
CREATE TABLE answers (
    id           BIGINT IDENTITY(1,1) PRIMARY KEY,
    post_id      BIGINT           NOT NULL,
    author_id    UNIQUEIDENTIFIER NOT NULL,
    body         NVARCHAR(MAX)    NOT NULL,
    upvote_count INT              DEFAULT 0,
    is_accepted  BIT              DEFAULT 0,
    created_at   DATETIME2        DEFAULT GETUTCDATE(),
    updated_at   DATETIME2,
    CONSTRAINT FK_Answer_Post   FOREIGN KEY (post_id)   REFERENCES posts(id),
    CONSTRAINT FK_Answer_Author FOREIGN KEY (author_id) REFERENCES users(id)
);

-- Resolve circular FK: posts.accepted_answer_id → answers.id
ALTER TABLE posts
    ADD CONSTRAINT FK_Post_AcceptedAnswer
    FOREIGN KEY (accepted_answer_id) REFERENCES answers(id);

-- ── VOTES ────────────────────────────────────────────────────
-- Tracks upvotes on answers and "good question" marks on posts.
CREATE TABLE votes (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id     UNIQUEIDENTIFIER NOT NULL,
    target_type VARCHAR(20)      NOT NULL,              -- ANSWER / QUESTION
    target_id   BIGINT           NOT NULL,
    vote_type   VARCHAR(20)      NOT NULL,              -- UPVOTE / GOOD_QUESTION
    created_at  DATETIME2        DEFAULT GETUTCDATE(),
    CONSTRAINT FK_Vote_User FOREIGN KEY (user_id) REFERENCES users(id)
);

-- ── MATERIALS ────────────────────────────────────────────────
-- Files / links shared within a space (PDFs, videos, external links, etc.).
CREATE TABLE materials (
    id            BIGINT IDENTITY(1,1) PRIMARY KEY,
    space_id      BIGINT           NOT NULL,
    uploaded_by   UNIQUEIDENTIFIER NOT NULL,
    title         VARCHAR(255)     NOT NULL,
    description   VARCHAR(1000),
    resource_type VARCHAR(30),                          -- PDF / LINK / IMAGE / VIDEO
    url           VARCHAR(1024),
    file_size_kb  INT,
    link_count    INT              DEFAULT 0,           -- number of users who saved this material
    created_at    DATETIME2        DEFAULT GETUTCDATE(),
    CONSTRAINT FK_Material_Space    FOREIGN KEY (space_id)    REFERENCES spaces(id),
    CONSTRAINT FK_Material_Uploader FOREIGN KEY (uploaded_by) REFERENCES users(id)
);

-- ── MATERIAL_LINKS ───────────────────────────────────────────
-- Tracks which users have saved/bookmarked a material.
CREATE TABLE material_links (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    material_id BIGINT           NOT NULL,
    user_id     UNIQUEIDENTIFIER NOT NULL,
    linked_at   DATETIME2        DEFAULT GETUTCDATE(),
    CONSTRAINT FK_Link_Material FOREIGN KEY (material_id) REFERENCES materials(id),
    CONSTRAINT FK_Link_User     FOREIGN KEY (user_id)     REFERENCES users(id)
);

-- ── NOTIFICATIONS ────────────────────────────────────────────
-- In-app notifications; sender_id is NULL for system-generated notifications.
CREATE TABLE notifications (
    id                BIGINT IDENTITY(1,1) PRIMARY KEY,
    recipient_id      UNIQUEIDENTIFIER NOT NULL,
    sender_id         UNIQUEIDENTIFIER NULL,            -- NULL = system notification
    notification_type VARCHAR(50)      NOT NULL,
    title             VARCHAR(255)     NOT NULL,
    message           VARCHAR(1000),
    reference_type    VARCHAR(30),                     -- POST / ANSWER / MATERIAL / SPACE
    reference_id      BIGINT,
    is_read           BIT              DEFAULT 0,
    created_at        DATETIME2        DEFAULT GETUTCDATE(),
    CONSTRAINT FK_Notif_Recipient FOREIGN KEY (recipient_id) REFERENCES users(id),
    CONSTRAINT FK_Notif_Sender    FOREIGN KEY (sender_id)    REFERENCES users(id)
);