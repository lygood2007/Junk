package fileserver;

import common.*;

import java.io.*;
import java.util.*;

/**
 * 
 * Class: DropboxFileServer
 * Description: The server for syncing files and talking to the master node
 *              , here we simulate the behavior of data storage.
 */
class DropboxFileServer {

	private int _port;
	private boolean _debug; 
	private boolean _useUI;
	private String _disk; // The location of the disk, is indeed the directory. 
	                             // Just simulate.
	//private DropboxFileServerMasterNettmp _serverNet;
	//private DropboxFileServerListenNet _clientNet;
	private DropboxFileServerMasterNet _masterNet;
	private Map<String, String> _map;
	private int _id; // The unique identifier for this file server
	private int _prio; // The priority of this file server. (1->5)
		               // The larger it is the higher it will be chosen to be the sync server
	private int _maxClientNum;
	
	private void _dlog(String str){
		if(_debug)
			System.out.println("[DropboxFileServer (DEBUG)]:" + str);
	}
	
	private void _elog(String str){
		System.err.println("[DropboxFileServer (ERROR)]:" + str);
	}
	
	private static void _log(String str){
		System.out.println("[DropboxFileServer]:" + str);
	}
	
    public DropboxFileServer(int id, int prio, int maxClientNum, int port, boolean debug, boolean useUI, String disk){
    	_port = port;
    	_debug = debug;
    	_useUI = useUI;
    	_disk = disk;
    	_id = id;
    	_prio = prio;
    	_maxClientNum = maxClientNum;
    	_map = new TreeMap<String, String>();
    	
    	initDisk(_disk);
    	initNet();
    	printStatus();
    }
    
    private void initDisk(String disk){
    	_dlog("Initialize disk");
    	File theDir = new File(disk);
    	// if the disk does not exist, create it
    	if (!theDir.exists()) {
    		_log("Creating directory: " +disk);
    		boolean result = theDir.mkdir();  

    		if(result) {    
    			_log(disk + " created.");  
    		}
    		else{
    			_elog("Cannot initialize the directory");
    			System.exit(1);
    		}
    	}
    	_dlog("Disk home: " + _disk);
    }
    
    private void initNet(){
    	_dlog("Initialize network..");
    	_masterNet = new DropboxFileServerMasterNet(this);
    }
    
    public void run(){
    	if(connectToMaster() == true){
    		// Spawn a new thread to listen to new clients
    		listenToClients();
    		// Use main thread to accepting master's message
    		// Block here until socket is closed
    		_masterNet.listen();
    	}
    	_dlog("Done!");
    }
    
    /**
     * connectToMaster: Connect to master using two threads ((main and another)).
     *                  main thread: mainly accept master's heartbeat message
     *                  and master's forwarded request from client.
     *                  new thread: mainly get input from user and request master.
     */
    private boolean connectToMaster(){
    	// Let the miracle happen!
    	return _masterNet.openConnections();
    }
    
    /**
     * listenToClients: listen to new client (spawn a new thread)
     */
    private void listenToClients(){
    	
    }
    
    public boolean addClient(String clientName){
    	assert _disk != null;
    	if(_map.containsKey(clientName)){
    		_elog("Client already exist");
    		return false;
    	}
    	// we create a new name for this client
    	String fullpath = _disk +  System.getProperty("file.separator") + clientName;
    	File newDir =  new File(fullpath);
    	_dlog("Add client " + clientName);
    	if (!newDir.exists()) {
    		_log("Creating directory: " +newDir);
    		boolean result = newDir.mkdir();  

    		if(result) {    
    			_log(newDir + " created.");  
    			_map.put(clientName, fullpath);
    		}
    		else{
    			_elog("Cannot create the directory");
    			// TODO: a better way to tell the client that it's failed
    			//System.exit(1);
    			return false;
    		}
    	}
    	_dlog("Client root: " + _disk);
    	return true;
    }
    
    public boolean removeClient(String clientName){
    	if(!_map.containsKey(clientName)){
    		return false;
    	}else
    	{
    		// remove the file
    		File tmp = new File(_map.get(clientName));
    		if(!tmp.delete()){
    			return false;
    		}
    		else
    		{
    			_map.remove(clientName);
    			return true;
    		}
    	}
    }
    
