package bgu.spl171.net.impl.TFTPtpc;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Vector;

import bgu.spl171.net.api.MessageEncoderDecoder;
import bgu.spl171.net.impl.packets.*;

public class MessageEncoderDecoderImpl<T> implements MessageEncoderDecoder<T> {

	private short opCode = 0;
	private Vector<Byte> bytes = new Vector<Byte>();
	private byte[] opCodeByte = new byte[2];
	private byte[] bytesArr = null; //the array that will contain the elements from the bytes vector
	private boolean opCodeReceived = false;
	private short packetSize = 0;
	private byte[] packetSizeArr;
	private byte[] blockNumArr = new byte[2];

	@Override
	public T decodeNextByte(byte nextByte) {
		bytes.add(nextByte);
		if (bytes.size() == 1)
			opCodeByte[0] = nextByte;
		if (bytes.size() == 2){
			opCodeByte[1] = nextByte;
			opCode = bytesToShort(opCodeByte);
			opCodeReceived = true;
		}
		if (opCodeReceived){
			switch (opCode) {
			case 1: //RRQ
				if (nextByte == 0){
					bytesArr = new byte[bytes.size()];
					bytesArr = vectorToArray(bytes);
					String fileToRead = new String(bytesArr, 2, bytesArr.length - 3, StandardCharsets.UTF_8); 
					bytes = new Vector<Byte>();
					opCodeReceived = false;
					return (T) new RWPacket(fileToRead, opCode);
				}
				break;
			case 2: //WRQ
				if (nextByte == 0){
					bytesArr = new byte[bytes.size()];
					bytesArr = vectorToArray(bytes);
					String fileToRead = new String(bytesArr, 2, bytesArr.length - 3, StandardCharsets.UTF_8); 
					bytes = new Vector<Byte>();
					opCodeReceived = false;
					return (T) new RWPacket(fileToRead, opCode);
				}
				break;
			case 3: //DATA
				if (bytes.size() == 3) {
					packetSizeArr = new byte[2];				
					packetSizeArr[0] = bytes.get(2);
				}
				if (bytes.size() == 4) {
					packetSizeArr[1] = bytes.get(3);
					packetSize = bytesToShort(packetSizeArr);	
				}
				if (bytes.size() == packetSize + 6) {
					bytesArr = new byte[bytes.size()];
					bytesArr = vectorToArray(bytes);
					short blockNum = bytesToShort(Arrays.copyOfRange(bytesArr, 4, 6));
					byte[] data = Arrays.copyOfRange(bytesArr, 6, bytesArr.length);
					bytes = new Vector<Byte>();
					opCodeReceived = false;
					return (T) new DATAPacket(packetSize, blockNum, data);
				}
				break;
			case 4: //ACK
				if (bytes.size() == 3) 
					blockNumArr[0] = nextByte;
				if (bytes.size() == 4) {
					blockNumArr[1] = nextByte;
					short blockNumber = bytesToShort(blockNumArr);
					bytes = new Vector<Byte>();
					opCodeReceived = false;
					return (T) new ACKPacket(blockNumber);
				}
				break;
			case 5: //ERROR
				byte[] ErrorCode = new byte[2];
				if (nextByte == 0) {
					ErrorCode[0] = bytes.get(2);
					ErrorCode[1] = bytes.get(3);
					bytesArr = new byte[bytes.size()];
					bytesArr = vectorToArray(bytes);
					String errorMessage = new String(bytesArr, 4, bytesArr.length - 5, StandardCharsets.UTF_8);
					bytes = new Vector<Byte>();
					opCodeReceived = false;
					return (T) new ERRORPacket(bytesToShort(ErrorCode),errorMessage);
				}
				break;
			case 6: //DIRQ
				if (bytes.size() ==  2) {
					bytes = new Vector<Byte>();
					opCodeReceived = false;
					return (T) new DIRPacket();
				}
				break;
			case 7: //LOGRQ
				if (nextByte == 0){
					bytesArr = new byte[bytes.size()];
					bytesArr = vectorToArray(bytes);
					String userName = new String(bytesArr, 2, bytesArr.length - 3, StandardCharsets.UTF_8);
					bytes = new Vector<Byte>();
					opCodeReceived = false;
					return (T) new LOGRQPacket(userName);
				}
				break;
			case 8: //DELRQ
				if (nextByte == 0){
					bytesArr = new byte[bytes.size()];
					bytesArr = vectorToArray(bytes);
					String fileName = new String(bytesArr, 2, bytesArr.length - 3, StandardCharsets.UTF_8);
					bytes = new Vector<Byte>();
					opCodeReceived = false;
					return (T) new DELPacket(fileName);
				}
				break;
			case 10: //DISC
				bytes = new Vector<Byte>();
				opCodeReceived = false;
				return (T) new DISCPacket();
			}
		}
		return null;
	}

