package bgu.spl171.net.impl.packets;

public class RWPacket extends Packet {
	private String fileName;
	
	public RWPacket(String fileName, short opCode){
		super((short) opCode);//1 for RRQ, 2 for WRQ
		this.fileName = fileName;
	}
	
	public String getFileName(){
		return fileName;
	}
}
