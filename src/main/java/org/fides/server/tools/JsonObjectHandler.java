package org.fides.server.tools;

import com.google.gson.JsonObject;

public class JsonObjectHandler {

  public static String getProperty(JsonObject jobj, String property) {

    if (jobj.has(property)) {
      return jobj.get(property).getAsString();
    }

    return null;

  }
}
