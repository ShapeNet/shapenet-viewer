package jme3dae.transformers;

/**
 * A function that transforms a value in some other value
 *
 * @param <V> the value type to be transformed
 * @param <R> the type of the transformed value
 * @author pgi
 */
public interface ValueTransformer<V, R> {

  /**
   * Transforms a value
   *
   * @param value the value to transform
   * @return the transformed value
   */
  TransformedValue<R> transform(V value);

  /**
   * Result of the transformation of some value
   *
   * @param <T> the type of the transformed value
   */
  class TransformedValue<T> {

    /**
     * Instance creator
     *
     * @param <T>   the type of the transformed value
     * @param value the value held by the new transformed value. Can be null.
     * @return a new TransformedValue
     */
    public static <T> TransformedValue<T> create(T value) {
      return new TransformedValue<T>(value);
    }

    private final T value;

    /**
     * Initializes this instance
     *
     * @param value the value assigned to this TransformedValue
     */
    protected TransformedValue(T value) {
      this.value = value;
    }

    /**
     * Returns true if the value is != null
     *
     * @return true if the value is not null
     */
    public boolean isDefined() {
      return value != null;
    }

    /**
     * Returns the value held by this TransformedValue. Null iff isDefined == false
     *
     * @return the value held by this TransformedValue
     */
    public T get() {
      return value;
    }

    /**
     * Returns true if this TransformedValue is defined and the held value
     * equals the given one.
     *
     * @param value the value to check agains.
     * @return true or false?
     */
    public boolean contains(T value) {
      return isDefined() && this.value.equals(value);
    }
  }
}
