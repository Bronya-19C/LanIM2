CREATE TABLE IF NOT EXISTS messages (
    message_id TEXT PRIMARY KEY,
    type       TEXT NOT NULL,
    sender_id  TEXT NOT NULL,
    room_id    TEXT NOT NULL,
    sequence   INTEGER NOT NULL,
    timestamp  INTEGER NOT NULL,
    payload    TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_messages_room_seq ON messages(room_id, sequence);

CREATE TABLE IF NOT EXISTS files (
    file_id         TEXT PRIMARY KEY,
    message_id      TEXT NOT NULL,
    file_name       TEXT NOT NULL,
    content_type    TEXT,
    total_size      INTEGER NOT NULL,
    total_chunks    INTEGER NOT NULL,
    checksum        TEXT,
    local_path      TEXT,
    received_chunks INTEGER DEFAULT 0,
    status          TEXT DEFAULT 'PENDING'
);
