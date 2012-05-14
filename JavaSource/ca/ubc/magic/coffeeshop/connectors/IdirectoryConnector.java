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
public class IdirectoryConnector implements Connector {
	
	CoffeeShop cs;
	String topic = "broker.idirectory";
	String topicConnector = "cs_idirectory";
	OSGiBrokerService BrokerIdirectory;
	OSGiBrokerClient ClientIdirectory;
	
	public IdirectoryConnector() throws OSGiBrokerException {
				
		try {
			cs = CoffeeShop.getInstance();		
			
			BrokerIdirectory = new OSGiBrokerService("broker.magic.ubc.ca:8800");
			ClientIdirectory = BrokerIdirectory.addClient("idirectoryconnector");
			try {
				ClientIdirectory.subscriber().subscribeHttp("cs_idirectory", false);
				ClientIdirectory.subscriber().subscribeHttp("broker.idirectory", false);
								
			} catch (OSGiBrokerException e1) {
				e1.printStackTrace();
			}			
			
			new Thread(new IdirectoryListener()).start();
			
		}
		catch (UnknownHostException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		
		
		
	}
	
	class IdirectoryListener implements Runnable {
		
		@Override
		public void run() {
			
			while (true){
				try {
					
					TopicEvent[] events = ClientIdirectory.subscriber().getEvents("pspi.idirectory", 1);
					
					for (int i=0; i< events.length; i++){
						
						String idName = events[i]. getAttribute("clientID");
						String phoneWord = "idirectory_phone";

						int containsPhone = idName.indexOf(phoneWord);
						
						//Map<String, String> map = new HashMap<String, String>();
						//map.put("message", String.valueOf(containsPhone));
						//ClientIdirectory.publisher().sendEvent("cs_idirectory", map);							
						
						//This might be removed. Apparently it stops receiving messages in 30 secs.						
						if ( containsPhone == 0 ) {
							Map<String, String> map = new HashMap<String, String>();
							map.put("message", "RUNNINGSTILL");
							ClientIdirectory.publisher().sendEvent("cs_idirectory", map);										
						}
						
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
