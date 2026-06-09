package com.alpha.lanim.client;

import com.alpha.lanim.model.Envelope;
import com.alpha.lanim.model.JoinAckPayload;
import com.alpha.lanim.net.TlsContext;

import java.io.IOException;

public class ImClient {

    public interface InboundListener {
        void onEnvelope(Envelope env);
    }

    public ImClient(TlsContext tlsContext) {
    }

    public JoinAckPayload connectAndJoin(String host, int port, boolean useTls,
                                          String peerId, String nickname, String roomSecret) throws Exception {
        return null;
    }

    public void send(Envelope env) throws IOException {
    }

    public void setListener(InboundListener listener) {
    }

    public void close() {
    }
}
