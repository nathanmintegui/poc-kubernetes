package arq.org.pcs.docker_manager_backend.response;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ContainerStatusSimplifiedResponse(String id,
                                                String nome,
                                                String imagem,
                                                String porta,
                                                Boolean ativo,
                                                Double cpuUsage,
                                                Double maxCpuUsage,
                                                Double ramUsage,
                                                Double maxRamUsage,
                                                Integer minReplica,
                                                Integer maxReplica,
                                                LocalDateTime horarioLeitura) {
}
