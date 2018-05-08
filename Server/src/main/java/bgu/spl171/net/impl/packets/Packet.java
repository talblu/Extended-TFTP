package bgu.spl171.net.impl.packets;

public abstract class Packet {
	private short OpCode;

	public Packet(short OpCode){
		this.OpCode = OpCode;
	}

	public short getOpCode(){
		return OpCode;
	}
}
