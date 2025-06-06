package arq.org.pcs.docker_load_api.factory;

import arq.org.pcs.docker_load_api.client.ProcessorClient;
import org.springframework.cloud.openfeign.FeignClientBuilder;
import org.springframework.stereotype.Component;

@Component
public class ProcessorFactory {

    private final FeignClientBuilder feignClientBuilder;

    public ProcessorFactory(FeignClientBuilder feignClientBuilder) {
        this.feignClientBuilder = feignClientBuilder;
    }

    public ProcessorClient createClient(String baseUrl) {
        return feignClientBuilder
                .forType(ProcessorClient.class, "api-client-" + baseUrl)
                .url(baseUrl)
                .build();
    }
}
