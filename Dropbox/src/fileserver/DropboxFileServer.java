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
	private DropboxFileServerNet _serverNet;
	private Map<String, String> _map;
	private int _id; // The unique identifier for this file server
	private int _prio; // The priority of this file server. (1->5)
		               // The larger it is the higher it will be chosen to be the sync server
	private int _maxClientNum;
	
	private void _dlog(String str){
		if(_debug)
			System.out.println("[DropboxFileServer (DEBUG)]:" + str);
	}
	
	private static void _elog(String str){
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
    	_serverNet = new DropboxFileServerNet(this);
    }
    
    public void run(){
    	connectToMaster();
    }
    
    private void connectToMaster(){
    	// Let the miracle happen!
    	_serverNet.openConnections();
    	//_serverNet.listenToClients();
    }
    
    public void addClient(String clientName){
    	assert _disk != null;
    	File newDir =  new File(_disk +  System.getProperty("file.separator") + clientName);
    	_dlog("Add client " + clientName);
    	if (!newDir.exists()) {
    		_log("Creating directory: " +newDir);
    		boolean result = newDir.mkdir();  

    		if(result) {    
    			_log(newDir + " created.");  
    			_map.put(clientName, newDir.getName());
    		}
    		else{
    			_elog("Cannot create the directory");
    			// TODO: a better way to tell the client that it's failed
    			//System.exit(1);
    		}
    	}
    	_dlog("Client root: " + _disk);
    	
    }
    
    public void removeClient(String clientName){
    	
    }
    
    public void printStatus(){
    	_log("Dropbox File Server configuration:");
    	_log("ID:" + Integer.toString(_id));
    	_log("Debug:" + Boolean.toString(_debug));
    	_log("UseUI:" + Boolean.toString(_useUI));
    	_log("Priority:" + Integer.toString(_prio));
    	_log("Max Client Num:" + Integer.toString(_maxClientNum));
    	_log("Current Client Num:" + Integer.toString(_map.size()));
    	_log("Root disk:" + _disk);
    	_log("Listen port:" + Integer.toString(_port));
    	System.out.println();
    }
    
    public static void usage(){
    	_log("Dropbox File Server:");
    	_log("/* You must specify the id for this server */");
    	_log("-id give an id to this server");
    	_log("-d: for debug mode (default false)" );
    	_log("-u: to use user interface (default false)");
    	_log("-p: to specify port for listening (default 5000)");
    	_log("-disk: root your server is located"
    			+ " (default using cwd/ServerRoot, where cwd is your current working directory)");
    	_log("-prio: the priority of this server (1-5)");
    	_log("-mc: the max number of clients can be connected in this server (0-3)");
    	
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
    	boolean debug = true;
    	boolean useUI = false;	
    	String disk = DropboxConstants.DROPBOX_SERVER_ROOT;
    	int id = 0;
    	int prio = 1;
    	int maxClientNum = DropboxConstants.MAX_CLIENTS_IN_FS;
    	
    	//usage();
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
    	while(true)
    	{
    		Scanner in = new Scanner(System.in);
    		String s = in.nextLine();
			_log("input:");
    	}
    	//DropboxFileServer server = new DropboxFileServer(id, prio, maxClientNum, port,debug,useUI,disk);
    	//server.run();
    }
}
