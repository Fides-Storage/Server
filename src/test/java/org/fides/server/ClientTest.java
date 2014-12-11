package org.fides.server;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import javax.net.ssl.SSLSocket;

import org.fides.components.Actions;
import org.fides.components.Responses;
import org.fides.server.files.UserFile;
import org.fides.server.files.UserManager;
import org.fides.tools.HashUtils;
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

	private final String authenticatedUsernameHash = HashUtils.hash("authenticatedUsername");

	/**
	 * Disables the static UserManager to prevent the creation of userfiles.
	 */
	@Before
	public void disableUserManager() {
		PowerMockito.mockStatic(UserManager.class);
		Mockito.when(UserManager.checkIfUserExists("createUsername")).thenReturn(false);
		Mockito.when(UserManager.checkIfUserExists(authenticatedUsernameHash)).thenReturn(true);
		Mockito.when(UserManager.unlockUserFile(authenticatedUsernameHash, "Thisisapassword")).thenReturn(new UserFile(authenticatedUsernameHash, "Thisisapassword"));
	}

	/**
	 * Tests whether a new user can be created that doesn't exists
	 */
	@Test
	public void testCreateUser() {
		Mockito.when(UserManager.saveUserFile(Mockito.any(UserFile.class))).thenReturn(true);

		JsonObject user = new JsonObject();
		user.addProperty(Actions.ACTION, Actions.CREATEUSER);
		user.addProperty(Actions.Properties.USERNAME_HASH, "createUsername");
		user.addProperty(Actions.Properties.PASSWORD_HASH, "Thisisapassword");

		try {
			Client client = new Client(mockedSSLSocket);

			ByteArrayOutputStream mockedRegisterStream = new ByteArrayOutputStream();
			DataOutputStream mockedDataRegisterStream = new DataOutputStream(mockedRegisterStream);
			client.createUser(user, mockedDataRegisterStream);

			ByteArrayInputStream inputStream = new ByteArrayInputStream(mockedRegisterStream.toByteArray());
			DataInputStream in = new DataInputStream(inputStream);

			JsonObject jobj = new Gson().fromJson(in.readUTF(), JsonObject.class);
			assertTrue(jobj.has(Responses.SUCCESSFUL));
			assertTrue(jobj.get(Responses.SUCCESSFUL).getAsBoolean());
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
		Mockito.when(UserManager.saveUserFile(Mockito.any(UserFile.class))).thenReturn(true);

		JsonObject user = new JsonObject();
		user.addProperty(Actions.ACTION, Actions.LOGIN);
		user.addProperty(Actions.Properties.USERNAME_HASH, "authenticatedUsername");
		user.addProperty(Actions.Properties.PASSWORD_HASH, "Thisisapassword");

		try {
			Client client = new Client(mockedSSLSocket);

			ByteArrayOutputStream mockedLoginStream = new ByteArrayOutputStream();
			DataOutputStream mockedDataLoginStream = new DataOutputStream(mockedLoginStream);
			assertTrue(client.authenticateUser(user, mockedDataLoginStream));

			ByteArrayInputStream inputStream = new ByteArrayInputStream(mockedLoginStream.toByteArray());
			DataInputStream in = new DataInputStream(inputStream);

			JsonObject jobj = new Gson().fromJson(in.readUTF(), JsonObject.class);
			assertTrue(jobj.has(Responses.SUCCESSFUL));
			assertTrue(jobj.get(Responses.SUCCESSFUL).getAsBoolean());
		} catch (IOException e) {
			e.printStackTrace();
			fail("IOException: " + e);
		}

	}

	/**
	 * Tests whether a valid user can authenticate
	 */
	@Test
	public void testAuthenticateInvalidUser() {
		Mockito.when(UserManager.saveUserFile(Mockito.any(UserFile.class))).thenReturn(true);

		JsonObject user = new JsonObject();
		user.addProperty(Actions.ACTION, Actions.LOGIN);
		user.addProperty(Actions.Properties.USERNAME_HASH, "invalidUsername");
		user.addProperty(Actions.Properties.PASSWORD_HASH, "Thisisapassword");

		try {
			Client client = new Client(mockedSSLSocket);

			ByteArrayOutputStream mockedLoginStream = new ByteArrayOutputStream();
			DataOutputStream mockedDataLoginStream = new DataOutputStream(mockedLoginStream);
			assertFalse(client.authenticateUser(user, mockedDataLoginStream));

			ByteArrayInputStream inputStream = new ByteArrayInputStream(mockedLoginStream.toByteArray());
			DataInputStream in = new DataInputStream(inputStream);

			JsonObject jobj = new Gson().fromJson(in.readUTF(), JsonObject.class);
			assertTrue(jobj.has(Responses.SUCCESSFUL));
			assertFalse(jobj.get(Responses.SUCCESSFUL).getAsBoolean());
		} catch (IOException e) {
			e.printStackTrace();
			fail("IOException: " + e);
		}

	}

	/**
	 * Tests whether a valid user can authenticate with invalid password
	 */
	@Test
	public void testAuthenticateInvalidPassword() {
		Mockito.when(UserManager.saveUserFile(Mockito.any(UserFile.class))).thenReturn(true);

		JsonObject user = new JsonObject();
		user.addProperty(Actions.ACTION, Actions.LOGIN);
		user.addProperty(Actions.Properties.USERNAME_HASH, "authenticatedUsername");
		user.addProperty(Actions.Properties.PASSWORD_HASH, "invalidPassword");

		try {
			Client client = new Client(mockedSSLSocket);

			ByteArrayOutputStream mockedLoginStream = new ByteArrayOutputStream();
			DataOutputStream mockedDataLoginStream = new DataOutputStream(mockedLoginStream);
			assertFalse(client.authenticateUser(user, mockedDataLoginStream));

			ByteArrayInputStream inputStream = new ByteArrayInputStream(mockedLoginStream.toByteArray());
			DataInputStream in = new DataInputStream(inputStream);

			JsonObject jobj = new Gson().fromJson(in.readUTF(), JsonObject.class);
			assertTrue(jobj.has(Responses.SUCCESSFUL));
			assertFalse(jobj.get(Responses.SUCCESSFUL).getAsBoolean());
		} catch (IOException e) {
			e.printStackTrace();
			fail("IOException: " + e);
		}

	}

	/**
	 * Tests whether a new user can be created that doesn't exists
	 */
	@Test
	public void testCreateStrangeUser() {

		JsonObject user = new JsonObject();
		user.addProperty(Actions.ACTION, Actions.CREATEUSER);
		user.addProperty(Actions.Properties.USERNAME_HASH, "/../createUsername");
		user.addProperty(Actions.Properties.PASSWORD_HASH, "Thisisapassword");

		try {
			Client client = new Client(mockedSSLSocket);

			ByteArrayOutputStream mockedRegisterStream = new ByteArrayOutputStream();
			DataOutputStream mockedDataRegisterStream = new DataOutputStream(mockedRegisterStream);
			client.createUser(user, mockedDataRegisterStream);

			ByteArrayInputStream inputStream = new ByteArrayInputStream(mockedRegisterStream.toByteArray());
			DataInputStream in = new DataInputStream(inputStream);

			JsonObject jobj = new Gson().fromJson(in.readUTF(), JsonObject.class);
			assertTrue(jobj.has(Responses.SUCCESSFUL));
			assertFalse(jobj.get(Responses.SUCCESSFUL).getAsBoolean());
		} catch (IOException e) {
			e.printStackTrace();
			fail("IOException: " + e);
		}

	}
}
