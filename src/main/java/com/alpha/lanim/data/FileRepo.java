package com.alpha.lanim.data;

import com.alpha.lanim.model.FileRecord;

import java.util.Optional;

public class FileRepo {

    public FileRepo() {
    }

    public void upsert(FileRecord record) {
    }

    public Optional<FileRecord> findByFileId(String fileId) {
        return null;
    }

    public void updateProgress(String fileId, int receivedChunks, String status) {
    }
}
