#!/bin/bash

yum update -yqe 0
yum install -y amazon-cloudwatch-agent
yum install -y https://s3.amazonaws.com/ec2-downloads-windows/SSMAgent/latest/linux_arm64/amazon-ssm-agent.rpm
yum install -y httpd
yum install -y mod_ssl

sudo systemctl enable amazon-ssm-agent
sudo systemctl start amazon-ssm-agent

echo "{\"agent\":{\"metrics_collection_interval\":60,\"run_as_user\":\"root\"},\"logs\":{\"logs_collected\":{\"files\":{\"collect_list\":[{\"file_path\":\"/var/log/httpd/ssl_access_log\",\"log_group_name\":\"ssl_access_log\",\"log_stream_name\":\"{instance_id}\"},{\"file_path\":\"/var/log/httpd/ssl_error_log\",\"log_group_name\":\"ssl_error_log\",\"log_stream_name\":\"{instance_id}\"},{\"file_path\":\"/var/log/httpd/ssl_request_log\",\"log_group_name\":\"ssl_request_log\",\"log_stream_name\":\"{instance_id}\"}]}}},\"metrics\":{\"append_dimensions\":{\"AutoScalingGroupName\":\"\${aws:AutoScalingGroupName}\",\"ImageId\":\"\${aws:ImageId}\",\"InstanceId\":\"\${aws:InstanceId}\",\"InstanceType\":\"\${aws:InstanceType}\"},\"metrics_collected\":{\"cpu\":{\"measurement\":[\"cpu_usage_idle\",\"cpu_usage_iowait\",\"cpu_usage_user\",\"cpu_usage_system\"],\"metrics_collection_interval\":60,\"resources\":[\"*\"],\"totalcpu\":false},\"disk\":{\"measurement\":[\"used_percent\",\"inodes_free\"],\"metrics_collection_interval\":60,\"resources\":[\"*\"]},\"diskio\":{\"measurement\":[\"io_time\",\"write_bytes\",\"read_bytes\",\"writes\",\"reads\"],\"metrics_collection_interval\":60,\"resources\":[\"*\"]},\"mem\":{\"measurement\":[\"mem_used_percent\"],\"metrics_collection_interval\":60},\"netstat\":{\"measurement\":[\"tcp_established\",\"tcp_time_wait\"],\"metrics_collection_interval\":60},\"swap\":{\"measurement\":[\"swap_used_percent\"],\"metrics_collection_interval\":60}}}}" > /opt/aws/amazon-cloudwatch-agent/bin/config.json

/opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl -a fetch-config -m ec2 -s -c file:/opt/aws/amazon-cloudwatch-agent/bin/config.json

umask 077

answers() {
        echo --
        echo Illinois
        echo Chicago
        echo Octank
        echo SomeOrganizationalUnit
        echo localhost.localdomain
        echo root@localhost.localdomain
}

PEM1=`/bin/mktemp /tmp/openssl.XXXXXX`
PEM2=`/bin/mktemp /tmp/openssl.XXXXXX`
trap "rm -f $PEM1 $PEM2" SIGINT
answers | /usr/bin/openssl req -newkey rsa:2048 -keyout $PEM1 -nodes -x509 -days 365 -out $PEM2 2> /dev/null
cat $PEM1 >  /etc/ssl/certs/localhost.key
cat $PEM2 > /etc/ssl/certs/localhost.crt
rm -f $PEM1 $PEM2

[[ ! -d "/etc/pki/tls/certs" ]] && mkdir /etc/pki/tls/certs
[[ ! -d "/etc/pki/tls/private" ]] && mkdir /etc/pki/tls/private

[[ ! -f "/etc/pki/tls/certs/localhost.crt" ]] && cp /etc/ssl/certs/localhost.crt /etc/pki/tls/certs/localhost.crt
[[ ! -f "/etc/pki/tls/private/localhost.key" ]] && cp /etc/ssl/certs/localhost.key /etc/pki/tls/private/localhost.key

umask 022

echo "RequestHeader set X-Forwarded-Proto \"https\"" > /etc/httpd/conf.d/proxy.conf
echo "ProxyPreserveHost on" >> /etc/httpd/conf.d/proxy.conf
echo "ProxyPass / \"http://%ALB_URL%/\"" >> /etc/httpd/conf.d/proxy.conf
echo "ProxyPassReverse / \"http://%ALB_URL%/\"" >> /etc/httpd/conf.d/proxy.conf

sudo systemctl start httpd
sudo systemctl enable httpd
