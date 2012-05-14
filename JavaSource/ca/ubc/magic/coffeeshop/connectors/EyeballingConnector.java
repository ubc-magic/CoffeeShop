package ca.ubc.magic.coffeeshop.connectors;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import ca.ubc.magic.coffeeshop.classes.CoffeeShop;
import ca.ubc.magic.coffeeshop.jaxb.Application;
import ca.ubc.magic.osgibroker.OSGiBrokerException;
import ca.ubc.magic.osgibroker.workgroups.TopicEvent;

/*
 * EyeballingConnector
 * 
 * Author: Jay Wakefield
 * Version: 1.0
 * 
 * This connector is for use with the PSPI Eyeballing appliation.  
 * It has been developed to allow Eyeballing to work with the container, 
 * as well as provides an example of a custom connector.
 */
public class EyeballingConnector implements Connector {
	
	SocketChannel sock;
	ByteBuffer buf;
	CoffeeShop cs;
	
	public EyeballingConnector() {
		try {
			cs = CoffeeShop.getInstance();
			
			// Set up socket for eyeballing events. Just listens on this socket.
			sock = SocketChannel.open();
			sock.configureBlocking(false);
			sock.connect(new InetSocketAddress(InetAddress.getByName("broker.magic.ubc.ca"), 8090));
			
			buf = ByteBuffer.allocate(1024);
			
			new Thread(new Listener()).start();
			
		}
		catch (UnknownHostException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * This Listener listens on the eyeballing socket for events. If events are
	 * received, it notifies the OSGiBroker that the game is being used.
	 * 
	 * Socket implemented is non-blocking. Must be non-blocking in order to
	 * allow the application to run, and this thread to monitor the events.
	 */
	private class Listener implements Runnable {
		
		@Override
		public void run() {
			try {
				// Set up non-blockig io
				Selector sel = Selector.open();
				while (sel.select(500) > 0) {
					
					Set<SelectionKey> keys = sel.selectedKeys();
					Iterator<SelectionKey> it = keys.iterator();
					
					while (it.hasNext()) {
						SelectionKey key = it.next();
						it.remove();
						
						if (key.isConnectable()) {
							while (true) {
								if (sock.read(buf) > 0) {
									// Got a message. Send it to the broker.
									Map<String, String> map = new HashMap<String, String>();
									map.put("message", "bogus");
									sendEvent(map);
									buf.clear();
								}
								else { 
									// Check to see if we can close this thread.
									Application app = cs.getCurrentApplication();
									if (!app.getApplicationShortName().equals("eyeballing")) {
										sock.close();
										break;
									}
								}
							}
							break;
						}
						break;
					}
				}
			}
			catch (IOException e) {
				
			}
			
		}
	}
	
	@Override
	public void receiveEvent(TopicEvent event) {
		// Nothing to do here. No need to do anything on receive.
	}
	
	@Override
	public void sendEvent(Map<String, String> paramaters) {
		Application app = cs.getCurrentApplication();
		// Check to see if we can stop the thread.
		if (!app.getApplicationShortName().equals("eyeballing")) {
			try {
				sock.close();
			}
			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else {
			try {
				// Publish the event to OSGiBroker.
				cs.publishEvent(paramaters);
			}
			catch (OSGiBrokerException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		sock.close();
	}
	
}
