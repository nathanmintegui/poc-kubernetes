package arq.org.pcs.docker_manager_backend.service;

import com.github.dockerjava.api.model.Statistics;

import java.util.Random;

public class Utils {

    private Utils() {
    }

    public static String randomPort() {
        Random random = new Random();
        int port = 10000 + random.nextInt(90000);
        return String.valueOf(port);
    }

    /**
     * Retorna quantidade de ram utilizada pelo container em megabytes.
     *
     * @param stats
     * @return
     */
    public static Double getRamUsage(Statistics stats) {
        assert stats != null;

        var ram = stats.getMemoryStats().getUsage().doubleValue() / 1048576;

        return ram;
    }
}
