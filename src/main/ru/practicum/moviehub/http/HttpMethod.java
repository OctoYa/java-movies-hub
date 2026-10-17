package ru.practicum.moviehub.http;

public enum HttpMethod {
    GET,
    POST,
    DELETE,
    PUT,
    UNKNOWN;

    public static HttpMethod from(String method) {
        if (method == null) {
            return UNKNOWN;
        }
        try {
            return HttpMethod.valueOf(method.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
