package jme3dae.utilities;

/**
 * Data stored in the DAENode wrapping the COLLADA element, defines the measuring unit
 * of the collada scene.
 *
 * @author pgi
 */
public class MeasuringUnit {

  /**
   * Instance creator
   *
   * @param meter the float value that defines the unit scale
   * @return a new MeasuringUnit
   */
  public static MeasuringUnit create(float meter) {
    return new MeasuringUnit(meter);
  }

  private final float METER;

  private MeasuringUnit(float meter) {
    METER = meter;
  }

  /**
   * Returns the unit scale (1 is 1 meter).
   *
   * @return the unit scale.
   */
  public float getMeter() {
    return METER;
  }
}
