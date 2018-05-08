package bgu.spl171.net.impl.TFTPreactor;

import bgu.spl171.net.impl.TFTPtpc.BidiMessagingProtocolImpl;
import bgu.spl171.net.impl.TFTPtpc.MessageEncoderDecoderImpl;
import bgu.spl171.net.srv.Server;

public class ReactorMain {
	public static void main(String[] args) {
		int port = Integer.parseInt(args[0]);
		Server.reactor (10, port, ()-> {return new BidiMessagingProtocolImpl();}, ()-> {return new MessageEncoderDecoderImpl();}).serve();
	}
}
