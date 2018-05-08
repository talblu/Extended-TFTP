package bgu.spl171.net.impl.TFTPtpc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Vector;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import bgu.spl171.net.api.bidi.BidiMessagingProtocol;
import bgu.spl171.net.api.bidi.Connections;
import bgu.spl171.net.impl.packets.*;

public class BidiMessagingProtocolImpl<T> implements BidiMessagingProtocol<T> {
	private boolean terminate = false;
	private ConnectionsImpl<Packet> server;
	private int Id;
	private Vector<ERRORPacket> errors = new Vector<>();
	private ConcurrentLinkedQueue<DATAPacket> data;
	private String fileNameToUse;
	private static ConcurrentHashMap<Integer, String> loggedIn = new ConcurrentHashMap<Integer, String>();

	@Override
	public void start(int connectionId, Connections<T> connections) {
		this.server = (ConnectionsImpl<Packet>)connections;
		this.Id = connectionId;
	}

	@Override
	public void process(T message) {
		short opCode = ((Packet)message).getOpCode();

		switch (opCode) {
		case 1 ://RRQ
			if (checkLogin())
				try {
					downloadFile(((RWPacket)message).getFileName());
				} catch (IOException e) {
					e.printStackTrace();
				}
			break;
		case 2: //WRQ
			checkLogin();
			uploadFile(((RWPacket)message).getFileName());
			break;
		case 3: //DATA 
			checkLogin();
			data.add((DATAPacket)message);
			server.send(Id, new ACKPacket(((DATAPacket)message).getBlockNum()));
			if (((DATAPacket)message).getPacketSize() < 512) {// Last DATAPacket
				short numOfBlocks = ((DATAPacket)message).getBlockNum();
				byte[] file = new byte[numOfBlocks*512];
				byte[][] fileChunks = new byte[numOfBlocks][];
				for (int i = 0 ; i < numOfBlocks ; i++) 
					fileChunks[i] = data.poll().getData();
				int counter = 0;
				for (int i = 0 ; i < numOfBlocks ; i++)
					for (int j = 0 ; j < fileChunks[i].length;j++){
						file[counter] = fileChunks[i][j];
						counter++;
					}
				FileOutputStream fos;
				try {
					fos = new FileOutputStream("Files/"+fileNameToUse);
					fos.write(file);
					fos.close();
					new File("Files/"+fileNameToUse);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				broadcast(new BCASTPacket((byte)1,fileNameToUse));
			}
			break;
		case 4: //ACK
			checkLogin();
			if (!data.isEmpty()){
				DATAPacket currDataPacket = data.peek();
				
				if (currDataPacket.getBlockNum() == ((ACKPacket)message).getBlockNum() + 1) {
					if (errors.isEmpty()) {
						server.send(Id, data.poll());
					}
				}
				else //block number in ACK is invalid
					errors.add(new ERRORPacket((short) 0,  "Illegal ACK number"));
			}
			break;
		case 6: //DIRQ
			if (checkLogin()){
				byte[] dirListBytes = getListing().getBytes();
				createData();
				if (dirListBytes.length == 0) {
                                    server.send(Id, new DATAPacket((short)0, (short)1, new byte[0]));
                                    break;
                                }
				double numOfPackets = Math.ceil(((double)dirListBytes.length)/512);
				for (int j = 0 ; j < numOfPackets ; j++){
					if (j < numOfPackets - 1){
						byte[] currBlock = Arrays.copyOfRange(dirListBytes, j, j+512);
						data.add(new DATAPacket((short)512, (short) (j + 1), currBlock));
					}
					else {//last block
						byte[] currBlock = Arrays.copyOfRange(dirListBytes, j, j + (dirListBytes.length % 512));
						data.add(new DATAPacket((short)(dirListBytes.length % 512), (short)(j + 1), currBlock));
					}
				}
				server.send(Id, data.poll());
			}
			break;
		case 7: //LOGRQ
			userLogin(((LOGRQPacket)message).getUserName());
			break;
		case 8: //DELRQ
			checkLogin();
			deleteFile(((DELPacket)message).getFileName());
			break;
		case 10: //DISC
			checkLogin();
			disconnect(Id);
			break;
		default: 
			errors.add(new ERRORPacket((short) 4,  "Illegal TFTP operation - Unknown Opcode"));
		}
		if (!errors.isEmpty()) {
			ERRORPacket minError = errors.remove(0);
			while (!errors.isEmpty()) {
				ERRORPacket currPacket = errors.remove(0);
				if (minError.getErrorCode() > currPacket.getErrorCode())
					minError = currPacket;
			}
			server.send(Id, minError); //send to client the lowest indexed error
		}
	}

	@Override
	public boolean shouldTerminate() {
		return terminate;
	}

	/**
	 * Creates a new concurrent linked queue of DATA Packets
	 */
	public void createData() {
		data = new ConcurrentLinkedQueue<DATAPacket>();
	}

	/**
	 * Id getter
	 * @return - the clien's unique ID
	 */
	public int getId() {
		return Id;
	}

	/**
	 * Error Vector getter
	 * @return the current error vector containing all current errors
	 */
	public Vector<ERRORPacket> getErrors() {
		return errors;
	}

	/**
	 * Checks if the current user is logged in
	 * @return - true if the user is logged in, else returns false
	 */
	private boolean checkLogin(){
		if (!loggedIn.containsKey(Id)){
			errors.add(new ERRORPacket((short)6, "User not logged in - Any opcode received before Login completes"));
			return false;
		}
		return true; //user is logged in
	}

	/**
	 * Handling RRQ - downloading a file to the client's computer
	 * @param fileName - the file name to download
	 * @throws IOException
	 */
	public void downloadFile(String fileName) throws IOException {
		File file = new File("Files/" + fileName);
		if (!file.exists())
			errors.add(new ERRORPacket((short)1, "File not found - RRQ of non-existing file"));
		else {
			if (errors.isEmpty()) {
				createData();
				splitFile("Files/" + fileName);
				server.send(Id, data.poll()); // Send the first data packet
			}
		}
	}

	/**
	 * Handling a WRQ - uploading a file received from the client
	 * @param fileName - the file name to upload
	 */
	public void uploadFile(String fileName) {//TODO CONCURRENCY!!
		File absent = new File("Files/" + fileName);
		if (absent.exists()) // the file was already uploaded
			errors.add(new ERRORPacket((short)1, "File already exists - File name exists on WRQ"));
		else {
			if (errors.isEmpty()){
				createData();
				fileNameToUse = fileName;
				server.send(Id, new ACKPacket((short)0));
			}
		}
	}

	/**
	 * Splits a file into DATA Packets of size <= 512 and adds them to eh data vector
	 * @param path - the path to the file to split
	 * @throws IOException
	 */
	public void splitFile(String path) throws IOException {
		Path pathObj = Paths.get(path);
		int counter = 0;
		int sizeCounter = 0;
		int blockNum = 1;
		Vector<Byte> vec = new Vector<>();
		byte[] fileBytes = Files.readAllBytes(pathObj);
		while (counter < fileBytes.length) {
			while ((sizeCounter<512) && fileBytes.length > counter){
				vec.add(fileBytes[counter]);
				sizeCounter++;
				counter++;
			}
			if (sizeCounter % 512 == 0) {
				data.add(new DATAPacket((short)512, (short)blockNum++, vectorToArray(vec)));
				vec.clear();
			}
			sizeCounter = 0;
		}
		data.add(new DATAPacket((short)vec.size(), (short)blockNum++, vectorToArray(vec)));
	}

	/**
	 * Handling DIRQ request - creates a list of all the files in the 'File' folder of the server
	 * @return - the list of existing files
	 */
	public String getListing(){
		File files = new File("Files");
		File[] listings = files.listFiles();
		String res = "";
		for (int i = 0 ; i < listings.length ; i++)
			res = res + listings[i].getName() + '\0';
		return res;
	}

	/**
	 * Adding a new client that tries to login, else, returning an error if the user is already logged in
	 * @param name - the user name to login.
	 */
	public void userLogin(String name) {
		String logged = loggedIn.putIfAbsent(Id, name);
		if (logged != null)
			errors.add(new ERRORPacket((short)7, "User already logged in - Login username already connected"));
		else {
			server.send(Id, new ACKPacket((short)0));
		}
	}

	/**
	 * turns a vector of bytes to an array of bytes
	 * @param vec - the byte vector to turn to an array
	 * @return - the byte array containing all the vector's value
	 */
	private byte[] vectorToArray(Vector<Byte> vec){
		byte[] bytesArr = new byte[vec.size()];
		for (int i=0; i<bytesArr.length; i++)
			bytesArr[i] = vec.remove(0);
		return bytesArr;
	}

	/**
	 * Handling DELRQ from client - deleting a file from the 'File' folder of the server
	 * @param fileName - the file name to be deleted
	 */
	public void deleteFile(String fileName) { 
		File currFile = new File("Files/" + fileName);
		if (currFile.exists()){
			if (errors.isEmpty()) {
				if (currFile.delete()) {//delete was successful
					server.send(Id, new ACKPacket((short)0));
					broadcast(new BCASTPacket((byte)0, fileName));
				}
				else {
					errors.add(new ERRORPacket((short)2, "Access violation - File cannot be written, read or deleted."));
				}
			}
			else { //there are errors
				if (!currFile.canWrite())
					errors.add(new ERRORPacket((short)2, "Access violation - File cannot be written, read or deleted."));
			}
		}
		else {
			errors.add(new ERRORPacket((short)1, "File not found - DELRQ of non-existing file"));
		}
	}

	/**
	 * Handling a DISC from the client - disconnecting a client
	 * @param id - the ID of the user that wants to disconnect
	 */
	public void disconnect(int id) { 
		if (errors.isEmpty()) {
			server.send(Id, new ACKPacket((short) 0));
			//server.disconnect(id);
			terminate = true;
		}
		loggedIn.remove(id);
	}

	/**
	 * Handling BCAST to all logged in clients
	 * @param msg - the message to broadcast to all the logged in clients
	 */
	public void broadcast(Packet msg) {
		for (Entry<Integer, String> entry : loggedIn.entrySet()){
			if ((server.getHandlers()).containsKey(entry.getKey())) {
				server.getHandlers().get(entry.getKey()).send(msg);
			}
		}
	}
}
