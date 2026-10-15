package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MoviesApiTest {
    private static final String BASE = "http://localhost:8080";
    private static MoviesServer server;
    private static HttpClient client;
    private static final Gson gson = new Gson();

    @BeforeAll
    static void beforeAll() {
        server = new MoviesServer();
        server.start();
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    @AfterAll
    static void afterAll() {
        if (server != null) {
            server.stop();
        }
    }

    @BeforeEach
    void setUp() {
        server.getStore().clear();
    }

    @Test
    @DisplayName("Возвращает пустой список, если нет фильмов")
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode());
        assertEquals("application/json; charset=UTF-8", resp.headers().firstValue("Content-Type").orElse(""));

        String body = resp.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"));
        List<Movie> movies = gson.fromJson(body, new ListOfMoviesTypeToken().getType());
        assertNotNull(movies);
        assertTrue(movies.isEmpty());
    }

    @Test
    @DisplayName("Возвращает список с ранее добавленными фильмами")
    void getMovies_whenHasMovies_returnsMoviesList() throws Exception {
        Movie m1 = new Movie("Inception", 2010);
        Movie m2 = new Movie("Interstellar", 2014);

        HttpRequest req1 = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(m1)))
                .build();
        client.send(req1, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        HttpRequest req2 = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(m2)))
                .build();
        client.send(req2, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode());
        assertEquals("application/json; charset=UTF-8", resp.headers().firstValue("Content-Type").orElse(""));

        List<Movie> movies = gson.fromJson(resp.body(), new ListOfMoviesTypeToken().getType());
        assertEquals(2, movies.size());
        assertEquals("Inception", movies.get(0).getTitle());
        assertEquals("Interstellar", movies.get(1).getTitle());
    }

    @Test
    @DisplayName("Добавляет фильм при корректных данных")
    void postMovie_whenValidData_returns201AndCreatedMovie() throws Exception {
        Movie movie = new Movie("The Dark Knight", 2008);
        String bodyJson = gson.toJson(movie);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(201, resp.statusCode());
        assertEquals("application/json; charset=UTF-8", resp.headers().firstValue("Content-Type").orElse(""));

        Movie created = gson.fromJson(resp.body(), Movie.class);
        assertNotNull(created.getId());
        assertEquals("The Dark Knight", created.getTitle());
        assertEquals(2008, created.getYear());
    }

    @Test
    @DisplayName("Возвращает ошибку при пустом title")
    void postMovie_whenTitleIsEmpty_returns422WithDetails() throws Exception {
        Movie movie = new Movie("", 2010);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(movie)))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp.statusCode());
        assertEquals("application/json; charset=UTF-8", resp.headers().firstValue("Content-Type").orElse(""));
        ErrorResponse err = gson.fromJson(resp.body(), ErrorResponse.class);
        assertNotNull(err);
        assertNotNull(err.getError());
        assertNotNull(err.getDetails());
        assertFalse(err.getDetails().isEmpty());
    }

    @Test
    @DisplayName("Возвращает ошибку при слишком длинном title (> 100 символов)")
    void postMovie_whenTitleIsTooLong_returns422WithDetails() throws Exception {
        String longTitle = "A".repeat(101);
        Movie movie = new Movie(longTitle, 2010);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(movie)))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp.statusCode());
        assertEquals("application/json; charset=UTF-8", resp.headers().firstValue("Content-Type").orElse(""));
        ErrorResponse err = gson.fromJson(resp.body(), ErrorResponse.class);
        assertNotNull(err);
        assertNotNull(err.getError());
        assertNotNull(err.getDetails());
        assertFalse(err.getDetails().isEmpty());
    }

    @Test
    @DisplayName("Возвращает ошибку при неверном year (меньше 1888 или больше текущего года + 1)")
    void postMovie_whenYearIsInvalid_returns422WithDetails() throws Exception {
        Movie movie = new Movie("Ancient Movie", 1800);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(movie)))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp.statusCode());
        assertEquals("application/json; charset=UTF-8", resp.headers().firstValue("Content-Type").orElse(""));
        ErrorResponse err = gson.fromJson(resp.body(), ErrorResponse.class);
        assertNotNull(err);
        assertNotNull(err.getError());
        assertNotNull(err.getDetails());
        assertFalse(err.getDetails().isEmpty());
    }

    @Test
    @DisplayName("Возвращает ошибку при неправильном Content-Type")
    void postMovie_whenInvalidContentType_returns415() throws Exception {
        Movie movie = new Movie("Matrix", 1999);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(movie)))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(415, resp.statusCode());
    }

    @Test
    @DisplayName("Возвращает ошибку при некорректном JSON")
    void postMovie_whenInvalidJson_returns422() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{invalid json}"))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp.statusCode());
        assertEquals("application/json; charset=UTF-8", resp.headers().firstValue("Content-Type").orElse(""));
        ErrorResponse err = gson.fromJson(resp.body(), ErrorResponse.class);
        assertNotNull(err);
        assertNotNull(err.getError());
    }

    @Test
    @DisplayName("Возвращает фильм по существующему id")
    void getMovieById_whenExists_returns200AndMovie() throws Exception {
        Movie movie = new Movie("Avatar", 2009);
        HttpRequest createReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(movie)))
                .build();
        HttpResponse<String> createResp = client.send(createReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        Movie created = gson.fromJson(createResp.body(), Movie.class);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/" + created.getId()))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode());
        assertEquals("application/json; charset=UTF-8", resp.headers().firstValue("Content-Type").orElse(""));
        Movie found = gson.fromJson(resp.body(), Movie.class);
        assertEquals(created.getId(), found.getId());
        assertEquals("Avatar", found.getTitle());
    }

    @Test
    @DisplayName("Возвращает ошибку, если фильм не найден")
    void getMovieById_whenNotFound_returns404() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/999"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(404, resp.statusCode());
        assertEquals("application/json; charset=UTF-8", resp.headers().firstValue("Content-Type").orElse(""));
        ErrorResponse err = gson.fromJson(resp.body(), ErrorResponse.class);
        assertNotNull(err);
        assertNotNull(err.getError());
    }

    @Test
    @DisplayName("Возвращает ошибку, если id не число")
    void getMovieById_whenInvalidId_returns400() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/abc"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode());
        assertEquals("application/json; charset=UTF-8", resp.headers().firstValue("Content-Type").orElse(""));
        ErrorResponse err = gson.fromJson(resp.body(), ErrorResponse.class);
        assertNotNull(err);
        assertNotNull(err.getError());
    }

    @Test
    @DisplayName("Удаляет фильм по существующему id")
    void deleteMovie_whenExists_returns204() throws Exception {
        Movie movie = new Movie("Memento", 2000);
        HttpRequest createReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(movie)))
                .build();
        HttpResponse<String> createResp = client.send(createReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        Movie created = gson.fromJson(createResp.body(), Movie.class);

        HttpRequest delReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/" + created.getId()))
                .DELETE()
                .build();

        HttpResponse<String> delResp = client.send(delReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(204, delResp.statusCode());

        HttpRequest getReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/" + created.getId()))
                .GET()
                .build();

        HttpResponse<String> getResp = client.send(getReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(404, getResp.statusCode());
    }

    @Test
    @DisplayName("Возвращает ошибку, если фильм не найден")
    void deleteMovie_whenNotFound_returns404() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/999"))
                .DELETE()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(404, resp.statusCode());
        assertEquals("application/json; charset=UTF-8", resp.headers().firstValue("Content-Type").orElse(""));
        ErrorResponse err = gson.fromJson(resp.body(), ErrorResponse.class);
        assertNotNull(err);
        assertNotNull(err.getError());
    }

    @Test
    @DisplayName("Возвращает ошибку, если id не число")
    void deleteMovie_whenInvalidId_returns400() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/abc"))
                .DELETE()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode());
        assertEquals("application/json; charset=UTF-8", resp.headers().firstValue("Content-Type").orElse(""));
        ErrorResponse err = gson.fromJson(resp.body(), ErrorResponse.class);
        assertNotNull(err);
        assertNotNull(err.getError());
    }

    @Test
    @DisplayName("Возвращает фильмы указанного года")
    void getMoviesByYear_whenValidYear_returnsFilteredMovies() throws Exception {
        Movie m1 = new Movie("Movie 2010 A", 2010);
        Movie m2 = new Movie("Movie 2020", 2020);
        Movie m3 = new Movie("Movie 2010 B", 2010);

        for (Movie m : List.of(m1, m2, m3)) {
            HttpRequest postReq = HttpRequest.newBuilder()
                    .uri(URI.create(BASE + "/movies"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(m)))
                    .build();
            client.send(postReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        }

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=2010"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode());
        assertEquals("application/json; charset=UTF-8", resp.headers().firstValue("Content-Type").orElse(""));
        List<Movie> movies = gson.fromJson(resp.body(), new ListOfMoviesTypeToken().getType());
        assertEquals(2, movies.size());
        assertTrue(movies.stream().allMatch(m -> m.getYear() == 2010));
    }

    @Test
    @DisplayName("Возвращает пустой список, если фильмов с таким годом нет")
    void getMoviesByYear_whenNoMoviesWithYear_returnsEmptyList() throws Exception {
        Movie m1 = new Movie("Movie 2010", 2010);

        HttpRequest postReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(m1)))
                .build();
        client.send(postReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=1999"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode());
        assertEquals("application/json; charset=UTF-8", resp.headers().firstValue("Content-Type").orElse(""));
        List<Movie> movies = gson.fromJson(resp.body(), new ListOfMoviesTypeToken().getType());
        assertNotNull(movies);
        assertTrue(movies.isEmpty());
    }

    @Test
    @DisplayName("Возвращает ошибку, если параметр year не число")
    void getMoviesByYear_whenInvalidYear_returns400() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=invalid"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode());
        assertEquals("application/json; charset=UTF-8", resp.headers().firstValue("Content-Type").orElse(""));
        ErrorResponse err = gson.fromJson(resp.body(), ErrorResponse.class);
        assertNotNull(err);
        assertNotNull(err.getError());
    }

    @Test
    @DisplayName("При неподдерживаемом HTTP-методе возвращается 405 Method Not Allowed")
    void methodNotAllowed_whenUnsupportedMethod_returns405() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .PUT(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(405, resp.statusCode());
    }
}