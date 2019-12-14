#!/bin/bash

. test_helper

echo "pre_test: started"
bash ./pre_test
if [ $? -ne 0 ]; then
    echo "pre_test: failed"
    exit 1
else
    echo "pre_test: succeeded"
fi

echo "dad: started"
${DAD_CMD} tasks.clj

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
