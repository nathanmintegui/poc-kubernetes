package arq.org.pcs.docker_manager_backend.repository;

import arq.org.pcs.docker_manager_backend.entity.Container;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContainerRepository extends JpaRepository<Container, Integer> {
}
