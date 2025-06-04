package arq.org.pcs.docker_image_processor.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class LoadService {

    @Async
    public void executeLoad(int secondsToRun) {
        long endTime = System.currentTimeMillis() + secondsToRun * 1000L;
        double result = 0;
        while (System.currentTimeMillis() < endTime) {
            for (int i = 0; i < 100000; i++) {
                result += Math.sin(i) * Math.tan(i) * Math.sqrt(i);
            }
        }
    }
}