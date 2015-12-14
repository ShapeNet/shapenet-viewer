package jme3dae.utilities;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class to stop program when a condition fails. Used for debugging.
 *
 * @author pgi
 */
public class Conditions {

  private Conditions() {
  }

  private static final Conditions INSTANCE = new Conditions();

  /**
   * The runtime exception thrown by this utility when a check fails.
   */
  public static class CheckException extends RuntimeException {

    public CheckException(String message) {
      super(message);
    }
  }

  /**
   * Null check
   *
   * @param value the value to check
   * @throws jme3dae.utilities.Conditions.CheckException if value is null
   */
  public static void checkNotNull(Object value) throws CheckException {
    if (value == null) {
      signalError("Expected not null value.");
    }
  }

  /**
   * True check
   *
   * @param expr the value to check
   * @throws jme3dae.utilities.Conditions.CheckException if expr is false
   */
  public static void checkTrue(boolean expr) throws CheckException {
    if (!expr) {
      signalError("expected true");
    }
  }

  /**
   * /**
   *
   * @param value
   * @param expected
   * @throws jme3dae.utilities.Conditions.CheckException
   */
  public static void checkValue(int value, int expected) throws CheckException {
    if (value != expected) {
      signalError("Expected " + expected + " got " + value);
    }
  }

  /**
   * True check
   *
   * @param expr         the value to check
   * @param errorMessage the message to emit if expr is false
   * @throws jme3dae.utilities.Conditions.CheckException if expr is false
   */
  public static void checkTrue(boolean expr, String errorMessage) throws CheckException {
    if (!expr) {
      signalError(errorMessage);
    }
  }

  /**
   * False check
   *
   * @param expr         the value to check
   * @param errorMessage the message to emit if expr is true
   * @throws jme3dae.utilities.Conditions.CheckException if expr is true
   */
  public static void checkFalse(boolean expr, String errorMessage) throws CheckException {
    if (expr) {
      signalError(errorMessage);
    }
  }

  private static void signalError(String errorMessage) {
    if (errorMessage == null) {
      errorMessage = "";
    }
    String log = "Condition failure";
    StackTraceElement[] stack = Thread.currentThread().getStackTrace();
    if (stack.length >= 3) {
      log += "\nAt: " + stack[3];
    }
    Logger.getLogger(Conditions.class.getName()).log(Level.SEVERE, log);
    if (errorMessage == null) {
      errorMessage = "";
    }
    throw new CheckException(errorMessage);
  }
}
