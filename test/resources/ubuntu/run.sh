#!/bin/bash

cd /root

bash ./pre_test
if [ $? -ne 0 ]; then
    echo "pre_test: failed"
    exit 1
else
    echo "pre_test: succeeded"
fi

which dad
if [ $? -eq 0 ]; then
    dad tasks.clj
else
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
