package bgu.spl171.net.impl.packets;

public class DELPacket extends Packet {
	private String fileName;
	
	public DELPacket(String FileName) {
		super((short)8);
		this.fileName = FileName;
	}
	
	public String getFileName(){
		return fileName;
	}
}
