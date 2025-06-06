package arq.org.pcs.docker_manager_backend.controller;

import arq.org.pcs.docker_manager_backend.service.DockerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.Executors;

@Controller
public class StatusContainersController {

    private final static int TEMPO_ENVIO_MS = 0;
    private final static long TEMPO_TIMEOUT = 0L;

    private final DockerService dockerService;
    private final ObjectMapper objectMapper;

    public StatusContainersController(DockerService dockerService, ObjectMapper objectMapper) {
        this.dockerService = dockerService;
        this.objectMapper = objectMapper;
    }

    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEvents() {
        SseEmitter emitter = new SseEmitter(TEMPO_TIMEOUT);

        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                while (true) {
                    emitter.send(objectMapper.writeValueAsString(dockerService.getContainerStatus()));
                    Thread.sleep(TEMPO_ENVIO_MS);
                }
            } catch (IOException | InterruptedException e) {
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }
}
