package arq.org.pcs.docker_manager_backend.controller;

import arq.org.pcs.docker_manager_backend.response.ContainerStatusResponse;
import arq.org.pcs.docker_manager_backend.service.DockerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("manager")
@RequiredArgsConstructor
public class ManagerController {

    private final DockerService dockerService;

    @GetMapping("/ports")
    public ContainerStatusResponse status() {
        return dockerService.getStatusContainers();
    }
}
