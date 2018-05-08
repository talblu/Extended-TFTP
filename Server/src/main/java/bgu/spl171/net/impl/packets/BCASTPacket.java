package bgu.spl171.net.impl.packets;

/**
 * This packet is for broadcasting a message to all logged in users.
 */
public class BCASTPacket extends Packet {
	private byte added;
	private String fileName;
	
	public BCASTPacket(byte added, String fileName){
		super((short)9);
		this.added = added; //1 for added, 0 for deleted
		this.fileName = fileName;
	}
	
	public byte getAddedDeleted(){
		return added;
	}
	
	public String getFileName(){
		return fileName;
	}
}
