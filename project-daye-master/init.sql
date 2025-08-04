-- MariaDB 초기화 스크립트
-- 데이터베이스 생성 (이미 docker-compose에서 생성됨)
-- USE demo_db;

-- 사용자 테이블 생성 (JPA가 자동으로 생성함)
-- CREATE TABLE IF NOT EXISTS users (
--     id BIGINT AUTO_INCREMENT PRIMARY KEY,
--     username VARCHAR(255) NOT NULL UNIQUE,
--     password VARCHAR(255) NOT NULL,
--     email VARCHAR(255) NOT NULL,
--     full_name VARCHAR(255),
--     enabled BOOLEAN DEFAULT TRUE,
--     account_non_expired BOOLEAN DEFAULT TRUE,
--     account_non_locked BOOLEAN DEFAULT TRUE,
--     credentials_non_expired BOOLEAN DEFAULT TRUE
-- );

-- 사용자 역할 테이블 생성 (JPA가 자동으로 생성함)
-- CREATE TABLE IF NOT EXISTS user_roles (
--     user_id BIGINT NOT NULL,
--     role VARCHAR(255) NOT NULL,
--     FOREIGN KEY (user_id) REFERENCES users(id)
-- );

-- 인덱스 생성
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- 댓글 테이블 생성 (JPA가 자동으로 생성함)
-- CREATE TABLE IF NOT EXISTS comments (
--     id BIGINT AUTO_INCREMENT PRIMARY KEY,
--     content TEXT NOT NULL,
--     post_id BIGINT NOT NULL,
--     author_id BIGINT NOT NULL,
--     parent_id BIGINT,
--     created_at DATETIME NOT NULL,
--     FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
--     FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE,
--     FOREIGN KEY (parent_id) REFERENCES comments(id) ON DELETE CASCADE
-- );

-- 댓글 인덱스 생성
CREATE INDEX IF NOT EXISTS idx_comments_post_id ON comments(post_id);
CREATE INDEX IF NOT EXISTS idx_comments_author_id ON comments(author_id);
CREATE INDEX IF NOT EXISTS idx_comments_parent_id ON comments(parent_id);
CREATE INDEX IF NOT EXISTS idx_comments_created_at ON comments(created_at); 