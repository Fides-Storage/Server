package org.fides.server;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Properties;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.fides.server.tools.PropertiesManager;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * This unittest tests the Server class
 */
@PowerMockIgnore("javax.net.ssl.*")
@RunWith(PowerMockRunner.class)
@PrepareForTest(PropertiesManager.class)
public class ServerTest {

	private static final int PORT = 4444;

	private static final String TRUSTSTOREPATH = "./truststore.ts";

	private static final String KEYSTOREPATH = "./keystore.jks";

	private static final char[] KEYSTOREPASSWORD = "12345678".toCharArray();

	private static PropertiesManager mockedPropertiesManager = Mockito.mock(PropertiesManager.class);

	/**
	 * Sets up the test class by adding a the necessary temporary files to the test folder.
	 */
	@BeforeClass
	public static void setUp() {

		// For testing purposes only
		Properties systemProps = System.getProperties();
		systemProps.put("javax.net.ssl.trustStore", TRUSTSTOREPATH);
		systemProps.put("javax.net.ssl.trustStorePassword", "");
		System.setProperties(systemProps);

		try {
			// This causes the mocked PropertiesManager to always return the test Data directory:
			Mockito.when(mockedPropertiesManager.getPort()).thenReturn(PORT);
			Mockito.when(mockedPropertiesManager.getKeystorePath()).thenReturn(KEYSTOREPATH);
			Mockito.when(mockedPropertiesManager.getKeystorePassword()).thenReturn(KEYSTOREPASSWORD);
		} catch (Exception e) {
			fail("Unexpected error in setUp: " + e.getMessage());
		}
	}

	/**
	 * Mocks the PropertiesManager to always return a mocked version of the PropertiesManager. This will cause the
	 * FileManager to use a testfolder instead of the main folder.
	 */
	@Before
	public void setUpMock() {
		PowerMockito.mockStatic(PropertiesManager.class);
		Mockito.when(PropertiesManager.getInstance()).thenReturn(mockedPropertiesManager);
	}

	/**
	 * Tests whether the socket is connected
	 */
	@Test
	public void testSocketConnectionIsConnected() {

		Server server = null;
		try {
			// Starting a Server
			server = new Server();
			Thread serverThread = new Thread(server);
			serverThread.start();

			// Starting the connection
			SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
			SSLSocket sslsocket = (SSLSocket) sslsocketfactory.createSocket("localhost", PORT);

			// Assert that the connection is active
			assertTrue(sslsocket.isConnected());

			// Close connection
			sslsocket.close();

		} catch (UnknownHostException e) {
			fail("UnknownHostException");
		} catch (IOException e) {
			fail("IOException");
		} finally {
			if (server != null) {
				server.kill();
			}
		}
	}

}
