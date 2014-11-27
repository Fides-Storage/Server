package org.fides.server.tools;

import com.google.gson.JsonObject;

//TODO Add Javadoc to this class
public final class JsonObjectHandler {

	private JsonObjectHandler() {
	}

	public static String getProperty(JsonObject jobj, String property) {

		if (jobj.has(property)) {
			return jobj.get(property).getAsString();
		}

		return null;

	}
}
