package org.fides.server.tools;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fides.components.Actions;
import org.fides.components.Responses;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * This communicationUtil can copy succesful or error messages to the given streams
 */
public class CommunicationUtil {

	/**
	 * Log for this class
	 */
	private static Logger log = LogManager.getLogger(CommunicationUtil.class);

	/**
	 * Copies an successful to the outputstream
	 *
	 * @param outputStream
	 *            The stream to copy the successful to
	 */
	public static void returnSuccessful(DataOutputStream outputStream) {
		try {
			JsonObject returnJobj = new JsonObject();
			returnJobj.addProperty(Responses.SUCCESSFUL, true);
			outputStream.writeUTF(new Gson().toJson(returnJobj));

		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}

	/**
	 * Copies an successful to the outputstream
	 *
	 * @param outputStream
	 *            The stream to copy the successful to
	 * @param properties
	 * 			  The properties to add in the successful message
	 */
	public static void returnSuccessfulWithProperties(DataOutputStream outputStream, Map<String, Object> properties) {
		try {
			JsonObject returnJobj = new JsonObject();
			returnJobj.addProperty(Responses.SUCCESSFUL, true);
			for (Map.Entry<String, Object> property : properties.entrySet()) {
				Object value = property.getValue();
				if (value instanceof String) {
					returnJobj.addProperty(property.getKey(), (String) value);
				} else if (value instanceof  Number) {
					returnJobj.addProperty(property.getKey(), (Number) value);
				} else if (value instanceof  Boolean) {
					returnJobj.addProperty(property.getKey(), (Boolean) value);
				} else if (value instanceof  Character) {
					returnJobj.addProperty(property.getKey(), (Character) value);
				} else {
					throw new IllegalArgumentException("Object may only be of type: String, Number, Boolean or Character");
				}
			}
			outputStream.writeUTF(new Gson().toJson(returnJobj));
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}




	/**
	 * Copies an errormessage to the outputstream
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
			log.error(e.getMessage());
		}
	}
}
