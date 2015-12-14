package jme3dae.utilities;

/**
 * A 3 values tuple
 *
 * @param <A> the type of the first value
 * @param <B> the type of the second value
 * @param <C> the type of the third value
 * @author pgi
 */
public class Tuple3<A, B, C> extends Tuple2<A, B> {

  /**
   * Instance creator
   *
   * @param <A> the type of the first value
   * @param <B> the type of the second value
   * @param <C> the type of the third value
   * @param a   the first value
   * @param b   the second value
   * @param c   the third value
   * @return a new Tuple3 instance
   */
  public static <A, B, C> Tuple3<A, B, C> create(A a, B b, C c) {
    return new Tuple3<A, B, C>(a, b, c);
  }

  private final C c;

  /**
   * Instance initializer
   *
   * @param a the first value
   * @param b the second value
   * @param c the third value
   */
  protected Tuple3(A a, B b, C c) {
    super(a, b);
    this.c = c;
  }

  /**
   * Returns the third value
   *
   * @return the third value
   */
  public C getC() {
    return c;
  }
}
