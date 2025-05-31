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

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

/**
 * RunContainerController class.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/api/containers")
public class RunContainerController {

    private final RunContainerService runContainerService;

    @GetMapping("/status")
    public ResponseEntity<String> Status() {
        return new ResponseEntity<String>("up", HttpStatus.OK);
    }

    /**
     * Run a new docker container by image name. </br>
     *
     * If the image is not found it returns NotFound.
     *
     * @return ResponseEntity<Void>
     * @throws Exception
     */
    @PostMapping("/up")
    public ResponseEntity<Void> up(@RequestParam String imageName) throws Exception {

        var response = runContainerService.run(imageName);

        return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
    }
}
