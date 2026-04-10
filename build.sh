#!/bin/bash
find $PWD/src -name '*java' > sources.txt
javac -cp "lib/*" -sourcepath src -d bin/ -encoding utf-8 -g @sources.txt
# to execute
# java -cp "bin:lib/*" <main class>