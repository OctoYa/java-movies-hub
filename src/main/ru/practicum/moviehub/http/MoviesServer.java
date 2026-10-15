package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpServer;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.net.InetSocketAddress;

public class MoviesServer {
    private static final int DEFAULT_PORT = 8080;
    private final int port;
    private final HttpServer server;
    private final MoviesStore store;

    public MoviesServer(MoviesStore store, int port) {
        this.store = store;
        this.port = port;
        try {
            this.server = HttpServer.create(new InetSocketAddress(this.port), 0);
            this.server.createContext("/movies", new MoviesHandler(this.store));
        } catch (IOException e) {
            throw new RuntimeException("Failed to start HTTP server", e);
        }
    }

    public MoviesServer(MoviesStore store) {
        this(store, DEFAULT_PORT);
    }

    public MoviesServer() {
        this(new MoviesStore(), DEFAULT_PORT);
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
    }

    public MoviesStore getStore() {
        return store;
    }
}