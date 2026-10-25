package es.udc.fic.fictionary.catalog.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public record TmdbMovieDto (
    long id,
    String title,
    String overview,

    @JsonProperty("poster_path")
    String posterPath,

    @JsonProperty ("release_date")
    LocalDate releaseDate,

    double popularity,

    @JsonProperty("vote_count")
    int voteCount,

    @JsonProperty("genre_ids")
    java.util.List<Integer> genreIds

) {}
