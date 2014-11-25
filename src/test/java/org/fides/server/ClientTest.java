package org.fides.server;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import javax.net.ssl.SSLSocket;

import org.fides.server.files.UserFile;
import org.fides.server.files.UserManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * This unittest tests the Client class
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(UserManager.class)
public class ClientTest {

	private SSLSocket mockedSSLSocket = mock(SSLSocket.class);

	/**
	 * Disables the static UserManager to prevent the creation of userfiles.
	 */
	@Before
	public void disableUserManager() {
		PowerMockito.mockStatic(UserManager.class);
		Mockito.when(UserManager.saveUserFile(Mockito.any(UserFile.class))).thenReturn(true);
		Mockito.when(UserManager.checkIfUserExists("createUsername")).thenReturn(false);
		Mockito.when(UserManager.checkIfUserExists("authenticatedUsername")).thenReturn(true);
		Mockito.when(UserManager.unlockUserFile("authenticatedUsername", "Thisisapassword")).thenReturn(new UserFile("authenticatedUsername", "Thisisapassword"));
	}

	/**
	 * Tests whether a new user can be created that doesn't exists
	 */
	@Test
	public void testCreateUser() {

		JsonObject user = new JsonObject();
		user.addProperty("action", "createUser");
		user.addProperty("username", "createUsername");
		user.addProperty("passwordHash", "Thisisapassword");

		try {
			Client client = new Client((SSLSocket) mockedSSLSocket);

			ByteArrayOutputStream mockedRegisterStream = new ByteArrayOutputStream();
			DataOutputStream mockedDataRegisterStream = new DataOutputStream(mockedRegisterStream);
			client.createUser(user, mockedDataRegisterStream);

			ByteArrayInputStream inputStream = new ByteArrayInputStream(mockedRegisterStream.toByteArray());
			DataInputStream in = new DataInputStream(inputStream);

			JsonObject jobj = new Gson().fromJson(in.readUTF(), JsonObject.class);
			assertTrue(jobj.has("successful"));
			assertTrue(jobj.get("successful").getAsBoolean());
		} catch (IOException e) {
			e.printStackTrace();
			fail("IOException: " + e);
		}

	}

	/**
	 * Tests whether a valid user can authenticate
	 */
	@Test
	public void testAuthenticateUser() {

		JsonObject user = new JsonObject();
		user.addProperty("action", "login");
		user.addProperty("username", "authenticatedUsername");
		user.addProperty("passwordHash", "Thisisapassword");

		try {
			Client client = new Client((SSLSocket) mockedSSLSocket);

			ByteArrayOutputStream mockedLoginStream = new ByteArrayOutputStream();
			DataOutputStream mockedDataLoginStream = new DataOutputStream(mockedLoginStream);
			assertTrue(client.authenticateUser(user, mockedDataLoginStream));

			ByteArrayInputStream inputStream = new ByteArrayInputStream(mockedLoginStream.toByteArray());
			DataInputStream in = new DataInputStream(inputStream);

			JsonObject jobj = new Gson().fromJson(in.readUTF(), JsonObject.class);
			assertTrue(jobj.has("successful"));
			assertTrue(jobj.get("successful").getAsBoolean());
		} catch (IOException e) {
			e.printStackTrace();
			fail("IOException: " + e);
		}

	}

}
