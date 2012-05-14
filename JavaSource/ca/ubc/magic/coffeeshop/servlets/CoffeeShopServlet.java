package ca.ubc.magic.coffeeshop.servlets;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import ca.ubc.magic.coffeeshop.classes.CoffeeShop;
import ca.ubc.magic.coffeeshop.connectors.Connector;
import ca.ubc.magic.coffeeshop.exceptions.ConfigurationException;
import ca.ubc.magic.coffeeshop.jaxb.Application;
import ca.ubc.magic.osgibroker.OSGiBrokerException;
import ca.ubc.magic.osgibroker.workgroups.Subscriber;
import ca.ubc.magic.osgibroker.workgroups.TopicEvent;

/**
 * CoffeeShopServlet
 * 
 * @author Jay Wakefield
 * @version 1.0
 * 
 *          This servlet is to be used with the OSGiBroker. Some topics (such as
 *          SMS and custom connecting applications) require that the container
 *          is notified when a message is received. For these, this servlet is
 *          registered with the broker on subscription.
 * 
 *          The servlet interprets and parses the message depending on which
 *          topic it came from.
 * 
 *          This class performs the main logic of routing messages for different
 *          parts of the application.
 * 
 *          1) SMS - If the message came in from the SMS topic, the message is
 *          parsed by looking for the SMS protocol keyword. "Select" messages
 *          are send to the CoffeeShop class for queueing an appliaction. "Say"
 *          messages are sent to the message board for posting. All other
 *          messages are forwarded to the currently running appliation's topic
 *          on the OSGiBroker.
 * 
 *          2) Custom Connector - If the message that is recieved is intended
 *          for an application with a custom connector, the message is forwarded
 *          to that connector.
 */
