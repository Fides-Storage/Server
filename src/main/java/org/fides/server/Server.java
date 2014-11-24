package org.fides.server;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStore;
import java.util.Properties;

import org.fides.server.tools.PropertiesManager;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

/**
 * 
 * @author Niels en Jesse
 * 
 */
public class Server implements Runnable {

	//private ServerSocket listener;
	private SSLServerSocket sslServerSocket;

	private volatile boolean isRunning = true;

	/**
	 * Constructor to create a new server socket
	 * 
	 * @throws IOException
	 *             Throws an IOException if the connection can't be made
	 */
	public Server() throws IOException {

		PropertiesManager pm = PropertiesManager.getInstance();


		SSLServerSocketFactory ssf = null;

		try {
			// set up key manager to do server authentication
			SSLContext ctx;
			KeyManagerFactory kmf;
			KeyStore ks;
			char[] passphrase = "12345678".toCharArray();

			ctx = SSLContext.getInstance("TLS");
			kmf = KeyManagerFactory.getInstance("SunX509");
			ks = KeyStore.getInstance("JKS");
			ks.load(new FileInputStream("/home/tom/Development/Prive/School/SchoolGit/cert/keystore.jks"), passphrase);
			kmf.init(ks, passphrase);
			ctx.init(kmf.getKeyManagers(), null, null);

			ssf = ctx.getServerSocketFactory();
		} catch (Exception e) {
			e.printStackTrace();
		}

		sslServerSocket = (SSLServerSocket) ssf.createServerSocket(pm.getPort());

//		SSLServerSocketFactory sslserversocketfactory =
//			(SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
//		sslServerSocket = (SSLServerSocket) sslserversocketfactory.createServerSocket(pm.getPort());




		System.out.println("Starting up the Fides server on port: " + pm.getPort());
		System.out.println("Using user directory: " + pm.getUserDir());
		System.out.println("Using data directory: " + pm.getDataDir());

		//listener = new ServerSocket(pm.getPort());
	}

	/**
	 * Accepting client connections
	 */
	public void run() {

		try {

			SSLSocket sslsocket;

			while (isRunning) {

				sslsocket = (SSLSocket) sslServerSocket.accept();
				ClientConnection client = new ClientConnection(sslsocket);
				Thread t = new Thread(client);
				t.start();
			}
		} catch (IOException e) {
			System.out.println("IOException on socket listen: " + e.getMessage());

		} finally {

			try {
				sslServerSocket.close();
			} catch (IOException e) {
				System.out.println("IOException on socket listen: " + e.getMessage());

			}
		}
	}

	/**
	 * Kills the running thread
	 */
	public void kill() {
		isRunning = false;
	}
}
