package arq.org.pcs.docker_manager_backend.dao;

import arq.org.pcs.docker_manager_backend.entity.Containers;

public record ContainerSimplifiedDAO(Containers containers,
                                     Double cpuUsage,
                                     Long runningReplica) {
}
