package org.fides.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.fides.server.tools.PropertiesManager;

/**
 * 
 * @author Niels en Jesse
 * 
 */
public class Server implements Runnable {

  private ServerSocket listener;

  private volatile boolean isRunning = true;

  /**
   * Constructor to create a new server socket
   * 
   * @throws IOException
   *           Throws an IOException if the connection can't be made
   */
  public Server() throws IOException {
    PropertiesManager pm = PropertiesManager.getInstance();

    System.out.println("Starting up the Fides server on port: " + pm.getPort());
    System.out.println("Using user directory: " + pm.getUserDir());
    System.out.println("Using data directory: " + pm.getDataDir());

    listener = new ServerSocket(pm.getPort());
  }

  /**
   * Accepting client connections
   */
  public void run() {

    try {

      Socket server;

      while (isRunning) {

        server = listener.accept();
        ClientConnection client = new ClientConnection(server);
        Thread t = new Thread(client);
        t.start();
      }
    } catch (IOException e) {
      System.err.println("IOException on socket listen: " + e.getMessage());
    } finally {

      try {
        listener.close();
      } catch (IOException e) {
        System.err.println("IOException on socket listen: " + e.getMessage());
      }
    }
  }

  /**
   * Kills the running thread
   */
  public void kill() {
    isRunning = false;

    try {
      new Socket("127.0.0.1", listener.getLocalPort()).close();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }
}
