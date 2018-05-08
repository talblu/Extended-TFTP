package bgu.spl171.net.impl.TFTPtpc;

import java.util.function.Supplier;

import bgu.spl171.net.api.MessageEncoderDecoder;
import bgu.spl171.net.api.bidi.BidiMessagingProtocol;
import bgu.spl171.net.impl.packets.Packet;
import bgu.spl171.net.srv.BaseServer;
import bgu.spl171.net.srv.BlockingConnectionHandler;

public class TPCMain<T> extends BaseServer<T> {

	public TPCMain(int port, Supplier<BidiMessagingProtocol<T>> protocolFactory, Supplier<MessageEncoderDecoder<T>> encdecFactory) {
		super(port, protocolFactory, encdecFactory);
	}

	@Override
	protected void execute(BlockingConnectionHandler<T> handler) {
		new Thread(handler).start();
	}
	
	public static void main(String[] args){

		Supplier<BidiMessagingProtocol<Packet>> protocolFactory = ()->{return new BidiMessagingProtocolImpl<Packet>();};
		Supplier<MessageEncoderDecoder<Packet>> encdecFactory = ()->{return new MessageEncoderDecoderImpl<Packet>();};
		
		BaseServer<Packet> server = new TPCMain<>(Integer.parseInt(args[0]), protocolFactory, encdecFactory);
		server.serve();
	}
}