package arq.org.pcs.docker_load_api.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "manager", url = "localhost:8080/manager")
public interface ManagerClient {

    @GetMapping("/ports")
    List<String> getPorts();
}
