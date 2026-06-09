package com.alpha.lanim.model;

public class FileRecord {

    private String fileId;
    private String messageId;
    private String fileName;
    private String contentType;
    private long totalSize;
    private int totalChunks;
    private String checksum;
    private String localPath;
    private int receivedChunks;
    private String status;

    public FileRecord() {
    }

    public FileRecord(String fileId, String messageId, String fileName, String contentType,
                      long totalSize, int totalChunks, String checksum,
                      String localPath, int receivedChunks, String status) {
    }

    public String getFileId() {
        return null;
    }

    public void setFileId(String fileId) {
    }

    public String getMessageId() {
        return null;
    }

    public void setMessageId(String messageId) {
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

    public String getLocalPath() {
        return null;
    }

    public void setLocalPath(String localPath) {
    }

    public int getReceivedChunks() {
        return 0;
    }

    public void setReceivedChunks(int receivedChunks) {
    }

    public String getStatus() {
        return null;
    }

    public void setStatus(String status) {
    }
}
