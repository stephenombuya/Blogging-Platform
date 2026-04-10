ALTER TABLE posts    ADD COLUMN deleted_at DATETIME NULL DEFAULT NULL;
ALTER TABLE comments ADD COLUMN deleted_at DATETIME NULL DEFAULT NULL;
CREATE INDEX idx_posts_deleted    ON posts    (deleted_at);
CREATE INDEX idx_comments_deleted ON comments (deleted_at);
