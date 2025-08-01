#!/bin/bash
REPO_DIR="build/repo/io/micronaut/oraclecloud"
for dir in "$REPO_DIR"/*/; do
  project_name=$(basename "$dir")
  ./gradlew ":$project_name:publishToSonatype"
  sleep 2
done
