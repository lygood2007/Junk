package master;

import java.io.*;
import java.net.*;
import java.util.*;

import common.ListenNet;
import common.ProtocolConstants;
/**
 * 
 * class MasterServerClusterNet
 * Description: Listen to new file server connected and spawn a new thread to handle it
 */
class MasterServerClusterNet implements ListenNet{
	private MasterServer _server;
	private ServerSocket _serverSocket;
	//private Thread _thread;
	
	private void _dlog(String str){
		if(_server.debugMode())
			System.out.println("[MasterServerClusterNet (DEBUG)]:" + str);
	}
	
	private static void _elog(String str){
		System.err.println("[MasterServerClusterNet (ERROR)]:" + str);
	}
	
	private static void _log(String str){
		System.out.println("[MasterServerClusterNet]:" + str);
	}
	
	public MasterServerClusterNet(MasterServer server){
		_server = server;
		assert _server != null;
	}
	
	private String receive(BufferedReader in){
		String from = null;
		try{
			if((from = in.readLine()) == null)
				throw new Exception("No response received");
		}catch(Exception e){
			_dlog("Receiving message response error");
		}
		return from;
	}
	
	private void parse(String str, Socket s){
		StringTokenizer st = new StringTokenizer(str);
		String tk = st.nextToken();
		if(tk.equals(ProtocolConstants.PACK_STR_ID_HEAD)){
			int id = Integer.parseInt(st.nextToken());
			int prio = Integer.parseInt(st.nextToken());
			int maxClientNum = Integer.parseInt(st.nextToken());
			FileServerNode node = _server.findFileServer(id);
			if(node == null){
				node = new FileServerNode();
				_server.insertFileServer(node);
			}
			// load all entries
			while(st.hasMoreTokens()){
				String key = st.nextToken();
				String val = st.nextToken();
				node.addEntry(key, val);
			}
			node.setID(id);
			node.setIP(s.getInetAddress().getHostAddress());
			node.setSocket(s);
			node.setPriority(prio);
			node.setMaxClients(maxClientNum);
			Thread t = new Thread(new MasterServerFileServerHandler(s, _server));
			node.setThread(t);
			t.start();
			// Send back confirmation, temporary use
			try
			{
				PrintWriter out = new PrintWriter(s.getOutputStream(), true);
				out.println(ProtocolConstants.PACK_STR_CONFIRM_HEAD);
			}catch(IOException e){
				e.printStackTrace();
			}
			
		}else if(tk.equals(ProtocolConstants.PACK_STR_USR_HEAD)){
			int id = Integer.parseInt(st.nextToken());
			Thread t = new Thread(new MasterServerFileServerHandler(s, _server));
			FileServerNode node = _server.findFileServer(id);
			if(node == null){
				node = new FileServerNode();
				_server.insertFileServer(node);
			}
			
			node.setThread(t);
			t.start();
			
			// Send back confirmation, temporary use
			try
			{
				PrintWriter out = new PrintWriter(s.getOutputStream(), true);
				out.println(ProtocolConstants.PACK_STR_CONFIRM_HEAD);
			}catch(IOException e){
				e.printStackTrace();
			}
		}else{
			
			//TODO: to be added
			_elog("Invalid package");
		}
	}
	
	public void listen(){
		_log("listening to new client...");
		// Now only support one client
    	
    	try{
    		_serverSocket = new ServerSocket(_server.clusterPort());
    		_serverSocket.setSoTimeout(1000*100);
    		while(true)
    		{
    			Socket fileServer = _serverSocket.accept();
    			_log("Get connection from " + fileServer.getInetAddress().getHostAddress());
    			//TODO: here, it should control how many threads we could accept at most
    			
    			// read the identification
    			BufferedReader in = new BufferedReader(new InputStreamReader(fileServer.getInputStream()));
    			String init = in.readLine();
    			parse(init, fileServer);
    		}
    	}catch(InterruptedIOException e){
    		_elog("Time out");
    		if(_server.debugMode())
    			e.printStackTrace();
    		
    	}catch(IOException e){
    		_elog("IO error occurs");
    		if(_server.debugMode())
    			e.printStackTrace();
    	}finally{
    		try{
    			//_log("Close connection from " + fileServer.getInetAddress().getHostAddress());
    			if(_serverSocket != null )
    				_serverSocket.close();
    			/*if( fileServer != null )
    				fileServer.close();*/
    		}catch(IOException e){
    			_elog("IO error occurs when closing socket");
        		if(_server.debugMode())
        			e.printStackTrace();
    		}
    	}
	}
	
	// Setup a timer
	private class ThreadCollection extends TimerTask{ 

		ArrayList<Thread> _threads;

		private void _log(String str){
			System.out.println("ThreadCollection:"+str);
		}

		public ThreadCollection(ArrayList<Thread> threads){
			_threads = threads;
		}

		@Override
		public void run(){
			_log("Threre are " + _threads.size() + " threads");

			// Just use thread pool...
			/*
			for(int i = _threads.size()-1; i>= 0; i--){
				Thread t= _threads.get(i);
				// TODO: really useful here
				// TEST
				if(!t.isAlive()){
					_threads.remove(i);
				}
			}
			 */
		}
	}
}
