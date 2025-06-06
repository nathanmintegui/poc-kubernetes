package arq.org.pcs.docker_manager_backend.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Statistics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashSet;
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
    private final Map<String, Thread> activeStatsThreads = new ConcurrentHashMap<>();

    public void startAutoMonitor() {
        Thread monitorThread = new Thread(() -> {
            while (true) {
                try {
                    List<Container> containers = dockerClient.listContainersCmd().withShowAll(true).exec();

                    Set<String> runningContainerIds = containers.stream()
                            .filter(c -> c.getStatus().contains("Up"))
                            .map(Container::getId)
                            .collect(Collectors.toSet());

                    for (String containerId : runningContainerIds) {
                        if (!activeStatsThreads.containsKey(containerId)) {
                            log.info("Iniciando listener para container: {}", containerId);
                            startStatsListener(containerId);
                        }
                    }

                    for (String containerId : new HashSet<>(activeStatsThreads.keySet())) {
                        if (!runningContainerIds.contains(containerId)) {
                            log.info("Container parado, encerrando listener: {}", containerId);
                            Thread thread = activeStatsThreads.remove(containerId);
                            if (thread != null && thread.isAlive()) {
                                thread.interrupt();
                            }
                        }
                    }

                    Thread.sleep(5000); // Espera 5 segundos antes de verificar novamente

                } catch (Exception e) {
                    log.error("Erro no monitor de containers: {}", e.getMessage());
                }
            }
        });

        monitorThread.setDaemon(true);
        monitorThread.setName("DockerStats-Monitor");
        monitorThread.start();
    }

    protected void startStatsListener(String containerId) {
        Thread statsThread = new Thread(() -> {
            try {
                dockerClient.statsCmd(containerId).exec(new ResultCallback.Adapter<Statistics>() {
                    @Override
                    public void onNext(Statistics stats) {
                        //log.info("[{}] Stats: {}", containerId, stats);

                        dockerService.salvarRegistroStatus(containerId, stats);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        log.error("[{}] Erro: {}", containerId, throwable.getMessage());
                    }

                    @Override
                    public void onComplete() {
                        log.info("[{}] Stream finalizada.", containerId);
                        activeStatsThreads.remove(containerId);
                    }
                });
            } catch (Exception e) {
                log.error("[{}] Falha ao iniciar stats: {}", containerId, e.getMessage());
            }
        });

        statsThread.setDaemon(true);
        statsThread.setName("DockerStats-" + containerId);
        activeStatsThreads.put(containerId, statsThread);
        statsThread.start();
    }
}
