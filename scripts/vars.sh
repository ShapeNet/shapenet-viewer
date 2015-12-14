#!/bin/bash

# Set up environment variables common to a number of Shapenet viewer
# executables.
#
# Normally this shouldn't be run directly, but instead source'd in
# other shell scripts.

JAVA=java
SCALA=scala

BIN=`dirname $0`
BASE=$BIN/..
CONFIG=$BASE/config
# Set shapnet viewer dir to checkout
export SHAPENET_VIEWER_DIR=$BASE/

# Built jar with all dependencies (and main class is edu.stanford.graphics.shapenet.jme3.viewer.Viewer)
SHAPENET_VIEWER_TARGET_DIR=$BASE/target/scala-2.11/
SHAPENET_VIEWER_JAR=$SHAPENET_VIEWER_TARGET_DIR/shapenet-viewer-assembly-1.0-SNAPSHOT.jar

SHAPENET_VIEWER_DEPS_JAR=$SHAPENET_VIEWER_TARGET_DIR/shapenet-viewer-assembly-1.0-SNAPSHOT-deps.jar
SHAPNET_VIEWER_CLASSES=$SHAPENET_VIEWER_TARGET_DIR/classes

# Data dir for shapenet data on dovahkiin
#export DATA_DIR=/mnt/etherion/data
if [ `hostname` = "dovahkiin" ] ; then
  export DATA_DIR=/media/data
  export USE_LOCAL_DATA=true
  echo "Using local data with DATA_DIR=$DATA_DIR"
fi

# Classpath
CLASSPATH=$SHAPENET_VIEWER_JAR
