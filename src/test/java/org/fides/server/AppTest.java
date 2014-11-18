package org.fides.server;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

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
	 * 
	 * Sleep is used to wait for the running thread in Server to complete
	 * 
	 */
	@After
	public void runAfter() {
		try {
			Thread.sleep(1000);
		}
		catch (InterruptedException e) {
			System.out.println(e.getMessage());
		}
		server.kill();
	}

	/**
	 * Tests whether the socket is connected
	 */
	@Test
	public void testSocketConnectionIsConnected() {
		Socket client;
		try {
			Gson gson = new Gson();

			client = new Socket("localhost", 4444);

			assertTrue(client.isConnected());

			JsonObject obj = new JsonObject();
			obj.addProperty("username", "ThisisKoen");
			obj.addProperty("password", "Thisisapassword");

			OutputStream outToServer = client.getOutputStream();
			DataOutputStream out =
				new DataOutputStream(outToServer);

			out.writeUTF(gson.toJson(obj));

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
