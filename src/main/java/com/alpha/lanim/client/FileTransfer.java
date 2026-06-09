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
        String contentType = Files.probeContentType(file);
        long totalSize = Files.size(file);
        int totalChunks = (int) ((totalSize + Constants.FILE_CHUNK_SIZE - 1) / Constants.FILE_CHUNK_SIZE);
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
                // optional: retransmit missing chunks
                break;
        }
    }

    private void handleFileMeta(Envelope env) {
        FileMetaPayload meta = JsonUtil.fromPayload(env.getPayload(), FileMetaPayload.class);
        if (meta == null) {
            return;
        }

        String fileName = meta.getFileName();
        String localPath = Constants.DEFAULT_FILES_PATH + "/" + meta.getFileId() + "_" + fileName;

        FileRecord record = new FileRecord(
                meta.getFileId(),
                env.getMessageId(),
                fileName,
                meta.getContentType(),
                meta.getTotalSize(),
                meta.getTotalChunks(),
                meta.getChecksum(),
                localPath,
                0,
                "PENDING"
        );
        fileRepo.upsert(record);

        try (RandomAccessFile raf = new RandomAccessFile(localPath, "rw")) {
            raf.setLength(meta.getTotalSize());
        } catch (IOException e) {
            e.printStackTrace();
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
            onFileComplete(env.getSenderId(), record);
        }
    }

    private void onFileComplete(String senderId, FileRecord record) {
        String localPath = record.getLocalPath();
        try {
            String actualChecksum = HashUtil.sha256HexFile(Paths.get(localPath));
            if (!actualChecksum.equals(record.getChecksum())) {
                System.err.println("File checksum mismatch: " + record.getFileName());
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        String senderLabel = localPeerId.equals(senderId) ? "You"
                : peerNicknames.getOrDefault(senderId, senderId);

        Platform.runLater(() -> {
            Path filePath = Paths.get(localPath);
            String contentType = record.getContentType();
            String fileName = record.getFileName();
            if (PreviewPane.canPreview(contentType, fileName)) {
                preview.showFile(filePath, contentType, fileName);
            }
            chat.appendFileNotice(senderLabel, fileName, record.getTotalSize());
        });
    }
}
