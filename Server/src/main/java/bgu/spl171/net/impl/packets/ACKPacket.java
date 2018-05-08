package bgu.spl171.net.impl.packets;

/**
 * 
 * Acknowledgement packet class.
 * Used for notifying arrival about requests\packets.
 */
public class ACKPacket extends Packet {
	private short blockNum;
	
	/**
	 * ACKPacket constructor
	 * @param blockNum - short representing the acknowledgement number:
	 * 0 = n - for confirming requests (delete, write, read..)
	 * 0 < n - for confirming arrival of data pocket number n
	 */
	public ACKPacket(short blockNum){
		super((short)4);
		this.blockNum = blockNum;
	}
	
	/**
	 * A getter for the blockNumber
	 * @return blockNum - the class variable.
	 */
	public short getBlockNum(){
		return blockNum;
	}
}