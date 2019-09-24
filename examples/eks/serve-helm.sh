#!/usr/bin/env bash
source ./helm.sh

helm serve --debug --repo-path ~/.helm/charts
