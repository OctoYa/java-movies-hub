package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MoviesStore {
    private final Map<Long, Movie> movies = new HashMap<>();
    private long idSequence = 0;

    public List<Movie> getAll() {
        return new ArrayList<>(movies.values());
    }

    public List<Movie> getByYear(int year) {
        List<Movie> result = new ArrayList<>();
        for (Movie movie : movies.values()) {
            if (movie.getYear() != null && movie.getYear() == year) {
                result.add(movie);
            }
        }
        return result;
    }

    public Optional<Movie> getById(long id) {
        return Optional.ofNullable(movies.get(id));
    }

    public Movie add(Movie movie) {
        movie.setId(++idSequence);
        movies.put(movie.getId(), movie);
        return movie;
    }

    public boolean delete(long id) {
        return movies.remove(id) != null;
    }

    public void clear() {
        movies.clear();
        idSequence = 0;
    }
}