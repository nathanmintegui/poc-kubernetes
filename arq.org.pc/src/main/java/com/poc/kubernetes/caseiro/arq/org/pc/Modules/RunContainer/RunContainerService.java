/**
 * MIT License
 * Copyright (c) 2025 Nathan Berger
 * See LICENSE file for more information.
 *
 * @author Nathan Berger
 * @version 1.0
 * @since 2025-05-31
 */

package com.poc.kubernetes.caseiro.arq.org.pc.Modules.RunContainer;

import java.util.Objects;

import com.poc.kubernetes.caseiro.arq.org.pc.Repositories.DockerImageRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * RunContainerService class.
 */
@RequiredArgsConstructor
@Service
public class RunContainerService {

    private final DockerImageRepository dockerImageRepository;

    public String run(String imageName) throws Exception {
        assert (Objects.isNull(imageName) == false);
        assert (imageName.isBlank() == false);

        dockerImageRepository.findByName(imageName)
                .orElseThrow(() -> new Exception("Image not found."));

        var dockerImage = dockerImageRepository.findByName(imageName)
                .orElse(null);

        if (dockerImage == null) {
            return "Image not found";
        }

        return "";
    }
}
