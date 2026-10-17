package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public abstract class BaseHttpHandler implements HttpHandler {
    protected static final String HEADER_CONTENT_TYPE = "Content-Type";
    protected static final String APPLICATION_JSON = "application/json";
    protected static final String CT_JSON = "application/json; charset=UTF-8";
    protected static final String CT_TEXT = "text/plain; charset=UTF-8";

    protected void sendJson(HttpExchange ex, HttpStatus status, String json) throws IOException {
        sendJson(ex, status.getCode(), json);
    }

    protected void sendJson(HttpExchange ex, int status, String json) throws IOException {
        byte[] resp = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set(HEADER_CONTENT_TYPE, CT_JSON);
        ex.sendResponseHeaders(status, resp.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(resp);
        }
    }

    protected void sendNoContent(HttpExchange ex) throws IOException {
        ex.sendResponseHeaders(HttpStatus.NO_CONTENT.getCode(), -1);
        ex.close();
    }

    protected void sendText(HttpExchange ex, HttpStatus status, String text) throws IOException {
        sendText(ex, status.getCode(), text);
    }

    protected void sendText(HttpExchange ex, int status, String text) throws IOException {
        byte[] resp = text.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set(HEADER_CONTENT_TYPE, CT_TEXT);
        ex.sendResponseHeaders(status, resp.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(resp);
        }
    }
}

