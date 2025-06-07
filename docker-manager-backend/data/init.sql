DROP TABLE IF EXISTS STATUS_CONTAINERS, containers, imagens CASCADE;

CREATE TABLE imagens
(
    id          INT AUTO_INCREMENT PRIMARY KEY,
    nome        VARCHAR(255) NOT NULL UNIQUE,
    max_cpu_usage DOUBLE NOT NULL DEFAULT 80,
    max_ram_usage DOUBLE NOT NULL DEFAULT 512,
    min_replica INT          NOT NULL DEFAULT 1,
    max_replica INT          NOT NULL DEFAULT 5
);

CREATE INDEX idx_imagens_nome ON imagens (nome);


CREATE TABLE containers
(
    id           INT AUTO_INCREMENT PRIMARY KEY,
    id_container VARCHAR(255) NOT NULL UNIQUE,
    id_imagem    INT          NOT NULL,
    num_port     VARCHAR(5)   NOT NULL UNIQUE,
    nome         VARCHAR(255) NOT NULL UNIQUE,
    status       VARCHAR(10)  NOT NULL CHECK (status IN ('UP', 'DOWN')),
    FOREIGN KEY (id_imagem) REFERENCES imagens (id)
);

CREATE INDEX idx_containers_id_imagem ON containers (id_imagem);
CREATE INDEX idx_containers_status ON containers (status);


CREATE TABLE STATUS_CONTAINERS
(
    id           INT AUTO_INCREMENT PRIMARY KEY,
    id_container INT       NOT NULL,
    cpu_usage DOUBLE NOT NULL,
    ram_usage DOUBLE NOT NULL,
    date         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_container) REFERENCES containers (id)
);

CREATE INDEX idx_status_container_id_container ON STATUS_CONTAINERS (id_container);
CREATE INDEX idx_status_container_date ON STATUS_CONTAINERS (date);
