package arq.org.pcs.docker_manager_backend.service;

import arq.org.pcs.docker_manager_backend.entity.Containers;
import arq.org.pcs.docker_manager_backend.entity.Imagens;
import arq.org.pcs.docker_manager_backend.entity.Status;
import arq.org.pcs.docker_manager_backend.entity.StatusContainers;
import arq.org.pcs.docker_manager_backend.repository.ContainerRepository;
import arq.org.pcs.docker_manager_backend.repository.ImagemRepository;
import arq.org.pcs.docker_manager_backend.repository.StatusContainersRepository;
import arq.org.pcs.docker_manager_backend.response.ContainerStatusResponse;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.InvocationBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static arq.org.pcs.docker_manager_backend.service.Utils.randomPort;

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
        try {
            dockerClient.startContainerCmd(containerId).exec();
            containerRepository.updateStatusByIdContainer(containerId, Status.UP);
        } catch (NotModifiedException e) {
            log.warn("Container not modified!", e);
        }
    }

    public void stopContainer(String containerId) {
        try {
            dockerClient.stopContainerCmd(containerId).exec();
            containerRepository.updateStatusByIdContainer(containerId, Status.DOWN);
        } catch (NotModifiedException e) {
            log.warn("Container not modified!", e);
        }
    }

    public void stopContainerForce(String containerId) {
        dockerClient.stopContainerCmd(containerId).exec();
    }

    public void deleteContainer(String containerId) {
        dockerClient.removeContainerCmd(containerId).exec();
    }

    public void createContainer(String imageName) {
        assert imageName != null && !imageName.isBlank();

        List<Imagens> imagens = imagemRepository.findByNome(imageName);
        Imagens imageToBePersisted;
        if (imagens.isEmpty()) {
            imageToBePersisted = Imagens.create(imageName);

            imagemRepository.save(imageToBePersisted);
        } else {
            imageToBePersisted = imagens.getFirst();
        }

        ExposedPort portaInterna = ExposedPort.tcp(8080);
        Ports portBindings = new Ports();
        var portaExterna = randomPort();
        portBindings.bind(portaInterna, Ports.Binding.bindPort(Integer.parseInt(portaExterna)));

        HostConfig hostConfig = HostConfig.newHostConfig()
                .withMemory(512 * 1024 * 1024L)    // 512MB de memória
                // Remova CPU quota ou aumente significativamente
                .withCpuQuota(100000L)  // Permite uso de até 1.0 CPU (100% de 1 core)
                .withCpuPeriod(100000L) // Período padrão de 100ms
                //.withCpuShares(1024)               // Peso relativo da CPU (default: 1024)
                //.withCpusetCpus("0-3")             // CPUs específicas a serem usadas (ex: "0,1" ou "0-3")
                .withPortBindings(portBindings);

        CreateContainerResponse container = dockerClient.createContainerCmd(imageName)
                .withExposedPorts(portaInterna)
                .withHostConfig(hostConfig)
                .exec();

        dockerClient.startContainerCmd(container.getId()).exec();
        InspectContainerResponse inspect = dockerClient.inspectContainerCmd(container.getId()).exec();

        Containers containerToBePersisted = Containers
                .builder()
                .idContainer(container.getId())
                .imagem(imageToBePersisted)
                .numPort(portaExterna)
                .nome(inspect.getName())
                .status(Status.UP)
                .startTime(LocalDateTime.now())
                .build();

        containerRepository.save(containerToBePersisted);
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
    public void salvarRegistroStatus(String containerId, Statistics stats, float cpuUsage, double ramUsage) {
        assert containerId != null && !containerId.isBlank();

        Containers container = containerRepository.findByIdContainer(containerId)
                .orElseGet(() -> createNewContainerRecord(containerId));

        StatusContainers statusContainer = StatusContainers
                .builder()
                .containers(container)
                .cpuUsage((double) cpuUsage)
                .ramUsage(ramUsage)
                .date(LocalDateTime.now())
                .build();

        statusContainersRepository.save(statusContainer);
    }

    private Containers createNewContainerRecord(String containerId) {
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

        return Containers
                .builder()
                .idContainer(containerId)
                .imagem(imagem)
                .numPort(c.getPorts()[0].getPublicPort() == null
                        ? randomPort()
                        : c.getPorts()[0].getPublicPort().toString())
                .nome(c.getNames()[0])
                .status(Status.UP)
                .build();
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
}
