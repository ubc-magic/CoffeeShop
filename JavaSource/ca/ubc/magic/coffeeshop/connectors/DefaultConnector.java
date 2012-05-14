package ca.ubc.magic.coffeeshop.connectors;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;

import ca.ubc.magic.coffeeshop.classes.CoffeeShop;
import ca.ubc.magic.osgibroker.OSGiBrokerException;
import ca.ubc.magic.osgibroker.workgroups.TopicEvent;

/**
 * DefaultConnector
 * 
 * @author Jay Wakefield
 * @version 1.0
 * 
 *          This is the default connector for all classes which do not have a
 *          custom connector. It is required in order to send messages received
 *          via SMS message to the correct OSGiBroker topic for the application.
 * 
 */
public class DefaultConnector implements Connector {
	
	private static CoffeeShop coffeeshop;
	private final Logger log = Logger.getLogger(DefaultConnector.class);
	
	static {
		try {
			coffeeshop = CoffeeShop.getInstance();
		}
		catch (FileNotFoundException e) {
			// This should never happen, so panic if it does
		}
		catch (IOException e) {
			// This should never happen, so panic if it does
		}
	}
	
	@Override
	public void receiveEvent(TopicEvent event) {
		// Do nothing. The message goes to the OSGiBroker,
		// events that are not custom configured should be
		// picked up by the application registered to the
		// topic.
		// In the case that this was a custom application,
		// we might do something interesting here.
	}
	
	@Override
	public void sendEvent(Map<String, String> paramaters) {
		try {
			coffeeshop.publishEvent(paramaters);
		}
		catch (OSGiBrokerException e) {
			log.error("Could not publish event to OSGiBroker.", e);
		}
	}
	
}
