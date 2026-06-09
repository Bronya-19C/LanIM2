package com.alpha.lanim.client;

import com.alpha.lanim.ui.ChatPane;
import com.alpha.lanim.ui.PreviewPane;
import com.alpha.lanim.data.FileRepo;
import com.alpha.lanim.model.Envelope;

import java.io.IOException;
import java.nio.file.Path;

public class FileTransfer {

    public FileTransfer(ImClient client, FileRepo fileRepo, ChatPane chat, PreviewPane preview,
                        String localPeerId, String roomId) {
    }

    public void send(Path file, String peerId, String roomId) throws IOException {
    }

    public void handle(Envelope env) {
    }
}
