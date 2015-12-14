package jme3dae.utilities;

import jme3dae.transformers.ValueTransformer;
import jme3dae.transformers.ValueTransformer.TransformedValue;

/**
 * Transforms a string in boolean
 *
 * @author pgi
 */
public class BooleanTransformer implements ValueTransformer<String, Boolean> {

  /**
   * Instance creator
   *
   * @return a new BooleanTransformer instance
   */
  public static BooleanTransformer create() {
    return new BooleanTransformer();
  }

  private BooleanTransformer() {
  }

  /**
   * Transforms a string in a boolean
   *
   * @param value a string, maybe null
   * @return a boolean or an undefined value if parsing fails
   */
  public TransformedValue<Boolean> transform(String value) {
    Boolean result = null;
    if (value != null) {
      value = value.trim();
      if ("true".equalsIgnoreCase(value)) {
        result = Boolean.TRUE;
      } else if ("false".equalsIgnoreCase(value)) {
        result = Boolean.FALSE;
      }
    }
    return TransformedValue.create(result);
  }
}
