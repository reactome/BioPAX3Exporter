ARG REPO_DIR=/opt/biopax3-exporter

# ===== stage 1 =====
FROM maven:3.9.6-eclipse-temurin-17-focal AS setup-env
ARG REPO_DIR
WORKDIR ${REPO_DIR}
COPY . .
SHELL ["/bin/bash", "-c"]


# ===== stage 2 =====
# Both modules; the command line's shaded jar (cli) is what runs.
FROM setup-env AS build-jar
# -nsu: no snapshot updates mid-build; ci/snapshots.sh checks what resolved.
RUN mvn -B -nsu clean package
# Fail if a snapshot dependency (graph-core) changed under the build (T010).
RUN ci/snapshots.sh check

# ===== stage 3 =====
FROM eclipse-temurin:17-jre-focal
ARG REPO_DIR
ARG JAR_FILE=cli/target/biopax3-exporter-0.0.1-SNAPSHOT.jar
WORKDIR ${REPO_DIR}

COPY --from=build-jar ${REPO_DIR}/${JAR_FILE} ./target/

# Create directory for output files
RUN mkdir -p /data/output

# Set default command to show help
ENTRYPOINT ["java", "-jar", "target/biopax3-exporter-0.0.1-SNAPSHOT.jar"]
CMD ["-h", "localhost", "-b", "7687", "-u", "", "-p", "", "-o", "/data/output"]
