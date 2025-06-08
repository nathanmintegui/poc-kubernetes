package arq.org.pcs.docker_manager_backend.service;


import arq.org.pcs.docker_manager_backend.entity.Containers;
import arq.org.pcs.docker_manager_backend.entity.Imagens;
import arq.org.pcs.docker_manager_backend.entity.Status;
import arq.org.pcs.docker_manager_backend.repository.ContainerRepository;
import arq.org.pcs.docker_manager_backend.repository.ImagemRepository;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Statistics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static arq.org.pcs.docker_manager_backend.service.Utils.randomPort;

@Slf4j
@Service
public class ContainerInitializerService {

    private final DockerService dockerService;
    private final ContainerRepository containerRepository;
    private final ImagemRepository imagemRepository;

    public ContainerInitializerService(DockerService dockerService, ContainerRepository containerRepository, ImagemRepository imagemRepository) {
        this.dockerService = dockerService;
        this.containerRepository = containerRepository;
        this.imagemRepository = imagemRepository;
    }

    //@EventListener(ApplicationReadyEvent.class)
    public void setupOnStartup() {
        log.info("Iniciando coleta de stats...");

        List<Containers> containersList = containerRepository.findAll();
        List<Container> containers = dockerService.listContainers(true);
        containers = containers.stream()
                .filter(c -> containersList.stream()
                        .noneMatch(c1 -> c1.getIdContainer().equals(c.getId())))
                .toList();

        List<Statistics> statistics = Collections.synchronizedList(new ArrayList<>());

        List<Containers> containersToBePersisted = new ArrayList<>();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<Void>> futures = containers.stream()
                    .map(container -> CompletableFuture.runAsync(
                            () -> {
                                Statistics stats = dockerService.statsContainer(container.getId());
                                if (stats != null) {
                                    log.info("Stats recebidas com sucesso");

                                    Imagens imagem = Imagens.create(container.getImage());

                                    Containers containerToBePersisted = Containers
                                            .builder()
                                            .idContainer(container.getId())
                                            .imagem(imagem)
                                            .numPort(container.getPorts().length == 0
                                                    ? randomPort()
                                                    : container.getPorts()[0].getPublicPort().toString())
                                            .nome(container.getNames()[0])
                                            .status(Status.UP)
                                            .build();

                                    containersToBePersisted.add(containerToBePersisted);
                                } else {
                                    log.error("Falha ao obter stats");
                                }
                            },
                            executor // Aqui usamos Executor correto
                    ))
                    .toList();

            futures.forEach(CompletableFuture::join);

            log.info("Coleta de stats finalizada: " + statistics.size());
        }

        List<Imagens> imagens = containersToBePersisted.stream()
                .map(Containers::getImagem)
                .toList();

        imagemRepository.saveAll(imagens);

        containerRepository.saveAll(containersToBePersisted);

        log.info("Coleta e persistência finalizadas.");
    }
}
