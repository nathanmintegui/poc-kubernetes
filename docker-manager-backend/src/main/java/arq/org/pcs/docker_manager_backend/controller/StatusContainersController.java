package arq.org.pcs.docker_manager_backend.controller;

import arq.org.pcs.docker_manager_backend.response.ContainerStatusSimplifiedResponse;
import arq.org.pcs.docker_manager_backend.service.DockerService;
import arq.org.pcs.docker_manager_backend.service.DockerStatsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Controller
public class StatusContainersController {

    private static final Logger log = LoggerFactory.getLogger(StatusContainersController.class);
    private static final int SEND_INTERVAL_MS = 1000; // 1 segundo entre atualizações
    private static final long CONNECTION_TIMEOUT_MS = 30 * 60 * 1000; // 30 minutos

    private final DockerClient dockerClient;
    private final DockerStatsService dockerStatsService;
    private final ExecutorService executor;

    public StatusContainersController(DockerClient dockerClient, DockerStatsService dockerStatsService) {
        this.dockerClient = dockerClient;
        this.dockerStatsService = dockerStatsService;
        this.executor = Executors.newCachedThreadPool();
    }

    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEvents() {
        SseEmitter emitter = new SseEmitter(CONNECTION_TIMEOUT_MS);
        AtomicBoolean isRunning = new AtomicBoolean(true);

        // Configura callbacks
        emitter.onCompletion(() -> {
            log.debug("SSE connection completed");
            isRunning.set(false);
        });
        emitter.onTimeout(() -> {
            log.debug("SSE connection timed out");
            isRunning.set(false);
            emitter.complete();
        });
        emitter.onError((ex) -> {
            log.error("SSE error: {}", ex.getMessage());
            isRunning.set(false);
        });

        executor.execute(() -> {
            try {
                while (isRunning.get()) {
                    try {
                        // 1. Obtenha a lista de containers
                        List<Container> containers = dockerClient.listContainersCmd().exec();

                        // 2. Crie uma lista de status simplificado
                        List<ContainerStatusSimplifiedResponse> statusList = containers.stream()
                                .map(container -> {
                                    // 3. Para cada container, obtenha as estatísticas
                                    float cpuUsage = dockerStatsService.calculateCpuUsage(container.getId());
                                    double ramUsage = dockerStatsService.getMemoryUsageMb(container.getId());

                                    // 4. Construa o objeto de resposta
                                    return ContainerStatusSimplifiedResponse.builder()
                                            .id(container.getId())
                                            .nome(container.getNames()[0])
                                            .imagem(container.getImage())
                                            .porta(getPortFromContainer(container))
                                            .ativo(container.getState().equals("running"))
                                            .cpuUsage((double) cpuUsage)
                                            .maxCpuUsage(100.0) // Valor padrão ou do banco
                                            .ramUsage(ramUsage)
                                            .maxRamUsage(512.0) // Valor padrão ou do banco
                                            .minReplica(1) // Valor padrão ou do banco
                                            .maxReplica(5) // Valor padrão ou do banco
                                            .horarioLeitura(LocalDateTime.now())
                                            .build();
                                })
                                .collect(Collectors.toList());

                        // 5. Envie os dados para o frontend
                        emitter.send(SseEmitter.event()
                                .data(statusList)
                                .id(String.valueOf(System.currentTimeMillis()))
                                .name("container-status"));

                        Thread.sleep(SEND_INTERVAL_MS);
                    } catch (Exception e) {
                        log.error("Error in SSE stream: {}", e.getMessage());
                        emitter.completeWithError(e);
                        break;
                    }
                }
            } finally {
                emitter.complete();
                isRunning.set(false);
            }
        });

        return emitter;
    }

    // Método auxiliar para obter a porta do container
    private String getPortFromContainer(Container container) {
        if (container.getPorts() != null && container.getPorts().length > 0) {
            ContainerPort port = container.getPorts()[0];
            return port.getPublicPort() != null ? String.valueOf(port.getPublicPort()) : "N/A";
        }
        return "N/A";
    }

    // Método para desligar o executor quando o aplicativo for encerrado
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}