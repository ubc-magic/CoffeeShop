package ca.ubc.magic.coffeeshop.connectors;

import java.util.Map;

import ca.ubc.magic.osgibroker.workgroups.TopicEvent;

/**
 * Connector
 * 
 * @author Jay Wakefield
 * @version 1.0
 * 
 *          This interface is to be implemented when developing custom
 *          connectors
 *          for applications to be plugged into the coffee shop application.
 *          It simply provides the event handler to call the appropriate method
 *          when it sends or receives an event. The methods for send and receive
 *          must be implemented by the connector developer.
 * 
 */
public interface Connector {
	
	/**
	 * Sends the given parameters to the OSGiBroker, and does programmer
	 * specific tasks.
	 * 
	 * Note that the programmer MUST publish events to the OSGiBroker in this
	 * method. This is important, because it notifies the OSGiBroker and the
	 * coffee shop application that the application is being interacted with. If
	 * events are not sent to the broker in this method, the appliaction will be
	 * removed from context on its first timer expiriation regardless of whether
	 * it is being interactd with or not.
	 * 
	 * @param paramaters
	 *            a list of parameters to send with the event to the OSGiBroker
	 */
	public void sendEvent(Map<String, String> paramaters);
	
	/**
	 * Processes an event received from the OSGiBroker. The contents of this
	 * method is completely up the connector programmer.
	 * 
	 * @param event
	 *            a TopicEvent object containing the information given by the
	 *            OSGiBroker
	 */
	public void receiveEvent(TopicEvent event);
}
