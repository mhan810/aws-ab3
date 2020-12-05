#!/bin/bash

yum update -y -q -e 0
yum install -y amazon-cloudwatch-agent
yum install -y https://s3.amazonaws.com/ec2-downloads-windows/SSMAgent/latest/linux_arm64/amazon-ssm-agent.rpm

sudo systemctl enable amazon-ssm-agent
sudo systemctl start amazon-ssm-agent
