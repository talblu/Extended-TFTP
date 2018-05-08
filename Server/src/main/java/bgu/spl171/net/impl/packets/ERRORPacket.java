package bgu.spl171.net.impl.packets;

public class ERRORPacket extends Packet {
	private short errorCode;
	private String errorMessage;

	public ERRORPacket(short errorCode, String errorMessage) {
		super((short)5);
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
	}
	
	public short getErrorCode(){
		return errorCode;
	}
	
	public String getErrorMessage(){
		return errorMessage;
	}
}