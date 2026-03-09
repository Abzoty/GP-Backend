/*  Database Schema - MS SQL Server 
   Version 1.0
*/

-- 1. Create Database
CREATE DATABASE GP_db;
GO

USE GP_db;
GO

-- 2. Create Tables (Ordered by dependency)

-- USERS Table 
CREATE TABLE users (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    student_id VARCHAR(50) UNIQUE,
    academic_year TINYINT, -- 1–5
    current_semester TINYINT, -- 1–10
    gpa DECIMAL(4,2),
    department VARCHAR(100),
    image_url VARCHAR(512),
    bio VARCHAR(500),
    is_active BIT DEFAULT 1,
    created_at DATETIME2 DEFAULT GETUTCDATE(),
    updated_at DATETIME2
);

-- GAMIFICATION_PROFILES Table 
CREATE TABLE gamification_profiles (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    xp_points INT DEFAULT 0,
    level SMALLINT DEFAULT 1,
    total_posts INT DEFAULT 0,
    total_answers INT DEFAULT 0,
    total_upvotes_received INT DEFAULT 0,
    total_materials_shared INT DEFAULT 0,
    current_streak_days SMALLINT DEFAULT 0,
    longest_streak_days SMALLINT DEFAULT 0,
    last_activity_date DATE,
    updated_at DATETIME2,
    CONSTRAINT FK_Gamification_User FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- XP_TRANSACTIONS Table 
CREATE TABLE xp_transactions (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL, -- e.g. POST_CREATED, ANSWER_UPVOTED
    xp_delta INT NOT NULL,
    reference_id BIGINT,
    reference_type VARCHAR(50), -- POST / ANSWER / MATERIAL / LOGIN
    created_at DATETIME2 DEFAULT GETUTCDATE(),
    CONSTRAINT FK_XP_User FOREIGN KEY (user_id) REFERENCES users(id)
);

-- COURSES_REGISTERED Table 
CREATE TABLE courses_registered (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    course_code VARCHAR(30) NOT NULL,
    course_name VARCHAR(200) NOT NULL,
    semester TINYINT,
    academic_year TINYINT,
    grade VARCHAR(5),
    is_current BIT DEFAULT 1,
    CONSTRAINT FK_Course_User FOREIGN KEY (user_id) REFERENCES users(id)
);

-- SPACES Table 
CREATE TABLE spaces (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    slug VARCHAR(120) NOT NULL UNIQUE,
    description VARCHAR(1000),
    category VARCHAR(80),
    course_code VARCHAR(30),
    created_by BIGINT,
    is_active BIT DEFAULT 1,
    member_count INT DEFAULT 0,
    created_at DATETIME2 DEFAULT GETUTCDATE(),
    CONSTRAINT FK_Space_Creator FOREIGN KEY (created_by) REFERENCES users(id)
);

-- SPACE_MEMBERSHIPS Table 
CREATE TABLE space_memberships (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    space_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(20) DEFAULT 'MEMBER', -- MEMBER / MODERATOR / OWNER
    joined_at DATETIME2 DEFAULT GETUTCDATE(),
    CONSTRAINT FK_Membership_Space FOREIGN KEY (space_id) REFERENCES spaces(id),
    CONSTRAINT FK_Membership_User FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT UQ_Space_User UNIQUE (space_id, user_id)
);

-- POSTS Table 
CREATE TABLE posts (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    space_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    title VARCHAR(300) NOT NULL,
    body NVARCHAR(MAX) NOT NULL,
    post_type VARCHAR(20) DEFAULT 'QUESTION', -- QUESTION / DISCUSSION
    is_solved BIT DEFAULT 0,
    accepted_answer_id BIGINT NULL, -- Added as nullable, updated later
    view_count INT DEFAULT 0,
    good_question_count INT DEFAULT 0,
    tags VARCHAR(500),
    created_at DATETIME2 DEFAULT GETUTCDATE(),
    updated_at DATETIME2,
    CONSTRAINT FK_Post_Space FOREIGN KEY (space_id) REFERENCES spaces(id),
    CONSTRAINT FK_Post_Author FOREIGN KEY (author_id) REFERENCES users(id)
);

-- ANSWERS Table 
CREATE TABLE answers (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    post_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    body NVARCHAR(MAX) NOT NULL,
    upvote_count INT DEFAULT 0,
    is_accepted BIT DEFAULT 0,
    created_at DATETIME2 DEFAULT GETUTCDATE(),
    updated_at DATETIME2,
    CONSTRAINT FK_Answer_Post FOREIGN KEY (post_id) REFERENCES posts(id),
    CONSTRAINT FK_Answer_Author FOREIGN KEY (author_id) REFERENCES users(id)
);

-- Circular Reference Fix for accepted_answer_id 
ALTER TABLE posts ADD CONSTRAINT FK_Post_AcceptedAnswer 
FOREIGN KEY (accepted_answer_id) REFERENCES answers(id);

-- VOTES Table 
CREATE TABLE votes (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    target_type VARCHAR(20) NOT NULL, -- ANSWER / QUESTION
    target_id BIGINT NOT NULL,
    vote_type VARCHAR(20) NOT NULL, -- UPVOTE / GOOD_QUESTION
    created_at DATETIME2 DEFAULT GETUTCDATE(),
    CONSTRAINT FK_Vote_User FOREIGN KEY (user_id) REFERENCES users(id)
);

-- MATERIALS Table 
CREATE TABLE materials (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    space_id BIGINT NOT NULL,
    uploaded_by BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    resource_type VARCHAR(30), -- PDF / LINK / IMAGE / VIDEO
    url VARCHAR(1024),
    file_size_kb INT,
    link_count INT DEFAULT 0,
    created_at DATETIME2 DEFAULT GETUTCDATE(),
    CONSTRAINT FK_Material_Space FOREIGN KEY (space_id) REFERENCES spaces(id),
    CONSTRAINT FK_Material_Uploader FOREIGN KEY (uploaded_by) REFERENCES users(id)
);

-- MATERIAL_LINKS Table 
CREATE TABLE material_links (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    material_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    linked_at DATETIME2 DEFAULT GETUTCDATE(),
    CONSTRAINT FK_Link_Material FOREIGN KEY (material_id) REFERENCES materials(id),
    CONSTRAINT FK_Link_User FOREIGN KEY (user_id) REFERENCES users(id)
);

-- NOTIFICATIONS Table 
CREATE TABLE notifications (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    recipient_id BIGINT NOT NULL,
    sender_id BIGINT NULL, -- null = system
    notification_type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message VARCHAR(1000),
    reference_type VARCHAR(30), -- POST / ANSWER / MATERIAL / SPACE
    reference_id BIGINT,
    is_read BIT DEFAULT 0,
    created_at DATETIME2 DEFAULT GETUTCDATE(),
    CONSTRAINT FK_Notif_Recipient FOREIGN KEY (recipient_id) REFERENCES users(id),
    CONSTRAINT FK_Notif_Sender FOREIGN KEY (sender_id) REFERENCES users(id)
);
