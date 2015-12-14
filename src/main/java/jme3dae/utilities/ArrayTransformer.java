package jme3dae.utilities;

import jme3dae.DAENode;
import jme3dae.collada14.ColladaSpec141.Names;
import jme3dae.transformers.ValueTransformer;

import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Transforms a node to an array of element T
 *
 * @author Angel Chang
 */
public class ArrayTransformer<T> implements TransformerPack<DAENode, T[]> {
  protected Function<String,T> transfomerFunction;
  protected ValueTransformer<String,T> transformer;
  protected IntFunction<T[]> arrayCreator;

  public ArrayTransformer(ValueTransformer<String,T> transformer, IntFunction<T[]> arrayCreator) {
    this.transformer = transformer;
    this.arrayCreator = arrayCreator;
    this.transfomerFunction = s -> transformer.transform(s).get();
  }

  private static final Pattern whitespacePattern = Pattern.compile("\\w+");
  @Override
  public TransformedValue<T[]> transform(DAENode node) {
    TransformedValue<String> text = node.getContent(TEXT);
    TransformedValue<Integer> count = node.getAttribute(Names.COUNT, INTEGER);
    Stream<String> stream = whitespacePattern.splitAsStream(text.get());
    T[] elements = stream.map(transfomerFunction).toArray(arrayCreator);
    return TransformedValue.create(elements);
  }
}
