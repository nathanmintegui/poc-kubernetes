package arq.org.pcs.docker_manager_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "status_containers")
public class StatusContainers {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_container", nullable = false)
    private Containers containers;

    @Column(name = "cpu_usage", nullable = false)
    private Double cpuUsage;

    @Column(name = "ram_usage", nullable = false)
    private Double ramUsage;

    @Column(name = "date", nullable = false)
    private LocalDateTime date;
}
