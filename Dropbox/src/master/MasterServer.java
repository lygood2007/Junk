package master;

import common.*;

import java.util.*;


/**
 * 
 * class MasterServer
 * Description: The big server! It's mainly responsible for handling dispatching client connection
 *              to a specified file server.
 */
class MasterServer {

	private int _clientsPort;
	private int _clusterPort;
	private boolean _debug;
	private boolean _useUI;
	private MasterServerNet _serverNet;
	private Map<String, Integer> _map;
	private LinkedList<FileServerNode> _fsnodes;
	private Timer _timer;
	
	private void _dlog(String str){
		if(_debug)
			System.out.println("[MasterServer (DEBUG)]:" + str);
	}
	
	private static void _elog(String str){
		System.err.println("[MasterServer (ERROR)]:" + str);
	}
	
	private static void _log(String str){
		System.out.println("[MasterServer]:" + str);
	}
	
	public MasterServer(int clientPort, int clusterPort, boolean useUI, boolean debug){
		_clientsPort = clientPort;
		_clusterPort = clusterPort;
		_useUI = useUI;
		_debug = debug;
		_map = new HashMap<String, Integer>();
		
		// Use tomic list 
		_fsnodes = /*(LinkedList<FileServerNode>) Collections.synchronizedList(*/new LinkedList<FileServerNode>();
		initNet();
		initTimer();
		printStatus();
	}
	
	private void initNet(){
		_serverNet = new MasterServerNet(this);
	}
	
	private void initTimer(){
		_timer = new Timer();
		_timer.scheduleAtFixedRate(new Echo(this), 1000, 1000);
	}
	
	public void run(){
		//_serverNet.listenToClients();
		_serverNet.run();
	}
	
	public void printStatus(){
		_log("Master Server configuration:");
		_log("Debug:" + Boolean.toString(_debug));
		_log("UseUI:" + Boolean.toString(_useUI));
		_log("Client port:" + Integer.toString(_clientsPort));
		_log("Cluster port:" + Integer.toString(_clusterPort));
    	System.out.println();
	}
	
	public void usage(){
		_log("Master Server:");
		_log("-d: for debug mode (default false)" );
		_log("-u: to use user interface (default false)");
		_log("-cp: to specify the listen port for clients (default " + DropboxConstants.MASTER_CLIENT_PORT+")");
		_log("-lp: to specify the listen port for cluster (default " + DropboxConstants.MASTER_CLUSTER_PORT+")");
    	System.out.println();
	}
	
	/**
	 * Getters
	 */
	public int clientsPort(){
		return _clientsPort;
	}
	
	public int clusterPort(){
		return _clusterPort;
	}
	
	public boolean debugMode(){
		return _debug;
	}
	
	public Map<String, Integer> getMap(){
		return _map;
	}
	
	public LinkedList<FileServerNode> getFS(){
		return _fsnodes;
	}
	
	public FileServerNode findFileServer(int id){
		for(FileServerNode fsn: _fsnodes){
			if(fsn.getID() == id){
				return fsn;
			}
		}
		return null;
	}
	
	public synchronized void insertFileServer(FileServerNode fs){
		_fsnodes.add(fs);
	}
	
	public synchronized void removeFileServer(int id){
		// TODO: need verification
		int i = 0;
		for(FileServerNode fsn: _fsnodes){
			if(id == fsn.getID()){
				/* TODO: clear everything in this record */
				fsn.clear();
				break;
			}
			i++;
		}
		_fsnodes.remove(i);
	}
	
	public synchronized void printFileServers(){
		
		_log("There are " + _fsnodes.size() + " file server connected");
		int i = 0;
		for(FileServerNode fsn: _fsnodes){
			_log("Server" + i);
			_log("ID:" + fsn.getID());
			_log("IP" + fsn.getIP());
			_log("MAX CLIENTS:" + fsn.getMaxClients());
			_log("PRIO:" + fsn.getPriority());
			System.out.println();
			i++;
		}
	}
	
	public void garbageCollection(){
		_dlog("Run garbage collection");
		/* Need to use iterator to loop */
		Iterator<FileServerNode> it = _fsnodes.iterator();
		while(it.hasNext()){
			FileServerNode node = it.next();
			if(!node.isAlive()){
				_log("Remove " + node.getID());
				it.remove();
			}
		}
	}
	
	/**
	 * 
	 * class Echo
	 * Description: Walk through the linked list and do garbage collection.
	 *              Also print out the status of the server
	 */
	private class Echo extends TimerTask{
		
		private MasterServer _server;
		
		private void _log(String str){
			System.out.println("[Echo]:"+str);
		}
		public Echo(MasterServer server){
			_server = server;
		}
		
		@Override
		public void run(){
			garbageCollection();
			printFileServers();
		}
		
	}
	
	public static void main(String[] args) {
		int clientPort = DropboxConstants.MASTER_CLIENT_PORT;
		int clusterPort = DropboxConstants.MASTER_CLUSTER_PORT;
		boolean useUI = false; // Not used now
		boolean debug = true;
		
		for( int i = 0; i < args.length; i++ ){
    		if(args[i].equals("-d"))
				debug = true;
			else if(args[i].equals("-u"))
				useUI = true;
			else if(args[i].equals("-cp")){
				i++;
				clientPort = Integer.parseInt(args[i]);
			}
			else if(args[i].equals("-lp")){
				i++;
				clusterPort = Integer.parseInt(args[i]);
			}
    	}
		
		MasterServer server = new MasterServer(clientPort, clusterPort, useUI, debug);
		server.run();
	}
}
