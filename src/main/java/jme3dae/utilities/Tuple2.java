package jme3dae.utilities;

/**
 * A value pair
 *
 * @param <A> the type of the first value
 * @param <B> the type of the second value.
 * @author pgi
 */
public class Tuple2<A, B> {

  /**
   * Instance creator.
   *
   * @param <A> the type of the first value
   * @param <B> the type of the second value
   * @param a   the first value
   * @param b   the second value
   * @return a new Tuple2
   */
  public static <A, B> Tuple2<A, B> create(A a, B b) {
    return new Tuple2<A, B>(a, b);
  }

  private final A a;
  private final B b;

  /**
   * Instance initializer
   *
   * @param a the first value
   * @param b the second value
   */
  protected Tuple2(A a, B b) {
    this.a = a;
    this.b = b;
  }

  /**
   * Returns the first value.
   *
   * @return the first value
   */
  public A getA() {
    return a;
  }

  /**
   * Returns the second value
   *
   * @return the second value.
   */
  public B getB() {
    return b;
  }
}
