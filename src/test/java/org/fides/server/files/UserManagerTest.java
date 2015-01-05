package org.fides.server.files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.fides.components.Actions;
import org.fides.components.Responses;
import org.fides.server.tools.PropertiesManager;
import org.fides.tools.HashUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * The JUnit Test Case for the UserManager
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ PropertiesManager.class, FileManager.class, UserManager.class })
@PowerMockIgnore("javax.crypto.*")
public class UserManagerTest {

	/**
	 * A mocked PropertiesManager which should always return the test User Directory
	 */
	private static PropertiesManager mockedPropertiesManager = Mockito.mock(PropertiesManager.class);

	/**
	 * The test User Directory
	 */
	private static File testUserDir;

	/**
	 * Sets up the test class by creating the test User Directory and mocking getUserDir function.
	 */
	@BeforeClass
	public static void setUp() {
		testUserDir = new File(PropertiesManager.getInstance().getUserDir(), "Test");
		if (!testUserDir.exists()) {
			assertTrue(testUserDir.mkdirs());
		}
		// This causes the mocked PropertiesManager to always return the test Data directory:
		Mockito.when(mockedPropertiesManager.getUserDir()).thenReturn(testUserDir.getAbsolutePath());
	}

	/**
	 * Mocks the PropertiesManager to always return a mocked version of the PropertiesManager. This will cause the
	 * FileManager to use a testfolder instead of the main folder.
	 * 
	 * @throws IOException
	 */
	@Before
	public void setUpMock() throws IOException {
		PowerMockito.mockStatic(PropertiesManager.class);
		Mockito.when(PropertiesManager.getInstance()).thenReturn(mockedPropertiesManager);

		PowerMockito.mockStatic(FileManager.class);
		String randomLocation = UUID.randomUUID().toString();
		Mockito.when(FileManager.createFile()).thenReturn(randomLocation);
	}

	/**
	 * Tests whether the userfile is created at the given path
	 */
	@Test
	public void testSaveUserFile() {
		String username = "User1";
		UserFile uf = new UserFile(username, "passwordHash");
		uf.addFile("testFile");

		try {
			assertTrue(Files.exists(Paths.get(testUserDir.getCanonicalPath(), username)));
		} catch (IOException e) {
			fail("IOException has occured: " + e.getMessage());
		}
	}

	/**
	 * Tests whether the file can correctly be opened
	 */
	@Test
	public void testUnlockUserFile() {
		String username = "User2";
		String filename = "testFile";
		String password = "passwordHash";

		UserFile uf = new UserFile(username, password);
		uf.addFile(filename);

		UserFile loadedFile = UserManager.unlockUserFile(username, password);

		assertNotNull(loadedFile);

		assertEquals(username, loadedFile.getUsernameHash());
		assertTrue(loadedFile.checkOwned(filename));
	}

	/**
	 * Tests whether the file cannot be opened with invalid password
	 */
	@Test
	public void testUnlockUserFileWithInvalidPassword() {
		String username = "User3";
		String filename = "testFile";
		String password = "passwordHash";
		String invalidPassword = "passwordHashInvalid";

		UserFile uf = new UserFile(username, password);
		uf.addFile(filename);

		UserFile loadedFile = UserManager.unlockUserFile(username, invalidPassword);

		assertNull(loadedFile);

	}

	/**
	 * Tests whether a new user can be created that doesn't exists
	 */
	@Test
	public void testCreateUser() {
		JsonObject user = new JsonObject();
		user.addProperty(Actions.ACTION, Actions.CREATEUSER);
		user.addProperty(Actions.Properties.USERNAME_HASH, "createUsername");
		user.addProperty(Actions.Properties.PASSWORD_HASH, "Thisisapassword");

		try {
			ByteArrayOutputStream mockedRegisterStream = new ByteArrayOutputStream();
			DataOutputStream mockedDataRegisterStream = new DataOutputStream(mockedRegisterStream);
			UserManager.createUser(user, mockedDataRegisterStream);

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
		String username = "authenticatedUsername";
		String password = "passwordHash";
		UserFile uf = new UserFile(HashUtils.hash(username), password);
		uf.addFile("testFile");

		try {
			assertTrue(Files.exists(Paths.get(testUserDir.getCanonicalPath(), HashUtils.hash(username))));
		} catch (IOException e) {
			fail("IOException has occured: " + e.getMessage());
		}

		JsonObject user = new JsonObject();
		user.addProperty(Actions.ACTION, Actions.LOGIN);
		user.addProperty(Actions.Properties.USERNAME_HASH, username);
		user.addProperty(Actions.Properties.PASSWORD_HASH, password);

		try {
			ByteArrayOutputStream mockedLoginStream = new ByteArrayOutputStream();
			DataOutputStream mockedDataLoginStream = new DataOutputStream(mockedLoginStream);
			assertNotNull(UserManager.authenticateUser(user, mockedDataLoginStream));

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

		JsonObject user = new JsonObject();
		user.addProperty(Actions.ACTION, Actions.LOGIN);
		user.addProperty(Actions.Properties.USERNAME_HASH, "invalidUsername");
		user.addProperty(Actions.Properties.PASSWORD_HASH, "Thisisapassword");

		try {
			ByteArrayOutputStream mockedLoginStream = new ByteArrayOutputStream();
			DataOutputStream mockedDataLoginStream = new DataOutputStream(mockedLoginStream);
			assertNull(UserManager.authenticateUser(user, mockedDataLoginStream));

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

		JsonObject user = new JsonObject();
		user.addProperty(Actions.ACTION, Actions.LOGIN);
		user.addProperty(Actions.Properties.USERNAME_HASH, "authenticatedUsernameInvalidPassword");
		user.addProperty(Actions.Properties.PASSWORD_HASH, "invalidPassword");

		try {
			ByteArrayOutputStream mockedLoginStream = new ByteArrayOutputStream();
			DataOutputStream mockedDataLoginStream = new DataOutputStream(mockedLoginStream);
			assertNull(UserManager.authenticateUser(user, mockedDataLoginStream));

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
			ByteArrayOutputStream mockedRegisterStream = new ByteArrayOutputStream();
			DataOutputStream mockedDataRegisterStream = new DataOutputStream(mockedRegisterStream);
			UserManager.createUser(user, mockedDataRegisterStream);

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
	 * Tears down the test class by clearing the test folder.
	 */
	@AfterClass
	public static void tearDown() {
		try {
			FileUtils.deleteDirectory(testUserDir);
		} catch (Exception e) {
			fail("Unexpected error in tearDown: " + e.getMessage());
		}
	}

}
