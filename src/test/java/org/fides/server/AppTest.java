package org.fides.server;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class AppTest {

	private Server server;

	/**
	 * Initiates the test
	 */
	@Before
	public void runBefore() {

		try {
			server = new Server(4444);
			Thread serverThread = new Thread(server);
			serverThread.start();
		}
		catch (IOException e) {
			fail("IOException");
		}
	}

	/**
	 * Tears the test down
	 */
	@After
	public void runAfter() {
		server.kill();
	}

	/**
	 * Tests whether the socket is connected
	 */
	@Test
	public void testSocketConnectionIsConnected() {

		Socket client;
		try {
			client = new Socket("localhost", 4444);

			assertTrue(client.isConnected());
			OutputStream outToServer = client.getOutputStream();
			DataOutputStream out =
				new DataOutputStream(outToServer);

			out.writeUTF("Hello from "
				+ client.getLocalSocketAddress());
			InputStream inFromServer = client.getInputStream();
			DataInputStream in =
				new DataInputStream(inFromServer);
			System.out.println("Server says " + in.readUTF());

			client.close();
		}
		catch (UnknownHostException e) {
			fail("UnknownHostException");
		}
		catch (IOException e) {
			fail("IOException");
		}

	}
}
