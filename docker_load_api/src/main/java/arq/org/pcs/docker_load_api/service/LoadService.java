package arq.org.pcs.docker_load_api.service;

import arq.org.pcs.docker_load_api.client.ManagerClient;
import arq.org.pcs.docker_load_api.client.ProcessorClient;
import arq.org.pcs.docker_load_api.factory.ProcessorFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class LoadService {

    private final ManagerClient managerClient;
    private final ProcessorFactory processorFactory;

    public LoadService(ProcessorFactory processorFactory, ManagerClient managerClient) {
        this.processorFactory = processorFactory;
        this.managerClient = managerClient;
    }

    public List<String> load() {
        Random random = new Random();
        List<String> resultados = new ArrayList<>();
        List<String> portas = managerClient.getPorts();

        for (String porta : portas) {
            int secondsToRun =  random.nextInt(56) + 5;

            String url = "http://localhost:" + porta;
            ProcessorClient client = processorFactory.createClient(url);
            resultados.add("porta: " + porta + " retorno: " + client.disparaProcessamento(secondsToRun) + " tempo: " + secondsToRun);
        }

        return resultados;
    }
}
