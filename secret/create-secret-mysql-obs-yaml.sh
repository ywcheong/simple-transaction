#!/bin/bash

SECRET_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd $SECRET_DIR

SECRET_SOURCE="secret_mysql_metric_password"
SECRET_TARGET="secret_mysql_metric_password_yaml"

if [ ! -f "$SECRET_SOURCE" ]; then
  echo "Error: Secret file '$SECRET_SOURCE' not found!"
  exit 1
fi

SECRET_CONTENTS=$(cat "$SECRET_SOURCE" | tr -d '\n')
cat <<EOF > "$SECRET_TARGET"
receivers:
  mysql:
    password: $SECRET_CONTENTS
EOF

if [ -f "$SECRET_TARGET" ]; then
  echo "${SECRET_DIR}/$0: Success"
else
  echo "${SECRET_DIR}/$0: Failed!!"
  exit 1
fi
