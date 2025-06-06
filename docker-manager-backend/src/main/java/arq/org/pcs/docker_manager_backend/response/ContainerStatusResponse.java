package arq.org.pcs.docker_manager_backend.response;

import java.util.List;

public record ContainerStatusResponse(List<String> portas) {
}
