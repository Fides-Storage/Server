package org.fides.server.tools;

import com.google.gson.JsonObject;

/**
 * Helper for jsonobject
 * 
 * @author niels
 * @author jesse
 *
 */
public final class JsonObjectHandler {

	private JsonObjectHandler() {
	}

	/**
	 * Get property from jsonobject
	 * 
	 * @param jobj
	 *            jsonobject where to property is taken from
	 * @param property
	 *            name to get property
	 * @return the value of the given property
	 */
	public static String getProperty(JsonObject jobj, String property) {

		if (jobj.has(property)) {
			return jobj.get(property).getAsString();
		}

		return null;
	}

}
