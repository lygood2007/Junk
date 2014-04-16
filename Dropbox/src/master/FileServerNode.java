package master;

import java.net.*;
import java.io.*;
import java.util.*;
/**
 * 
 * class: FileServerNode
 * Description: Used for storing the connected file servers
 */
class FileServerNode {

	private Thread _thread;
	private Thread _userThread;
	private Socket _userSocket;
	private Socket _socket;
	private String _ip;
	private int _id;
	private int _prio;
	private int _maxClients;
	private Map<String, String> _mp;
	
	public FileServerNode(){
		_mp = new TreeMap<String, String>();
	}
	
	public synchronized void setThread(Thread t){ 
		_thread = t;
	}
	
	public synchronized void setUserThread(Thread ut){ 
		_userThread = ut;
	}
	
	public synchronized void setSocket(Socket s){
		assert _socket == null;
		_socket = s;
	}
	
	public synchronized void setUserSocket(Socket s){
		_userSocket = s;
	}
	
	public synchronized void setID(int id){
		_id = id;
	}
	
	public synchronized void setIP(String ip){
		_ip = ip;
	}
	
	public synchronized void setPriority(int priority){ 
		_prio = priority;
	}
	
	public synchronized void setMaxClients(int maxClients){
		_maxClients = maxClients;
	}
	
	public int getMaxClients(){
		return _maxClients;
	}
	
	public int getNumClients(){
		return _mp.size();
	}
	
	public int getID(){
		return _id;
	}
	
	public int getPriority(){
		return _prio;
	}
	
	public String getIP(){
		return _ip;
	}
	
	public synchronized void clearMap(){
		_mp.clear();
	}
	
	public synchronized void addEntry(String key, String val){	
		_mp.put(key, val);
	}
	
	public synchronized void clear(){
		// TODO: cancel threads
		try
		{
			if(!_userSocket.isClosed())
				_userSocket.close();
			if(!_socket.isClosed())
				_socket.close();
		}catch(IOException e){
			e.printStackTrace();
		}
		_ip = null;
		_mp.clear();
		_mp = null;
	}
}
