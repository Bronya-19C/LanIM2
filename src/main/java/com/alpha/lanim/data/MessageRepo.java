package com.alpha.lanim.data;

import com.alpha.lanim.model.Envelope;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageRepo {

    public MessageRepo() {
        // 无需初始化连接
    }

    public void insert(Envelope e) throws SQLException {
        String sql = "INSERT OR IGNORE INTO messages (message_id, type, sender_id, room_id, sequence, timestamp, payload) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = Database.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, e.getMessageId());
            stmt.setString(2, e.getType());
            stmt.setString(3, e.getSenderId());
            stmt.setString(4, e.getRoomId());
            stmt.setInt(5, e.getSequence());
            stmt.setLong(6, e.getTimestamp());
            stmt.setObject(7, e.getPayload());
            stmt.executeUpdate();
        }
    }

    public List<Envelope> recentByRoom(String roomId, int limit) throws SQLException {
        String sql = """
            SELECT message_id, type, sender_id, room_id, sequence, timestamp, payload
            FROM (
                SELECT * FROM messages
                WHERE room_id = ?
                ORDER BY sequence DESC
                LIMIT ?
            ) sub
            ORDER BY sequence ASC
            """;
        List<Envelope> result = new ArrayList<>();
        try (Connection conn = Database.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, roomId);
            stmt.setInt(2, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Envelope e = new Envelope();
                    e.setMessageId(rs.getString("message_id"));
                    e.setType(rs.getString("type"));
                    e.setSenderId(rs.getString("sender_id"));
                    e.setRoomId(rs.getString("room_id"));
                    e.setSequence(rs.getInt("sequence"));
                    e.setTimestamp(rs.getLong("timestamp"));
                    e.setPayload(rs.getObject("payload"));
                    result.add(e);
                }
            }
        }
        return result;
    }

    public void trimRoom(String roomId, int maxCount) throws SQLException {
        String sql = """
            DELETE FROM messages
            WHERE room_id = ?
              AND sequence < (
                  SELECT MIN(sequence)
                  FROM (
                      SELECT sequence
                      FROM messages
                      WHERE room_id = ?
                      ORDER BY sequence DESC
                      LIMIT ?
                  )
              )
            """;
        try (Connection conn = Database.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, roomId);
            stmt.setString(2, roomId);
            stmt.setInt(3, maxCount);
            stmt.executeUpdate();
        }
    }

    public int maxSequence(String roomId) throws SQLException {
        String sql = "SELECT COALESCE(MAX(sequence), 0) AS max_seq FROM messages WHERE room_id = ?";
        try (Connection conn = Database.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, roomId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("max_seq");
                }
                return 0;
            }
        }
    }
}