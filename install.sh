#!/bin/bash
set -e

JSTACHE_VERSION=1.0-SNAPSHOT
JSTACHE_CONF=/usr/lib/jstache-$JSTACHE_VERSION/conf/config.properties

# Identify Linux Flavour
if [ -f /etc/debian_version ] ; then
    PGROUP="nobody:nogroup"
elif [ -f /etc/redhat-release ] ; then
    PGROUP="nobody:nobody"
else
    PGROUP="nobody:nogroup"
fi


if [ -f $JSTACHE_CONF ]
then
    cp $JSTACHE_CONF $JSTACHE_CONF.bak
fi

mvn -fpom.xml clean assembly::assembly

INSTALL_ROOT=$(pwd)/target
echo 'Installing JSTACHE Server ...' 
cd /usr/lib && tar -xvzf $INSTALL_ROOT/jstache-$JSTACHE_VERSION.tar.gz 
mkdir -p /var/run/jstache/ 
touch /var/run/jstache/jstached.pid 
chown $PGROUP /var/run/jstache/ 
chown $PGROUP /var/run/jstache/jstached.pid 
mkdir -p /var/log/jstache/ 
chmod 777 /var/log/jstache/ 
chown -R $PGROUP /usr/lib/jstache-$JSTACHE_VERSION/ 
ln -sf /usr/lib/jstache-$JSTACHE_VERSION/jstached /etc/init.d/jstached 

if [ -f $JSTACHE_CONF.bak ]
then
    echo "JStache installed, retained settings in /usr/lib/jstache-$JSTACHE_VERSION/conf/server.properties"
    cp $JSTACHE_CONF $JSTACHE_CONF.clean
    mv $JSTACHE_CONF.bak $JSTACHE_CONF
else
    echo "JStache installed, don't forget to update /usr/lib/jstache-$JSTACHE_VERSION/conf/server.properties"
fi
