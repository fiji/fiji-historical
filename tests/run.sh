#!/bin/sh

# Tests for fiji

# Run all jython tests contained in this current folder
#
CLASSPATH=.:../ij.jar:$(find ../jars/ -name "*.jar" -exec printf ":%s" {} \;)$(find ../plugins/ -name "*.jar" -exec printf ":%s" {} \;)
java -Dpython.home="../jars/jython21" -Dplugins.dir=".." -classpath $CLASSPATH org.python.util.jython *.py
