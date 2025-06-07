package arq.org.pcs.docker_manager_backend.service;

import arq.org.pcs.docker_manager_backend.dao.ContainerSimplifiedDAO;
import arq.org.pcs.docker_manager_backend.entity.Containers;
import arq.org.pcs.docker_manager_backend.entity.Imagens;
import arq.org.pcs.docker_manager_backend.entity.Status;
import arq.org.pcs.docker_manager_backend.entity.StatusContainers;
import arq.org.pcs.docker_manager_backend.repository.ContainerRepository;
import arq.org.pcs.docker_manager_backend.repository.ImagemRepository;
import arq.org.pcs.docker_manager_backend.repository.StatusContainersRepository;
import arq.org.pcs.docker_manager_backend.response.ContainerStatusResponse;
import arq.org.pcs.docker_manager_backend.response.ContainerStatusSimplifiedResponse;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.Statistics;
import com.github.dockerjava.core.InvocationBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class DockerService {

    private final DockerClient dockerClient;
    private final ContainerRepository containerRepository;
    private final StatusContainersRepository statusContainersRepository;
    private final ImagemRepository imagemRepository;

    public List<com.github.dockerjava.api.model.Container> listContainers(boolean all) {
        return dockerClient.listContainersCmd().withShowAll(all).exec();
    }

    public List<Image> listImages() {
        return dockerClient.listImagesCmd().exec();
    }

    public List<Image> filterImages(String filterName) {
        return dockerClient.listImagesCmd().withImageNameFilter(filterName).exec();
    }

    public void startContainer(String containerId) {
        dockerClient.startContainerCmd(containerId).exec();
        containerRepository.updateStatusByIdContainer(containerId, Status.UP);
    }

    public void stopContainer(String containerId) {
        dockerClient.stopContainerCmd(containerId).exec();
        containerRepository.updateStatusByIdContainer(containerId, Status.DOWN);
    }

    public void deleteContainer(String containerId) {
        dockerClient.removeContainerCmd(containerId).exec();
    }

    public void createContainer(String imageName) {
        dockerClient.createContainerCmd(imageName).exec();
    }

    public Statistics statsContainer(String containerId) {
        InvocationBuilder.AsyncResultCallback<Statistics> callback = new InvocationBuilder.AsyncResultCallback<>();
        dockerClient.statsCmd(containerId).exec(callback);
        Statistics stats = null;
        try {
            stats = callback.awaitResult();
            callback.close();
        } catch (RuntimeException | IOException e) {
            // you may want to throw an exception here
        }
        return stats; // this may be null or invalid if the container has terminated
    }

    public ContainerStatusResponse getStatusContainers() {
        return new ContainerStatusResponse(statusContainersRepository.findQualifiedNumPorts());
    }

    @Transactional
    public void salvarRegistroStatus(String containerId, Statistics stats) {
        assert containerId != null && !containerId.isBlank();
        assert stats != null;

        Containers container = containerRepository.findByIdContainer(containerId)
                .orElseGet(() -> {
                    Container c = getContainerById(containerId);

                    Imagens imagem = Imagens
                            .builder()
                            .nome(c.getImage())
                            .maxCpuUsage(80.0)
                            .maxRamUsage(512.0)
                            .minReplica(1)
                            .maxReplica(5)
                            .build();

                    imagemRepository.save(imagem);

                    Containers newContainer = Containers
                            .builder()
                            .idContainer(containerId)
                            .imagem(imagem)
                            .numPort(c.getPorts()[0].getPublicPort() == null
                                    ? randomPort()
                                    : c.getPorts()[0].getPublicPort().toString())
                            .nome(c.getNames()[0])
                            .status(Status.UP)
                            .build();

                    containerRepository.save(newContainer);

                    return newContainer;
                });

        long cpuDelta = stats.getCpuStats().getCpuUsage().getTotalUsage() - stats.getPreCpuStats().getCpuUsage().getTotalUsage();

        var numCpus = stats.getCpuStats().getOnlineCpus();
        double cpuPercent = ((double) cpuDelta / 1_000_000_000L) * numCpus * 100.0;

        var ram = stats.getMemoryStats().getUsage().doubleValue() / 1048576;

        StatusContainers statusContainer = StatusContainers
                .builder()
                .containers(container)
                .cpuUsage(cpuPercent)
                .ramUsage(ram)
                .date(LocalDateTime.now())
                .build();

        statusContainersRepository.save(statusContainer);
    }

    public Container getContainerById(String containerId) {
        if (containerId == null || containerId.isBlank()) {
            throw new IllegalArgumentException("O ID do container não pode ser nulo ou vazio");
        }

        return dockerClient.listContainersCmd()
                .withShowAll(true)
                .exec()
                .stream()
                .filter(c -> c.getId().startsWith(containerId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Container não encontrado: " + containerId));
    }

    private String randomPort() {
        Random random = new Random();
        int port = 1000 + random.nextInt(9000);
        return String.valueOf(port);
    }

    public List<ContainerStatusSimplifiedResponse> getContainerStatus() {
        int NUMERO_THREADS = Runtime.getRuntime().availableProcessors();

        List<Container> containers = dockerClient
                .listContainersCmd()
                .withShowAll(true)
                .exec();

        List<ContainerSimplifiedDAO> containerSimplifiedDAOs = containerRepository.getContainersSimplified(
                LocalDateTime.now().minusMinutes(10));
        Map<String, ContainerSimplifiedDAO> containerDAOMap = containerSimplifiedDAOs.stream()
                .collect(Collectors.toMap(ContainerSimplifiedDAO::id, dao -> dao));

        ExecutorService executor = Executors.newFixedThreadPool(NUMERO_THREADS);
        List<Future<ContainerStatusSimplifiedResponse>> futures = new ArrayList<>();

        for (Container container : containers) {
            futures.add(executor.submit(() -> {
                Statistics stats = statsContainer(container.getId());
                ContainerSimplifiedDAO dao = containerDAOMap.get(container.getId());

                if (dao == null || stats == null) return null;

                return ContainerStatusSimplifiedResponse.builder()
                        .id(container.getId())
                        .nome(container.getNames()[0])
                        .imagem(container.getImage())
                        .porta(dao.porta())
                        .ativo(stats.getPidsStats().getCurrent() != 0)
                        .cpuUsage(dao.cpuUsage())
                        .maxCpuUsage(dao.maxCpuUsage())
                        .ramUsage(getRamUsage(stats))
                        .maxRamUsage(dao.maxRamUsage())
                        .minReplica(dao.minReplica())
                        .maxReplica(dao.maxReplica())
                        .horarioLeitura(LocalDateTime.now())
                        .build();
            }));
        }

        executor.shutdown();

        List<ContainerStatusSimplifiedResponse> response = new ArrayList<>();
        for (Future<ContainerStatusSimplifiedResponse> future : futures) {
            try {
                ContainerStatusSimplifiedResponse result = future.get();
                if (result != null) response.add(result);
            } catch (InterruptedException | ExecutionException e) {
            }
        }

        return response;
    }

    /**
     * Retorna quantidade de ram utilizada pelo container em megabytes.
     *
     * @param stats
     * @return
     */
    private Double getRamUsage(Statistics stats) {
        assert stats != null;

        var ram = stats.getMemoryStats().getUsage().doubleValue() / 1048576;

        return ram;
    }
}
