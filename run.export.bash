#!/bin/bash

$MONGO_DIR/bin/mongoexport --db sbtest --collection sbtest1 --out ~/timbo.txt
