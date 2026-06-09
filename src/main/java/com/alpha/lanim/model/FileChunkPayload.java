package com.alpha.lanim.model;

public class FileChunkPayload {

    private String fileId;
    private int chunkIndex;
    private int totalChunks;
    private String data; // Base64

    public FileChunkPayload() {
    }

    public FileChunkPayload(String fileId, int chunkIndex, int totalChunks, String data) {
    }

    public String getFileId() {
        return null;
    }

    public void setFileId(String fileId) {
    }

    public int getChunkIndex() {
        return 0;
    }

    public void setChunkIndex(int chunkIndex) {
    }

    public int getTotalChunks() {
        return 0;
    }

    public void setTotalChunks(int totalChunks) {
    }

    public String getData() {
        return null;
    }

    public void setData(String data) {
    }
}
