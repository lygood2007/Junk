package master;

import java.net.*;
import java.io.*;
/**
 * 
 * Class MasterServerFileServerHandler
 * Description: Handles the new connected file server
 */
class MasterServerFileServerAccept implements Runnable{
	private Socket _sock;
	private BufferedReader _in;
	private PrintWriter _out;
	private MasterServer _server;
	
	private void _dlog(String str){
		if(_server.debugMode())
			System.out.println("[MasterServerFileServerHandler (DEBUG)]:" + str);
	}
	
	private static void _elog(String str){
		System.err.println("[MasterServerFileServerHandler (ERROR)]:" + str);
	}
	
	private static void _log(String str){
		System.out.println("[MasterServerFileServerHandler]:" + str);
	}
	
	public MasterServerFileServerAccept(Socket sock,MasterServer server){
		_sock = sock;
		_server = server;
		_elog("adsasd");
		assert _server != null;
		try{
		_out = new PrintWriter(_sock.getOutputStream(), true);
		_in = new BufferedReader(new InputStreamReader(_sock.getInputStream()));
		}catch(IOException e){
			_elog(e.toString());
			if(_server.debugMode()){
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public void run(){
		/*try{
			_sock.close();
			_out.close();
			_in.close();
		}catch(IOException e){
			e.printStackTrace();
		}*/
	}
	
	public Socket getSocket(){
		return _sock;
	}
}
