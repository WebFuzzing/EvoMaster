FROM amazoncorretto:21-alpine-jdk

COPY core/target/evomaster.jar .

ENTRYPOINT [  \
    "java", \
    "-Xmx4G", \
    "-jar", "evomaster.jar", \
    "--runningInDocker", "true" \
]


###################
###### NOTES ######
###################
# Build
# docker build -t webfuzzing/evomaster  .
#
# Run
# docker run webfuzzing/evomaster  <options>
#
# Publish (latest, otherwise tag with :TAG)
# docker login
# docker push webfuzzing/evomaster
#
# Example remote BB
# docker run -v "/$(pwd)/generated_tests":/generated_tests webfuzzing/evomaster --blackBox true --bbSwaggerUrl https://api.apis.guru/v2/openapi.yaml  --outputFormat JAVA_JUNIT_4 --maxTime 10s --ratePerMinute 60
#
# Example local BB
# docker run -v "/$(pwd)/generated_tests":/generated_tests  webfuzzing/evomaster  --blackBox true --bbSwaggerUrl http://host.docker.internal:8080/v3/api-docs --maxTime 5s
#
# Example WB  (NOT IMPLEMENTED YET)
# docker run -v "/$(pwd)/generated_tests":/generated_tests  webfuzzing/evomaster --dockerLocalhost true
#
# Setting for existing em.yaml
# -v "/$(pwd)/em.yaml":/em.yaml
#
# Debugging
# docker run -it --entrypoint sh  webfuzzing/evomaster
#
#
#
#
#
#
#