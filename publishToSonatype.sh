#!/bin/bash

REPO_DIR="build/repo/io/micronaut/oraclecloud"

for dir in "$REPO_DIR"/*/; do
  # Get the last part of the path, e.g., micronaut-oraclecloud-bmc-accessgovernancecp
  project_name=$(basename "$dir")

  echo "Publishing $project_name ..."
  ./gradlew ":$project_name:publishToSonatype"

  echo "Sleeping for 2 seconds..."
  sleep 2
done
