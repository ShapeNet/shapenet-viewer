package jme3dae.utilities;

import java.util.ListIterator;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jme3dae.transformers.ValueTransformer;

/**
 * Transformed a list of white space separated float strings in a float array
 *
 * @author pgi
 */
public class FloatListTransformer implements ValueTransformer<String, float[]> {

  /**
   * Instance creator
   *
   * @return a new FloatListTransformer instance.
   */
  public static FloatListTransformer create() {
    return new FloatListTransformer();
  }

  private FloatListTransformer() {
  }

  private static final Collector<Float, ?, float[]> toFloatArray =
    Collectors.collectingAndThen(Collectors.toList(), floatList -> {
      float[] array = new float[floatList.size()];
      for (ListIterator<Float> iterator = floatList.listIterator(); iterator.hasNext(); ) {
        array[iterator.nextIndex()] = iterator.next();
      }
      return array;
    });

  private static final Pattern whitespacePattern = Pattern.compile("\\s+");
  /**
   * Transforms a string in a float array
   *
   * @param value the string to transform
   * @return an array of float or an undefined value if parsing fails.
   */
  public TransformedValue<float[]> transform(String value) {
    float[] result;
    try {
      Function<String,Float> mapper = token -> Float.parseFloat(token);
      Stream<String> stream = whitespacePattern.splitAsStream(value);
      result = stream.filter( s -> !s.isEmpty()).map(mapper).collect(toFloatArray);
    } catch (NumberFormatException ex) {
      Logger.getLogger(getClass().getName()).log(Level.SEVERE, "", ex);
      result = null;
    }
    return TransformedValue.create(result);
  }
}
