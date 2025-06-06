package arq.org.pcs.docker_manager_backend.repository;

import arq.org.pcs.docker_manager_backend.dao.ContainerSimplifiedDAO;
import arq.org.pcs.docker_manager_backend.entity.Containers;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ContainerRepository extends JpaRepository<Containers, Integer> {

    Optional<Containers> findByIdContainer(String idContainer);

    @Query("""
                SELECT new arq.org.pcs.docker_manager_backend.dao.ContainerSimplifiedDAO(
                    c.idContainer,
                    c.numPort,
                    i.maxCpuUsage,
                    i.maxRamUsage,
                    i.minReplica,
                    i.maxReplica,
                    COALESCE((
                        SELECT s.cpuUsage
                        FROM StatusContainers s
                        WHERE s.containers = c
                          AND s.date > :minDate
                        ORDER BY s.date DESC
                        LIMIT 1
                    ), 0.0)
                )
                FROM Containers c
                JOIN c.imagem i
            """)
    List<ContainerSimplifiedDAO> getContainersSimplified(@Param("minDate") LocalDateTime minDate);

}
