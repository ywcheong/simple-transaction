#!/bin/bash

SECRET_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"]
cd $SECRET_DIR

# Generate 4096-bit RSA private key
openssl genpkey -algorithm RSA -out "secret_server_jwt_prvkey" -pkeyopt rsa_keygen_bits:4096

# Extract public key in PEM format
openssl rsa -pubout -in "secret_server_jwt_prvkey" -out "secret_server_jwt_pubkey"

echo "Keys generated:"
echo "Private key: $SECRET_DIR/secret_server_jwt_prvkey"
echo "Public key:  $SECRET_DIR/secret_server_jwt_pubkey"