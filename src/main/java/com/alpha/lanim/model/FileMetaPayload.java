package com.alpha.lanim.model;

public class FileMetaPayload {

    private String fileId;
    private String fileName;
    private String contentType;
    private long totalSize;
    private int totalChunks;
    private String checksum;

    public FileMetaPayload() {
    }

    public FileMetaPayload(String fileId, String fileName, String contentType,
                           long totalSize, int totalChunks, String checksum) {
    }

    public String getFileId() {
        return null;
    }

    public void setFileId(String fileId) {
    }

    public String getFileName() {
        return null;
    }

    public void setFileName(String fileName) {
    }

    public String getContentType() {
        return null;
    }

    public void setContentType(String contentType) {
    }

    public long getTotalSize() {
        return 0L;
    }

    public void setTotalSize(long totalSize) {
    }

    public int getTotalChunks() {
        return 0;
    }

    public void setTotalChunks(int totalChunks) {
    }

    public String getChecksum() {
        return null;
    }

    public void setChecksum(String checksum) {
    }
}
