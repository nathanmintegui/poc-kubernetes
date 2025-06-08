package arq.org.pcs.docker_manager_backend.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Statistics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Component
public class DockerStatusMonitor {

    private final DockerClient dockerClient;
    private final DockerService dockerService;
    private final DockerStatsService dockerStatsService;
    private final Map<String, Thread> activeStatsThreads = new ConcurrentHashMap<>();

    public void startAutoMonitor() {
        Thread monitorThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    monitorContainers();
                    Thread.sleep(5000); // Espera 5 segundos
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.info("Monitor de containers interrompido");
                } catch (Exception e) {
                    log.error("Erro no monitor de containers: {}", e.getMessage());
                }
            }
        });

        monitorThread.setDaemon(true);
        monitorThread.setName("DockerStats-Monitor");
        monitorThread.start();
    }

    private void monitorContainers() {
        List<Container> containers = dockerClient.listContainersCmd().exec();
        Set<String> runningContainerIds = containers.stream()
                .map(Container::getId)
                .collect(Collectors.toSet());

        // Inicia listeners para novos containers
        startListenersForNewContainers(runningContainerIds);

        // Remove listeners para containers parados
        cleanupStoppedContainers(runningContainerIds);
    }

    private void startListenersForNewContainers(Set<String> runningContainerIds) {
        runningContainerIds.forEach(containerId -> {
            if (!activeStatsThreads.containsKey(containerId)) {
                log.info("Iniciando listener para container: {}", containerId);
                startStatsListener(containerId);
            }
        });
    }

    private void cleanupStoppedContainers(Set<String> runningContainerIds) {
        activeStatsThreads.keySet().stream()
                .filter(containerId -> !runningContainerIds.contains(containerId))
                .forEach(containerId -> {
                    log.info("Container parado, encerrando listener: {}", containerId);
                    Thread thread = activeStatsThreads.remove(containerId);
                    if (thread != null) {
                        thread.interrupt();
                    }
                });
    }

    protected void startStatsListener(String containerId) {
        Thread statsThread = new Thread(() -> {
            ResultCallback<Statistics> callback = new ResultCallback.Adapter<Statistics>() {
                @Override
                public void onNext(Statistics stats) {
                    processContainerStats(containerId, stats);
                }

                @Override
                public void onError(Throwable throwable) {
                    log.error("[{}] Erro no stream de stats: {}", containerId, throwable.getMessage());
                    activeStatsThreads.remove(containerId);
                    try {
                        close();
                    } catch (IOException e) {
                        log.warn("[{}] Erro ao fechar callback: {}", containerId, e.getMessage());
                    }
                }

                @Override
                public void onComplete() {
                    log.info("[{}] Stream de stats finalizada", containerId);
                    activeStatsThreads.remove(containerId);
                    try {
                        close();
                    } catch (IOException e) {
                        log.warn("[{}] Erro ao fechar callback: {}", containerId, e.getMessage());
                    }
                }
            };

            try {
                dockerClient.statsCmd(containerId).exec(callback);
            } catch (Exception e) {
                log.error("[{}] Falha ao iniciar stats: {}", containerId, e.getMessage());
                activeStatsThreads.remove(containerId);
                try {
                    callback.close();
                } catch (IOException ex) {
                    log.warn("[{}] Erro ao fechar callback: {}", containerId, ex.getMessage());
                }
            }
        });

        statsThread.setDaemon(true);
        statsThread.setName("DockerStats-" + containerId);
        activeStatsThreads.put(containerId, statsThread);
        statsThread.start();
    }

    private void processContainerStats(String containerId, Statistics stats) {
        try {
            // Usa o DockerStatsService para cálculos consistentes
            float cpuUsage = dockerStatsService.calculateCpuUsage(containerId);
            double ramUsage = dockerStatsService.getMemoryUsageMb(containerId);

            if (cpuUsage >= 0 && ramUsage >= 0) {
                dockerService.salvarRegistroStatus(containerId, stats, cpuUsage, ramUsage);
            }
        } catch (Exception e) {
            log.error("[{}] Erro ao processar stats: {}", containerId, e.getMessage());
        }
    }
}