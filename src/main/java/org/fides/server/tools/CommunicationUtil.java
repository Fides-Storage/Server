package org.fides.server.tools;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fides.components.Responses;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * This communicationUtil can copy successful or error messages to the given streams
 */
public final class CommunicationUtil {

	/**
	 * Log for this class
	 */
	private static final Logger LOG = LogManager.getLogger(CommunicationUtil.class);

	private CommunicationUtil() {
	}

	/**
	 * Copies a successful to the DataOutputStream
	 * 
	 * @param outputStream
	 *            The stream to copy the successful to
	 * @throws IOException
	 */
	public static void returnSuccessful(DataOutputStream outputStream) throws IOException {
		JsonObject returnJobj = new JsonObject();
		returnJobj.addProperty(Responses.SUCCESSFUL, true);
		outputStream.writeUTF(new Gson().toJson(returnJobj));
	}

	/**
	 * Copies a successful to the DataOutputStream and waits for a successful from the DataInputStream.
	 * 
	 * @param outputStream
	 *            The stream to copy the successful to
	 * @param inputStream
	 *            The stream to read the successful from
	 * @throws IOException
	 */
	public static boolean uploadSuccessful(DataOutputStream outputStream, DataInputStream inputStream) throws IOException {
		JsonObject returnJobj = new JsonObject();
		returnJobj.addProperty(Responses.SUCCESSFUL, true);
		outputStream.writeUTF(new Gson().toJson(returnJobj));

		String message = inputStream.readUTF();
		JsonObject response = new Gson().fromJson(message, JsonObject.class);
		if (response.has(Responses.SUCCESSFUL) && response.get(Responses.SUCCESSFUL).getAsBoolean()) {
			return true;
		}
		return false;
	}

	/**
	 * Copies a successful with properties to the DataOutputStream
	 * 
	 * @param outputStream
	 *            The stream to copy the successful to
	 * @param properties
	 *            The properties to add in the successful message
	 * @throws IOException
	 */
	public static void returnSuccessfulWithProperties(DataOutputStream outputStream, Map<String, Object> properties) throws IOException {
		JsonObject returnJobj = new JsonObject();
		returnJobj.addProperty(Responses.SUCCESSFUL, true);
		for (Map.Entry<String, Object> property : properties.entrySet()) {
			Object value = property.getValue();
			if (value instanceof String) {
				returnJobj.addProperty(property.getKey(), (String) value);
			} else if (value instanceof Number) {
				returnJobj.addProperty(property.getKey(), (Number) value);
			} else if (value instanceof Boolean) {
				returnJobj.addProperty(property.getKey(), (Boolean) value);
			} else if (value instanceof Character) {
				returnJobj.addProperty(property.getKey(), (Character) value);
			} else if (value instanceof JsonElement) {
				returnJobj.add(property.getKey(), (JsonElement) value);
			} else {
				throw new IllegalArgumentException("Object may only be of type: String, Number, Boolean or Character");
			}
		}
		outputStream.writeUTF(new Gson().toJson(returnJobj));
	}

	/**
	 * Copies an errormessage to the DataOutputStream
	 * 
	 * @param outputStream
	 *            The stream to copy the message to
	 * @param errorMessage
	 *            The message to copy
	 */
	public static void returnError(DataOutputStream outputStream, String errorMessage) {
		try {
			JsonObject returnJobj = new JsonObject();
			returnJobj.addProperty(Responses.SUCCESSFUL, false);
			returnJobj.addProperty(Responses.ERROR, errorMessage);
			outputStream.writeUTF(new Gson().toJson(returnJobj));
		} catch (IOException e) {
			LOG.error(e.getMessage());
		}
	}
}
