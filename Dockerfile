FROM alpine:latest

# Instala bash, docker client e dependências necessárias
RUN apk update && \
    apk add --no-cache \
    openjdk21 \
    bash \
    docker-cli \
    curl

# Define JAVA_HOME
ENV JAVA_HOME=/usr/lib/jvm/java-21-openjdk \
    PATH="$JAVA_HOME/bin:$PATH"

# Define o diretório de trabalho
WORKDIR /app

# Copia sua aplicação para dentro do container
COPY docker-manager-backend/ /app/docker-manager-backend/

# Comando padrão: abre o shell
CMD ["sh"]


