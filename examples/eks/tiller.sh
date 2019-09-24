#!/usr/bin/env bash
source ./ns-tiller.sh

kubectl create namespace $TILLER_NAMESPACE
tiller -listen=localhost:44134 -storage=secret -logtostderr
