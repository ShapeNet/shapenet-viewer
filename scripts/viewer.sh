#!/bin/bash

# Example bash script to viewer from jar
BIN=`dirname $0`
source ${BIN}/vars.sh

PROPS=${1:-$CONFIG/viewer.conf}

# Arguments as bash array
ARGS=("-conf" "$PROPS")

# Run viewer with maximum 
# Use -Xmx10g to specify amount of memory to use
# Use "${ARGS[@]}" so arguments are expanded to "$1" "$2" ... preserving spaces
date
$JAVA -jar $SHAPENET_VIEWER_JAR ${ARGS[@]}
date
