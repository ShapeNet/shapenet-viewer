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
SHAPENET_VIEWER_JAR=$SHAPENET_VIEWER_TARGET_DIR/shapenet-viewer-assembly-0.1.0.jar

SHAPENET_VIEWER_DEPS_JAR=$SHAPENET_VIEWER_TARGET_DIR/shapenet-viewer-assembly-0.1.0-deps.jar
SHAPNET_VIEWER_CLASSES=$SHAPENET_VIEWER_TARGET_DIR/classes

# Classpath
CLASSPATH=$SHAPENET_VIEWER_JAR
