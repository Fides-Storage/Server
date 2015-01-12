package org.fides.server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.fides.components.Actions;
import org.fides.server.files.UserFile;
import org.fides.server.files.UserManager;
import org.fides.server.tools.UserLocker;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import javax.net.ssl.SSLSocket;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

/**
 * This unittest tests the Client class
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ UserLocker.class, UserManager.class })
public class ClientTest {

	private static SSLSocket sslSocket = mock(SSLSocket.class);

	private static Client client = mock(Client.class);

	private static DataInputStream dataInputStream = null;

	private static ClientFileConnector clientFileConnector;

	/**
	 * This function is called before the beginning of this class. It makes sure that the handleActions and run function
	 * is called by the real method and not the mocked one. It also sets the server variable in the client.
	 *
	 * @throws IOException
	 */
	@BeforeClass
	public static void beforeClass() throws IOException {
		// Setup client
		Mockito.doCallRealMethod().when(client).handleActions(Mockito.any(DataInputStream.class), Mockito.any(ClientFileConnector.class), Mockito.any(DataOutputStream.class));
		Mockito.doCallRealMethod().when(client).run();
		Whitebox.setInternalState(client, "server", sslSocket);

		when(sslSocket.getOutputStream()).thenReturn(new ByteArrayOutputStream());
	}

	/**
	 * This function should be called before every test. It mocks the UserLocker, UserManager and ClientFileConnector.
	 */
	@Before
	public void before() {
		PowerMockito.mockStatic(UserLocker.class);
		PowerMockito.mockStatic(UserManager.class);
		clientFileConnector = mock(ClientFileConnector.class);
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
		verify(clientFileConnector, Mockito.never()).updateKeyFile(Mockito.any(InputStream.class), Mockito.any(DataOutputStream.class));
		verify(clientFileConnector, Mockito.never()).updateFile(Mockito.any(InputStream.class), Mockito.any(JsonObject.class), Mockito.any(DataOutputStream.class));
		verify(clientFileConnector, Mockito.never()).uploadFile(Mockito.any(InputStream.class), Mockito.any(DataOutputStream.class));
		verify(clientFileConnector, Mockito.never()).removeFile(Mockito.any(JsonObject.class), Mockito.any(DataOutputStream.class));
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
		verify(clientFileConnector, Mockito.never()).updateKeyFile(Mockito.any(InputStream.class), Mockito.any(DataOutputStream.class));
		verify(clientFileConnector, Mockito.never()).updateFile(Mockito.any(InputStream.class), Mockito.any(JsonObject.class), Mockito.any(DataOutputStream.class));
		verify(clientFileConnector, Mockito.never()).uploadFile(Mockito.any(InputStream.class), Mockito.any(DataOutputStream.class));
		verify(clientFileConnector, Mockito.never()).removeFile(Mockito.any(JsonObject.class), Mockito.any(DataOutputStream.class));
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
		verify(clientFileConnector, Mockito.times(1)).updateKeyFile(Mockito.any(InputStream.class), Mockito.any(DataOutputStream.class));
		verify(clientFileConnector, Mockito.never()).updateFile(Mockito.any(InputStream.class), Mockito.any(JsonObject.class), Mockito.any(DataOutputStream.class));
		verify(clientFileConnector, Mockito.never()).uploadFile(Mockito.any(InputStream.class), Mockito.any(DataOutputStream.class));
		verify(clientFileConnector, Mockito.never()).removeFile(Mockito.any(JsonObject.class), Mockito.any(DataOutputStream.class));
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
		verify(clientFileConnector, Mockito.never()).updateKeyFile(Mockito.any(InputStream.class), Mockito.any(DataOutputStream.class));
		verify(clientFileConnector, Mockito.times(1)).updateFile(Mockito.any(InputStream.class), Mockito.any(JsonObject.class), Mockito.any(DataOutputStream.class));
		verify(clientFileConnector, Mockito.never()).uploadFile(Mockito.any(InputStream.class), Mockito.any(DataOutputStream.class));
		verify(clientFileConnector, Mockito.never()).removeFile(Mockito.any(JsonObject.class), Mockito.any(DataOutputStream.class));
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
		verify(clientFileConnector, Mockito.never()).updateKeyFile(Mockito.any(InputStream.class), Mockito.any(DataOutputStream.class));
		verify(clientFileConnector, Mockito.never()).updateFile(Mockito.any(InputStream.class), Mockito.any(JsonObject.class), Mockito.any(DataOutputStream.class));
		verify(clientFileConnector, Mockito.times(1)).uploadFile(Mockito.any(InputStream.class), Mockito.any(DataOutputStream.class));
		verify(clientFileConnector, Mockito.never()).removeFile(Mockito.any(JsonObject.class), Mockito.any(DataOutputStream.class));
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
		verify(clientFileConnector, Mockito.never()).updateKeyFile(Mockito.any(InputStream.class), Mockito.any(DataOutputStream.class));
		verify(clientFileConnector, Mockito.never()).updateFile(Mockito.any(InputStream.class), Mockito.any(JsonObject.class), Mockito.any(DataOutputStream.class));
		verify(clientFileConnector, Mockito.never()).uploadFile(Mockito.any(InputStream.class), Mockito.any(DataOutputStream.class));
		verify(clientFileConnector, Mockito.times(1)).removeFile(Mockito.any(JsonObject.class), Mockito.any(DataOutputStream.class));
	}

}
