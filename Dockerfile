FROM amazoncorretto:17-alpine-jdk

COPY core/target/evomaster.jar .

#Keep --add-opens in sync with makeExecutable.sh
ENTRYPOINT [  \
    "java", \
    "-Xmx4G", \
    "--add-opens", "java.base/java.net=ALL-UNNAMED", \
    "-jar", "evomaster.jar", \
    # see https://www.howtogeek.com/devops/how-to-connect-to-localhost-within-a-docker-container/
    "--sutControllerHost",  "host.docker.internal" \
]