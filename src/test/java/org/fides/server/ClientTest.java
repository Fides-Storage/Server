package org.fides.server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * This unittest tests the Client class
 */
public class ClientTest {


	private SSLSocket mocketSSLSocket = mock(SSLSocket.class);

	private ByteArrayInputStream mocketInputStream;

	private ByteArrayOutputStream mocketOutputStream = new ByteArrayOutputStream();

	/**
	 * Tests whether the socket is connected
	 */
	@Test
	public void testSocketConnectionCreateUser() {

//		JsonObject user = new JsonObject();
//		user.addProperty("action", "createUser");
//		user.addProperty("username", "ThisisKoen");
//		user.addProperty("passwordHash", "Thisisapassword");
//
//		mocketInputStream = new ByteArrayInputStream(new Gson().toJson(user).getBytes());
//		when(mocketSSLSocket.getInputStream()).thenReturn();
//
//		Client client = new Client((SSLSocket) mocketSSLSocket);
//
//
//
//
//		try {
//			assertTrue(client.authenticateUser(user, new DataOutputStream(mocketSSLSocket.getOutputStream())));
//		} catch (IOException e) {
//			e.printStackTrace();
//		}

//		try {
//
//			OutputStream outToServer = client.getOutputStream();
//			DataOutputStream out = new DataOutputStream(outToServer);
//
//			out.writeUTF(new Gson().toJson(obj));
//
//			InputStream inFromServer = client.getInputStream();
//			DataInputStream in = new DataInputStream(inFromServer);
//
//			JsonObject jobj = new Gson().fromJson(in.readUTF(), JsonObject.class);
//			assertTrue(jobj.has("succesfull"));
//			System.out.println(jobj.toString());
//			assertTrue(jobj.get("succesfull").getAsBoolean());
//
//			client.close();
//		} catch (UnknownHostException e) {
//			fail("UnknownHostException");
//		} catch (IOException e) {
//			fail("IOException");
//		}
	}

}