    public void printStatus(){
    	_log("**Dropbox File Server configuration:");
    	_log("ID:" + _id);
    	_log("Debug:" + _debug);
    	_log("UseUI:" + _useUI);
    	_log("Priority:" + _prio);
    	_log("Max Client Num:" + _maxClientNum);
    	_log("Current Client Num:" + _map.size());
    	_log("Root disk:" + _disk);
    	_log("Listen port:" + _port);
    	Map<String, String> mp = _map;
		Iterator it = mp.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry pair = (Map.Entry)it.next();
			 _log("<Clients name>:" + pair.getKey() + " <Client dir>	:" +pair.getValue()); 
		}
    	System.out.println();
    }
    
    public static void usage(){
    	_log("Dropbox File Server:");
    	_log("/* You must specify the id for this server */");
    	_log("-id give an id to this server");
    	_log("-d: for debug mode (default false)" );
    	_log("-u: to use user interface (default false)");
    	_log("-p: to specify port for listening (default "+
    			DropboxConstants.FILE_SERVER_PORT+")");
    	_log("-disk: root your server is located"
    			+ " (default using cwd/"+DropboxConstants.FILE_SERVER_ROOT+
    			", where cwd is your current working directory)");
    	_log("-prio: the priority of this server ("+DropboxConstants.MIN_PRIO+
    			"-"+DropboxConstants.MAX_PRIO+")");
    	_log("-mc: the max number of clients can be connected in this server (0-"+
    			DropboxConstants.MAX_CLIENTS_IN_FS+")");
    	
    	System.out.println();
    }
    
    /**
     * Getters
     */
    public boolean debugMode(){
    	return _debug;
    }
    
    public synchronized void toogleDebug(){
    	_debug = !_debug;
    }
    
    public int listenPort(){
    	return _port;
    }
    
    public String disk(){
    	return _disk;
    }
    
    public Map<String, String> getMap(){
    	return _map;
    }
    
    public int getID(){
    	return _id;
    }
    
    public int getPrio(){
    	return _prio;
    }
    
    public synchronized void setPrio(int prio){
    	_prio = prio;
    }
    
    public int getMaxClientNum(){
    	return _maxClientNum;
    }
    
    public int getCurClientNum(){
    	return _map.size();
    }
    
    public static void main(String[] args) {
    	
    	int port = DropboxConstants.FILE_SERVER_PORT;
    	boolean debug = false;
    	boolean useUI = false;	
    	String disk = DropboxConstants.FILE_SERVER_ROOT;
    	int id = 0;
    	int prio = 1;
    	int maxClientNum = DropboxConstants.MAX_CLIENTS_IN_FS;
    	
    	if(args.length < 2){
    		usage();
    		System.exit(1);
    	}
    	
    	for( int i = 0; i < args.length; i++ ){
    		
    		if(args[i].equals("-d"))
				debug = true;
			else if(args[i].equals("-u"))
				useUI = true;
			else if(args[i].equals("-p")){
				i++;
				port = Integer.parseInt(args[i]);
			}
			else if(args[i].equals("-disk")){
				i++;
				disk = args[i];
			}else if(args[i].equals("-id")){
				i++;
				id = Integer.parseInt(args[i]);
			}else if(args[i].equals("-prio")){
				i++;
				prio = Integer.parseInt(args[i]);
				prio = Math.max(prio, DropboxConstants.MIN_PRIO);
				prio = Math.min(prio, DropboxConstants.MAX_PRIO);
			}
			else if(args[i].equals("-mc")){
				i++;
				maxClientNum = Integer.parseInt(args[i]);
				maxClientNum = Math.max(maxClientNum, 0);
				maxClientNum = Math.min(maxClientNum, DropboxConstants.MAX_CLIENTS_IN_FS);
			}
    	}
    	
    	DropboxFileServer server = new DropboxFileServer(id, prio, maxClientNum, port,debug,useUI,disk);
    	server.run();
    }
}
