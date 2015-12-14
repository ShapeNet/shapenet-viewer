package jme3dae.collada14.transformers;

import jme3dae.collada14.ColladaSpec141.Semantic;
import jme3dae.transformers.ValueTransformer;
import jme3dae.transformers.ValueTransformer.TransformedValue;
import jme3dae.utilities.Todo;

/**
 * Transforms a string into a Semantic value.
 *
 * @author pgi
 */
public class SemanticTransformer implements ValueTransformer<String, Semantic> {

  /**
   * Instance initializer.
   *
   * @return a new SemanticTransformer
   */
  public static SemanticTransformer create() {
    return new SemanticTransformer();
  }

  private SemanticTransformer() {
  }

  /**
   * Transforms the given string into a Semantic value
   *
   * @param value a string holding the text representation of a collada semantic
   *              value
   * @return the semantic value or an undefined value if parsing fails.
   */
  public TransformedValue<Semantic> transform(String value) {
    Semantic r = null;
    try {
      r = Semantic.valueOf(value.trim());
    } catch (IllegalArgumentException ex) {
      Todo.task("implement semantic: " + value);
    }
    return TransformedValue.create(r);
  }
}
