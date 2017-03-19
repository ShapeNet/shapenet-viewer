[![Build Status](https://travis-ci.org/ShapeNet/shapenet-viewer.svg)](https://travis-ci.org/ShapeNet/shapenet-viewer)
# [ShapeNet](www.shapenet.org) Model Viewer and Renderer

This Java+Scala code was used to render the [ShapeNet](www.shapenet.org) model screenshots and thumbnails.  It can handle loading of OBJ+MTL, COLLADA DAE, KMZ, and PLY format 3D meshes.

This is a realtime OpenGL-based renderer.  If you would like to use a raytracing framework for rendering, then a fork of the [Mitsuba renderer](https://github.com/shi-jian/mitsuba-shapenet) has been created by [Jian Shi](https://github.com/shi-jian) to handle ShapeNet models.

Compiling
=========
[Java JDK 1.8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) is required. Make sure the JDK paths are added to the system path.  Please also ensure that the following commands are in your path: `java`, `make`, `wget`.

For compilation on Linux, just run `make`.

Running the ShapeNet Viewer
===========================
Main class: `edu.stanford.graphics.shapenet.jme3.viewer.Viewer`

Set `WORK_DIR` (where output screenshots are saved) and `SHAPENET_VIEWER_DIR` (to shapenet-viewer checkout) as needed.

Run `scripts/viewer.sh` to start the viewer.  Look in `config/viewer.conf` for general settings and `config/screenshots.conf` for screenshot generation settings used for ShapeNet thumbnails.

Once the viewer has started, press `F1` to bring up the help screen, and `F4` to bring up the command console.  The `help` command can give you an overview, and tab completion is a good way to get a feel for all available options.

Before you use the viewer you must register a local copy of ShapeNetCore by specifying the path to the root directory of the extracted ShapeNetCore archive using:

      register shapeNetCore <path>

After registering the path to your local ShapeNet, you can display models by running one of the following commands:

      load model <modelId>
      load model 3dw.1a04e3eab45ca15dd86060f189eb133
      load model random
      load model random 3dw chair
      
Note that the bicycle synset comes from `yobi3d`, and to display a model from the bicycle synset you would do 

      load model yobi3d.0Ap5HKn9y3
      load model random yobi3d bicycle

We do not currently have textures for the `yobi3d` models.      
      
You can also load models directly from the file system or the web using

      load model <filepath>
      load model <url>
      
You can save screenshots for the currently loaded model using:

      save model screenshots
     
Or you can start a batch process to render all screenshots using:

      save model screenshots all 3dw <category>

where `<category>` can optionally specify a specific category subset (e.g., `chair`).

All screenshots are saved to `$WORK_DIR/screenshots/models` or `$WORK_DIR/screenshots/modelsByCategory/<category>` if the category specifier was used.  If you want to manually specify the base output directory for screenshots use the command:

      set modelScreenshotDir <path>

Any following `save model screenshots` commands will save files under the given path.

Ordering of models rendered in a batch is randomized by default, and existing screenshots are not re-rendered (so it is safe to restart the process without regnerating existing screenshots).  A few important parameters that control the screenshot rendering are given below (along with their default values). These can be set using the command `set <paramName> <value>` or by adding `viewer.<paramName> = <value>` to the `.conf` file:

- `nImagesPerModel = 8` : how many equally-spaced turntable positions (increments of the camera azimuth angle) to render
- `cameraAngleFromHorizontal = 30` : camera elevation in degrees from horizontal (ground) plane
- `includeCanonicalViews = true` : whether to render top, bottom, left, right, front and back views in addition to turntable views
- `cameraPositionStrategy = fit` : options are `distance` (set camera distance from closest object bounding box edge to be equal to `modelDistanceScale * maxModelDimension`) or `fit` (translate camera so that model fits within frame) or `distance_to_centroid` (set camera distance from object centroid to be equal to `modelDistanceScale * maxModelDimension`)
- `addFloor = false` : whether a floor plane should be added below the model

The viewer caches loaded models in memory so if you modify a model and would like to reload it from disk, use the `clear cache` command.

If you would like to switch to loading `KMZ` models from the ShapeNet web server to compare with the `OBJ+MTL` models, use the command `set loadFormat kmz` (and `set loadFormat obj` to revert to default local loading).  The ShapeNet website thumbnails were all rendered from the `KMZ` format models.

ShapeNetSem
===========

You can also register a local copy of ShapeNetSem by specifying the path to the root directory containing all downloaded and extracted ShapeNetSem files:

      register shapeNetSem <path>

The directory structure of ShapeNetSem should look something like this:

     <ShapeNetSem.v0>/metadata.csv
                     /COLLADA/...
                     /models/...
                     /textures/...
      
You can display ShapeNetSem models using the `wss` prefix.

      load model wss.1a4216ac5ffbf1e89c7ce4b816b39bd0
      load model random wss chair

Batch Rendering with the ShapeNet Viewer
===========================

The viewer can take in scripts in a given `.conf` file to perform batch rendering tasks.  See the examples in the `conf` directory, and particulary the `batch-render-example.conf` file.

Contact
=======

Please feel free to contact us if you have any questions about the viewer framework.  Issue reports or pull requests with improvements are welcome!  If you use this framework in your research please cite the [ShapeNet  report](http://shapenet.cs.stanford.edu/resources/shapenet.bib).
