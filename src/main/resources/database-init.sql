/*
Covalent GP Backend — Database Schema
Engine  : MS SQL Server
Version : 2.0  (UUID primary keys)

NOTE: With spring.jpa.hibernate.ddl-auto=update, Hibernate manages the schema
automatically. This script is provided for:
    1. Reference / documentation
    2. Setting up a fresh database from scratch (run once)
    3. CI/CD environments where Hibernate auto-update is disabled

Run this script against an empty database:
    USE master; CREATE DATABASE GP_db; GO
    USE GP_db; -- then paste the rest of this file
*/

USE GP_db;
GO

-- ─────────────────────────────────────────────────────
-- USERS
-- ─────────────────────────────────────────────────────
CREATE TABLE users (
    id               UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID() PRIMARY KEY,
    email            VARCHAR(255)     NOT NULL UNIQUE,
    password_hash    VARCHAR(255)     NOT NULL,
    full_name        VARCHAR(150)     NOT NULL,
    student_id       VARCHAR(50)      UNIQUE,
    academic_year    INT,                            -- 1–5
    current_semester INT,                            -- 1–10
    gpa              DECIMAL(4,2),
    department       VARCHAR(100),
    image_url        VARCHAR(512),
    bio              VARCHAR(500),
    is_active        BIT              DEFAULT 1,
    created_at       DATETIME2        DEFAULT GETUTCDATE(),
    updated_at       DATETIME2
);
GO

