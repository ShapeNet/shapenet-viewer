package jme3dae.collada14.transformers;

import jme3dae.DAENode;
import jme3dae.collada14.ColladaSpec141.Names;
import jme3dae.collada14.ColladaSpec141.Semantic;
import jme3dae.transformers.ValueTransformer.TransformedValue;
import jme3dae.utilities.Todo;
import jme3dae.utilities.TransformerPack;

/**
 * Transforms a DAENode holding an input_shared collada node into an InputShared value (if possible).
 *
 * @author pgi
 */
public class InputSharedTransformer implements TransformerPack<DAENode, InputShared> {

  /**
   * Instance creator.
   *
   * @return a new InputSharedTransformer.
   */
  public static InputSharedTransformer create() {
    return new InputSharedTransformer();
  }

  private InputSharedTransformer() {
  }

  /**
   * Transforms a DAENode into an InputShared
   *
   * @param value a DAENode wrapping a collada input_shared element.
   * @return the result of the transformation. Can be undefined if the transformation
   * fails.
   */
  public TransformedValue<InputShared> transform(DAENode value) {
    InputShared result = null;
    if (value.hasName(Names.INPUT)) {
      TransformedValue<Integer> offset = value.getAttribute(Names.OFFSET, INTEGER);
      TransformedValue<Semantic> semantic = value.getAttribute(Names.SEMANTIC, SEMANTIC);
      DAENode linkedSource = value.getLinkedSource();
      if (semantic.contains(Semantic.VERTEX)) {
        linkedSource = linkedSource.getChild(Names.INPUT).getLinkedSource();
      }

      if (offset.isDefined() && linkedSource.isDefined() && semantic.isDefined()) {
        TransformedValue<Integer> set = value.getAttribute(Names.SET, INTEGER);
        //Todo.task("transform: Spec says this can be empty");
        TransformedValue<String[]> idrefArray = linkedSource.getChild(Names.IDREF_ARRAY).getContent(NAME_LIST);
        TransformedValue<String[]> nameArray = linkedSource.getChild(Names.NAME_ARRAY).getContent(NAME_LIST);
        TransformedValue<boolean[]> boolArray = linkedSource.getChild(Names.BOOL_ARRAY).getContent(BOOLEAN_LIST);
        TransformedValue<float[]> floatArray = linkedSource.getChild(Names.FLOAT_ARRAY).getContent(FLOAT_LIST);
        TransformedValue<int[]> intArray = linkedSource.getChild(Names.INT_ARRAY).getContent(INTEGER_LIST);
        DAENode tcomm = linkedSource.getChild(Names.TECHNIQUE_COMMON);
        DAENode accessor = tcomm.getChild(Names.ACCESSOR);
        TransformedValue<Integer> stride = accessor.getAttribute(Names.STRIDE, INTEGER);
        result = InputShared.create(semantic.get(), offset.get(), stride.get(), set.get(), idrefArray.get(), nameArray.get(), boolArray.get(), floatArray.get(), intArray.get());
      } else {
        Todo.checkParsingOf(value, "offset, linked source or semantic not found");
      }
    }
    return TransformedValue.create(result);
  }


  /**
   * Gets the normals if any are available
   *
   * @param value
   * @return
   */
  public TransformedValue<InputShared> transformNormal(DAENode value) {
    InputShared result = null;
    if (value.hasName(Names.INPUT)) {
      TransformedValue<Integer> offset = value.getAttribute(Names.OFFSET, INTEGER);
      TransformedValue<Semantic> semantic = value.getAttribute(Names.SEMANTIC, SEMANTIC);
      DAENode linkedSource = value.getLinkedSource();
      if (semantic.contains(Semantic.VERTEX)) // Changed by larynx to get the normals also (to make it work with Sketchup 8)
      {
        // Changed by larynx to get the normals also (to make it work with Sketchup 8)
        for (DAENode i : linkedSource.getChildren(Names.INPUT)) {
          TransformedValue<Semantic> vertSemantic = i.getAttribute(Names.SEMANTIC, SEMANTIC);
          if (vertSemantic.contains(Semantic.NORMAL)) {
            linkedSource = i.getLinkedSource();
            semantic = vertSemantic;
          }
        }
      }

      if (offset.isDefined() && linkedSource.isDefined() && semantic.isDefined()) {
        if (semantic.contains(Semantic.NORMAL)) {
          TransformedValue<Integer> set = value.getAttribute(Names.SET, INTEGER);
          //Todo.task("transformNormal: Spec says this can be empty");
          TransformedValue<String[]> idrefArray = linkedSource.getChild(Names.IDREF_ARRAY).getContent(NAME_LIST);
          TransformedValue<String[]> nameArray = linkedSource.getChild(Names.NAME_ARRAY).getContent(NAME_LIST);
          TransformedValue<boolean[]> boolArray = linkedSource.getChild(Names.BOOL_ARRAY).getContent(BOOLEAN_LIST);
          TransformedValue<float[]> floatArray = linkedSource.getChild(Names.FLOAT_ARRAY).getContent(FLOAT_LIST);
          TransformedValue<int[]> intArray = linkedSource.getChild(Names.INT_ARRAY).getContent(INTEGER_LIST);
          DAENode tcomm = linkedSource.getChild(Names.TECHNIQUE_COMMON);
          DAENode accessor = tcomm.getChild(Names.ACCESSOR);
          TransformedValue<Integer> stride = accessor.getAttribute(Names.STRIDE, INTEGER);
          result = InputShared.create(semantic.get(), offset.get(), stride.get(), set.get(), idrefArray.get(), nameArray.get(), boolArray.get(), floatArray.get(), intArray.get());
        } else {
          // OKAY
          //Todo.checkParsingOf(value, "normal not found");
        }
      } else {
        Todo.checkParsingOf(value, "offset, linked source or semantic not found");
      }
    }
    return TransformedValue.create(result);
  }

}
