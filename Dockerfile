ARG REPO_DIR=/opt/biopax3-exporter

# ===== stage 1 =====
FROM maven:3.9.6-eclipse-temurin-17-focal AS setup-env
ARG REPO_DIR
WORKDIR ${REPO_DIR}
COPY . .
SHELL ["/bin/bash", "-c"]

# run ktlint if container started
ENTRYPOINT []
CMD mvn verify | grep -i --color=never 'ktlint' > lint.log && exit 1 || exit 0

# ===== stage 2 =====
FROM setup-env AS build-jar
RUN mvn clean package

# ===== stage 3 =====
FROM eclipse-temurin:17-jre-focal
ARG REPO_DIR
ARG JAR_FILE=target/biopax3-exporter-0.0.1-SNAPSHOT.jar
WORKDIR ${REPO_DIR}

COPY --from=build-jar ${REPO_DIR}/${JAR_FILE} ./target/

# Create directory for output files
RUN mkdir -p /data/output

# Set default command to show help
ENTRYPOINT ["java", "-jar", "target/biopax3-exporter-0.0.1-SNAPSHOT.jar"]
CMD ["-h", "localhost", "-b", "7687", "-u", "", "-p", "", "-o", "/data/output"]
