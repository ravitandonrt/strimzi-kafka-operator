#!/usr/bin/env bash
#brew install kubernetes-helm
source ./ns-tiller.sh

kubectl config current-context

#helm init --upgrade --service-account tiller --history-max 200
helm init --client-only
helm repo update --debug
