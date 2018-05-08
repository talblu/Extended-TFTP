package bgu.spl171.net.impl.TFTPtpc;

import java.io.IOException;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import bgu.spl171.net.srv.ConnectionHandler;
import bgu.spl171.net.srv.NonBlockingConnectionHandler;

public class ConnectionsImpl<T> implements bgu.spl171.net.api.bidi.Connections<T> {
	private ConcurrentHashMap<Integer, ConnectionHandler<T>> handlers;
	private AtomicInteger idCounter;
	
	public ConnectionsImpl(){
		handlers = new ConcurrentHashMap<Integer, ConnectionHandler<T>>();
		idCounter = new AtomicInteger(0);
	}

	@Override
	public boolean send(int connectionId, T msg) {
		if (handlers.containsKey(connectionId)) {
			handlers.get(connectionId).send(msg);
			return true;
		}
		return false;
	}

	@Override
	public void broadcast(T msg) {
		for (Entry<Integer, ConnectionHandler<T>> entry : handlers.entrySet()){
			if (handlers.containsKey(entry.getValue()))
				handlers.get(entry.getValue()).send(msg);
		}
	}

	@Override
	public void disconnect(int connectionId) {
                        try{ 
                            handlers.get(connectionId).close();
                            }
                        catch (IOException e) {
                            e.printStackTrace();
                        }
			handlers.remove(connectionId);
			idCounter.set(connectionId);
	}
	/**
	 * @return - the map of all connected clients that contains their connection handlers and a unique ID for each client
	 */
	public ConcurrentHashMap<Integer, ConnectionHandler<T>> getHandlers() {
		return handlers;
	}
	
	/**
	 * @return - the unique ID of the current client
	 */
	public int getId() {
		return idCounter.get();
	}
	
	/**
	 * adding a new client that wants to connect to the map of all connection handlers. In addition, creates a unique ID for the client
	 * @param handler - the connection hanlder to be put in the map
	 */
	public void add(ConnectionHandler<T> handler){
		handlers.put(idCounter.get(), handler);
		while (handlers.containsKey(idCounter.incrementAndGet()));
	}
}
