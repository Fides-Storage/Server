package org.fides.server;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateFactory;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

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
			server = new Server();
			Thread serverThread = new Thread(server);
			serverThread.start();
		} catch (IOException e) {
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
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			System.out.println(e.getMessage());
		}
		server.kill();
	}

	// TODO: Add actual assertions.
	/**
	 * Tests whether the socket is connected
	 */
	@Test
	public void testSocketConnectionIsConnected() {


		//For testing purposes only
		Properties systemProps = System.getProperties();
		systemProps.put( "javax.net.ssl.trustStore", "/home/tom/Development/Prive/School/SchoolGit/cert/truststore.ts");
		systemProps.put("javax.net.ssl.trustStorePassword", "");
		System.setProperties(systemProps);

		SSLSocket sslsocket;
		try {
			SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
			sslsocket = (SSLSocket) sslsocketfactory.createSocket("localhost", 4444);

			SSLContext context = SSLContext.getInstance("TLS");

			SSLSession session = sslsocket.getSession();
			java.security.cert.Certificate[] servercerts = session.getPeerCertificates();

			Gson gson = new Gson();

			assertTrue(sslsocket.isConnected());

			JsonObject obj = new JsonObject();
			obj.addProperty("username", "ThisisKoen");
			obj.addProperty("password", "Thisisapassword");

			OutputStream outToServer = sslsocket.getOutputStream();
			DataOutputStream out = new DataOutputStream(outToServer);

			out.writeUTF(gson.toJson(obj));

			sslsocket.close();
		} catch (UnknownHostException e) {
			fail("UnknownHostException");
		} catch (IOException e) {
			fail("IOException");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}
}
