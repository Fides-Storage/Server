package org.fides.server;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.fides.server.tools.PropertiesManager;
import org.junit.After;
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

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * Unit test for simple App.
 */
@PowerMockIgnore("javax.net.ssl.*")
@RunWith(PowerMockRunner.class)
@PrepareForTest(PropertiesManager.class)
public class AppTest {

	private static File testUserDir;

	private static File testDataDir;

	private static int port = 4444;

	private static PropertiesManager mockedPropertiesManager = Mockito.mock(PropertiesManager.class);

	private Server server;

	/**
	 * Sets up the test class by adding a the necessary temporary files to the test folder.
	 */
	@BeforeClass
	public static void setUp() {
		//For testing purposes only
		Properties systemProps = System.getProperties();
		systemProps.put("javax.net.ssl.trustStore", "/home/tom/Development/Prive/School/SchoolGit/cert/truststore.ts");
		systemProps.put("javax.net.ssl.trustStorePassword", "");
		System.setProperties(systemProps);

		try {
			testUserDir = new File(PropertiesManager.getInstance().getUserDir(), "Test");
			if (!testUserDir.exists()) {
				testUserDir.mkdirs();
			}
			testDataDir = new File(PropertiesManager.getInstance().getDataDir(), "Test");
			if (!testDataDir.exists()) {
				testDataDir.mkdirs();
			}

			// This causes the mocked PropertiesManager to always return the test Data directory:
			Mockito.when(mockedPropertiesManager.getUserDir()).thenReturn(testUserDir.getAbsolutePath());
			Mockito.when(mockedPropertiesManager.getDataDir()).thenReturn(testDataDir.getAbsolutePath());
			Mockito.when(mockedPropertiesManager.getPort()).thenReturn(port);
			Mockito.when(mockedPropertiesManager.getKeystorePath()).thenReturn("./keystore.jks");
			Mockito.when(mockedPropertiesManager.getKeystorePassword()).thenReturn("12345678".toCharArray());

		} catch (Exception e) {
			fail("Unexpected error in setUp: " + e.getMessage());
		}
	}

	/**
	 * Mocks the PropertiesManager to always return a mocked version of the PropertiesManager. This
	 * will cause the FileManager to use a testfolder instead of the main folder.
	 */
	@Before
	public void setUpMock() {
		PowerMockito.mockStatic(PropertiesManager.class);
		Mockito.when(PropertiesManager.getInstance()).thenReturn(mockedPropertiesManager);

		try {
			server = new Server();
			Thread serverThread = new Thread(server);
			serverThread.start();
		} catch (IOException e) {
			fail("IOException");
		}
	}

	// TODO: Add actual assertions.

	/**
	 * Tests whether the socket is connected
	 */
	@Test
	public void testSocketConnectionIsConnected() {

		SSLSocket sslsocket;
		try {
			SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
			sslsocket = (SSLSocket) sslsocketfactory.createSocket("localhost", 4444);

			SSLContext context = SSLContext.getInstance("TLS");

			SSLSession session = sslsocket.getSession();
			java.security.cert.Certificate[] servercerts = session.getPeerCertificates();

			Gson gson = new Gson();

			assertTrue(sslsocket.isConnected());



			OutputStream outToServer = sslsocket.getOutputStream();
			DataOutputStream out = new DataOutputStream(outToServer);


			sslsocket.close();
		} catch (UnknownHostException e) {
			fail("UnknownHostException");
		} catch (IOException e) {
			fail("IOException");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Tests whether the socket is connected
	 */
	@Test
	public void testSocketConnectionCreateUser() {
		SSLSocket client;
		try {

			SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
			client = (SSLSocket) sslsocketfactory.createSocket("localhost", 4444);

			assertTrue(client.isConnected());

			JsonObject obj = new JsonObject();
			obj.addProperty("action", "createUser");
			obj.addProperty("username", "ThisisKoen");
			obj.addProperty("passwordHash", "Thisisapassword");

			OutputStream outToServer = client.getOutputStream();
			DataOutputStream out = new DataOutputStream(outToServer);

			out.writeUTF(new Gson().toJson(obj));

			InputStream inFromServer = client.getInputStream();
			DataInputStream in = new DataInputStream(inFromServer);

			JsonObject jobj = new Gson().fromJson(in.readUTF(), JsonObject.class);
			assertTrue(jobj.has("succesfull"));
			System.out.println(jobj.toString());
			assertTrue(jobj.get("succesfull").getAsBoolean());

			client.close();
		} catch (UnknownHostException e) {
			fail("UnknownHostException");
		} catch (IOException e) {
			fail("IOException");
		}
	}

	/**
	 * Tears the test down
	 * <p/>
	 * Sleep is used to wait for the running thread in Server to complete
	 */
	@After
	public void runAfter() {
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			System.out.println(e.getMessage());
		}
		server.kill();
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
