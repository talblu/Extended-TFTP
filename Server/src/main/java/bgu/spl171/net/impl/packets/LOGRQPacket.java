package bgu.spl171.net.impl.packets;

public class LOGRQPacket extends Packet {
	private String userName;

	public LOGRQPacket(String userName){
		super((short) 7);
		this.userName =  userName;
	}

	public String getUserName(){
		return userName;
	}
}
