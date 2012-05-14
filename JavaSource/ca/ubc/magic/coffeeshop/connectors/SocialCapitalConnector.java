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
 * This connector is for use with the SocialCapital  
 * It has been developed to allow SocialCapital to work with the container, 
 * 
 */
public class SocialCapitalConnector implements Connector {
	
	CoffeeShop cs;
	String topic = "osn";
	String topicConnector = "cs_socialcapital";

	OSGiBrokerService BrokerSocialcapital;
	OSGiBrokerClient ClientSocialcapital;
	
	
	public SocialCapitalConnector() throws OSGiBrokerException {
				
		try {
			cs = CoffeeShop.getInstance();

			BrokerSocialcapital = new OSGiBrokerService("broker.magic.ubc.ca:8800");
			ClientSocialcapital = BrokerSocialcapital.addClient("socialcapitalconnector");
			
			try {
				ClientSocialcapital.subscriber().subscribeHttp("osn", false);
				ClientSocialcapital.subscriber().subscribeHttp("cs_socialcapital", false);
								
			} catch (OSGiBrokerException e1) {
				e1.printStackTrace();
			}
			
			
			Map<String, String> map = new HashMap<String, String>();
			map.put("message", "CONSTRUCTOR");
			sendEvent(map);
			
			new Thread(new SocialcapitalListener()).start();
			
		}
		catch (UnknownHostException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		
		
		
	}
	
	class SocialcapitalListener implements Runnable {
		
		@Override
		public void run() {
			
			while (true){
				try {
					TopicEvent[] events = ClientSocialcapital.subscriber().getEvents("osn", 1);
							
					if ( events.length > 0 ) { // if we have at least one event.
						Map<String, String> map = new HashMap<String, String>();
						map.put("message", "RUNNINGSTILL" );
						ClientSocialcapital.publisher().sendEvent("cs_socialcapital", map);							
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
