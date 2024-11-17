#!/bin/bash
set -e

TAG='jo6r/qr:0.1'

docker build -t $TAG .
docker login -u jo6r
docker push $TAG
