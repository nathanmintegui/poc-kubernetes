package arq.org.pcs.docker_manager_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
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
    private Integer minReplica;

    @Column(name = "max_replica", nullable = false)
    private Integer maxReplica;

    @OneToMany(mappedBy = "imagem", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<Containers> containersEntities;

    public static Imagens create(String imageName) {
        assert imageName != null && !imageName.isBlank();

        return Imagens
                .builder()
                .nome(imageName)
                .maxCpuUsage(80.0)
                .maxRamUsage(512.0)
                .minReplica(1)
                .maxReplica(5)
                .build();
    }
}
