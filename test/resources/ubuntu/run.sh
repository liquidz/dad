#!/bin/bash

cd /root

bash ./pre_test
if [ $? -ne 0 ]; then
    echo "pre_test: failed"
    exit 1
else
    echo "pre_test: succeeded"
fi

ls -1 dad.linux-amd64 > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "dad: using native image"
    ./dad.linux-amd64 tasks.clj
else
    echo "dad: using JAR"
    java -jar daddy.jar tasks.clj
fi
if [ $? -ne 0 ]; then
    echo "dad: failed"
    exit 1
else
    echo "dad: succeeded"
fi

bash ./post_test
if [ $? -ne 0 ]; then
    echo "post_test: failed"
    exit 1
else
    echo "post_test: succeeded"
fi
