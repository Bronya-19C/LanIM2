package com.alpha.lanim.net;

import com.alpha.lanim.model.Envelope;

import java.io.DataInputStream;
import com.google.gson.Gson;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public final class FrameCodec {

    private FrameCodec() {
    }

    public static byte[] encode(Envelope env) {
        Gson gson = new Gson();
        byte[] jsonBytes = gson.toJson(env).getBytes(StandardCharsets.UTF_8);
        ByteBuffer buf = ByteBuffer.allocate(4 + jsonBytes.length);
        buf.putInt(jsonBytes.length);
        buf.put(jsonBytes);
        return buf.array();
    }

    public static Envelope decode(byte[] frame) throws IOException {
        if (frame == null || frame.length < 4) {
            throw new IOException("Invalid frame: too short");
        }

        ByteBuffer buffer = ByteBuffer.wrap(frame);
        int length = buffer.getInt();

        if (length <= 0 || length > 10 * 1024 * 1024) {
            throw new IOException("Invalid JSON length: " + length);
        }

        if (frame.length < 4 + length) {
            throw new IOException("Frame truncated: expected " + (4 + length)
                    + " bytes but got " + frame.length);
        }

        byte[] jsonBytes = new byte[length];
        System.arraycopy(frame, 4, jsonBytes, 0, length);

        String jsonString = new String(jsonBytes, StandardCharsets.UTF_8);
        Gson gson = new Gson();
        return gson.fromJson(jsonString, Envelope.class);
    }

    public static Envelope decode(DataInputStream in) throws IOException {
        int length = in.readInt();
        if (length <= 0 || length > 1024 * 1024) {
            throw new IOException("Invalid frame length: " + length);
        }
        byte[] body = new byte[length];
        in.readFully(body);
        return decode(body);
    }
}
