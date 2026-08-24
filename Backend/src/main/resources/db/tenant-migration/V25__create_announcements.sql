CREATE TABLE announcements (
    id BIGSERIAL PRIMARY KEY,
    sender_user_id BIGINT NOT NULL,
    title VARCHAR(180) NOT NULL,
    message TEXT NOT NULL,
    audience_type VARCHAR(40) NOT NULL,
    audience_id BIGINT,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_announcements_sender FOREIGN KEY (sender_user_id) REFERENCES users(id)
);

CREATE INDEX idx_announcements_created_at ON announcements(created_at DESC);
CREATE INDEX idx_announcements_sender ON announcements(sender_user_id);

CREATE TABLE announcement_recipients (
    id BIGSERIAL PRIMARY KEY,
    announcement_id BIGINT NOT NULL,
    recipient_type VARCHAR(20) NOT NULL,
    recipient_id BIGINT NOT NULL,
    read_at TIMESTAMP,
    CONSTRAINT fk_announcement_recipient_announcement FOREIGN KEY (announcement_id) REFERENCES announcements(id) ON DELETE CASCADE,
    CONSTRAINT uq_announcement_recipient UNIQUE (announcement_id, recipient_type, recipient_id)
);

CREATE INDEX idx_announcement_recipients_lookup
    ON announcement_recipients(recipient_type, recipient_id, announcement_id);

INSERT INTO role_permissions (role_id, permission)
SELECT id, 'CREATE_ANNOUNCEMENT' FROM roles WHERE name = 'ADMIN'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission)
SELECT id, 'VIEW_ANNOUNCEMENT' FROM roles WHERE name = 'ADMIN'
ON CONFLICT DO NOTHING;
