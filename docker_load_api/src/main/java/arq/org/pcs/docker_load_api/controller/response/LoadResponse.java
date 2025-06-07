package arq.org.pcs.docker_load_api.controller.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter @Setter
public class LoadResponse {

    private String port;
    private int seconds;
    private String response;
}
