package es.udc.fic.fictionary.catalog.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public record TmdbTvDto(
        long id,

        @JsonProperty("name")
        String name,

        String overview,

        @JsonProperty("poster_path")
        String posterPath,

        @JsonProperty("first_air_date")
        LocalDate firstAirDate,

        double popularity,

        @JsonProperty("vote_count")
        int voteCount,

        @JsonProperty("genre_ids")
        java.util.List<Integer> genreIds
) {}