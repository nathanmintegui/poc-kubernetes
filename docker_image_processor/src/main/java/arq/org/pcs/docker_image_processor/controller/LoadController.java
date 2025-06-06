package arq.org.pcs.docker_image_processor.controller;

import arq.org.pcs.docker_image_processor.service.LoadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/load")
public class LoadController {

    private final LoadService loadService;

    public LoadController(LoadService loadService) {
        this.loadService = loadService;
    }

    @GetMapping("/{secondsToRun}")
    public ResponseEntity<String> triggerLoad(@PathVariable int secondsToRun) {
        loadService.executeLoad(secondsToRun);
        return ResponseEntity.ok("Load started");
    }
}
