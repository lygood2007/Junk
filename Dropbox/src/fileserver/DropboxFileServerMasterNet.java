package fileserver;

import java.io.*;
import java.net.*;
import java.util.*;

import common.*;

class DropboxFileServerUserNet implements Runnable{
	
	private DropboxFileServer _server;
	private PrintWriter _userOut;
	private BufferedReader _userIn;
	private Socket _sock;
	
	private void _dlog(String str){
		if(_server.debugMode())
			System.out.println("[DropboxFileServerUserNet (DEBUG)]:" + str);
	}
	
	private static void _elog(String str){
		System.err.println("[DropboxFileServerUserNet (ERROR)]:" + str);
	}
	
	private static void _log(String str){
		System.out.println("[DropboxFileServerUserNet]:" + str);
	}
	
	public DropboxFileServerUserNet(DropboxFileServer server, PrintWriter userOut,
			BufferedReader userIn, Socket sock){
		_sock = sock;
		_server = server;
		_userIn = userIn;
		_userOut = userOut;
	}
	
	private void usage(){
		_log("File Server usage:");
		_log("-q: request the Master's status");
		_log("-s: shutdown the current file server");
		_log("-h: show usage0");
		_log("-p: set the priority of this server (1 -> 5)");
		_log("-d: toggle debug mode");
		_log("-a: add a client (should be deprecated when client can connect to file server)");
		_log("-r: remove a client (should be deprecated when client can connect to file server)");
	}
	
	private void parse(String str){
		StringTokenizer st = new StringTokenizer(str);
		if(st.countTokens() == 1){
			if(str.equals("-q")){
				// request the master's status
			}else if(str.equals("-s")){

			}else if(str.equals("-h")){
				usage();
			}else if(str.equals("-d")){
				_server.toogleDebug();
			}
		}
		else if(st.countTokens() == 2){
			String tmp = st.nextToken();
			if(tmp.equals("-p")){
				_server.setPrio(Integer.parseInt(st.nextToken()));
			}
			else if(tmp.equals("-r")){
				
			}
			else {
				_elog("Invalid Input");
			}
		}
		else if(st.countTokens() == 3){
			
		}
		else{
			_elog("Invalid input");
		}
	}
	
	@Override
	public void run(){
		usage();
		Scanner in = new Scanner(System.in);
		try
			{
		while(_sock.isConnected() && _sock.isClosed() && !Thread.currentThread().isInterrupted()){
			Thread.sleep(1);
			
			_log("input:");
			String s = in.nextLine();
			
			parse(s);
			// TODO: to be added
			_userOut.println(s);
			}
		}catch(InterruptedException e){
				Thread.currentThread().interrupt();
			}
		
		_log("The connection to master is broken");
	}
	
	public void stop(){
		Thread.currentThread().interrupt();
	}
}
/**
 * 
 * Class: DropboxFileServerMasterNet
 * Description: Handles the network connection to master node, it actually has one pair of sockets,
 *              one for accepting master's request, one for sending file server's request 
 */
class DropboxFileServerMasterNet implements ConnectNet{

	private String _serverIP;
	private int _serverPort;
	private Socket _sock;
	private Socket _userSock;
	private PrintWriter _out;
	private BufferedReader _in;
	
	private PrintWriter _userOut;
	private BufferedReader _userIn;
	private DropboxFileServer _server;
	
	private Thread _userThread;
	//private ReentrantLock _lock;
	
	private void _dlog(String str){
		if(_server.debugMode())
			System.out.println("[DropboxFileServerMasterNet (DEBUG)]:" + str);
	}
	
	private static void _elog(String str){
		System.err.println("[DropboxFileServerMasterNet (ERROR)]:" + str);
	}
	
	private static void _log(String str){
		System.out.println("[DropboxFileServerMasterNet]:" + str);
	}
	
	public void sendAll(){
		assert _out != null;
		String output = Integer.toString(_server.getID()) + " " + Integer.toString(_server.getPrio()) +
				" " + Integer.toString(_server.getMaxClientNum());
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
		String output = ProtocolConstants.PACK_STR_USR_HEAD + " " + Integer.toString(_server.getID());
		_userOut.println(output);
	}
	
	public boolean connect(){
		_log("Trying to connect to master node...");
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
			_elog("Unknown host!");
			if(_server.debugMode())
				e.printStackTrace();
			connected = false;
		}catch(IOException e){
			_elog("Errors occur when creating socket");
			if(_server.debugMode())
				e.printStackTrace();
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
				_log("Successful connection to " + _serverIP + ":" + _serverPort);


				return connected;
			}else{
				connected = false;
				// TODO: If false, should retry after ... seconds
			}
		}
		catch(Exception e){
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
	
	public void spawnUserThread(){
		assert _userIn != null && _userOut != null && _userSock != null;
		
		_userThread = new Thread(new DropboxFileServerUserNet(_server, _userOut, _userIn, _userSock));
		_userThread.start();
	}
	public void listen(){
		while(_sock.isConnected()&&!_sock.isClosed()){
			try
			{
				String line = receive(_in); // if it receives null, it means the connection is broken
				_dlog(line);
				parse(line);
			}catch(Exception e){
				break;
				// Break the loop
			}
		}
		// No longer connected, close it
		_log("socked is closed");
	}
	
	private void parse(String str){
		StringTokenizer st = new StringTokenizer(str);
		String tkn = st.nextToken();
		if(tkn.equals(ProtocolConstants.PACK_STR_HEARTBEAT_HEAD)){
			
		}else if(tkn.equals(ProtocolConstants.PACK_STR_REQUEST_FS_HEAD)){
			
		}else{
			_elog("Invalid header, skip.");
		}
	}
	
	private String receive(BufferedReader in) throws Exception{
		String from = null;
		try{
			if((from = in.readLine()) == null)
				throw new Exception("No response received");
		}catch(Exception e){
			_dlog("Receiving message response error");
			// So what does this mean, it usually means the connection is broken
			throw new Exception("Connection is broken");
		}
		return from;
	}
	
	/**
	 * Close: close the connection
	 */
	public void close(){
		
		if(_sock != null && !_sock.isClosed()){
			_dlog("Closing the receiving thread");
			try{
				_sock.close();
				_in.close();
				_out.close();
				_dlog("Success!");
			}catch(IOException e){
				_elog("IO errors occur when closing socket");
				if(_server.debugMode())
					e.printStackTrace();
			}
		}
		
		_dlog("Closing the receiving thread");
		if(_userSock != null && !_userSock.isClosed()){
			_userThread.interrupt();
			try{
				//TODO: I think close the socket will make the user thread stop
				//      Need verification
				_userSock.close();
				_userIn.close();
				_userOut.close();
				_dlog("Success!");
			}catch(IOException e){
				_elog("IO errors occur when closing socket");
				if(_server.debugMode())
					e.printStackTrace();
			}
		}
	}
	
	/**
	 * Constructor
	 */
	public DropboxFileServerMasterNet(DropboxFileServer server){
		_serverIP = DropboxConstants.MASTER_IP;
		_serverPort = DropboxConstants.MASTER_CLUSTER_PORT;
		_sock = null;
		_server = server;
		assert _server != null;
	}
	
	/**
	 * Getters
	 */
	public String getServerIP(){
		return _serverIP;
	}
	
	public int getPort(){
		return _serverPort;
	}
	
	public Socket getSocket(){
		return _sock;
	}
	
	public boolean isConnected(){
		return _sock.isConnected();
	}
}