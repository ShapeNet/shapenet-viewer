package jme3dae.utilities;

/**
 * Utility class to print todo tasks. The printed message includes the line of
 * the code where the Todo has been called. Used for debug purposes. Enabled if
 * the system property "checkenabled" is not null.
 *
 * @author pgi
 */
public class Todo {
  private static final boolean DISABLED = System.getProperty("checksenabled") == null;

  private static void log(String message) {
    if (DISABLED) return;

    StackTraceElement[] trace = Thread.currentThread().getStackTrace();
    String where = "";
    if (trace != null && trace.length >= 3) {
      where = trace[3].toString();
    }
    System.out.printf("***TODO***[%s][%s]%n", where, message);
  }

  /**
   * Prins a message if enabled.
   *
   * @param description the description of the task. Eg Todo.task("do something").
   */
  public static void task(String description) {
    if (DISABLED) return;
    log(description);
  }

  /**
   * A predefined message to implement some part of code.
   */
  public static void implementThis() {
    if (DISABLED) return;
    log("implement this method");
  }

  /**
   * A predefined message to parse some node.
   *
   * @param node the element to parse
   */
  public static void parse(Object node) {
    if (DISABLED) return;
    log("implement parsing of node\n" + node);
  }

  /**
   * A predefined message to check the parsing of some node.
   *
   * @param node the node to parse
   * @param why  the reason why parsing is not fully implemented.
   */
  public static void checkParsingOf(Object node, String why) {
    if (DISABLED) return;
    log("Can't parse node \n" + node + "\n" + why);
  }
}
