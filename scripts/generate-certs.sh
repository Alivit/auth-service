#!/bin/bash

export MSYS_NO_PATHCONV=1

mkdir -p certs
cd certs

openssl req -x509 -sha256 -days 3650 -nodes -newkey rsa:4096 \
  -subj "//O=Project/CN=ProjectRootCA" \
  -keyout root-ca.key -out root-ca.crt

cat > keycloak.ext << EOF
subjectAltName = DNS:keycloak, DNS:localhost, IP:127.0.0.1
EOF

openssl req -new -newkey rsa:2048 -nodes \
  -subj "//O=Project/CN=keycloak" \
  -keyout keycloak.key -out keycloak.csr

openssl x509 -req -sha256 -days 365 -in keycloak.csr \
  -CA root-ca.crt -CAkey root-ca.key -CAcreateserial \
  -extfile keycloak.ext -out keycloak.crt

openssl req -new -newkey rsa:2048 -nodes \
  -subj "//O=Project/CN=auth-service" \
  -keyout auth-client.key -out auth-client.csr

openssl x509 -req -sha256 -days 365 -in auth-client.csr \
  -CA root-ca.crt -CAkey root-ca.key -CAcreateserial \
  -out auth-client.crt

rm *.csr keycloak.ext root-ca.srl