#!/bin/bash

yum update -y -q -e 0

##
## do not need to install because image already has them
#yum install -y amazon-cloudwatch-agent
#yum install -y https://s3.amazonaws.com/ec2-downloads-windows/SSMAgent/latest/linux_arm64/amazon-ssm-agent.rpm
#yum install -y httpd
#yum install -y mod_ssl

#sudo systemctl enable amazon-ssm-agent
#sudo systemctl start amazon-ssm-agent

# Install jq to manipulate json
yum install -y jq

echo "{\"agent\":{\"metrics_collection_interval\":30,\"run_as_user\":\"root\"},\"logs\":{\"logs_collected\":{\"files\":{\"collect_list\":[{\"file_path\":\"/opt/liferay/current/tomcat-9.0.37/logs/catalina.out\",\"log_group_name\":\"AgentPortal\",\"log_stream_name\":\"{instance_id}\"}]}}},\"metrics\":{\"append_dimensions\":{\"AutoScalingGroupName\":\"\${aws:AutoScalingGroupName}\",\"ImageId\":\"\${aws:ImageId}\",\"InstanceId\":\"\${aws:InstanceId}\",\"InstanceType\":\"\${aws:InstanceType}\"},\"metrics_collected\":{\"cpu\":{\"measurement\":[\"cpu_usage_idle\",\"cpu_usage_iowait\",\"cpu_usage_user\",\"cpu_usage_system\"],\"metrics_collection_interval\":30,\"resources\":[\"*\"],\"totalcpu\":false},\"disk\":{\"measurement\":[\"used_percent\",\"inodes_free\"],\"metrics_collection_interval\":30,\"resources\":[\"*\"]},\"diskio\":{\"measurement\":[\"io_time\",\"write_bytes\",\"read_bytes\",\"writes\",\"reads\"],\"metrics_collection_interval\":30,\"resources\":[\"*\"]},\"mem\":{\"measurement\":[\"mem_used_percent\"],\"metrics_collection_interval\":30},\"netstat\":{\"measurement\":[\"tcp_established\",\"tcp_time_wait\"],\"metrics_collection_interval\":30},\"swap\":{\"measurement\":[\"swap_used_percent\"],\"metrics_collection_interval\":30}}}}" > /opt/aws/amazon-cloudwatch-agent/bin/config.json

/opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl -a fetch-config -m ec2 -s -c file:/opt/aws/amazon-cloudwatch-agent/bin/config.json

SECRETS_FILE=/tmp/aws_secrets.out

aws secretsmanager get-secret-value --secret-id %DB_SECRET_NAME% \
        --query 'SecretString' --region us-east-1 --output text > ${SECRETS_FILE}

HOST=$(jq .host ${SECRETS_FILE} | tr -d '"')
PASSWORD=$(jq .password ${SECRETS_FILE} | tr -d '"')
USERNAME=$(jq .username ${SECRETS_FILE} | tr -d '"')

rm ${SECRETS_FILE}

TOMCAT_ENV=/etc/init.d/tomcat_env

echo "export LIFERAY_JDBC_PERIOD_DEFAULT_PERIOD_PASSWORD=\"${PASSWORD}\"" > ${TOMCAT_ENV}
echo "export LIFERAY_JDBC_PERIOD_DEFAULT_PERIOD_URL=\"jdbc:sqlserver://${HOST};databaseName=liferay\"" >> ${TOMCAT_ENV}
echo "export LIFERAY_JDBC_PERIOD_DEFAULT_PERIOD_USERNAME=${USERNAME}" >> ${TOMCAT_ENV}

echo "export LIFERAY_WEB_PERIOD_SERVER_PERIOD_FORWARDED_PERIOD_HOST_PERIOD_ENABLED=true" >> ${TOMCAT_ENV}
echo "export LIFERAY_WEB_PERIOD_SERVER_PERIOD_FORWARDED_PERIOD_PROTOCOL_PERIOD_ENABLED=true" >> ${TOMCAT_ENV}
echo "export LIFERAY_REDIRECT_PERIOD_URL_PERIOD_SECURITY_PERIOD_MODE=domain" >> ${TOMCAT_ENV}
echo "export LIFERAY_REDIRECT_PERIOD_URL_PERIOD_DOMAINS_PERIOD_ALLOWED=*.amazonaws.com" >> ${TOMCAT_ENV}

/etc/init.d/tomcat start