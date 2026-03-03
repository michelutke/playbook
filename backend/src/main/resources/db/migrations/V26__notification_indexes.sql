CREATE INDEX idx_notifications_user_created ON notifications(user_id, created_at DESC);
CREATE INDEX idx_notifications_user_read ON notifications(user_id, read);
CREATE INDEX idx_push_tokens_user ON push_tokens(user_id);
CREATE INDEX idx_notification_dedup_created ON notification_dedup(created_at);
