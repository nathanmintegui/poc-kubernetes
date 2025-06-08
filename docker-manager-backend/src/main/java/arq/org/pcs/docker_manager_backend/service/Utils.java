package arq.org.pcs.docker_manager_backend.service;

import java.util.Random;

public class Utils {

    private Utils() {
    }

    public static String randomPort() {
        Random random = new Random();
        int port = 10000 + random.nextInt(90000);
        return String.valueOf(port);
    }
}
