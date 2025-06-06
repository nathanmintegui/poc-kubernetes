package arq.org.pcs.docker_load_api.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "processor", url = "")
public interface ProcessorClient {

    @GetMapping("/load/{secondsToRun}")
    String disparaProcessamento(@PathVariable int secondsToRun);
}
