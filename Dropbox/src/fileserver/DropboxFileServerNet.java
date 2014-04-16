package fileserver;

import java.util.*;
import java.util.concurrent.locks.*;

import common.DropboxConstants;

/**
 * 
 * Class: DropboxFileServerNet
 * Description: Used for handling the network request from client.
 */
/**
 * TODO: should add the network interface for the connection to master node
 * 
 */
class DropboxFileServerNet {

	private Thread _clientThread;
	private Thread _userThread;
	private DropboxFileServer _server;
	private ReentrantLock _lock;
	private DropboxFileServerMasterNet _net; // in main thread
	
	private Map<String, String> _map;// future use
	
	private void _dlog(String str){
		if(_server.debugMode())
			System.out.println("[DropboxFileServerNet (DEBUG)]:" + str);
	}
	
	private void _elog(String str){
		System.err.println("[DropboxFileServerNet (ERROR)]:" + str);
	}
	
	private void _log(String str){
		System.out.println("[DropboxFileServerNet]:" + str);
	}
	
	public DropboxFileServerNet(DropboxFileServer server){
		// TODO: map is for future use
		_server = server;
		assert _server != null;
		_lock = new ReentrantLock();
		_net = new DropboxFileServerMasterNet(_server);
	}
	
	public void openConnections(){
		_log("Try to connect to Master...");
		
		_dlog("openConnections(){");
		boolean isConnected = _net.connect();
		_dlog("}");
		
		if(!isConnected){
			_log("Cannot connect to master");
			//TODO: make it better, not just simply kill the whole
			System.exit(1);
		}else{
			_log("Success!");
			_dlog("Spawn a new thread to listen to new clients");
			_clientThread = new Thread(new ClientListner(_server));
			_clientThread.start();
			// Master net also spawn a new thread to get user input
			// TODO: should spawn another new thread to get user input to simulate crash, etc.
			_net.spawnUserThread();
			_net.listen();
		}
	}
	
	public void closeConnections(){
		_log("Close the connection...");
		_dlog("closeConnections(){");
		_net.close();
		_dlog("}");
	}
	
	/**
	 * class ClientListener
	 * Description: The thread for listening new client
	 */
	private class ClientListner implements Runnable {
		
		private DropboxFileServerClientNet _net;
		
		public ClientListner(DropboxFileServer server){	
			_net = new DropboxFileServerClientNet(server);
		}
		
		public void run(){
			_net.listen();
		}
	}
}
