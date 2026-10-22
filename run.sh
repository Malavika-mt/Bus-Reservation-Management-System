#!/bin/sh
mkdir -p out
javac -cp "lib/*" -d out src/*.java || exit 1
java -cp "out:lib/*" MainApplication "$@"
