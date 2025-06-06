package arq.org.pcs.docker_manager_backend.repository;

import arq.org.pcs.docker_manager_backend.entity.StatusContainers;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface StatusContainersRepository extends JpaRepository<StatusContainers, Integer> {

    @Query(
            value = """
                    SELECT NUM_PORT
                    FROM (
                             SELECT s.id,
                                    s.cpu_usage,
                                    s.ram_usage,
                                    s.id_container,
                                    C.NUM_PORT,
                                    i.MAX_CPU_USAGE,
                                    i.MAX_RAM_USAGE,
                                    ROW_NUMBER() OVER (PARTITION BY s.id_container ORDER BY s.ID DESC) AS row_num
                             FROM STATUS_CONTAINERS s
                                      LEFT JOIN CONTAINERS C ON s.ID_CONTAINER = C.ID
                                      LEFT JOIN IMAGENS I ON C.ID_IMAGEM = I.ID
                             WHERE C.STATUS = 'UP'
                         ) AS subquery
                    WHERE row_num <= 5
                    GROUP BY ID_CONTAINER, NUM_PORT, MAX_CPU_USAGE, MAX_RAM_USAGE
                    HAVING AVG(ram_usage) <= MAX(MAX_RAM_USAGE)
                       AND AVG(cpu_usage) <= MAX(MAX_CPU_USAGE)
                    """,
            nativeQuery = true
    )
    List<String> findQualifiedNumPorts();
}
