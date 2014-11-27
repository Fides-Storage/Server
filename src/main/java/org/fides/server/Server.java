package org.fides.server;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

import org.apache.commons.io.IOUtils;
import org.fides.server.tools.PropertiesManager;

/**
 * @author Niels en Jesse
 */
public class Server implements Runnable {

	// private ServerSocket listener;
	private SSLServerSocket sslServerSocket;

	private volatile boolean isRunning = true;

	/**
	 * Constructor to create a new server socket
	 * 
	 * @throws IOException
	 *             Throws an IOException if the connection can't be made
	 */
	public Server() throws IOException {

		// Instantiating the propertiesmanager
		PropertiesManager propertiesManager = PropertiesManager.getInstance();

		try {
			// Set up the key manager for server authentication
			SSLContext sslContext = SSLContext.getInstance("TLS");
			KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
			KeyStore keyStore = KeyStore.getInstance("JKS");

			// Load the given keystore with the given password
			keyStore.load(new FileInputStream(propertiesManager.getKeystorePath()), propertiesManager.getKeystorePassword());
			keyManagerFactory.init(keyStore, propertiesManager.getKeystorePassword());

			// Load the keymanagers in the sslcontext
			sslContext.init(keyManagerFactory.getKeyManagers(), null, null);

			// Create a SSLServerSocketFactory from the SSLContext
			SSLServerSocketFactory sslServerSocketFactory = sslContext.getServerSocketFactory();

			// Create the SSLServerSocket from the factory on the given port
			sslServerSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(propertiesManager.getPort());
		} catch (Exception e) {
			e.printStackTrace();
		}

		// TODO: Printing usefull information
		System.out.println("Server started on port: " + propertiesManager.getPort());
		System.out.println("Using user directory: " + propertiesManager.getUserDir());
		System.out.println("Using data directory: " + propertiesManager.getDataDir());

	}

	/**
	 * Accepting client connections
	 */
	public void run() {

		while (isRunning) {
			try {

				// The SSLSocket that will handle the connection
				// Listens for a connection to be made to this socket and accepts
				SSLSocket sslsocket = (SSLSocket) sslServerSocket.accept();
				// Set the socket timeout on 10 seconds, when changing this value change it also on the client
				sslsocket.setSoTimeout(10000);
				// Create a client object from the connection
				Client client = new Client(sslsocket);
				// Start a thread with the created Client
				Thread t = new Thread(client);
				t.start();

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
		IOUtils.closeQuietly(sslServerSocket);

	}
}
