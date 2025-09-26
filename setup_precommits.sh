#!/bin/bash -xe
# Sets up pre-commit hooks for secret scanning

# Ensure we're in the repo's directory
SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)
cd $SCRIPT_DIR

# Install dependencies
# (gitleaks and trufflehog aren't necessary for the hooks, but this will make
# them available locally to be ran if required)
#
# pre-commit framework: https://pre-commit.com/
# Gitleaks: https://gitleaks.io/
# TruffleHog: https://trufflesecurity.com/trufflehog
# GitGuardian Shield: https://github.com/GitGuardian/ggshield
brew install pre-commit gitleaks trufflehog ggshield

# Install pre-commit hooks and set up hook environments
pre-commit install --install-hooks

# Update pre-commit hook configs from their remotes
pre-commit autoupdate