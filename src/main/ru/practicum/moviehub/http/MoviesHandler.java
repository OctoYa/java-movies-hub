package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MoviesHandler extends BaseHttpHandler {
    private final MoviesStore store;
    private final Gson gson = new Gson();

    public MoviesHandler(MoviesStore store) {
        this.store = store;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        try {
            String method = ex.getRequestMethod();
            switch (method) {
                case "GET":
                    handleGet(ex);
                    break;
                case "POST":
                    handlePost(ex);
                    break;
                case "DELETE":
                    handleDelete(ex);
                    break;
                default:
                    ex.sendResponseHeaders(405, -1);
                    ex.close();
                    break;
            }
        } catch (Exception e) {
            sendJson(ex, 500, gson.toJson(new ErrorResponse("Внутренняя ошибка сервера")));
        }
    }

    private void handleGet(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        if (path.equals("/movies") || path.equals("/movies/")) {
            String query = ex.getRequestURI().getQuery();
            if (query != null && !query.isEmpty()) {
                Map<String, String> queryParams = parseQuery(query);
                if (queryParams.containsKey("year")) {
                    try {
                        int year = Integer.parseInt(queryParams.get("year"));
                        sendJson(ex, 200, gson.toJson(store.getByYear(year)));
                        return;
                    } catch (NumberFormatException e) {
                        sendJson(ex, 400, gson.toJson(new ErrorResponse("Некорректный формат года")));
                        return;
                    }
                }
            }
            sendJson(ex, 200, gson.toJson(store.getAll()));
            return;
        }

        Long id = extractId(path);
        if (id == null) {
            sendJson(ex, 400, gson.toJson(new ErrorResponse("Некорректный ID")));
            return;
        }

        Optional<Movie> movieOpt = store.getById(id);
        if (movieOpt.isPresent()) {
            sendJson(ex, 200, gson.toJson(movieOpt.get()));
        } else {
            sendJson(ex, 404, gson.toJson(new ErrorResponse("Фильм не найден")));
        }
    }

    private void handlePost(HttpExchange ex) throws IOException {
        String contentType = ex.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.toLowerCase().startsWith("application/json")) {
            ex.sendResponseHeaders(415, -1);
            ex.close();
            return;
        }

        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Movie movie;
        try {
            movie = gson.fromJson(body, Movie.class);
            if (movie == null) {
                sendJson(ex, 422, gson.toJson(new ErrorResponse("Невалидный JSON")));
                return;
            }
        } catch (Exception e) {
            sendJson(ex, 422, gson.toJson(new ErrorResponse("Невалидный JSON")));
            return;
        }

        List<String> details = validateMovie(movie);
        if (!details.isEmpty()) {
            sendJson(ex, 422, gson.toJson(new ErrorResponse("Ошибка валидации", details)));
            return;
        }

        Movie created = store.add(movie);
        sendJson(ex, 201, gson.toJson(created));
    }

    private void handleDelete(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        Long id = extractId(path);
        if (id == null) {
            sendJson(ex, 400, gson.toJson(new ErrorResponse("Некорректный ID")));
            return;
        }

        boolean deleted = store.delete(id);
        if (deleted) {
            sendNoContent(ex);
        } else {
            sendJson(ex, 404, gson.toJson(new ErrorResponse("Фильм не найден")));
        }
    }

    private Long extractId(String path) {
        String prefix = "/movies/";
        if (!path.startsWith(prefix) || path.length() <= prefix.length()) {
            return null;
        }
        String idStr = path.substring(prefix.length());
        try {
            return Long.parseLong(idStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null) return map;
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=");
            if (kv.length == 2) {
                map.put(kv[0], kv[1]);
            } else if (kv.length == 1) {
                map.put(kv[0], "");
            }
        }
        return map;
    }

    private List<String> validateMovie(Movie movie) {
        List<String> details = new ArrayList<>();
        if (movie.getTitle() == null || movie.getTitle().trim().isEmpty()) {
            details.add("Название фильма не может быть пустым");
        } else if (movie.getTitle().length() > 100) {
            details.add("Название фильма не может превышать 100 символов");
        }

        int maxYear = Year.now().getValue() + 1;
        if (movie.getYear() == null || movie.getYear() < 1888 || movie.getYear() > maxYear) {
            details.add("Год выпуска должен быть от 1888 до " + maxYear);
        }

        return details;
    }
}
