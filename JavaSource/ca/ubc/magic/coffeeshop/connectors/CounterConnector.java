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


public class CounterConnector implements Connector {
	
	CoffeeShop cs;
	String topic = "counter";
	String topicConnector = "cs_counter";

	OSGiBrokerService BrokerCounter;
	OSGiBrokerClient ClientCounter;
		
	public CounterConnector() throws OSGiBrokerException {
				
		try {
			cs = CoffeeShop.getInstance();			
						
			BrokerCounter = new OSGiBrokerService("localhost:8800");
			ClientCounter = BrokerCounter.addClient("counterconnector");
			
			try {
				ClientCounter.subscriber().subscribeHttp("counter", false);
				ClientCounter.subscriber().subscribeHttp("cs_counter", false);
								
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
					TopicEvent[] events = ClientCounter.subscriber().getEvents("counter", 1);
							
					if ( events.length > 0 ) { // if we have at least one event.
						Map<String, String> map = new HashMap<String, String>();
						map.put("message", "RUNNINGSTILL" );
						ClientCounter.publisher().sendEvent("cs_counter", map);							
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
