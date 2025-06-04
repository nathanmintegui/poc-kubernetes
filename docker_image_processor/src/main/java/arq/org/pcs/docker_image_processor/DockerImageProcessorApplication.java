package arq.org.pcs.docker_image_processor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class DockerImageProcessorApplication {

	public static void main(String[] args) {
		SpringApplication.run(DockerImageProcessorApplication.class, args);
	}

}
