package arq.org.pcs.docker_manager_backend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Data
@Entity
@Table(name = "imagens")
public class Imagens {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @Column(name = "nome", nullable = false, unique = true)
    private String nome;

    @Column(name = "max_cpu_usage", nullable = false)
    private Double maxCpuUsage;

    @Column(name = "max_ram_usage", nullable = false)
    private Double maxRamUsage;

    @Column(name = "min_replica", nullable = false)
    private Double minReplica;

    @Column(name = "max_replica", nullable = false)
    private Double maxReplica;

    @OneToMany(mappedBy = "imagem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Containers> containersEntities;
}
