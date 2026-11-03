package es.udc.fic.fictionary.api;

import es.udc.fic.fictionary.TestcontainersConfiguration;
import es.udc.fic.fictionary.catalog.MediaType;
import es.udc.fic.fictionary.catalog.TmdbClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static es.udc.fic.fictionary.api.ApiTestSupport.longValue;
import static es.udc.fic.fictionary.api.TmdbFixtures.DRAMA;
import static es.udc.fic.fictionary.api.TmdbFixtures.HORROR;
import static es.udc.fic.fictionary.api.TmdbFixtures.SCI_FI;
import static es.udc.fic.fictionary.api.TmdbFixtures.movie;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** RF-02, RF-12 y RF-13: búsqueda, exploración por género y ficha de contenido. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class CatalogApiTests {

    @Autowired WebApplicationContext context;
    @MockitoBean TmdbClient tmdbClient;

    private ApiTestSupport api;

    @BeforeEach
    void setUp() {
        api = ApiTestSupport.create(context);
        when(tmdbClient.getGenres(MediaType.MOVIE)).thenReturn(List.of(SCI_FI, HORROR, DRAMA));
        when(tmdbClient.getGenres(MediaType.TV)).thenReturn(List.of(DRAMA));
    }

    @Test
    void searchMarksHiddenGemsAndReturnsGenres() throws Exception {
        when(tmdbClient.searchMovies("matrix", 1)).thenReturn(List.of(
                movie(603, "Matrix", 80.0, List.of(878), null),
                movie(777, "Rareza", 3.0, List.of(27), null)));

        api.get("/api/catalog/search?query=matrix&type=MOVIE", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("Matrix")))
                .andExpect(jsonPath("$[0].hiddenGem", is(false)))
                .andExpect(jsonPath("$[0].genres", hasItem("Ciencia ficcion")))
                .andExpect(jsonPath("$[1].hiddenGem", is(true)));
    }

    @Test
    void searchWithoutTextNorGenreReturnsNothingAndSkipsTmdb() throws Exception {
        api.get("/api/catalog/search?query=", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(tmdbClient, never()).searchMovies(anyString(), anyInt());
        verify(tmdbClient, never()).searchTv(anyString(), anyInt());
        verify(tmdbClient, never()).discoverMovies(anyInt(), anyBoolean());
        verify(tmdbClient, never()).discoverTv(anyInt(), anyBoolean());
    }

    @Test
    void searchByTextAndGenreKeepsOnlyMatchingTitles() throws Exception {
        when(tmdbClient.searchMovies("matrix", 1)).thenReturn(List.of(
                movie(603, "Matrix", 80.0, List.of(878), null),
                movie(777, "Matrix de terror", 3.0, List.of(27), null)));

        api.get("/api/catalog/search?query=matrix&type=MOVIE&genre=878", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].tmdbId", is(603)));
    }

    @Test
    void exploreByGenreUsesDiscoverWhenThereIsNoText() throws Exception {
        when(tmdbClient.discoverMovies(27, true)).thenReturn(List.of(
                movie(5, "Joya de terror", 2.0, List.of(27), null)));

        api.get("/api/catalog/search?type=MOVIE&genre=27&niche=true", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].hiddenGem", is(true)));

        verify(tmdbClient).discoverMovies(27, true);
        verify(tmdbClient, never()).searchMovies(anyString(), anyInt());
    }

    @Test
    void genresEndpointListsGenresSortedByName() throws Exception {
        api.get("/api/catalog/genres?type=MOVIE", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", contains("Ciencia ficcion", "Drama", "Terror")));
    }

    @Test
    void detailStoresTheTitleWithItsGenresAndCachesIt() throws Exception {
        when(tmdbClient.getMovie(603L))
                .thenReturn(movie(603, "Matrix", 80.0, List.of(878), List.of(SCI_FI)));

        var first = api.get("/api/catalog/603?type=MOVIE", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.genres", hasItem("Ciencia ficcion")));
        var second = api.get("/api/catalog/603?type=MOVIE", null)
                .andExpect(status().isOk());

        assertThat(longValue(second, "$.id")).isEqualTo(longValue(first, "$.id"));
        // La segunda consulta sale de la BD local: TMDB solo se llama una vez
        verify(tmdbClient, times(1)).getMovie(603L);
    }

    @Test
    void detailOfAnUnknownTitleFails() throws Exception {
        api.get("/api/catalog/999999?type=MOVIE", null)
                .andExpect(status().isNotFound());
    }
}
