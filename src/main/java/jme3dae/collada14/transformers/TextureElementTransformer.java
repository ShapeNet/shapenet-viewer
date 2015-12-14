package jme3dae.collada14.transformers;

import com.jme3.texture.Texture;
import jme3dae.DAENode;
import jme3dae.collada14.ColladaSpec141.Names;
import jme3dae.transformers.ValueTransformer.TransformedValue;
import jme3dae.utilities.Conditions;
import jme3dae.utilities.Todo;
import jme3dae.utilities.TransformerPack;

/**
 * Transforms a texture collada element in a JME3 texture.
 *
 * @author pgi
 */
public class TextureElementTransformer implements TransformerPack<DAENode, Texture> {

  public static TextureElementTransformer create() {
    return new TextureElementTransformer();
  }

  private TextureElementTransformer() {
  }

  /**
   * Takes a texture node and returns a jme3 texture instance.
   *
   * @param textureNode the texture node to transform
   * @return a jme3 texture or an undefined value if parsing fails.
   */
  public TransformedValue<Texture> transform(DAENode textureNode) {
    Texture texture = null;
    if (textureNode != null && textureNode.hasName(Names.TEXTURE)) {
      TransformedValue<String> textureLink = textureNode.getAttribute(Names.TEXTURE, TEXT);
      Conditions.checkTrue(textureLink.isDefined());

      DAENode textureLinkedNode = textureNode.getLinkedNode(textureLink.get());
      if (textureLinkedNode.hasName(Names.NEWPARAM)) {
        DAENode samplerNode = textureLinkedNode.getChild(Names.SAMPLER2D);
        Conditions.checkTrue(samplerNode.hasName(Names.SAMPLER2D));

        TransformedValue<String> sourceLink = samplerNode.getChild(Names.SOURCE).getContent(TEXT);
        Conditions.checkTrue(sourceLink.isDefined());

        DAENode surface = samplerNode.getLinkedNode(sourceLink.get()).getChild(Names.SURFACE);
        DAENode initFrom = surface.getChild(Names.INIT_FROM);
        Conditions.checkTrue(initFrom.hasName(Names.INIT_FROM));

        TransformedValue<String> imageLink = initFrom.getContent(TEXT);
        DAENode imageNode = initFrom.getLinkedNode(imageLink.get());
        texture = imageNode.getParsedData(Texture.class);
      } else if (textureLinkedNode.hasName(Names.IMAGE)) {
        texture = textureLinkedNode.getParsedData(Texture.class);
        DAENode extra = textureNode.getChild(Names.EXTRA);
        DAENode tech = extra.getChild(Names.TECHNIQUE);
        if (tech.getAttribute(Names.PROFILE, TEXT).contains("MAYA")) {
          TransformedValue<Boolean> wrapU = tech.getChild("wrapU").getContent(BOOLEAN);
          TransformedValue<Boolean> wrapV = tech.getChild("wrapV").getContent(BOOLEAN);
          Todo.task("parse MAYA technique");
        }
        Todo.implementThis();
      }
      Todo.task("parse the remaining values, check for optional/missing nodes");
    }
    return TransformedValue.create(texture);
  }
}
