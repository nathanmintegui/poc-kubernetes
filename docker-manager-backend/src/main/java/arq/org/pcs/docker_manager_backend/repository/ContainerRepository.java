package arq.org.pcs.docker_manager_backend.repository;

import arq.org.pcs.docker_manager_backend.entity.Containers;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ContainerRepository extends JpaRepository<Containers, Integer> {
    Optional<Containers> findByIdContainer(String idContainer);
}
