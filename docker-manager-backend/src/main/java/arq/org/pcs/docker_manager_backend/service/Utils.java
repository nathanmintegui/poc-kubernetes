package arq.org.pcs.docker_manager_backend.service;

import lombok.RequiredArgsConstructor;

import java.util.Random;

@RequiredArgsConstructor
public class Utils {

    public static String randomPort() {
        Random random = new Random();
        int port = 10000 + random.nextInt(55536);
        return String.valueOf(port);
    }
}
