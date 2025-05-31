/**
 * MIT License
 * Copyright (c) 2025 Nathan Berger
 * See LICENSE file for more information.
 *
 * @author Nathan Berger
 * @version 1.0
 * @since 2025-05-31
 */

package com.poc.kubernetes.caseiro.arq.org.pc.Models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * DockerImage class model.
 */
@Entity
@Table(name = "docker_images")
public class DockerImage {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Integer id;

	@Column(name = "name")
	private String name;

	public DockerImage() {
	}

	public DockerImage(Integer id, String name) {
		this.id = id;
		this.name = name;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
