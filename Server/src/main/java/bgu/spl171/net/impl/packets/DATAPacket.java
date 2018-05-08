package bgu.spl171.net.impl.packets;

public class DATAPacket extends Packet {
	private short packetSize;
	private short blockNum;
	private byte[] data;
	
	public DATAPacket(short packetSize, short blockNum, byte[] data){
		super((short)3);
		this.packetSize = packetSize;
		this.blockNum = blockNum;
		this.data = data;
	}
	
	public short getPacketSize(){
		return packetSize;
	}
	
	public short getBlockNum(){
		return blockNum;
	}
	
	public byte[] getData(){
		return data;
	}
}
