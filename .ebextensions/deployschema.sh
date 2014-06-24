#!/bin/sh
/usr/bin/mysql \
    -u $RDS_USERNAME \
    -p$RDS_PASSWORD \
    -h $RDS_HOSTNAME \
    $RDS_DB_NAME \
    -e 'create table if not exists counter (name varchar(255) primary key, amount int not null default 0)'