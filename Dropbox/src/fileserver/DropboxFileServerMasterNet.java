package fileserver;

import java.io.*;
import java.net.*;
import java.util.*;

import common.DropboxConstants;
import common.ProtocolConstants;

/**
 * 
 * Class: DropboxFileServerMasterNet
 * Description: Handles the connection to master net
 */
class DropboxFileServerMasterNet {

	private DropboxFileServer _server;
	private DropboxFileServerUserNet _userNet;
	
	private Socket _sock;
	private PrintWriter _out;
	private BufferedReader _in;
	
	private Socket _userSock;
	private PrintWriter _userOut;
	private BufferedReader _userIn;
	
	/* Always use that for now */
	private String _serverIP = DropboxConstants.MASTER_IP;
	private int _serverPort = DropboxConstants.MASTER_CLUSTER_PORT;
	
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
	
	public DropboxFileServerMasterNet(DropboxFileServer server){
		// TODO: map is for future use
		_server = server;
		assert _server != null;
		//_net = new DropboxFileServerMasterNettmp(_server);
	}
	
	public boolean openConnections(){
		_log("Try to connect to Master...");
		
		_dlog("openConnections(){");
		boolean isConnected = connect();
		_dlog("}");
		
		if(!isConnected){
			_log("Cannot connect to Master");
			//TODO: make it better, not just simply kill the whole
		}else{
			_log("Success!");
			//_clientThread = new Thread(new ClientListner(_server));
			//_clientThread.start();
			// Master net also spawn a new thread to get user input
			// TODO: should spawn another new thread to get user input to simulate crash, etc.
			//_net.spawnUserThread();
			//_net.listen();
		}
		return isConnected;
	}
	
	public void listen(){
		// Firstly spawn a new thread and then the main thread also start listening
		_userNet = new DropboxFileServerUserNet(_server, _userOut, _userIn, _userSock);
		_userNet.start();
		
		/* Use main thread, cannot be stopped */
		while(!_sock.isClosed()){
			try
			{
				String line = receive(_in); // if it receives null, it means the connection is broken
				_dlog(line);
				parse(line);
			}catch(Exception e){
				_elog(e.toString());
				break;
				// Break the loop
			}
		}
		
		/* Clear */	
		// Close main thread
		close();
	}
	
	private void parse(String str){
		StringTokenizer st = new StringTokenizer(str);
		String tkn = st.nextToken();
		if(tkn.equals(ProtocolConstants.PACK_STR_HEARTBEAT_HEAD)){
			_log("I got heartbeat");
		}else if(tkn.equals(ProtocolConstants.PACK_STR_REQUEST_FS_HEAD)){
			
		}else{
			_elog("Invalid header, skip.");
		}
	}
	
	public void sendAll(){
		assert _out != null;
		String output = _server.getID() + " " + _server.getPrio() +
				" " + _server.getMaxClientNum();
		Map<String, String> mp = _server.getMap();
		Iterator it = mp.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry pair = (Map.Entry)it.next();
			output += " " + pair.getKey() + " " +pair.getValue(); 
		}
		output = ProtocolConstants.PACK_STR_ID_HEAD + " " + output;
		_out.println(output);
	}
	
	public void sendUserID(){
		assert _userOut != null;
		String output = ProtocolConstants.PACK_STR_USR_HEAD + " " + _server.getID();
		_userOut.println(output);
	}
	
	// TODO: setup a timeout mechanism, if the master is not running, retry.
	private boolean connect(){
		boolean connected = true;
		try{
			_sock = new Socket(_serverIP, _serverPort);
			_out = new PrintWriter(_sock.getOutputStream(), true);
			_in = new BufferedReader(new InputStreamReader(_sock.getInputStream()));
			sendAll();
			// give the server the current ip address of file server

			_userSock = new Socket(_serverIP, _serverPort);
			_userOut = new PrintWriter(_userSock.getOutputStream(), true);
			_userIn = new BufferedReader(new InputStreamReader(_userSock.getInputStream()));
			sendUserID();
		}catch(UnknownHostException e){
			_elog(e.toString());
			if(_server.debugMode()){
				e.printStackTrace();
			}
			connected = false;
		}catch(IOException e){
			_elog(e.toString());
			if(_server.debugMode()){
				e.printStackTrace();
			}
			connected = false;
		}
		if(!connected){
			return connected;
		}
		try
		{
			
			String response = receive(_in);
			String responseUser = receive(_userIn);

			assert response != null && responseUser != null;
			// Hand shaked
			if(response.equals(ProtocolConstants.PACK_STR_CONFIRM_HEAD)&&
					responseUser.equals(ProtocolConstants.PACK_STR_CONFIRM_HEAD)){
				connected = true;
				
				return connected;
			}else{
				connected = false;
				// TODO: If false, should retry after ... seconds
			}
		}
		catch(Exception e){
			_elog(e.toString());
			connected = false;
		}
		if(!connected){
			/* Remove the pair of socket */
			_sock = null;
			_in = null;
			_out = null;
			
			_userSock = null;
			_userOut = null;
			_userIn = null;
		}
		return connected;
	}
	
	private String receive(BufferedReader in) throws Exception{
		String from = null;
		try{
			if((from = in.readLine()) == null)
				throw new Exception("No response received");
		}catch(Exception e){ 
			_elog(e.toString());
			// So what does this mean, it usually means the connection is broken
			throw new Exception("Connection is broken");
		}
		return from;
	}
	
	public void closeConnections(){
		_log("Close the connection...");
		close();
	}
	
	private void close(){
		_dlog("Do main thread closing...");
		if(_sock != null && !_sock.isClosed()){
			_dlog("Closing the receiving thread");
			try{
				_sock.close();
				_in.close();
				_out.close();
				
				_sock = null;
				_in = null;
				_out = null;
				_dlog("Success!");
			}catch(IOException e){
				_elog(e.toString());
				if(_server.debugMode()){
					e.printStackTrace();
				}
			}
		}
		else{
			if(_sock == null){
				throw new NullPointerException();
			}else{
				_sock = null;
				_in = null;
				_out = null;
			}
		}
		
		_dlog("Cancel the user input thread");
		if(_userNet != null){
				_userNet.stop();
				_userNet = null;
		}
		if(_userSock != null && !_userSock.isClosed()){
			_dlog("Closing the user input thread");
			//Stop the user thread
			try{
				_userSock.close();
				_userIn.close();
				_userOut.close();
				_userSock = null;
				_userIn = null;
				_userOut = null;
				_dlog("Success!");
			}catch(IOException e){
				_elog(e.toString());
				if(_server.debugMode()){
					e.printStackTrace();
				}
			}
		}else{
			if(_userSock == null){
				throw new NullPointerException();
			}else{
				//_elog("Already closed");
				_userSock = null;
				_in = null;
				_out = null;
			}
		}
		_dlog("Finished");
	}
}
