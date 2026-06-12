package com.alpha.lanim.client;

import com.alpha.lanim.model.Envelope;
import com.alpha.lanim.model.JoinAckPayload;
import com.alpha.lanim.model.JoinPayload;
import com.alpha.lanim.model.MessageType;
import com.alpha.lanim.net.FrameCodec;
import com.alpha.lanim.net.SocketChannel;
import com.alpha.lanim.net.TlsContext;
import com.alpha.lanim.util.Constants;
import com.alpha.lanim.util.JsonUtil;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ImClient {

    public interface InboundListener {
        void onEnvelope(Envelope env);
    }

    private final TlsContext tlsContext;
    private SocketChannel channel;
    private InboundListener listener;
    private volatile boolean running;
    private Thread readThread;
    private CompletableFuture<JoinAckPayload> joinFuture;

    public ImClient(TlsContext tlsContext) {
        this.tlsContext = tlsContext;
    }

    public JoinAckPayload connectAndJoin(String host, int port, boolean useTls,
                                          String peerId, String nickname, String roomSecret) throws Exception {
        channel = SocketChannel.connect(host, port, useTls, tlsContext);
        joinFuture = new CompletableFuture<>();
        running = true;

        readThread = new Thread(() -> {
            try {
                while (running) {
                    byte[] frame = channel.readFrame();
                    if (frame == null) {
                        break;
                    }
                    Envelope env = FrameCodec.decode(frame);
                    if (env == null) {
                        continue;
                    }

                    if (MessageType.JOIN_ACK.name().equals(env.getType()) && !joinFuture.isDone()) {
                        JoinAckPayload payload = JsonUtil.fromPayload(env.getPayload(), JoinAckPayload.class);
                        joinFuture.complete(payload);
                    } else if (listener != null) {
                        listener.onEnvelope(env);
                    }
                }
            } catch (IOException e) {
                if (running) {
                    joinFuture.completeExceptionally(e);
                }
            } finally {
                if (!joinFuture.isDone()) {
                    joinFuture.completeExceptionally(new IOException("Connection closed"));
                }
            }
        }, "im-client-reader");
        readThread.setDaemon(true);
        readThread.start();

        JoinPayload jp = new JoinPayload(peerId, nickname, roomSecret);
        Envelope joinEnv = new Envelope(
                MessageType.JOIN.name(),
                UUID.randomUUID().toString(),
                peerId,
                null,
                0,
                System.currentTimeMillis(),
                jp
        );
        send(joinEnv);

        try {
            return joinFuture.get(Constants.JOIN_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw new Exception("Join timed out after " + Constants.JOIN_TIMEOUT_MS + "ms");
        }
    }

    public void send(Envelope env) throws IOException {
        byte[] frame = FrameCodec.encode(env);
        channel.writeFrame(frame);
    }

    public void setListener(InboundListener listener) {
        this.listener = listener;
    }

    public void close() {
        running = false;
        if (readThread != null) {
            readThread.interrupt();
        }
        if (channel != null) {
            try {
                channel.close();
            } catch (IOException ignored) {
                // shutdown path: socket may already be closed
            }
        }
    }
}
