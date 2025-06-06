package arq.org.pcs.docker_manager_backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DockerStatusRunner implements ApplicationRunner {

    private final DockerStatusMonitor monitor;

    @Override
    public void run(ApplicationArguments args) {
        monitor.startAutoMonitor();
    }
}
