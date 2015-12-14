package jme3dae.utilities;

import jme3dae.collada14.transformers.SemanticTransformer;
import jme3dae.transformers.ValueTransformer;

/**
 * A base interface for transformers that make frequent use of some value
 * transformers (numbers, text...)
 *
 * @param <V> the type of the value required by the transformer
 * @param <R> the type of the restul returned by the transformer
 * @author pgi
 */
public interface TransformerPack<V, R> extends ValueTransformer<V, R> {

  /**
   * Transformes a string into a Semantic value
   */
  SemanticTransformer SEMANTIC = SemanticTransformer.create();

  /**
   * Transforms a string into an integer
   */
  IntegerTransformer INTEGER = IntegerTransformer.create();

  /**
   * Transforms a string into an integer array
   */
  IntegerListTransformer INTEGER_LIST = IntegerListTransformer.create();

  /**
   * Transforms a string into a float array
   */
  FloatListTransformer FLOAT_LIST = FloatListTransformer.create();

  /**
   * Transforms a string into a string
   */
  PlainTextTransformer TEXT = PlainTextTransformer.create();

  /**
   * Transforms a string into a string array
   */
  NameListTransformer NAME_LIST = NameListTransformer.create();

  /**
   * Transforms a string into a float
   */
  FloatTransformer FLOAT = FloatTransformer.create();

  /**
   * Transforms a string into a boolean
   */
  BooleanTransformer BOOLEAN = BooleanTransformer.create();

  /**
   * Transforms a string into a boolean array
   */
  BooleanListTransformer BOOLEAN_LIST = BooleanListTransformer.create();
}