	@Override
	public byte[] encode(T message) {
		Packet msg = (Packet)message;
		byte[] PacketBytes = null;
		byte[][] collection = null;
		switch (msg.getOpCode()){
		case 3: //DATA
			DATAPacket packet = (DATAPacket)msg;
			collection = new byte[4][];
			collection[0] = shortToBytes(packet.getOpCode());
			collection[1] = shortToBytes(packet.getPacketSize());
			collection[2] = shortToBytes(packet.getBlockNum());
			collection[3] = packet.getData();
			PacketBytes = mergeArr(collection);
			break;
		case 4: //ACK
			ACKPacket ackPacket = (ACKPacket)msg;
			collection = new byte[2][];
			collection[0] = shortToBytes(ackPacket.getOpCode());
			collection[1] = shortToBytes(ackPacket.getBlockNum());
			PacketBytes = mergeArr(collection);
			break;
		case 5: //ERROR
			ERRORPacket errPacket = (ERRORPacket)msg;
			String errorMsg = errPacket.getErrorMessage();
			errorMsg += '\0';
			collection = new byte[3][];
			collection[0] = shortToBytes(errPacket.getOpCode());
			collection[1] = shortToBytes(errPacket.getErrorCode());
			collection[2] = errorMsg.getBytes();
			PacketBytes = mergeArr(collection);
			break;
		case 9: //BCAST
			BCASTPacket bcastPacket = (BCASTPacket)msg;
			byte[] delAdd = {bcastPacket.getAddedDeleted()};
			String fileName = bcastPacket.getFileName();
			fileName += '\0';
			collection = new byte[3][];
			collection[0] = shortToBytes(bcastPacket.getOpCode());
			collection[1] = delAdd;
			collection[2] = fileName.getBytes();
			PacketBytes = mergeArr(collection);
			break;
		}
		return PacketBytes;
	}

	/**
	 * Creates a new packet according to the received opcode and returns it.
	 * @return a new packet to be used
	 */
	public Packet createPacket() { //packet to be sent to server, applies only for client to server communication
		//notice that we explicitly requesting that the string will be decoded from UTF-8
		//this is not actually required as it is the default encoding in java.
		byte[] bytesArr = new byte[bytes.size()];
		for (int i=0; i<bytesArr.length; i++)
			bytesArr[i] = bytes.remove(0);
		short opCode = bytesToShort(Arrays.copyOf(bytesArr, 2));
		int end = findEnd(bytesArr);
		switch(opCode) {
		case 1: //RRQ
			String fileToRead = new String(bytesArr, 2, end, StandardCharsets.UTF_8);
			return new RWPacket(fileToRead, opCode);
		case 2: //WRQ
			String fileToWrite = new String(bytesArr, 2, end, StandardCharsets.UTF_8);
			return new RWPacket(fileToWrite, opCode);
		case 3: //DATA
			short packetSize = bytesToShort(Arrays.copyOfRange(bytesArr, 2, 4));
			short blockNum = bytesToShort(Arrays.copyOfRange(bytesArr, 4, 6));
			byte[] data = Arrays.copyOfRange(bytesArr, 7, bytesArr.length);
			return new DATAPacket(packetSize, blockNum, data);
		case 4: //ACK
			short blockNumber = bytesToShort(Arrays.copyOfRange(bytesArr, 2, 4));
			return new ACKPacket(blockNumber);
		case 6: //DIRQ
			return new DIRPacket();
		case 7: //LOGRQ
			String userName = new String(bytesArr, 2, end, StandardCharsets.UTF_8);
			return new LOGRQPacket(userName);
		case 8: //DELRQ
			String fileName = new String(bytesArr, 2, end, StandardCharsets.UTF_8);
			return new DELPacket(fileName);
		case 10: //DISC
			return new DISCPacket();
		default: return null;
		}
	}

	/**
	 * turns a short value into bytes
	 * @param num - the short value to be turned into bytes
	 * @return - a byte array symbolizing the value of num
	 */
	public static byte[] shortToBytes(short num)
	{
		byte[] bytesArr = new byte[2];
		bytesArr[0] = (byte)((num >> 8) & 0xFF);
		bytesArr[1] = (byte)(num & 0xFF);
		return bytesArr;
	}

	/**
	 * turns a byte array into short
	 * @param byteArr - the byte array to turn to short
	 * @return - a short value symbolizing the byte array byteArr
	 */
	public static short bytesToShort(byte[] byteArr)
	{
		short result = (short)((byteArr[0] & 0xff) << 8);
		result += (short)(byteArr[1] & 0xff);
		return result;
	}

	/**
	 * finds and returns the end index of a byte array ('/0')
	 * @param arr - the byte array
	 * @return - the index of the char '/0' indicating the end of the byte array, or -1 if '/0' does not exist in the byte array
	 */
	public int findEnd (byte[] arr) {
		for (int i = 0; i<arr.length; i++) {
			if (arr[i] == '\0')
				return i; // returns the index of the last byte of this message
		}
		return -1; // Didn't file the end of the message
	}

	/**
	 * merging a few byte arrays into one big byte array
	 * @param arr - a 2-dimensional byte array that contains all the byte array to merge
	 * @return - a byte array that is a result of merging all the lines in the 2-dimensional byte array
	 */
	private byte[] mergeArr(byte[][] arr){ //merge all the arrays
		int arrLen = 0;
		for (int i = 0 ; i < arr.length ; i++)
			arrLen += arr[i].length;
		byte[] res = new byte[arrLen];
		int ind = 0;
		for (int i = 0 ; i < arr.length ; i++)
			for (int j = 0 ; j < arr[i].length ; j++)
				res[ind++] = arr[i][j];
		return res;
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
}
