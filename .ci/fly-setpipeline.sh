#!/usr/bin/env bash
fly --target utility set-pipeline -p EvoMaster -c .ci/pipeline.yml
