#!/usr/bin/env bash
fly --target utility set-pipeline -p utility-java-framework -c .ci/pipeline.yml
