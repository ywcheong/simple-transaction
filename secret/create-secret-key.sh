#!/bin/bash

KEY_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Generate 4096-bit RSA private key
openssl genpkey -algorithm RSA -out "$KEY_DIR/secret_server_jwt_prvkey" -pkeyopt rsa_keygen_bits:4096

# Extract public key in PEM format
openssl rsa -pubout -in "$KEY_DIR/secret_server_jwt_prvkey" -out "$KEY_DIR/secret_server_jwt_pubkey"

echo "Keys generated:"
echo "Private key: $KEY_DIR/secret_server_jwt_prvkey"
echo "Public key:  $KEY_DIR/secret_server_jwt_pubkey"