-- ─────────────────────────────────────────────────────
-- REFRESH TOKENS  (internal — Long PK for efficiency)
-- ─────────────────────────────────────────────────────
CREATE TABLE refresh_tokens (
    id          BIGINT           IDENTITY(1,1) PRIMARY KEY,
    token       VARCHAR(255)     NOT NULL UNIQUE,
    user_id     UNIQUEIDENTIFIER NOT NULL,           -- FK to users.id
    family_id   VARCHAR(255)     NOT NULL,           -- groups tokens from one login session
    revoked     BIT              NOT NULL DEFAULT 0,
    expiry_date DATETIMEOFFSET   NOT NULL,
    CONSTRAINT FK_RefreshToken_User FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
GO

-- ─────────────────────────────────────────────────────
-- GAMIFICATION PROFILES
-- ─────────────────────────────────────────────────────
CREATE TABLE gamification_profiles (
    id                     UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID() PRIMARY KEY,
    user_id                UNIQUEIDENTIFIER NOT NULL UNIQUE,
    xp_points              INT              DEFAULT 0,
    level                  SMALLINT         DEFAULT 1,
    total_posts            INT              DEFAULT 0,
    total_answers          INT              DEFAULT 0,
    total_upvotes_received INT              DEFAULT 0,
    total_materials_shared INT              DEFAULT 0,
    current_streak_days    SMALLINT         DEFAULT 0,
    longest_streak_days    SMALLINT         DEFAULT 0,
    last_activity_date     DATE,
    updated_at             DATETIME2,
    CONSTRAINT FK_Gamification_User FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
GO

-- ─────────────────────────────────────────────────────
-- XP TRANSACTIONS  (append-only audit log)
-- ─────────────────────────────────────────────────────
CREATE TABLE xp_transactions (
    id             UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID() PRIMARY KEY,
    user_id        UNIQUEIDENTIFIER NOT NULL,
    event_type     VARCHAR(50)      NOT NULL,        -- POST_CREATED, ANSWER_UPVOTED, etc.
    xp_delta       INT              NOT NULL,        -- positive = earned, negative = deducted
    reference_id   UNIQUEIDENTIFIER,                 -- UUID of the triggering entity (nullable)
    reference_type VARCHAR(50),                      -- POST / ANSWER / MATERIAL / LOGIN
    created_at     DATETIME2        DEFAULT GETUTCDATE(),
    CONSTRAINT FK_XP_User FOREIGN KEY (user_id) REFERENCES users(id)
);
GO

-- ─────────────────────────────────────────────────────
-- COURSES REGISTERED
-- ─────────────────────────────────────────────────────
CREATE TABLE courses_registered (
    id            UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID() PRIMARY KEY,
    user_id       UNIQUEIDENTIFIER NOT NULL,
    course_code   VARCHAR(30)      NOT NULL,
    course_name   VARCHAR(200)     NOT NULL,
    semester      SMALLINT,
    academic_year SMALLINT,
    grade         VARCHAR(5),
    result        DECIMAL(3,1),
    is_current    BIT              DEFAULT 1,
    CONSTRAINT FK_Course_User FOREIGN KEY (user_id) REFERENCES users(id)
);
GO

-- ─────────────────────────────────────────────────────
-- SPACES
-- ─────────────────────────────────────────────────────
CREATE TABLE spaces (
    id           UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID() PRIMARY KEY,
    name         VARCHAR(100)     NOT NULL UNIQUE,
    slug         VARCHAR(120)     NOT NULL UNIQUE,
    description  VARCHAR(1000),
    category     VARCHAR(80),
    course_code  VARCHAR(30),
    created_by   UNIQUEIDENTIFIER,
    is_active    BIT              DEFAULT 1,
    member_count INT              DEFAULT 0,
    created_at   DATETIME2        DEFAULT GETUTCDATE(),
    CONSTRAINT FK_Space_Creator FOREIGN KEY (created_by) REFERENCES users(id)
);
GO

-- ─────────────────────────────────────────────────────
-- SPACE MEMBERSHIPS
-- ─────────────────────────────────────────────────────
CREATE TABLE space_memberships (
    id        UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID() PRIMARY KEY,
    space_id  UNIQUEIDENTIFIER NOT NULL,
    user_id   UNIQUEIDENTIFIER NOT NULL,
    role      VARCHAR(20)      DEFAULT 'MEMBER',    -- MEMBER / MODERATOR / OWNER
    joined_at DATETIME2        DEFAULT GETUTCDATE(),
    CONSTRAINT FK_Membership_Space FOREIGN KEY (space_id) REFERENCES spaces(id),
    CONSTRAINT FK_Membership_User  FOREIGN KEY (user_id)  REFERENCES users(id),
    CONSTRAINT UQ_Space_User       UNIQUE (space_id, user_id)
);
GO

-- ─────────────────────────────────────────────────────
-- POSTS
-- ─────────────────────────────────────────────────────
CREATE TABLE posts (
    id                 UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID() PRIMARY KEY,
    space_id           UNIQUEIDENTIFIER NOT NULL,
    author_id          UNIQUEIDENTIFIER NOT NULL,
    title              VARCHAR(300)     NOT NULL,
    body               NVARCHAR(MAX)    NOT NULL,
    post_type          VARCHAR(20)      DEFAULT 'QUESTION',   -- QUESTION / DISCUSSION
    is_solved          BIT              DEFAULT 0,
    accepted_answer_id UNIQUEIDENTIFIER NULL,                 -- circular FK, added below
    view_count         INT              DEFAULT 0,
    good_question_count INT             DEFAULT 0,
    tags               VARCHAR(500),
    created_at         DATETIME2        DEFAULT GETUTCDATE(),
    updated_at         DATETIME2,
    CONSTRAINT FK_Post_Space  FOREIGN KEY (space_id)  REFERENCES spaces(id),
    CONSTRAINT FK_Post_Author FOREIGN KEY (author_id) REFERENCES users(id)
);
GO

-- ─────────────────────────────────────────────────────
-- ANSWERS
-- ─────────────────────────────────────────────────────
CREATE TABLE answers (
    id           UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID() PRIMARY KEY,
    post_id      UNIQUEIDENTIFIER NOT NULL,
    author_id    UNIQUEIDENTIFIER NOT NULL,
    body         NVARCHAR(MAX)    NOT NULL,
    upvote_count INT              DEFAULT 0,
    is_accepted  BIT              DEFAULT 0,
    created_at   DATETIME2        DEFAULT GETUTCDATE(),
    updated_at   DATETIME2,
    CONSTRAINT FK_Answer_Post   FOREIGN KEY (post_id)   REFERENCES posts(id),
    CONSTRAINT FK_Answer_Author FOREIGN KEY (author_id) REFERENCES users(id)
);
GO

-- Circular FK: posts.accepted_answer_id → answers.id (added AFTER answers table exists)
ALTER TABLE posts
    ADD CONSTRAINT FK_Post_AcceptedAnswer
    FOREIGN KEY (accepted_answer_id) REFERENCES answers(id);
GO

-- ─────────────────────────────────────────────────────
-- VOTES
-- ─────────────────────────────────────────────────────
CREATE TABLE votes (
    id          UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID() PRIMARY KEY,
    user_id     UNIQUEIDENTIFIER NOT NULL,
    target_type VARCHAR(20)      NOT NULL,   -- ANSWER / QUESTION
    target_id   UNIQUEIDENTIFIER NOT NULL,   -- UUID of the voted-on entity (no FK — polymorphic)
    vote_type   VARCHAR(20)      NOT NULL,   -- UPVOTE / GOOD_QUESTION
    created_at  DATETIME2        DEFAULT GETUTCDATE(),
    CONSTRAINT FK_Vote_User FOREIGN KEY (user_id) REFERENCES users(id)
);
GO

-- ─────────────────────────────────────────────────────
-- MATERIALS
-- ─────────────────────────────────────────────────────
CREATE TABLE materials (
    id            UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID() PRIMARY KEY,
    space_id      UNIQUEIDENTIFIER NOT NULL,
    uploaded_by   UNIQUEIDENTIFIER NOT NULL,
    title         VARCHAR(255)     NOT NULL,
    description   VARCHAR(1000),
    resource_type VARCHAR(30),                -- PDF / LINK / IMAGE / VIDEO
    url           VARCHAR(1024),
    file_size_kb  INT,
    link_count    INT              DEFAULT 0,
    created_at    DATETIME2        DEFAULT GETUTCDATE(),
    CONSTRAINT FK_Material_Space    FOREIGN KEY (space_id)    REFERENCES spaces(id),
    CONSTRAINT FK_Material_Uploader FOREIGN KEY (uploaded_by) REFERENCES users(id)
);
GO

-- ─────────────────────────────────────────────────────
-- MATERIAL LINKS  (users saving materials to their collection)
-- ─────────────────────────────────────────────────────
CREATE TABLE material_links (
    id          UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID() PRIMARY KEY,
    material_id UNIQUEIDENTIFIER NOT NULL,
    user_id     UNIQUEIDENTIFIER NOT NULL,
    linked_at   DATETIME2        DEFAULT GETUTCDATE(),
    CONSTRAINT FK_Link_Material FOREIGN KEY (material_id) REFERENCES materials(id),
    CONSTRAINT FK_Link_User     FOREIGN KEY (user_id)     REFERENCES users(id)
);
GO

-- ─────────────────────────────────────────────────────
-- NOTIFICATIONS
-- ─────────────────────────────────────────────────────
CREATE TABLE notifications (
    id                UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID() PRIMARY KEY,
    recipient_id      UNIQUEIDENTIFIER NOT NULL,
    sender_id         UNIQUEIDENTIFIER NULL,           -- null = system notification
    notification_type VARCHAR(50)      NOT NULL,
    title             VARCHAR(255)     NOT NULL,
    message           VARCHAR(1000),
    reference_type    VARCHAR(30),                     -- POST / ANSWER / MATERIAL / SPACE
    reference_id      UNIQUEIDENTIFIER,                -- UUID of the related entity (nullable)
    is_read           BIT              DEFAULT 0,
    created_at        DATETIME2        DEFAULT GETUTCDATE(),
    CONSTRAINT FK_Notif_Recipient FOREIGN KEY (recipient_id) REFERENCES users(id),
    CONSTRAINT FK_Notif_Sender    FOREIGN KEY (sender_id)    REFERENCES users(id)
);
GO
