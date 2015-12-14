package jme3dae.collada14;

/**
 * Holds constants defined in the collada spec and used by the parser.
 *
 * @author pgi
 */
public interface ColladaSpec141 {

  /**
   * FXSurfaceType declared in collada spec
   */
  enum FXSurfaceType {
    TYPE_UNTYPED,
    TYPE_1D,
    TYPE_2D,
    TYPE_3D,
    TYPE_CUBE,
    TYPE_DEPTH,
    TYPE_RECT,
  }

  /**
   * Node type enumeration
   */
  enum NodeType {
    JOINT, NODE
  }

  /**
   * Semantics of the collada spec
   */
  enum Semantic {
    BINORMAL,
    COLOR,
    CONTINUITY,
    IMAGE,
    INPUT,
    IN_TANGENT,
    INTERPOLATION,
    INV_BIND_MATRIX,
    JOINT,
    LINEAR_STEPS,
    MORPH_TARGET,
    MORPH_WEIGHT,
    NORMAL,
    OUTPUT,
    OUT_TANGENT,
    POSITION,
    TANGENT,
    TEXBINORMAL,
    TEXCOORD,
    TEXTANGENT,
    UV,
    VERTEX,
    WEIGHT
  }

  /**
   * Names declared in the spec and used by the parser
   */
  interface Names {
    String OUTPUT = "output";
    String CHANNEL = "channel";
    String SAMPLER = "sampler";
    String ANIMATION = "animation";
    String LIBRARY_ANIMATIONS = "library_animations";
    String CONSTANT = "constant";
    String UP_AXIS = "up_axis";
    String AUTHORING_TOOL = "authoring_tool";
    String CONTRIBUTOR = "contributor";
    String METER = "meter";
    String UNIT = "unit";
    String PROFILE = "profile";
    String SID = "sid";
    String LAMBERT = "lambert";
    String LIBRARY_IMAGES = "library_images";
    String EFFECT = "effect";
    String DATA = "data";
    String VCOUNT = "vcount";
    String BLINN = "blinn";
    String SKELETON = "skeleton";
    String SKIN = "skin";
    String BIND_SHAPE_MATRIX = "bind_shape_matrix";
    String JOINTS = "joints";
    String VERTEX_WEIGHTS = "vertex_weights";
    String V = "v";
    String MORPH = "morph";
    String FORMAT = "format";
    String WRAP_S = "wrap_s";
    String WRAP_T = "wrap_t";
    String MINFILTER = "minfilter";
    String MAGFILTER = "magfilter";
    String MIPFILTER = "mipfilter";
    String BORDER_COLOR = "border_color";
    String MIPMAP_MAXLEVEL = "mipmap_maxlevel";
    String MIPMAP_BIAS = "mipmap_bias";
    String TEXTURE = "texture";
    String TEXCOORD = "texcoord";
    String CONSTANT_ATTENUATION = "constant_attenuation";
    String LINEAR_ATTENUATION = "linear_attenuation";
    String QUADRATIC_ATTENUATION = "quadratic_attenuation";
    String FALLOF_ANGLE = "falloff_angle";
    String FALLOF_EXPONENT = "fallof_exponent";
    String IDREF_ARRAY = "IDREF_array";
    String NAME_ARRAY = "Name_array";
    String BOOL_ARRAY = "bool_array";
    String INT_ARRAY = "int_array";
    String DIRECTIONAL = "directional";
    String POINT = "point";
    String SPOT = "spot";
    String SYMBOL = "symbol";
    String TARGET = "target";
    String PARAM = "param";
    String INSTANCE_MATERIAL = "instance_material";
    String COLOR = "color";
    String FLOAT = "float";
    String EMISSION = "emission";
    String AMBIENT = "ambient";
    String DIFFUSE = "diffuse";
    String SPECULAR = "specular";
    String SHININESS = "shininess";
    String REFLECTIVE = "reflective";
    String REFLECTIVITY = "reflectivity";
    String TRANSPARENT = "transparent";
    String TRANSPARENCY = "transparency";
    String INDEX_OF_REFRACTION = "index_of_refraction";
    String PHONG = "phong";
    String TECHNIQUE = "technique";
    String NEWPARAM = "newparam";
    String ANNOTATE = "annotate";
    String PROFILE_CG = "profile_CG";
    String PROFILE_GLES = "profile_GLES";
    String PROFILE_GLSL = "profile_GLSL";
    String PROFILE_COMMON = "profile_COMMON";
    String IMAGE = "image";
    String INSTANCE_EFFECT = "instance_effect";
    String TECHNIQUE_HINT = "technique_hint";
    String SETPARAM = "setparam";
    String ID = "id";
    String VISUAL_SCENE = "visual_scene";
    String EXTRA = "extra";
    String EVALUATE_SCENE = "evaluate_scene";
    String NODE = "node";
    String LINESTRIPS = "linestrips";
    String POLYGONS = "polygons";
    String POLYLIST = "polylist";
    String TRIFANS = "trifans";
    String TRISTRIPS = "tristrips";
    String LINES = "lines";
    String LIGHT = "light";
    String INSTANCE_VISUAL_SCENE = "instance_visual_scene";
    String INSTANCE_PHYSICS_SCENE = "instance_physics_scene";
    String URL = "url";
    String FLOAT_ARRAY = "float_array";
    String GEOMETRY = "geometry";
    String CONVEX_MESH = "convex_mesh";
    String BIND_MATERIAL = "bind_material";
    String COLLADA = "COLLADA";
    String ASSET = "asset";
    String LIBRARY_CAMERAS = "library_cameras";
    String LIBRARY_EFFECTS = "library_effects";
    String LIBRARY_LIGHTS = "library_lights";
    String LIBRARY_MATERIALS = "library_materials";
    String LIBRARY_GEOMETRIES = "library_geometries";
    String LIBRARY_VISUAL_SCENES = "library_visual_scenes";
    String LIBRARY_PHYISCS_MATERIALS = "library_phyisics_materials";
    String LIBRARY_PHYSICS_MODELS = "library_physics_models";
    String LIBRARY_PHYSICS_SCENES = "library_physics_scenes";
    String SCENE = "scene";
    String VERTICES = "vertices";
    String P = "p";
    String LAYER = "layer";
    String INPUT = "input";
    String COUNT = "count";
    String MATERIAL = "material";
    String MESH = "mesh";
    String NAME = "name";
    String OFFSET = "offset";
    String SEMANTIC = "semantic";
    String SHAPE = "shape";
    String SOURCE = "source";
    String SET = "set";
    String TECHNIQUE_COMMON = "technique_common";
    String TYPE = "type";
    String LOOKAT = "lookat";
    String MATRIX = "matrix";
    String ROTATE = "rotate";
    String SCALE = "scale";
    String SKEW = "skew";
    String SPLINE = "spline";
    String TRANSLATE = "translate";
    String INSTANCE_CAMERA = "instance_camera";
    String INSTANCE_CONTROLLER = "instance_controller";
    String INSTANCE_GEOMETRY = "instance_geometry";
    String INSTANCE_LIGHT = "instance_light";
    String INSTANCE_NODE = "instance_node";
    String TRIANGLES = "triangles";
    String ACCESSOR = "accessor";
    String STRIDE = "stride";
    String SURFACE = "surface";
    String SAMPLER2D = "sampler2D";
    String INIT_FROM = "init_from";
  }

  /**
   * Default values defined in the collada spec used by the parser
   */
  interface DefaultValues {
    int ACCESSOR_STRIDE = 1;
    NodeType NODE_TYPE = NodeType.NODE;
    int SAMPLER2D_MIPMAP_BIAS = 0;
    int SAMPLER2D_MIPMAP_MAXLEVEL = 255;
    String NODE_LAYER = "default";
    String NODE_NAME = "";
  }
}
