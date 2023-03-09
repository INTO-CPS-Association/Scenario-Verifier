#!/bin/bash

# Run the docker container
docker run -it --rm -v "${PWD}":/app/bin/user --name scenario_verifier_image  scenario_verifier:latest