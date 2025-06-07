package arq.org.pcs.docker_load_api.client;

import arq.org.pcs.docker_load_api.controller.response.PortsResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "manager", url = "localhost:8080/manager")
public interface ManagerClient {

    @GetMapping("/ports")
    PortsResponse getPorts();
}
