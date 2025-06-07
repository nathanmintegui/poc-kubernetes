package arq.org.pcs.docker_manager_backend.repository;

import arq.org.pcs.docker_manager_backend.entity.Imagens;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImagemRepository extends JpaRepository<Imagens, Integer> {
    List<Imagens> findByNome(String nome);
}