public class CoffeeShopServlet extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
	
	private final Logger log = Logger.getLogger(CoffeeShopServlet.class);
	
	private CoffeeShop coffeeshop;
	private Connector conn;
	
	@Override
	public void init() throws ServletException {
		try {
			// Get coffeeshop instance.
			this.coffeeshop = CoffeeShop.getInstance();
		}
		catch (FileNotFoundException e) {
			log.fatal("Problem trying to find configuration file.", e);
			throw new ConfigurationException(e);
		}
		catch (IOException e) {
			log.fatal("Problem reading configuration file.", e);
			throw new ConfigurationException(e);
		}
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doPost(req, resp);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		try {
			// Get the events received on this URL
			TopicEvent[] events = Subscriber.parseEvents(req.getInputStream());
			TopicEvent event = events[0];
			
			Runnable r = new processEventThread(event);
			new Thread(r).start();
		}
		catch (OSGiBrokerException e) {
			// If we get in here, then the message received was not from the
			// OSGiBroker, so we let the custom handler deal with it
			doApplicationSend(req.getParameterMap());
		}
	}
	
	/*
	 * This sub-class prevents a bug in the OSGI broker. It's a quick and dirty fix
	 * to the lack of threads in the OGSiBroker version 3.
	 *  
	 */
	class processEventThread extends Thread {
		
		TopicEvent event;
		
		public processEventThread (TopicEvent event) {
			this.event = event;
		}
		
        public void run() { //TODO: test
        
        	if (event.getTopic().equals(coffeeshop.getProperties().getProperty("smsDefaultTopic"))) {
				// If recieved via the SMS topic, decode the message
				log.info("got sms message"); //TODOL remove
				smsDecode(event);
			} 
			
			else if (event.getTopic().equals(coffeeshop.getProperties().getProperty("osnDefaultTopic"))) {
				// If recieved via the OSN topic, decode the message
				log.info("got osn message"); //TODOL remove
				osnDecode(event);
			}
			
			else if (event.getTopic().equals(coffeeshop.getProperties().getProperty("coffeeShopMenuTopicName"))) {
				// If message received is for the menu, decode for the menu.
				// NOTE: This conditional is most likely @deprecated.
				doMenuOperation(Integer.parseInt(event.getAttribute("menuNum")));
			}
			else if (event.getTopic().equals(coffeeshop.getCurrentApplication().getConnectionInfo().getTopic())) {
				// If we get here it means that the OSGiBroker received
				// something, and now the custom connector should deal with it
				// Note that we only register this servlet if a custom connector
				// is defined.
				// Therefore we should not get here with the default connector
				// Also, notify the coffee shop that an event was received on
				// the custom connector
				coffeeshop.notifyCustomEvent();
				doApplicationReceive(event);
			}

        }
	}
	
	
	/*
	 * (non-JavaDoc)
	 * 
	 * Decode the sms message that we have received.
	 * 
	 * "Select" messages are sent to the CoffeeShop class for queueing an
	 * appliaction. "Say" messages are sent to the message board for posting.
	 * All other messages are forwarded to the currently running appliation's
	 * topic on the OSGiBroker.
	 */
	private void smsDecode(TopicEvent event) {
		
		// Trim the message
		String message = event.getAttribute("message").toLowerCase().trim();
		
		String identifier;
		try {
			// Get the keyword
			identifier = message.substring(0, message.indexOf(" ")).trim();
		}
		catch (Exception e) {
			identifier = null;
		}
		
		// Route the message based on keyword
		if ("select".equals(identifier)) {
			String num = message.substring(message.indexOf(" ")).trim();
			try {
				// Queue the application
				doMenuOperation(Integer.parseInt(num));
			}
			catch (NumberFormatException e) {
				// User screwed the message up. Ignore it.
				log.info("Incorrectly formatted SMS message: " + message);
			}
		}
		else if ("say".equals(identifier)) {
			try {
				// Send the message to the message board
				doSMSBoardOperation(event);
			}
			catch (OSGiBrokerException e) {
				log.error("Could not send message to message board", e);
			}
		}
		else {
			// TODO : This might need to change. We might not need this because
			// the running application already knows about it!
			// There is no keyword, so kick the message up to the application in
			// context.
			Map<String, String> map = new HashMap<String, String>();
			for (String attr : event.getNameArray()) {
				map.put(attr, event.getAttribute(attr));
			}
			doApplicationSend(map);
		}
		
	}
	
	/*
	 * (non-JavaDoc)
	 * 
	 * Decode the twitter message that we have received.
	 * 
	 * "Select" messages are sent to the CoffeeShop class for queueing an
	 * appliaction. "Say" messages are sent to the message board for posting.
	 * All other messages are forwarded to the currently running appliation's
	 * topic on the OSGiBroker.
	 */
	//TODO: add decoding DM or mentions. 
	private void osnDecode(TopicEvent event) { //TODO: test
				
		// Trim the message
		String message = event.getAttribute("data").toLowerCase().trim();
		
		String identifier;
		try {
			// Get the keyword
			identifier = message.substring(0, message.indexOf(" ")).trim();
		}
		catch (Exception e) {
			identifier = null;
		}
		
		// Route the message based on keyword
		if ("select".equals(identifier)) {
			String num = message.substring(message.indexOf(" ")).trim();
			try {
				// Queue the application
				doMenuOperation(Integer.parseInt(num));
			}
			catch (NumberFormatException e) {
				// User screwed the message up. Ignore it.
				log.info("Incorrectly formatted osn message: " + message);
			}
		}
		else if ("say".equals(identifier)) {
			try {
				// Send the message to the message board
				doOSNBoardOperation(event);
			}
			catch (OSGiBrokerException e) {
				log.error("Could not send message to message board", e);
			}
		}
		else {
			// TODO : This might need to change. We might not need this because
			// the running application already knows about it!
			// There is no keyword, so kick the message up to the application in
			// context.
			HashMap<String, String> map = new HashMap<String, String>();
			for (String attr : event.getNameArray()) {
				map.put(attr, event.getAttribute(attr));
			}
			doApplicationSend(map);
		}
		
	}
	
	
	
	
	/*
	 * (non-JavaDoc)
	 * 
	 * Queue the selected application
	 */
	private void doMenuOperation(int menuNumber) {
		coffeeshop.queueApplication(menuNumber - 1);
	}
	
	/*
	 * (non-JavaDoc) 
	 * Send the recieved sms message to the message board.
	 */
	private void doSMSBoardOperation(TopicEvent event) throws OSGiBrokerException {
		
		HashMap<String, String> map = new HashMap<String, String>();
		
		// Get the SMS number (last 4 digits) and the message.
		String phoneNumber = event.getAttribute("from").trim();
		String message = event.getAttribute("message").trim();
		
		if (phoneNumber.length() < 4)
				phoneNumber = "user";
		
		map.put("name", phoneNumber.substring(phoneNumber.length() - 4));
		map.put("message", message.substring(message.indexOf(" ")).trim());
		
		// Push to the board's topic.
		coffeeshop.getMessageBoard().pushEvent(map);
	}
	
	/*
	 * (non-JavaDoc) 
	 * Send the recieved osn message to the message board.
	 */
	private void doOSNBoardOperation(TopicEvent event) throws OSGiBrokerException {
		
		HashMap<String, String> map = new HashMap<String, String>();
		
		// Get the SMS number (last 4 digits) and the message.
		String username = event.getAttribute("friendlyname").trim();
		String message = event.getAttribute("data").trim();
		
		map.put("name", username);
		map.put("message", message.substring(message.indexOf(" ")).trim());
		
		// Push to the board's topic.
		coffeeshop.getMessageBoard().pushEvent(map);
	}
	
	
	/*
	 * (non-JavaDoc)
	 * 
	 * Send message to custom connector receive method.
	 */
	private void doApplicationReceive(TopicEvent event) {
		if (!initializeConnector())
			conn.receiveEvent(event);
	}
	
	/*
	 * (non-JavaDoc)
	 * 
	 * Send message to the custom connector send method.
	 */
	private void doApplicationSend(Map<String, String> params) {
		if (!initializeConnector())
			conn.sendEvent(params);
	}
	
	/*
	 * (non-JavaDoc)
	 * 
	 * Initializes the custom connector required by the currently running
	 * application.
	 * 
	 * Returns true if the application was successfully initialized.
	 * Returns false if the connector already exists and is ready for use.
	 */
	@SuppressWarnings("unchecked")
	private boolean initializeConnector() {
		
		Application currentApp = coffeeshop.getCurrentApplication();
		String connectorName = currentApp.getConnectionInfo().getConnectorClass();
		
		if (conn == null || !conn.getClass().getName().equals(connectorName)) {
			try {
				Class<Connector> clazz = (Class<Connector>) Class.forName(connectorName);
				conn = clazz.newInstance();
				return true;
			}
			catch (ClassNotFoundException e) {
				log.error("Could not find connector class", e);
			}
			catch (InstantiationException e) {
				log.error("Could not instantiate connector class", e);
			}
			catch (IllegalAccessException e) {
				log.error("Could not access connector class", e);
			}
		}
		return false;
		
	}
	
	@Override
	public void destroy() {
		super.destroy();
		try {
			coffeeshop.shutdown();
			coffeeshop.saveState(getServletContext());
		}
		catch (URISyntaxException e) {
			log.error("Could not save server state.", e);
		}
		catch (IOException e) {
			log.error("Could not save server state.", e);
		}
	}
}
