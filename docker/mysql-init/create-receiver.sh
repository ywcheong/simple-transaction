#!/bin/bash
set -euo pipefail

ROOT_PW=$(cat /run/secrets/secret_mysql_root_password)
METRIC_PW=$(cat /run/secrets/secret_mysql_metric_password)

mysql -uroot -p"$ROOT_PW" <<-EOSQL

CREATE USER IF NOT EXISTS 'streceiver'@'%' IDENTIFIED BY '$METRIC_PW';

GRANT SELECT ON performance_schema.* TO 'streceiver'@'%';
GRANT SELECT ON performance_schema.table_io_waits_summary_by_table TO 'streceiver'@'%';
GRANT SELECT ON performance_schema.table_io_waits_summary_by_index_usage TO 'streceiver'@'%';
GRANT SELECT ON performance_schema.events_statements_summary_by_digest TO 'streceiver'@'%';
GRANT SELECT ON performance_schema.table_lock_waits_summary_by_table TO 'streceiver'@'%';
GRANT REPLICATION CLIENT ON *.* TO 'streceiver'@'%';

FLUSH PRIVILEGES;

EOSQL
