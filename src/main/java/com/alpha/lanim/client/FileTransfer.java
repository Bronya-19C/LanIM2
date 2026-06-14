package com.alpha.lanim.client;

import com.alpha.lanim.data.FileRepo;
import com.alpha.lanim.model.*;
import com.alpha.lanim.ui.ChatPane;
import com.alpha.lanim.ui.PreviewPane;
import com.alpha.lanim.util.Constants;
import com.alpha.lanim.util.HashUtil;
import com.alpha.lanim.util.JsonUtil;
import javafx.application.Platform;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FileTransfer {

    private final ImClient client;
    private final FileRepo fileRepo;
    private final ChatPane chat;
    private final PreviewPane preview;
    private final String localPeerId;
    private final String roomId;
    private final Map<String, String> peerNicknames = new ConcurrentHashMap<>();

    public FileTransfer(ImClient client, FileRepo fileRepo, ChatPane chat, PreviewPane preview,
                        String localPeerId, String roomId) {
        this.client = client;
        this.fileRepo = fileRepo;
        this.chat = chat;
        this.preview = preview;
        this.localPeerId = localPeerId;
        this.roomId = roomId;

        try {
            Files.createDirectories(Paths.get(Constants.DEFAULT_FILES_PATH));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onUserJoined(String peerId, String nickname) {
        peerNicknames.put(peerId, nickname);
    }

    public void onUserLeft(String peerId) {
        peerNicknames.remove(peerId);
    }

    public void send(Path file, String peerId, String roomId) throws IOException {
        String checksum = HashUtil.sha256HexFile(file);
        String fileName = file.getFileName().toString();
        String contentType = PreviewPane.resolveContentType(Files.probeContentType(file), fileName);
        long totalSize = Files.size(file);
        int totalChunks = totalSize == 0 ? 0
                : (int) ((totalSize + Constants.FILE_CHUNK_SIZE - 1) / Constants.FILE_CHUNK_SIZE);
        String fileId = UUID.randomUUID().toString();

        FileMetaPayload meta = new FileMetaPayload(fileId, fileName, contentType, totalSize, totalChunks, checksum);
        Envelope metaEnv = new Envelope(
                MessageType.FILE_META.name(),
                UUID.randomUUID().toString(),
                peerId,
                roomId,
                0,
                System.currentTimeMillis(),
                meta
        );
        client.send(metaEnv);

        if (totalChunks > 0) {
            try (FileInputStream fis = new FileInputStream(file.toFile())) {
                byte[] buffer = new byte[Constants.FILE_CHUNK_SIZE];
                int chunkIndex = 0;
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    byte[] chunkData = new byte[bytesRead];
                    System.arraycopy(buffer, 0, chunkData, 0, bytesRead);
                    String base64Data = Base64.getEncoder().encodeToString(chunkData);

                    FileChunkPayload chunkPayload = new FileChunkPayload(fileId, chunkIndex, totalChunks, base64Data);
                    Envelope chunkEnv = new Envelope(
                            MessageType.FILE_CHUNK.name(),
                            UUID.randomUUID().toString(),
                            peerId,
                            roomId,
                            0,
                            System.currentTimeMillis(),
                            chunkPayload
                    );
                    client.send(chunkEnv);
                    chunkIndex++;
                }
            }
        }

        notifyPreview(file, contentType, fileName);
    }

    public void handle(Envelope env) {
        String type = env.getType();
        if (type == null) {
            return;
        }

        switch (type) {
            case "FILE_META":
                handleFileMeta(env);
                break;
            case "FILE_CHUNK":
                handleFileChunk(env);
                break;
            case "FILE_CHUNK_ACK":
                break;
        }
    }

    private void handleFileMeta(Envelope env) {
        FileMetaPayload meta = JsonUtil.fromPayload(env.getPayload(), FileMetaPayload.class);
        if (meta == null) {
            return;
        }

        String fileName = meta.getFileName();
        Path localFilePath = Paths.get(Constants.DEFAULT_FILES_PATH,
                meta.getFileId() + "_" + sanitizeFileName(fileName));

        try {
            Files.createDirectories(localFilePath.getParent());
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        boolean fileAlreadyComplete = false;
        try {
            fileAlreadyComplete = Files.exists(localFilePath)
                    && meta.getTotalChunks() > 0
                    && Files.size(localFilePath) == meta.getTotalSize();
        } catch (IOException ignored) {
        }

        FileRecord record = new FileRecord(
                meta.getFileId(),
                env.getMessageId(),
                fileName,
                PreviewPane.resolveContentType(meta.getContentType(), fileName),
                meta.getTotalSize(),
                meta.getTotalChunks(),
                meta.getChecksum(),
                localFilePath.toString(),
                fileAlreadyComplete ? meta.getTotalChunks() : 0,
                fileAlreadyComplete ? "COMPLETE" : "PENDING"
        );
        fileRepo.upsert(record);

        if (!fileAlreadyComplete) {
            try (RandomAccessFile raf = new RandomAccessFile(localFilePath.toFile(), "rw")) {
                raf.setLength(meta.getTotalSize());
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

        if (meta.getTotalChunks() == 0 || fileAlreadyComplete) {
            onFileComplete(env.getSenderId(), meta.getFileId());
        }
    }

    private void handleFileChunk(Envelope env) {
        FileChunkPayload chunk = JsonUtil.fromPayload(env.getPayload(), FileChunkPayload.class);
        if (chunk == null) {
            return;
        }

        var recordOpt = fileRepo.findByFileId(chunk.getFileId());
        if (recordOpt.isEmpty()) {
            return;
        }
        FileRecord record = recordOpt.get();

        byte[] data;
        try {
            data = Base64.getDecoder().decode(chunk.getData());
        } catch (IllegalArgumentException e) {
            return;
        }

        try (RandomAccessFile raf = new RandomAccessFile(record.getLocalPath(), "rw")) {
            long offset = (long) chunk.getChunkIndex() * Constants.FILE_CHUNK_SIZE;
            raf.seek(offset);
            raf.write(data);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        int received = record.getReceivedChunks() + 1;
        fileRepo.updateProgress(chunk.getFileId(), received,
                received >= record.getTotalChunks() ? "COMPLETE" : "PENDING");

        if (received >= record.getTotalChunks()) {
            onFileComplete(env.getSenderId(), chunk.getFileId());
        }
    }

    private void onFileComplete(String senderId, String fileId) {
        var recordOpt = fileRepo.findByFileId(fileId);
        if (recordOpt.isEmpty()) {
            return;
        }
        FileRecord record = recordOpt.get();
        Path filePath = Paths.get(record.getLocalPath());

        if (!Files.exists(filePath)) {
            System.err.println("Received file missing on disk: " + record.getFileName());
            return;
        }

        try {
            String actualChecksum = HashUtil.sha256HexFile(filePath);
            if (!actualChecksum.equals(record.getChecksum())) {
                System.err.println("File checksum mismatch: " + record.getFileName()
                        + " (still attempting preview)");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        String senderLabel = localPeerId.equals(senderId) ? "You"
                : peerNicknames.getOrDefault(senderId, senderId);
        String contentType = PreviewPane.resolveContentType(record.getContentType(), record.getFileName());
        String fileName = record.getFileName();
        long totalSize = record.getTotalSize();

        Platform.runLater(() -> {
            notifyPreview(filePath, contentType, fileName);
            chat.appendFileNotice(senderLabel, fileName, totalSize);
        });
    }

    private void notifyPreview(Path path, String contentType, String fileName) {
        Runnable show = () -> preview.showFile(path, contentType, fileName);
        if (Platform.isFxApplicationThread()) {
            show.run();
        } else {
            Platform.runLater(show);
        }
    }

    private static String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "file";
        }
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
