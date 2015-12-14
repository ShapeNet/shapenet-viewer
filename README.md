# [ShapeNet](www.shapenet.org) Model Viewer and Renderer

This Java+Scala code was used to render the [ShapeNet](www.shapenet.org) model screenshots and thumbnails.  It can handle loading of OBJ+MTL, COLLADA DAE, KMZ, and PLY format 3D meshes.

Requirements
========
[Java JDK 1.8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html). Make sure the JDK paths are added to the system path.

Compiling on Linux
==================
For compilation on Linux, you will need to have the Oracle Java JDK 1.8 installed.
You will need to have the following commands on your path: `java`, `make`, `wget`.

Then run `make`.

Running the ShapeNet Viewer
===========================
Main class: `edu.stanford.graphics.shapenet.jme3.viewer.Viewer`

Set `WORK_DIR` (where output screenshots are saved) and `SHAPENET_VIEWER_DIR` (to shapenet-viewer checkout) as needed.

Run `scripts/viewer.sh` to start the viewer.  Look in `config/viewer.conf` for general settings and `config/screenshots.conf` for screenshot generation settings used for ShapeNet thumbnails.
