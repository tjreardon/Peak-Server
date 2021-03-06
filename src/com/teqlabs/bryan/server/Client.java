package com.teqlabs.bryan.server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Logger;

import com.teqlabs.bryan.commands.Command;
import com.teqlabs.bryan.commands.CommandCore;
import com.teqlabs.bryan.commands.CommandThread;

//import net.homeip.mleclerc.omnilink.messagebase.ReplyMessage;
import net.xeoh.plugins.base.PluginManager;

public class Client implements Runnable {

	protected Socket conn = null;
	protected boolean accepting = false;
	private BufferedInputStream incoming = null;
	private BufferedOutputStream outgoing = null;
	private PluginManager pluginManager = null;
	private CommandCore commands = new CommandCore();
	private Logger Log;
	
	Client(ServerSocket ss, int recvBufLen, PluginManager pluginManager, Logger log) throws IOException {
		this.conn = ss.accept();
		this.accepting = true;
		this.incoming = new BufferedInputStream(conn.getInputStream());
		this.outgoing = new BufferedOutputStream(conn.getOutputStream());
		this.pluginManager = pluginManager;
		this.Log = log;
	}

	@Override
	public void run() {
		
		try {
			
			//While client is sending
			while(this.accepting) {
				
				//Read input stream
				byte[] bytes = readInputStream();
				if (bytes == null)
					break;
				
				//Create command string
				String json = new String(bytes, "UTF8");
				Command command = new Command(json, commands, pluginManager, Log);
				
				try {
					(new CommandThread(command)).start();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			//Finally close the connection
			close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void close() throws IOException {
		Log.info("Connection to client closed.");
		this.conn.close();
	}
	
	public byte[] readInputStream() throws IOException {
		
		byte[] bytes = new byte[1024];
		int byteCount = incoming.read(bytes);
		
		if(byteCount == -1) {
			this.accepting = false;
			return null;
		}
		
		return bytes;
	}

	public boolean canAuthenticate(String authentication) throws IOException {
		
		//Initiate byte array for incoming key;
		byte[] bytes = new byte[88];
		int byteCount = this.incoming.read(bytes);
		
		//Check the connection to socket; false if connection is closed;
		if(byteCount == -1) {
			this.accepting = false;
		}
		
		//Check if incoming key matches our authentication string
		if(authentication.equals(new String(bytes, "UTF8"))) {
			Log.info("Client authenticated successfully!");
			this.accepting = true;
		} else {
			Log.info("Client failed to authenticate!");
			this.accepting = false;
		}
		
		//Return response to client
		writeOutputStream(Boolean.toString(accepting));
				
		return this.accepting;
	}

	public void writeOutputStream(String s) throws IOException {
		
		//Write to the BufferedOutputStream and flush it.
		byte[] response = s.getBytes();
		this.outgoing.write(response, 0, response.length);
		this.outgoing.flush();

	}

}