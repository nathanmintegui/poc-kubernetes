package arq.org.pcs.docker_manager_backend.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Statistics;
import com.github.dockerjava.core.InvocationBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DockerStatsService {

    private final DockerClient dockerClient;
    private final Map<String, CpuStats> previousStatsMap = new HashMap<>();

    /**
     * Calcula o uso de CPU de um container
     * @param containerId ID do container
     * @return Porcentagem de uso da CPU (0-100) ou -1 se não for possível calcular
     */
    public synchronized float calculateCpuUsage(String containerId) {
        try {
            Statistics currentStats = getContainerStats(containerId);
            if (currentStats == null) return -1;

            CpuStats current = extractCpuStats(currentStats);
            CpuStats previous = previousStatsMap.get(containerId);

            // Se não temos estatísticas anteriores, armazene e retorne -1
            if (previous == null) {
                previousStatsMap.put(containerId, current);
                return -1;
            }

            // Cálculo do uso da CPU
            float cpuDelta = current.containerCpuUsage - previous.containerCpuUsage;
            float systemDelta = current.systemCpuUsage - previous.systemCpuUsage;

            if (systemDelta <= 0 || cpuDelta <= 0) {
                return 0;
            }

            float cpuPercent = (cpuDelta / systemDelta) * current.onlineCpus * 100;
            cpuPercent = Math.min(cpuPercent, 100); // Limita a 100%

            // Atualiza estatísticas anteriores para a próxima chamada
            previousStatsMap.put(containerId, current);

            return cpuPercent;

        } catch (Exception e) {
            log.error("Erro ao calcular uso de CPU para container {}: {}", containerId, e.getMessage());
            return -1;
        }
    }

    /**
     * Obtém estatísticas de memória do container
     * @param containerId ID do container
     * @return Uso de memória em MB
     */
    public double getMemoryUsageMb(String containerId) {
        try {
            Statistics stats = getContainerStats(containerId);
            if (stats == null || stats.getMemoryStats() == null || stats.getMemoryStats().getUsage() == null) {
                return -1;
            }
            return stats.getMemoryStats().getUsage() / (1024.0 * 1024.0);
        } catch (Exception e) {
            log.error("Erro ao obter uso de memória para container {}: {}", containerId, e.getMessage());
            return -1;
        }
    }

    /**
     * Obtém estatísticas do container de forma síncrona
     */
    private Statistics getContainerStats(String containerId) {
        InvocationBuilder.AsyncResultCallback<Statistics> callback = new InvocationBuilder.AsyncResultCallback<>();
        try {
            dockerClient.statsCmd(containerId).exec(callback);
            Statistics stats = callback.awaitResult();
            callback.close();
            return stats;
        } catch (RuntimeException | IOException e) {
            log.error("Erro ao obter estatísticas do container {}: {}", containerId, e.getMessage());
            return null;
        } finally {
            try {
                callback.close();
            } catch (IOException e) {
                log.warn("Erro ao fechar callback: {}", e.getMessage());
            }
        }
    }

    /**
     * Extrai dados relevantes de CPU das estatísticas
     */
    private CpuStats extractCpuStats(Statistics stats) {
        CpuStats cpuStats = new CpuStats();
        cpuStats.systemCpuUsage = stats.getCpuStats().getSystemCpuUsage() != null
                ? stats.getCpuStats().getSystemCpuUsage() : 0;
        cpuStats.containerCpuUsage = stats.getCpuStats().getCpuUsage().getTotalUsage() != null
                ? stats.getCpuStats().getCpuUsage().getTotalUsage() : 0;
        cpuStats.onlineCpus = stats.getCpuStats().getOnlineCpus() != null
                ? stats.getCpuStats().getOnlineCpus() : 1;
        return cpuStats;
    }

    private static class CpuStats {
        long systemCpuUsage;
        long containerCpuUsage;
        long onlineCpus;
    }
}