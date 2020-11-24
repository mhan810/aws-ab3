#!/bin/bash

yum update -yqe 0
yum install -y httpd
yum install -y mod_ssl

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

echo "ProxyPass / \"http://%ALB_URL%/\"" > /etc/httpd/conf.d/proxy.conf
echo "ProxyPassReverse / \"http://%ALB_URL%/\"" >> /etc/httpd/conf.d/proxy.conf

sudo systemctl start httpd
sudo systemctl enable httpd
