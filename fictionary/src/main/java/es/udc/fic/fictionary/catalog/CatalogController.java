package es.udc.fic.fictionary.catalog;

import es.udc.fic.fictionary.catalog.dto.TitleResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/search")
    public ResponseEntity<List<TitleResponse>> search(
            @RequestParam String query,
            @RequestParam(required = false) MediaType type) {
        return ResponseEntity.ok(catalogService.search(query, type));
    }

    @GetMapping("/{tmdbId}")
    public ResponseEntity<TitleResponse> getTitle(
            @PathVariable long tmdbId,
            @RequestParam MediaType type) {
        return ResponseEntity.ok(catalogService.getOrFetchTitle(tmdbId, type));
    }
}