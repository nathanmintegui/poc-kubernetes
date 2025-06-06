package arq.org.pcs.docker_manager_backend.dao;

public record ContainerSimplifiedDAO(String id,
                                     String porta,
                                     Double maxCpuUsage,
                                     Double maxRamUsage,
                                     Integer minReplica,
                                     Integer maxReplica,
                                     Double cpuUsage) {
}
