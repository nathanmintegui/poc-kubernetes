package arq.org.pcs.docker_image_processor.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class LoadService {

    // Configurações de controle
    private static final int BATCH_SIZE = 50000;  // Reduzido para consumir menos CPU por ciclo
    private static final int SLEEP_MS = 10;      // Intervalo entre ciclos para controle

    @Async
    public void executeLoad(int secondsToRun) {
        long endTime = System.currentTimeMillis() + secondsToRun * 1000L;
        double result = 0;

        while (System.currentTimeMillis() < endTime) {
            // Ciclo de processamento controlado
            result += computeBatch();

            // Pausa para controle de carga
            try {
                Thread.sleep(SLEEP_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private double computeBatch() {
        double batchResult = 0;
        for (int i = 0; i < BATCH_SIZE; i++) {
            batchResult += Math.sin(i) * Math.tan(i) * Math.sqrt(i);
        }
        return batchResult;
    }
}