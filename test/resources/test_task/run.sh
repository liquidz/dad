#!/bin/bash

echo "pre_test: started"
bash ./pre_test
if [ $? -ne 0 ]; then
    echo "pre_test: failed"
    exit 1
else
    echo "pre_test: succeeded"
fi

echo "dad: started"
ls -1 dad.linux-amd64 > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "dad: using native image"
    ./dad.linux-amd64 tasks.clj
else
    ls -1 dad > /dev/null 2>&1
    if [ $? -eq 0 ]; then
        echo "dad: using native image"
        ./dad tasks.clj
    else
        echo "dad: using JAR"
        java -jar dad.jar tasks.clj
    fi
fi

if [ $? -ne 0 ]; then
    echo "dad: failed"
    exit 1
else
    echo "dad: succeeded"
fi

echo "post_test: started"
bash ./post_test
if [ $? -ne 0 ]; then
    echo "post_test: failed"
    exit 1
else
    echo "post_test: succeeded"
fi

echo "clean up"
bash ./clean_up
