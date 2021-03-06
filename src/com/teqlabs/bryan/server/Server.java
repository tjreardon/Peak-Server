package com.teqlabs.bryan.server;

//import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import net.xeoh.plugins.base.PluginManager;
import net.xeoh.plugins.base.impl.PluginManagerFactory;
import net.xeoh.plugins.base.util.uri.ClassURI;

public class Server {
  
  private final static int RECV_BUF_LEN = 1024; // receive buffer length
  
  private PluginManager pluginManager = PluginManagerFactory.createPluginManager();
  private final static Logger LOG = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
  private Configuration config = new Configuration(); // default configuration for the server
  private List<Client> clients = new ArrayList<Client>();
  private boolean running = false; // specifies if server is running

  public Server() { // create server from default configuration settings
  }

  public Server(Configuration c) { // create server from configuration
    this.config = c;
  }
  
  public void loadPlugins() {
	  this.pluginManager.addPluginsFrom(ClassURI.CLASSPATH);
  }
  
  public void run() throws IOException {
	  
	//Setup Logging environment

	FileHandler file = new FileHandler(config.serverLog);
	file.setFormatter(new SimpleFormatter());
	LOG.setLevel(Level.INFO);
	LOG.addHandler(file);
    
	// create server socket and set timeout to 0
	ServerSocket ss = new ServerSocket(config.port, 0, InetAddress.getByName("0.0.0.0"));
	ss.setSoTimeout(0);
	
	LOG.info("Server running on port: " + config.port);
	
	// create new client
	Client client = new Client(ss, RECV_BUF_LEN, pluginManager, LOG);
	
	// while client is accepting create new client thread for the client
	while(client.accepting) {
		if(client.canAuthenticate(config.authentication)) {
			this.clients.add(client);
			(new ClientThread(client)).start();
		} else {
			client.close(); // close connection to client because it can't authenticate
		}
		client = new Client(ss, RECV_BUF_LEN, pluginManager, LOG); // create new client
	}
    
  }
  
  public boolean isRunning() {
	return this.running;
  }
  
  public PluginManager getPluginManager() {
	  return this.pluginManager;
  }

}