package arq.org.pcs.docker_manager_backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "containers")
public class Containers {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @Column(name = "id_container", nullable = false, unique = true)
    private String idContainer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_imagem", nullable = false)
    private Imagens imagem;

    @Column(name = "numPort", length = 5, nullable = false, unique = true)
    private String numPort;

    @Column(name = "nome", nullable = false, unique = true)
    private String nome;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    @JsonIgnore
    @OneToMany(mappedBy = "containers", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StatusContainers> statusContainerEntities;
}
