package arq.org.pcs.docker_manager_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DockerManagerBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(DockerManagerBackendApplication.class, args);
    }

}
