package com.alpha.lanim.server;

import com.alpha.lanim.data.Database;
import com.alpha.lanim.net.SocketChannel;
import com.alpha.lanim.net.TlsContext;
import com.alpha.lanim.util.Constants;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImServer {

    private final int port;
    private final boolean useTls;
    private final TlsContext tlsContext;
    private final RoomRegistry registry;
    private final ExecutorService executor;

    private volatile boolean running;
    private ServerSocket serverSocket;

    public ImServer(int port, boolean useTls, TlsContext tlsContext) {
        this.port = port;
        this.useTls = useTls;
        this.tlsContext = tlsContext;
        this.registry = new RoomRegistry();
        this.executor = Executors.newCachedThreadPool();
    }

    public void start() throws Exception {
        Database.init();

        if (useTls && tlsContext != null) {
            SSLServerSocketFactory ssf = tlsContext.serverContext().getServerSocketFactory();
            serverSocket = ssf.createServerSocket(port);
        } else {
            serverSocket = new ServerSocket(port);
        }

        running = true;
        System.out.println("LanIM server listening on port " + port + (useTls ? " (TLS)" : " (plain)"));

        while (running) {
            try {
                Socket raw = serverSocket.accept();
                handleConnection(raw);
            } catch (IOException e) {
                if (running) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void handleConnection(Socket raw) {
        try {
            SocketChannel channel = SocketChannel.fromSocket(raw, useTls);
            executor.submit(new ClientSession(channel, registry));
        } catch (Exception e) {
            e.printStackTrace();
            try {
                raw.close();
            } catch (IOException ignored) {
            }
        }
    }

    public void shutdown() {
        running = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        registry.shutdown();
        executor.shutdownNow();
    }

    public static void main(String[] args) {
        int port = Constants.DEFAULT_SERVER_PORT;
        boolean useTls = true;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--port":
                    if (i + 1 < args.length) {
                        port = Integer.parseInt(args[++i]);
                    }
                    break;
                case "--plain":
                    useTls = false;
                    break;
                case "--tls":
                    useTls = true;
                    break;
            }
        }

        TlsContext tlsContext = new TlsContext();
        try {
            tlsContext.init();
        } catch (Exception e) {
            System.err.println("Warning: TLS init failed, falling back to plain mode");
            useTls = false;
        }

        ImServer server = new ImServer(port, useTls, tlsContext);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down...");
            server.shutdown();
        }));

        try {
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
