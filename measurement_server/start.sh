#!/bin/bash

#exec >> /var/log/mobiperf 2>&1
echo "####### Running /home/michigan_1/init/start.sh at `date` ########"

#cd /home/michigan_1/mobiperf

services="Downlink:12001:tcp Uplink:12002:tcp ServerConfig:12003:tcp UDPServer:31341:udp"

start() {
        echo "Attempting to start $1 ..."
        java -Xmx128M -jar mlab/$1.jar &
        sleep 2
        jarpid=$!
}

for service in $services
do
        IFS=':' read jar port proto <<< "$service"
        start $jar
        started=no
        for i in {1..5}; do
                if [ -n $jarpid ] && ps $jarpid &> /dev/null ; then
                        ncopt=""
                        if [ $proto == "udp" ]; then
                                ncopt="-u"
                        fi
                        if nc -z $ncopt localhost $port >> /dev/null; then
                                started=yes
                                echo "${jar} successfully started and listening on port ${port}."
                                break
                        else
                                echo "${jar} not listening on port ${port}.  Sleeping for ${i}s before trying again."
                                sleep $i
                        fi
                else
                        echo "${jar} seeems to have crashed."
                        start $jar
                fi
        done

        if [ $started == "no" ]; then
                echo "${jar} failed to start for some reason."
                exit 1
        fi
done

sudo tcpdump -i eth0 -e -tt -vvv -s 100 -U -G 86400 -w 'tcpdump-%s' 'tcp and (port 12001 or port 12002)'
