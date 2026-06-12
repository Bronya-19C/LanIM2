package com.alpha.lanim.data;

import com.alpha.lanim.model.FileRecord;

import java.sql.*;
import java.util.Optional;

public class FileRepo {

    public FileRepo() {
    }

    public void upsert(FileRecord record) {
        String sql = """
            INSERT OR REPLACE INTO files 
            (file_id, message_id, file_name, content_type, total_size, total_chunks, 
             checksum, local_path, received_chunks, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = Database.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, record.getFileId());
            stmt.setString(2, record.getMessageId());
            stmt.setString(3, record.getFileName());
            stmt.setString(4, record.getContentType());
            stmt.setLong(5, record.getTotalSize());
            stmt.setInt(6, record.getTotalChunks());
            stmt.setString(7, record.getChecksum());
            stmt.setString(8, record.getLocalPath());
            stmt.setInt(9, record.getReceivedChunks());
            stmt.setString(10, record.getStatus());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Upsert file record failed", e);
        }
    }

    public Optional<FileRecord> findByFileId(String fileId) {
        String sql = "SELECT * FROM files WHERE file_id = ?";
        try (Connection conn = Database.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, fileId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    FileRecord record = new FileRecord();
                    record.setFileId(rs.getString("file_id"));
                    record.setMessageId(rs.getString("message_id"));
                    record.setFileName(rs.getString("file_name"));
                    record.setContentType(rs.getString("content_type"));
                    record.setTotalSize(rs.getLong("total_size"));
                    record.setTotalChunks(rs.getInt("total_chunks"));
                    record.setChecksum(rs.getString("checksum"));
                    record.setLocalPath(rs.getString("local_path"));
                    record.setReceivedChunks(rs.getInt("received_chunks"));
                    record.setStatus(rs.getString("status"));
                    return Optional.of(record);
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Find file by id failed", e);
        }
    }

    public void updateProgress(String fileId, int receivedChunks, String status) {
        String sql = "UPDATE files SET received_chunks = ?, status = ? WHERE file_id = ?";
        try (Connection conn = Database.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, receivedChunks);
            stmt.setString(2, status);
            stmt.setString(3, fileId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Update file progress failed", e);
        }
    }
}