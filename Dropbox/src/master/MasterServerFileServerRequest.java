package master;

import java.io.*;
import java.net.*;

import common.*;
/**
 * 
 * class MasterServerFileServerRequest
 * Description: Actually just responsible for sending heartbeat message to
 *              get the status of file servers
 */
public class MasterServerFileServerRequest extends ThreadBase{

	private MasterServer _server;
	private PrintWriter _out;
	private BufferedReader _in;
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
	
	public MasterServerFileServerRequest(MasterServer server, PrintWriter out,
					BufferedReader in, Socket sock){
		super("MasterServerFileServerRequest",server.debugMode());
		_server = server;
		assert _server != null;
		_sock = sock;
		_in = in;
		_out = out;
	}
	
	public void sendMessage(String str) throws Exception{
		assert str != null;
		if(_out.checkError()){
			throw new Exception("Socket is closed");
		}
		_out.println(str);
	}
	
	
	private void sendHeartBeat() throws Exception{
		sendMessage(ProtocolConstants.PACK_STR_HEARTBEAT_HEAD);
		String reply = receive(_in);
		if(reply.equals(ProtocolConstants.PACK_STR_HEARTBEAT_HEAD)){
			_dlog("HearBeat confirmed");
		}
	}
	
	public void run()
	{
		Thread thisThread = Thread.currentThread();
		try{
		while(thisThread == _t){
			synchronized (this){
				while(_suspended){
					wait();
				}
			}
			Thread.sleep(DropboxConstants.HEART_BEAT_HZ);
			sendHeartBeat();
		}
		}catch(InterruptedException e){
			_elog(e.toString());
			if(_server.debugMode())
				e.printStackTrace();
		}catch(Exception e){
			_elog(e.toString());
			if(_server.debugMode())
				e.printStackTrace();
		}
		
		clear();
		_log(_threadName + " is stopped");
	}
	
	private String receive(BufferedReader in) throws Exception{
		String from = null;
		try{
			if((from = in.readLine()) == null)
				throw new Exception("No response received");
		}catch(Exception e){
			_elog(e.toString());
			// If it gets here, it means the socket is closed by the 
			// other side
			throw new Exception("Connection is broken");
		}
		return from;
	}
	
	public void clear(){
		_dlog("Do clear..");
		try{
			if(!_sock.isClosed())
				_sock.close();
			/* Close stream */
			_in.close();
			_out.close();

			/* Set to null */
			_sock = null;
			_in = null;
			_out = null;

		}catch(IOException e){
			_elog(e.toString());
			if(_server.debugMode())
				e.printStackTrace();
		}
		_dlog("Finished");
	}
}
