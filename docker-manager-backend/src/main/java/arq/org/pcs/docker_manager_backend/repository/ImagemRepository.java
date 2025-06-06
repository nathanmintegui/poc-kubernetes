package arq.org.pcs.docker_manager_backend.repository;

import arq.org.pcs.docker_manager_backend.entity.Imagens;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImagemRepository extends JpaRepository<Imagens, Integer> {
}
