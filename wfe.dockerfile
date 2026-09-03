######## MUST be in sync with Dockerfile in this folder
FROM amazoncorretto:21-alpine-jdk

COPY core/target/evomaster.jar .


######## MUST be in sync with what used in WFE
ENTRYPOINT ["/bin/sh", "-c", \
    "java  \
    -jar /evomaster.jar \
    --runningInDocker true \
    --showProgress false \
    --maxTime ${TIME_SECONDS}s \
    --schema ${SCHEMA_FILE_PATH} \
    --base http://${HOST}:${PORT} \
    --outputFolder ${OUTPUT_FOLDER} \
    ${AUTH_FILE_PATH:+--configPath} ${AUTH_FILE_PATH} \
    ${AUTH_EXTERNAL_OVERRIDE:+--overrideAuthExternalEndpointURL} ${AUTH_EXTERNAL_OVERRIDE} \
    ${EXTRA_CONFIGS} \
    " \
]


# docker build -t webfuzzing/wfe-snapshot:<wfe-version>  -f wfe.dockerfile .