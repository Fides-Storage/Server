package org.fides.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import javax.net.ssl.SSLSocket;

import org.fides.components.Actions;
import org.fides.components.Responses;
import org.fides.server.files.UserFile;
import org.fides.server.files.UserManager;
import org.fides.server.tools.UserLocker;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

/**
 * This unittest tests the Client class
 */
@PowerMockIgnore("javax.management.*")
@RunWith(PowerMockRunner.class)
@PrepareForTest({ UserLocker.class, UserManager.class })
public class ClientTest {

	private static SSLSocket sslSocket;

	private static Client client;

	private static DataInputStream dataInputStream = null;

	private static ClientFileConnector clientFileConnector;

	/**
	 * This function should be called before every test. It mocks the UserLocker, UserManager and ClientFileConnector.
	 * It makes sure that the handleActions and run function is called by the real method and not the mocked one. It
	 * also sets the server variable in the client.
	 * 
	 * @throws IOException
	 */
	@Before
	public void before() throws IOException {
		PowerMockito.mockStatic(UserLocker.class);
		PowerMockito.mockStatic(UserManager.class);
		clientFileConnector = mock(ClientFileConnector.class);

		sslSocket = mock(SSLSocket.class);
		when(sslSocket.getOutputStream()).thenReturn(new ByteArrayOutputStream());

		// Setup client
		client = mock(Client.class);
		Mockito.doCallRealMethod().when(client).handleActions(Mockito.any(DataInputStream.class), Mockito.any(ClientFileConnector.class), Mockito.any(DataOutputStream.class));
		Mockito.doCallRealMethod().when(client).run();
		Whitebox.setInternalState(client, "server", sslSocket);
	}

	/**
	 * Clears all the set variables
	 */
	@After
	public void after() {
		client = null;
		clientFileConnector = null;
		dataInputStream = null;
		sslSocket = null;

	}

	/**
	 * This function should be called before testing anything from the run function. this will set the action of the
	 * stream
	 * 
	 * @param action
	 *            The action to set
	 * @throws IOException
	 */
	private void beforeRun(String action) throws IOException {
		// Set UserFile to null
		Whitebox.setInternalState(client, "userFile", (UserFile) null);

		// Create a action in json format
		JsonObject actionRequest = new JsonObject();
		actionRequest.addProperty(Actions.ACTION, action);
		// Create the disconnect action in json format
		JsonObject disconnectRequest = new JsonObject();
		disconnectRequest.addProperty(Actions.ACTION, Actions.DISCONNECT);

		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);

		// Write the action
		dataOutputStream.writeUTF(new Gson().toJson(actionRequest));

		// Write the disconnect action
		dataOutputStream.writeUTF(new Gson().toJson(disconnectRequest));

