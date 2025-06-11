package arq.org.pcs.docker_manager_backend.jobs;

import arq.org.pcs.docker_manager_backend.dao.ContainerSimplifiedDAO;
import arq.org.pcs.docker_manager_backend.entity.Containers;
import arq.org.pcs.docker_manager_backend.entity.Imagens;
import arq.org.pcs.docker_manager_backend.entity.Status;
import arq.org.pcs.docker_manager_backend.entity.StatusContainers;
import arq.org.pcs.docker_manager_backend.repository.ContainerRepository;
import arq.org.pcs.docker_manager_backend.repository.ImagemRepository;
import arq.org.pcs.docker_manager_backend.repository.StatusContainersRepository;
import arq.org.pcs.docker_manager_backend.service.DockerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AutoScallingJob {

    public static final PageRequest ULTIMOS_5_REGISTROS = PageRequest.of(0, 5);
    private static final long VIDA_MINIMA = 20L;
    private static final long TEMPO_AMOSTRAGEM = 30L;

    private final ContainerRepository containerRepository;
    private final ImagemRepository imagemRepository;
    private final StatusContainersRepository statusContainersRepository;
    private final DockerService dockerService;

    @Scheduled(fixedDelay = 3000)
    public void task() {
        LocalDateTime dateNow = LocalDateTime.now();
        LocalDateTime startTime = dateNow.minusSeconds(TEMPO_AMOSTRAGEM);

        //List<ContainerSimplifiedDAO> containerSimplifiedDAOs =  containerRepository.getContainersSimplified(startTime);

        List<Imagens> imagens = imagemRepository.findAll()
                .stream()
                .map(imagem -> {
                    // Filtra os containers UP
                    List<Containers> containersUp = imagem.getContainersEntities().stream()
                            .filter(container -> container.getStatus() == Status.UP)
                            .toList();

                    // Atualiza a imagem com a nova lista (caso o setter esteja disponível)
                    imagem.setContainersEntities(containersUp);

                    return imagem;
                })
                // Só mantém imagens que possuem containers UP
                .filter(imagem -> !imagem.getContainersEntities().isEmpty())
                .toList();

        if (!imagens.isEmpty()) {
            for(Imagens imagem : imagens) {
                List<Containers> containers = imagem.getContainersEntities();

                long totalCpu = 0L;
                long mediaCpu;
                long runningReplica = containers.size();
                String idMenorCpu = null;
                long menorCpu = 999999L;
                for (Containers container : containers) {
                    if (container.getStartTime().plusSeconds(VIDA_MINIMA).isBefore(LocalDateTime.now())) {
                        List<StatusContainers> statusList = statusContainersRepository.getAvgContainerCpu(container.getId(), ULTIMOS_5_REGISTROS);

                        long mediaCpuStats = statusList.stream()
                                .map(StatusContainers::getCpuUsage)  // Obtém todos os valores de CPU
                                .map(Double::longValue)              // Converte Double para Long
                                .reduce(0L, Long::sum)
                                / statusList.size();           // Soma todos os valores

                        if (menorCpu > mediaCpuStats) {
                            idMenorCpu = container.getIdContainer();
                            menorCpu = mediaCpuStats;
                        }
                        totalCpu += mediaCpuStats;
                    } else {
                        totalCpu += 5L;
                    }
                }
                mediaCpu =  totalCpu / runningReplica;

                if (mediaCpu >= imagem.getMaxCpuUsage()) {
                    if (runningReplica < imagem.getMaxReplica()) {
                        dockerService.createContainer(imagem.getNome());
                        // TODO logica para ver se tem algum container com essa imagem stopado
                        break;
                    }
                } else {
                    if (runningReplica > imagem.getMinReplica()) {
                        if (mediaCpu < 40L) {
                            dockerService.stopContainer(idMenorCpu);
                            break;
                        }
                    }
                }
            }
        }
    }
}
