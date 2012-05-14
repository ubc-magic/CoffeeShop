package ca.ubc.magic.coffeeshop.classes;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import ca.ubc.magic.osgibroker.OSGiBrokerClient;
import ca.ubc.magic.osgibroker.OSGiBrokerException;
import ca.ubc.magic.osgibroker.OSGiBrokerService;
import ca.ubc.magic.osgibroker.workgroups.TopicEvent;

/**
 * MessageBoard
 * 
 * @author Jay Wakefield
 * @version 1.0
 * 
 *          The MessageBoard object handles the pushing and pulling of events
 *          that are intended for the CoffeeShop message board.
 *          The class was created to provide some abstraction and
 *          differentiation between the CoffeeShop itself and the MessageBoard.
 *          Since the MessageBoard is technically its own application within the
 *          CoffeeShop, we hadle its events seperately.
 * 
 */
public class MessageBoard {
	
	/* OSGiBroker information */
	private OSGiBrokerService osgiBroker;
	private OSGiBrokerClient client;
	private String topic;
	private String smstopic;
	
	/* log4j Logger */
	private final Logger log = Logger.getLogger(MessageBoard.class);
	
	/**
	 * Constructor
	 * Creates a new instance of the message board, and registers it with the
	 * OSGiBroker with the supplied client and topic
	 * 
	 * Client it requuired so that it is registered with the coffee shop client,
	 * mostly to kepp things consistent.
	 * 
	 * @param client
	 *            the previously registered OSGiBroker client
	 * @param topic
	 *            the name of the topic for the message board
	 */
	public MessageBoard (Properties prop) {
		this.osgiBroker = new OSGiBrokerService(prop.getProperty("osgiHost") + ":" + prop.getProperty("osgiPort"));
		this.client = osgiBroker.addClient(prop.getProperty("messageBoardClientName")); //TODO: unchecked
		this.topic = prop.getProperty("messageBoardTopicName");
	}
	
	/**
	 * Subscribes the message board with the OSGiBroker
	 */
	public void subscribe() {
		try {
			client.subscriber().subscribeHttp(topic, false);
			log.info("Successfully subscribed");
		}
		catch (OSGiBrokerException e) {
			if (e.getStatus() == 409) {
				log.warn("Already subscribed to OSGiBroker topic " + topic);
			}
			else {
				log.warn("Could not subscribe to OSGiBroker topic " + topic + ". Reason unknown.", e);
			}
		}
	}
	
	/**
	 * Unsubscribes the message board with the OSGiBroker
	 */
	public void unsubscribe() {
		try {
			client.subscriber().unsubscribeHttp(topic);
			log.info("Successfully unsubscribed");
		}
		catch (OSGiBrokerException e) {
			if (e.getStatus() == 409) {
				log.warn("Already unsubscribed to OSGiBroker topic " + topic);
			}
			else {
				log.warn("Could unubscribe to OSGiBroker topic " + topic + ". Reason unknown.", e);
			}
		}
	}
	
	/**
	 * Polls the OSGiBroker for new messages intended for the message board.
	 * This method waits three seconds for new messages if none exist at calling
	 * time.
	 * If no messages exist, an empty list is returned.
	 * 
	 * @return a list of messages new TopicEvent objects, or an empty list if
	 *         there are no new messages
	 */
	public List<TopicEvent> getNewMessages() {
		TopicEvent[] events = null;
		try {
			events = client.subscriber().getEvents(topic, 3);
		}
		catch (OSGiBrokerException e) {
			log.error("Could nor recieve events from OSGiBroker.", e);
		}
		
		return Arrays.asList(events);
	}
	
	/**
	 * Sends a new event (or message) to the OSGiBroker on the message board
	 * topic.
	 * 
	 * @param attributes
	 *            attributes to be displayed in the message
	 * @throws OSGiBrokerException
	 *             if sending the message to the OSGiBroker fails
	 */
	public void pushEvent(HashMap<String, String> attributes) throws OSGiBrokerException { //Changed to HashMap
		client.publisher().sendEvent(topic, attributes);
	}
}
