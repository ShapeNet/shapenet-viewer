package jme3dae.utilities;

import java.util.function.ToIntFunction;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import jme3dae.transformers.ValueTransformer;

/**
 * Transforms a sequence of white space separated int strings into an array
 * of int values
 *
 * @author pgi
 */
public class IntegerListTransformer implements ValueTransformer<String, int[]> {

  /**
   * Instance creator
   *
   * @return a new IntegerListTransformer
   */
  public static IntegerListTransformer create() {
    return new IntegerListTransformer();
  }

  private IntegerListTransformer() {
  }

  private static final Pattern whitespacePattern = Pattern.compile("\\s+");
  /**
   * Transforms a string in a sequence of integers
   *
   * @param value the string to transform
   * @return an array of int or an undefined value if parsing fails.
   */
  public TransformedValue<int[]> transform(String value) {
    int[] result;
    try {
      ToIntFunction<String> mapper = token -> Integer.parseInt(token);
      Stream<String> stream = whitespacePattern.splitAsStream(value);
      result = stream.filter( s -> !s.isEmpty()).mapToInt(mapper).toArray();
    } catch (NumberFormatException ex) {
      Logger.getLogger(getClass().getName()).log(Level.SEVERE, "", ex);
      result = null;
    }
    return TransformedValue.create(result);
  }
}
