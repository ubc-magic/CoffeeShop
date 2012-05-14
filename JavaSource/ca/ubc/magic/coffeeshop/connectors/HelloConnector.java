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
import ca.ubc.magic.osgibroker.OSGiBrokerClient;
import ca.ubc.magic.osgibroker.OSGiBrokerException;
import ca.ubc.magic.osgibroker.OSGiBrokerService;
import ca.ubc.magic.osgibroker.workgroups.TopicEvent;

/*
 * EyeballingConnector
 * 
 * Author: Roberto Calderon
 * Version: 1.0
 * 
 * This connector is for use with the SocialWall  
 * It has been developed to allow SocialWall to work with the container, 
 * 
 */
public class HelloConnector implements Connector {
	
	CoffeeShop cs;
	String topic = "hello";
	String topicConnector = "cs_hello";

	OSGiBrokerService BrokerHello;
	OSGiBrokerClient ClientHello;
		
	public HelloConnector() throws OSGiBrokerException {
				
		try {
			cs = CoffeeShop.getInstance();			
						
			BrokerHello = new OSGiBrokerService("localhost:8800");
			ClientHello = BrokerHello.addClient("helloconnector");
			
			try {
				ClientHello.subscriber().subscribeHttp("hello", false);
				ClientHello.subscriber().subscribeHttp("cs_hello", false);
								
			} catch (OSGiBrokerException e1) {
				e1.printStackTrace();
			}
			
			Map<String, String> map = new HashMap<String, String>();
			map.put("message", "CONSTRUCTOR");
			sendEvent(map);
			
			new Thread(new SocialwallListener()).start();
			
		}
		catch (UnknownHostException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		
		
		
	}
	
	class SocialwallListener implements Runnable {
		
		@Override
		public void run() {
		
				
			
		
			
			while (true){
				try {
					TopicEvent[] events = ClientHello.subscriber().getEvents("hello", 1);
							
					if ( events.length > 0 ) { // if we have at least one event.
						Map<String, String> map = new HashMap<String, String>();
						map.put("message", "RUNNINGSTILL" );
						ClientHello.publisher().sendEvent("cs_hello", map);							
					}

					Thread.sleep(5000);
					
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				catch (OSGiBrokerException e1) {
					e1.printStackTrace();
				}
					
			}
			
		}
	}
	
	@Override
	public void receiveEvent(TopicEvent event) {
		// Nothing to do here. No need to do anything on receive.
	}
	
	@Override
	public void sendEvent(Map<String, String> paramaters) {
		//
	}

	
}
