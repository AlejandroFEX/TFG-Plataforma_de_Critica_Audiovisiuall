package es.udc.fic.fictionary.simulation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Herramienta de evaluación del ranking (RF-14). Requiere sesión iniciada y se puede
 * desactivar con fictionary.simulation.enabled=false (responde 404).
 */
@RestController
@RequestMapping("/api/simulation")
public class SimulationController {

    private final SimulationService simulationService;
    private final boolean enabled;

    public SimulationController(
            SimulationService simulationService,
            @Value("${fictionary.simulation.enabled:false}") boolean enabled) {
        this.simulationService = simulationService;
        this.enabled = enabled;
    }

    @PostMapping("/run")
    public ResponseEntity<SimulationResult> run(
            @RequestParam(defaultValue = "10") int curators,
            @RequestParam(defaultValue = "5") int voters,
            @RequestParam(defaultValue = "10") int titles,
            @RequestParam(defaultValue = "42") long seed) {
        if (!enabled) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(simulationService.run(curators, voters, titles, seed));
    }
}
