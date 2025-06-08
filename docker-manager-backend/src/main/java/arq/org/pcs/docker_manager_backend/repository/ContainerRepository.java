package arq.org.pcs.docker_manager_backend.repository;

import arq.org.pcs.docker_manager_backend.dao.ContainerSimplifiedDAO;
import arq.org.pcs.docker_manager_backend.entity.Containers;
import arq.org.pcs.docker_manager_backend.entity.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ContainerRepository extends JpaRepository<Containers, Integer> {

    Optional<Containers> findByIdContainer(String idContainer);

    @Query("""
                SELECT new arq.org.pcs.docker_manager_backend.dao.ContainerSimplifiedDAO(
                    c,
                    COALESCE(
                        (SELECT sc.cpuUsage 
                         FROM c.statusContainerEntities sc 
                         WHERE sc.date > :minDate
                         ORDER BY sc.date DESC
                         LIMIT 1),
                        0.0
                    ),
                    (SELECT COUNT(c2) 
                     FROM Containers c2 
                     WHERE c2.status = 'UP' 
                       AND c2.imagem = c.imagem)
                )
                FROM Containers c
                WHERE c.status = 'UP'
            """)
    List<ContainerSimplifiedDAO> getContainersSimplified(@Param("minDate") LocalDateTime minDate);

    List<Containers> findByStatus(Status status);

    @Modifying
    @Transactional
    @Query("UPDATE Containers c SET c.status = :status WHERE c.idContainer = :id")
    void updateStatusByIdContainer(@Param("id") String id, @Param("status") Status status);
}
