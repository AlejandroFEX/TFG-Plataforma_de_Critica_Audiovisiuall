package es.udc.fic.fictionary.simulation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/simulation")
public class SimulationController {

    private final SimulationService simulationService;

    public SimulationController(SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    @PostMapping("/run")
    public ResponseEntity<SimulationResult> run(
            @RequestParam(defaultValue = "10") int curators,
            @RequestParam(defaultValue = "5") int voters,
            @RequestParam(defaultValue = "10") int titles,
            @RequestParam(defaultValue = "42") long seed) {
        return ResponseEntity.ok(
                simulationService.run(curators, voters, titles, seed));
    }
}