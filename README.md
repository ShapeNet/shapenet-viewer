[![Build Status](https://travis-ci.org/ShapeNet/shapenet-viewer.svg)](https://travis-ci.org/ShapeNet/shapenet-viewer)
# [ShapeNet](www.shapenet.org) Model Viewer and Renderer

This Java+Scala code was used to render the [ShapeNet](www.shapenet.org) model screenshots and thumbnails.  It can handle loading of OBJ+MTL, COLLADA DAE, KMZ, and PLY format 3D meshes.

This is a realtime OpenGL-based renderer.  If you would like to use a raytracing framework for rendering, then a fork of the [Mitsuba renderer](https://github.com/shi-jian/mitsuba-shapenet) has been created by [Jian Shi](https://github.com/shi-jian) to handle ShapeNet models.

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

Once the viewer has started, press 'F1' to bring up the help screen, and 'F4' to bring up the console.  

By default, KMZ model are loaded from the web and cached in `WORK_DIR/cache`.  

If you already have a local copy of ShapeNetCore, you specify the location of ShapeNetCore by:

      register shapeNetCore <path>
      
After registering the path to your local ShapeNet, you can display models by running one of the following commands:

      load model <modelId>
      load model random
      load model random 3dw chair
      
You can also load models directly from the file system or the web using

      load model <filepath>
      load model <url>
      
You can save screenshots for the currently loaded model using:

      save model screenshots
     