		// Set the datainputstream to the new DataInputStream containing the action
		dataInputStream = new DataInputStream(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));
		when(sslSocket.getInputStream()).thenReturn(dataInputStream);
	}

	/**
	 * This test will verify if the createuser function is called with the correct action
	 * 
	 * @throws Exception
	 */
	@Test
	public void runTestCreateUser() throws Exception {
		beforeRun(Actions.CREATE_USER);
		client.run();

		verifyStatic(Mockito.times(1));
		UserManager.createUser(Mockito.any(JsonObject.class), Mockito.any(DataOutputStream.class));
		verifyStatic(Mockito.never());
		UserManager.authenticateUser(Mockito.any(JsonObject.class), Mockito.any(DataOutputStream.class));
	}

	/**
	 * This test will verify if the authenticateuser function is called with the correct action
	 * 
	 * @throws Exception
	 */
	@Test
	public void runTestLogin() throws Exception {
		beforeRun(Actions.LOGIN);
		client.run();

		verifyStatic(Mockito.never());
		UserManager.createUser(Mockito.any(JsonObject.class), Mockito.any(DataOutputStream.class));
		verifyStatic(Mockito.times(1));
		UserManager.authenticateUser(Mockito.any(JsonObject.class), Mockito.any(DataOutputStream.class));
	}

	/**
	 * This function should be called before testing anything from the handle function. This will set a mock for the
	 * userfile and adds a given action to the stream
	 *
	 * @param action
	 *            The action to put on the stream
	 * @throws IOException
	 */
	private void beforeHandleAction(String action) throws IOException {
		// Mock the UserFile
		UserFile userFile = mock(UserFile.class);
		Whitebox.setInternalState(client, "userFile", userFile);

		// Create a action in json format
		JsonObject actionRequest = new JsonObject();
		actionRequest.addProperty(Actions.ACTION, action);
		// Create the disconnect action in json format
		JsonObject disconnectRequest = new JsonObject();
		disconnectRequest.addProperty(Actions.ACTION, Actions.DISCONNECT);

		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);

		// Write the action
		dataOutputStream.writeUTF(new Gson().toJson(actionRequest));

		// Write the disconnect action
		dataOutputStream.writeUTF(new Gson().toJson(disconnectRequest));

		// Set the datainputstream to the new DataInputStream containing the action
		dataInputStream = new DataInputStream(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));
	}

	/**
	 * This test will verify if the downloadkeyfile function is called with the correct action
	 *
	 * @throws Exception
	 */
	@Test
	public void handleGetKeyFileAction() throws Exception {
		beforeHandleAction(Actions.GET_KEY_FILE);
		client.handleActions(dataInputStream, clientFileConnector, null);

		// Verify function calls
		verify(clientFileConnector, Mockito.times(1)).downloadKeyFile(Mockito.any(DataOutputStream.class));
		verify(clientFileConnector, Mockito.never()).downloadFile(Mockito.any(JsonObject.class), Mockito.any(DataOutputStream.class));
		verify(clientFileConnector, Mockito.never()).updateKeyFile(Mockito.any(DataInputStream.class), Mockito.any(DataOutputStream.class));
		verify(clientFileConnector, Mockito.never()).updateFile(Mockito.any(DataInputStream.class), Mockito.any(JsonObject.class), Mockito.any(DataOutputStream.class));
		verify(clientFileConnector, Mockito.never()).uploadFile(Mockito.any(DataInputStream.class), Mockito.any(DataOutputStream.class));
		verify(clientFileConnector, Mockito.never()).removeFile(Mockito.any(JsonObject.class), Mockito.any(DataOutputStream.class));
		verify(client, Mockito.never()).requestLocations(Mockito.any(DataOutputStream.class));
	}

	/**
	 * This test will verify if the downloadFile function is called with the correct action
	 *
	 * @throws Exception
	 */
	@Test
	public void handleDownloadFileAction() throws Exception {
		beforeHandleAction(Actions.GET_FILE);
		client.handleActions(dataInputStream, clientFileConnector, null);

		// Verify function calls
		verify(clientFileConnector, Mockito.never()).downloadKeyFile(Mockito.any(DataOutputStream.class));
		verify(clientFileConnector, Mockito.times(1)).downloadFile(Mockito.any(JsonObject.class), Mockito.any(DataOutputStream.class));
		verify(clientFileConnector, Mockito.never()).updateKeyFile(Mockito.any(DataInputStream.class), Mockito.any(DataOutputStream.class));
		verify(clientFileConnector, Mockito.never()).updateFile(Mockito.any(DataInputStream.class), Mockito.any(JsonObject.class), Mockito.any(DataOutputStream.class));
		verify(clientFileConnector, Mockito.never()).uploadFile(Mockito.any(DataInputStream.class), Mockito.any(DataOutputStream.class));
		verify(clientFileConnector, Mockito.never()).removeFile(Mockito.any(JsonObject.class), Mockito.any(DataOutputStream.class));
		verify(client, Mockito.never()).requestLocations(Mockito.any(DataOutputStream.class));
	}

	/**
	 * This test will verify if the updatekeyfile function is called with the correct action
	 *
	 * @throws Exception
	 */
	@Test
	public void handleUpdateKeyFileAction() throws Exception {
		beforeHandleAction(Actions.UPDATE_KEY_FILE);
		client.handleActions(dataInputStream, clientFileConnector, null);

		// Verify function calls
		verify(clientFileConnector, Mockito.never()).downloadKeyFile(Mockito.any(DataOutputStream.class));
		verify(clientFileConnector, Mockito.never()).downloadFile(Mockito.any(JsonObject.class), Mockito.any(DataOutputStream.class));
		verify(clientFileConnector, Mockito.times(1)).updateKeyFile(Mockito.any(DataInputStream.class), Mockito.any(DataOutputStream.class));
		verify(clientFileConnector, Mockito.never()).updateFile(Mockito.any(DataInputStream.class), Mockito.any(JsonObject.class), Mockito.any(DataOutputStream.class));
		verify(clientFileConnector, Mockito.never()).uploadFile(Mockito.any(DataInputStream.class), Mockito.any(DataOutputStream.class));
		verify(clientFileConnector, Mockito.never()).removeFile(Mockito.any(JsonObject.class), Mockito.any(DataOutputStream.class));
		verify(client, Mockito.never()).requestLocations(Mockito.any(DataOutputStream.class));
	}

	/**
	 * This test will verify if the updatefile function is called with the correct action
	 *
	 * @throws Exception
	 */
	@Test
	public void handleUpdateFileAction() throws Exception {
		beforeHandleAction(Actions.UPDATE_FILE);
		client.handleActions(dataInputStream, clientFileConnector, null);

		// Verify function calls
		verify(clientFileConnector, Mockito.never()).downloadKeyFile(Mockito.any(DataOutputStream.class));
		verify(clientFileConnector, Mockito.never()).downloadFile(Mockito.any(JsonObject.class), Mockito.any(DataOutputStream.class));
		verify(clientFileConnector, Mockito.never()).updateKeyFile(Mockito.any(DataInputStream.class), Mockito.any(DataOutputStream.class));
		verify(clientFileConnector, Mockito.times(1)).updateFile(Mockito.any(DataInputStream.class), Mockito.any(JsonObject.class), Mockito.any(DataOutputStream.class));
		verify(clientFileConnector, Mockito.never()).uploadFile(Mockito.any(DataInputStream.class), Mockito.any(DataOutputStream.class));
		verify(clientFileConnector, Mockito.never()).removeFile(Mockito.any(JsonObject.class), Mockito.any(DataOutputStream.class));
		verify(client, Mockito.never()).requestLocations(Mockito.any(DataOutputStream.class));
	}

	/**
	 * This test will verify if the uploadfile function is called with the correct action
	 *
	 * @throws Exception
	 */
	@Test
	public void handleUploadFileAction() throws Exception {
		beforeHandleAction(Actions.UPLOAD_FILE);
		client.handleActions(dataInputStream, clientFileConnector, null);

		// Verify function calls
		verify(clientFileConnector, Mockito.never()).downloadKeyFile(Mockito.any(DataOutputStream.class));
		verify(clientFileConnector, Mockito.never()).downloadFile(Mockito.any(JsonObject.class), Mockito.any(DataOutputStream.class));
		verify(clientFileConnector, Mockito.never()).updateKeyFile(Mockito.any(DataInputStream.class), Mockito.any(DataOutputStream.class));
		verify(clientFileConnector, Mockito.never()).updateFile(Mockito.any(DataInputStream.class), Mockito.any(JsonObject.class), Mockito.any(DataOutputStream.class));
		verify(clientFileConnector, Mockito.times(1)).uploadFile(Mockito.any(DataInputStream.class), Mockito.any(DataOutputStream.class));
		verify(clientFileConnector, Mockito.never()).removeFile(Mockito.any(JsonObject.class), Mockito.any(DataOutputStream.class));
		verify(client, Mockito.never()).requestLocations(Mockito.any(DataOutputStream.class));
	}

	/**
	 * This test will verify if the removefile function is called with the correct action
	 *
	 * @throws Exception
	 */
	@Test
	public void handleRemoveFileAction() throws Exception {
		beforeHandleAction(Actions.REMOVE_FILE);
		client.handleActions(dataInputStream, clientFileConnector, null);

		// Verify function calls
		verify(clientFileConnector, Mockito.never()).downloadKeyFile(Mockito.any(DataOutputStream.class));
		verify(clientFileConnector, Mockito.never()).downloadFile(Mockito.any(JsonObject.class), Mockito.any(DataOutputStream.class));
		verify(clientFileConnector, Mockito.never()).updateKeyFile(Mockito.any(DataInputStream.class), Mockito.any(DataOutputStream.class));
		verify(clientFileConnector, Mockito.never()).updateFile(Mockito.any(DataInputStream.class), Mockito.any(JsonObject.class), Mockito.any(DataOutputStream.class));
		verify(clientFileConnector, Mockito.never()).uploadFile(Mockito.any(DataInputStream.class), Mockito.any(DataOutputStream.class));
		verify(clientFileConnector, Mockito.times(1)).removeFile(Mockito.any(JsonObject.class), Mockito.any(DataOutputStream.class));
		verify(client, Mockito.never()).requestLocations(Mockito.any(DataOutputStream.class));
	}

	/**
	 * This test will verify if the requestLocations function is called with the correct action
	 *
	 * @throws Exception
	 */
	@Test
	public void handleRequestLocations() throws Exception {
		beforeHandleAction(Actions.REQUEST_LOCATIONS);
		client.handleActions(dataInputStream, clientFileConnector, null);

		verify(clientFileConnector, Mockito.never()).downloadKeyFile(Mockito.any(DataOutputStream.class));
		verify(clientFileConnector, Mockito.never()).downloadFile(Mockito.any(JsonObject.class), Mockito.any(DataOutputStream.class));
		verify(clientFileConnector, Mockito.never()).updateKeyFile(Mockito.any(DataInputStream.class), Mockito.any(DataOutputStream.class));
		verify(clientFileConnector, Mockito.never()).updateFile(Mockito.any(DataInputStream.class), Mockito.any(JsonObject.class), Mockito.any(DataOutputStream.class));
		verify(clientFileConnector, Mockito.never()).uploadFile(Mockito.any(DataInputStream.class), Mockito.any(DataOutputStream.class));
		verify(clientFileConnector, Mockito.never()).removeFile(Mockito.any(JsonObject.class), Mockito.any(DataOutputStream.class));
		verify(client, Mockito.times(1)).requestLocations(Mockito.any(DataOutputStream.class));
	}

	/**
	 * This test will verify if the requestLocations function is correctly executed
	 *
	 * @throws Exception
	 */
	@Test
	public void requestLocations() throws IOException {
		Mockito.doCallRealMethod().when(client).requestLocations(Mockito.any(DataOutputStream.class));

		final String location1 = "SomeLocation";
		final String location2 = "Another Location";
		final String location3 = "gysg8fh390jifda";

		Set<String> locations = new HashSet<>();
		locations.add(location1);
		locations.add(location2);
		locations.add(location3);

		UserFile userFileMock = mock(UserFile.class);
		when(userFileMock.getLocations()).thenReturn(locations);
		Whitebox.setInternalState(client, UserFile.class, userFileMock);

		ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
		client.requestLocations(new DataOutputStream(byteOut));

		DataInputStream dataIn = new DataInputStream(new ByteArrayInputStream(byteOut.toByteArray()));

		Gson gson = new Gson();

		JsonObject jObject = gson.fromJson(dataIn.readUTF(), JsonObject.class);
		Type collectionType = new TypeToken<Set<String>>() {
		}.getType();
		Set<String> locationsFromJson = gson.fromJson(jObject.get(Responses.LOCATIONS), collectionType);

		assertEquals(3, locationsFromJson.size());
		assertTrue(locationsFromJson.contains(location1));
		assertTrue(locationsFromJson.contains(location2));
		assertTrue(locationsFromJson.contains(location3));

	}
}
