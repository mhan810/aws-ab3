#!/bin/bash

yum update -yqe 0
yum install -y jq

SECRETS_FILE=/tmp/aws_secrets.out

aws secretsmanager get-secret-value --secret-id OctankDatabaseSecret3E0A356-8hHDehGriakF \
        --query 'SecretString' --region us-east-1 --output text > ${SECRETS_FILE}

HOST=$(jq .host ${SECRETS_FILE} | tr -d '"')
PASSWORD=$(jq .password ${SECRETS_FILE} | tr -d '"')
USERNAME=$(jq .username ${SECRETS_FILE} | tr -d '"')

rm ${SECRETS_FILE}

TOMCAT_ENV=/etc/init.d/tomcat_env

echo "export LIFERAY_JDBC_PERIOD_DEFAULT_PERIOD_PASSWORD=\"${PASSWORD}\"" > ${TOMCAT_ENV}
echo "export LIFERAY_JDBC_PERIOD_DEFAULT_PERIOD_URL=\"jdbc:sqlserver://${HOST};databaseName=liferay\"" >> ${TOMCAT_ENV}
echo "export LIFERAY_JDBC_PERIOD_DEFAULT_PERIOD_USERNAME=${USERNAME}" >> ${TOMCAT_ENV}