package arq.org.pcs.docker_manager_backend.jobs;

import arq.org.pcs.docker_manager_backend.entity.Containers;
import arq.org.pcs.docker_manager_backend.entity.Status;
import arq.org.pcs.docker_manager_backend.repository.ContainerRepository;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;


@Slf4j
@Component
public class RecoveryContainerJob {

    private final DockerClient dockerClient;
    private final ContainerRepository containerRepository;

    @Value("${flag.recovery.container.job.logger}")
    private Boolean logger;

    public RecoveryContainerJob(DockerClient dockerClient, ContainerRepository containerRepository) {
        this.dockerClient = dockerClient;
        this.containerRepository = containerRepository;
    }

    /**
     * Tenta subir os containers que morreram a cada 1s.
     */
    @Scheduled(fixedRate = 1000)
    public void task() {
        List<Container> containers = dockerClient
                .listContainersCmd()
                .withShowAll(true)
                .exec();

        List<Containers> containersList = containerRepository.findByStatus(Status.UP);

        for (Containers container : containersList) {
            Optional<Container> containerStats = containers.stream()
                    .filter(c -> c.getId().equals(container.getIdContainer()))
                    .findFirst();

            if (containerStats.isPresent()) {
                var isContainerInactive = !Objects.equals(containerStats.get().getState(), "running");

                if (isContainerInactive) {
                    /*
                     * TODO: Pegar uma porta aleatória para subir o container ou manter a mesma que ele já estava?
                     *  pode ocorrer o risco de se manter a que já estava ter outro container utilizando ela....
                     * */
                    dockerClient.startContainerCmd(containerStats.get().getId()).exec();
                    containerRepository.updateStatusByIdContainer(containerStats.get().getId(), Status.UP);
                }
            }
        }

        if (logger) {
            log.info("Running recovery container job: {}", java.time.LocalDateTime.now());
        }
    }
}
