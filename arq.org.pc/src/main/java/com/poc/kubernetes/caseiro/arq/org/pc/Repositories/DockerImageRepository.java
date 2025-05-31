/**
 * MIT License
 * Copyright (c) 2025 Nathan Berger
 * See LICENSE file for more information.
 *
 * @author Nathan Berger
 * @version 1.0
 * @since 2025-05-31
 */

package com.poc.kubernetes.caseiro.arq.org.pc.Repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.poc.kubernetes.caseiro.arq.org.pc.Models.DockerImage;

@Repository
public interface DockerImageRepository extends JpaRepository<DockerImage, Integer> {

	Optional<DockerImage> findByName(String name);
}
