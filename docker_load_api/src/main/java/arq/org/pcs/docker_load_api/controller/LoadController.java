package arq.org.pcs.docker_load_api.controller;

import arq.org.pcs.docker_load_api.controller.response.LoadResponse;
import arq.org.pcs.docker_load_api.service.LoadService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/load")
public class LoadController {

    private final LoadService loadService;

    public LoadController(LoadService loadService) {
        this.loadService = loadService;
    }

    @GetMapping("/start")
    public List<LoadResponse> getDadosDeTodasApis() {
        return loadService.load();
    }
}

