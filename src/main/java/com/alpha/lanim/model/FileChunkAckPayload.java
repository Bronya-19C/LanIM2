package com.alpha.lanim.model;

import java.util.List;

public class FileChunkAckPayload {

    private String fileId;
    private List<Integer> missingChunks;

    public FileChunkAckPayload() {
    }

    public FileChunkAckPayload(String fileId, List<Integer> missingChunks) {
    }

    public String getFileId() {
        return null;
    }

    public void setFileId(String fileId) {
    }

    public List<Integer> getMissingChunks() {
        return null;
    }

    public void setMissingChunks(List<Integer> missingChunks) {
    }
}
