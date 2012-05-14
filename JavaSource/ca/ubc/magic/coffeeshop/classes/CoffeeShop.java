package ca.ubc.magic.coffeeshop.classes;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;

import javax.servlet.ServletContext;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.log4j.Logger;

import ca.ubc.magic.coffeeshop.connectors.DefaultConnector;
import ca.ubc.magic.coffeeshop.exceptions.ConfigurationException;
import ca.ubc.magic.coffeeshop.jaxb.Application;
import ca.ubc.magic.coffeeshop.jaxb.ObjectFactory;
import ca.ubc.magic.osgibroker.OSGiBrokerClient;
import ca.ubc.magic.osgibroker.OSGiBrokerException;
import ca.ubc.magic.osgibroker.OSGiBrokerService;
import ca.ubc.magic.osgibroker.workgroups.TopicEvent;

/**
 * CoffeeShop
 * 
 * @author Jay Wakefield
 * @version 1.0
 * 
 *          This class handles the main logic of the coffeeshop application.
 *          It is responsible for loading configurations, storing the list of
 *          applications, and performing the logic that attends to switching
 *          main applications.
 * 
 *          As such, this class is implemented using the singleton design
 *          pattern, since it is not appropriate to have many instances of the
 *          CoffeeShop running in the applcation.
 * 
 *          To get the instance, use the getInstance method.
 * 
 */
public class CoffeeShop {
	
	/* Configuration Information to find configuration files */
	private static final String CONFIG_PATH = "/ca/ubc/magic/coffeeshop/config/";
	private static final String CONFIG_FILE = "config.properties";
	private String appURL;
	
	/* log4j Logger */
	private final Logger log = Logger.getLogger(CoffeeShop.class);
	
	/* Instance of class for singleton implementation */
	private static CoffeeShop INSTANCE = null;
	
	/* Broker variables */
	private OSGiBrokerService osgiBroker;
	private OSGiBrokerClient osgiClient;
	
	/* Member variables */
	private Properties prop;
	private List<Application> applications;
	private List<Application> allApps;
	private ApplicationQueue<Application> queue;
	private Application defaultApp;
	private Application context;
	private MessageBoard messageBoard;
	private Timer contextTimer;
	
	/* Status variables */
	private boolean contextChange = false;
	private boolean newConfiguration = false;
	private boolean customAppEvent = false;
	
	/**
	 * Gets a CoffeeShop instance. The CoffeeShop returned is configured and
	 * loaded.
	 * 
	 * @return a CoffeeShop instance
	 * @throws FileNotFoundException
	 *             - if any configuration files (either properties file or XML
	 *             files) can not be found
	 * @throws IOException
	 *             - if any configuration files (either properties file or XML
	 *             fail when opening or during reading
	 */
	public static CoffeeShop getInstance() throws FileNotFoundException, IOException {
		if (INSTANCE == null) {
			INSTANCE = new CoffeeShop();
		}
		return INSTANCE;
	}
	
	/*
	 * (non-Javadoc)
	 * CoffeeShop constructor.
	 * - Loads properties file
	 * - Loads all configured Application definition files (XML)
	 * - Subscribes to given topics needed for the CoffeeShop
	 * - Starts the context switch timer</ul>
	 */
	private CoffeeShop() throws FileNotFoundException, IOException {
		loadProperties();
		loadApplications();
		startup();
	}
	
	/*
	 * (non-JavaDoc)
	 * Starts the coffee shop by subscribing the appropriate application with
	 * the broker, sets up the application timer and starts that timer.
	 */
	private void startup() throws FileNotFoundException, IOException {
		log.info("Starting configuration process...");
		configureOSGiBroker();
		startTimer();
	}
	
	/*
	 * (non-Javadoc)
	 * Loads properties from the specified properties file
	 * Properties file just be located in CONFIG_PATH and must be named
	 * CONFIG_FILE
	 */
	private void loadProperties() throws FileNotFoundException, IOException {
		// Load properties file
		log.info("Loading properties configuration.");
		InputStream is = this.getClass().getClassLoader().getResourceAsStream(CONFIG_PATH + CONFIG_FILE);
		prop = new Properties();
		prop.load(is);
		appURL = "http://" + prop.getProperty("coffeeshopHost") + ":" + prop.getProperty("coffeeshopPort");
	}
	
	/*
	 * (non-Javadoc)
	 * Loads all application definition files as specified in the properties
	 * file. This method uses JAXB generated classes to load from XML to POJOs.
	 * These Application classes can then be used throughout java backend.
	 * 
	 * Application definition files must also be located on the CONFIG_PATH
	 * directory.
	 * 
	 * Once all appliations are loaded, they are seperated into lists, depending
	 * on their runtime status.
	 * This status is either:
	 * 
	 * 1) Default Application. This means that users will not be able to
	 * "choose" this application via OSGiBroker. This application will run when
	 * the Coffee Shop is "idle"
	 * 
	 * 2) Configured Application. This means that is is loaded into the large
	 * display, and may be chosen to run.
	 * 
	 * 3) Available Application. This means that the appilication has been
	 * loaded, but is not visible on the large screen, and is not available to
	 * run within the large display. It has been configured and loaded as an
	 * application that has the ability to run, but doesn't.
	 */
	@SuppressWarnings("unchecked")
	private void loadApplications() throws FileNotFoundException, IOException {
		// Load configured applications
		try {
			JAXBContext jc = JAXBContext.newInstance(Application.class.getPackage().getName());
			Unmarshaller u = jc.createUnmarshaller();
			
			allApps = new ArrayList<Application>();
			applications = new ArrayList<Application>();
			List<String> configuredAppNames = Arrays.asList(prop.getProperty("applicationsToRun").split(","));
			
			StringTokenizer tok = new StringTokenizer(prop.getProperty("allApplications"), ",");
			
			while (tok.hasMoreElements()) {
				String file = tok.nextToken();
				String path = CONFIG_PATH + file + ".xml";
				log.info("Loading file: " + path);
				JAXBElement<Application> a = (JAXBElement<Application>) u.unmarshal(this.getClass().getClassLoader()
				        .getResourceAsStream(path));
				
				// Seperate the applications into their various categories
				if (prop.getProperty("defaultApplication").equals(file)) {
					defaultApp = a.getValue();
				}
				else if (!configuredAppNames.contains(a.getValue().getApplicationShortName())) {
					allApps.add(a.getValue());
				}
				else {
					applications.add(a.getValue());
				}				
			}
			
			// At startup only, set the current context application to the
			// default application
			context = defaultApp;
			queue = new ApplicationQueue<Application>();
		}
		catch (JAXBException e) {
			// Something bad happened when unmarshalling the XML.
			throw new IOException(e);
		}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * This method configures and subscribes the connection to the OSGiBroker as
	 * needed by the application.
	 */
	private void configureOSGiBroker() {
		// Get instances of the broker and register a client.
		osgiBroker = new OSGiBrokerService(prop.getProperty("osgiHost") + ":" + prop.getProperty("osgiPort"));
		osgiClient = osgiBroker.addClient(prop.getProperty("coffeeShopClientName"));
		
		// subscribe to the coffee shop application menu, the message board,
		// and the default applications		
		messageBoard = new MessageBoard(prop);
		messageBoard.subscribe();
		
		
		///HERE!!!		
		subscribe(prop.getProperty("coffeeShopMenuTopicName"), appURL + "/CoffeeShop/coffeeShop.do");
			
		// Check the type of connector. If the connector is not default, then
		// register with the CoffeeShop servlet for processing.
		boolean isDefault = context.getConnectionInfo().getConnectorClass().equals(DefaultConnector.class.getName());
		subscribe(defaultApp.getConnectionInfo().getTopic(), !isDefault ? appURL + "/CoffeeShop/coffeeShop.do" : null);
		
		// Register the SMS topic with the coffeeshop servlet for processing
		subscribe(prop.getProperty("smsDefaultTopic"), appURL + "/CoffeeShop/coffeeShop.do");

		// Register the OSN topic
		subscribe(prop.getProperty("osnDefaultTopic"), appURL + "/CoffeeShop/coffeeShop.do"); //TODO: uncomment.	
		
	}
	

	
	/*
	 * (non-Javadoc)
	 * Starts the context timer.
	 * The timer's schedule is defined based on the configured minimum idle time
	 * as specified in the current application's configuration file
	 */
	private void startTimer() {
		contextTimer = new Timer();
		contextTimer.scheduleAtFixedRate(new ContextSwitchTask(), context.getMinumumIdleTime() * 1000, context
		        .getMinumumIdleTime() * 1000 + 5000);
	}
	
	/*
	 * (non-Javadoc)
	 * Stops the current timer.
	 */
	private void stopTimer() {
		try {
			contextTimer.cancel();
		}
		catch (NullPointerException e) {
			// Ignore this. Someone tried to cancel a timer that didn't exist.
		}
	}
	
	/*
	 * (non-Javadoc)
	 * Subscribe to the given topic
	 * 
	 * Topic is a mandatory field
	 * if there is no servlet, pass null for servletUrl
	 */
	private void subscribe(String topic, String servletUrl) {
		try {
			if (servletUrl == null) {
				osgiClient.subscriber().subscribeHttp(topic, false);
				log.info("Successfully subcscribed to " + topic);
			}
			else {
				osgiClient.subscriber().subscribeHttp(topic, false, servletUrl);
				log.info("Successfully subcscribed to " + topic + " , with url " + servletUrl);
			}
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
	
	/*
	 * (non-Javadoc)
	 * Unsubscribe to the given topic
	 */
	private void unsubscribe(String topic) {
		try {
			osgiClient.subscriber().unsubscribeHttp(topic);
			log.info("Successfully unsubscribed from " + topic);
		}
		catch (OSGiBrokerException e) {
			if (e.getStatus() == 409) {
				log.warn("Already unsubscribed to OSGiBroker topic " + topic);
			}
			else {
				log.warn("Could not unsubscribe to OSGiBroker topic " + topic + ". Reason unknown.", e);
			}
		}
	}
	
	/**
	 * Perform a context switch.
	 * 
	 * Doing this removes the current running
	 * application and puts either the next queued application in its place, or
	 * replaces it with the default application.
	 * 
	 * If the method is called while the default application is running and
	 * there is no new application ready to take its place, the default
	 * application will continue running.
	 */
	public synchronized void doContextSwitch() {
		
		boolean changed = true;
		
		// Get the current running topic so that we can unsubsribe after the
		// switch
		String previousTopic = context.getConnectionInfo().getTopic();
		
		
		try {
			// Get any application queued
			context = queue.dequeue();
		}
		catch (NoSuchElementException e) {
			/*RVCA: removed this to let application run indefinitely*/	
			/*
			// If nothing is queued, put the default application in
			if (context == defaultApp) {
				changed = false;
			}
			else {
				context = defaultApp;
			}
			*/
			
			changed=false; //RVCA: if nothing is queued leave the application in place indefinitely.
		}
		finally {
			// Will enter this conditional if the application has changed
			if (changed) {
				
				// Pause everything!
				stopTimer();
				// unsubscrbe the old topic, and listen to the new topic
				unsubscribe(previousTopic);
				
				// subscribe the topic, if it has a custom connector, register
				// the servlet to handle received messages
				boolean isDefault = context.getConnectionInfo().getConnectorClass().equals(
				        DefaultConnector.class.getName());
				subscribe(context.getConnectionInfo().getTopic(), !isDefault ? appURL + "/CoffeeShop/coffeeShop.do"
				        : null);
				
				if (!isDefault) {
					try {
						// If it has a custom connector, we have to notify the
						// servlet and the OSGiBroker that we wil be having
						// messages on this topic. So send a dummy message to
						// wake everything up.
						Map<String, String> map = new HashMap<String, String>();
						map.put("message", "wakeup");
						publishEvent(map);
					}
					catch (OSGiBrokerException e) {
						log.error("Could not send wakeup message to topic: " + context.getConnectionInfo().getTopic(),
						        e);
					}
				}
				
				// reset the timer
				startTimer();
				
				// make sure the application knows something has changed
				contextChange = true;
				
				log.info("Context switch occured to application: " + context.getApplicationName());
			}
		}
	}
	
	/**
	 * Determines if the application in context has changed since the last time
	 * this method has been called.
	 * 
	 * @return true if the application in context has changed, else returns
	 *         false
	 */
	public synchronized boolean isContextChange() {
		if (contextChange) {
			contextChange = false;
			return true;
		}
		else {
			return false;
		}
	}
	
	/**
	 * Put the selected application in the queue to run
	 * 
	 * @param applicationNum
	 *            - the number (indexed from zero) of the application to run.
	 */
	public void queueApplication(int applicationNum) {
		try {
			Application a = applications.get(applicationNum);
			queue.enqueue(a);
			log.info("Application queued: " + a.getApplicationName());
		}
		catch (IndexOutOfBoundsException e) {
			log.info("Someone tried to select an application that has not been configured. ID: " + applicationNum);
		}
		
	}
	
	/**
	 * Getter for the queue of applications to run.
	 * 
	 * @return an ordered List object containing the instances of applications
	 *         in the queue.
	 */
	public List<Application> getQueuedApplications() {
		return queue.getOrderedList();
	}
	
	/**
	 * Get the list of loaded applications that are configured to run.
	 * 
	 * @return a list of loaded applications
	 */
	public List<Application> getApplications() {
		return applications;
	}
	
	/**
	 * Get the list of all applications available but not configured to run.
	 * 
	 * @return a List of applications available, but are not configured to run.
	 */
	public List<Application> getAllApplications() {
		return allApps;
	}
	
	/**
	 * Get the current running application
	 * 
	 * @return the currently running application
	 */
	public Application getCurrentApplication() {
		return context;
	}
	
	/**
	 * Get the default application
	 * 
	 * @return the configured default application
	 */
	public Application getDefaultApplication() {
		return defaultApp;
	}
	
	/**
	 * Set the default application
	 * 
	 * @param a
	 *            application to set as default
	 */
	public void setDefaultApplication(Application a) {
		this.defaultApp = a;
	}
	
	/**
	 * Get the message board for this coffee shop
	 * 
	 * @return the message board instance for this coffee shop
	 */
	public MessageBoard getMessageBoard() {
		return messageBoard;
	}
	
	/**
	 * Get the configuration properties of the system
	 * 
	 * @return configuration properties
	 */
	public Properties getProperties() {
		return prop;
	}
	
	/**
	 * Publishes a map of attributes to the currently running application's
	 * OSGiBroker topic.
	 * This should be used by any objects or connectors that need to easily
	 * publish to the running application
	 * 
	 * @param attributes
	 *            a map of string attributes to publish to the OSGiBroker
	 * @throws OSGiBrokerException
	 *             of publishing fails
	 */
	public void publishEvent(Map<String, String> attributes) throws OSGiBrokerException {		
		String topic = context.getConnectionInfo().getTopic();
		osgiClient.publisher().sendEvent(topic, attributes);
	}
	
	/**
	 * Saves the state of the configuration of the application.
	 * This method writes the state of the configuration to the application
	 * properties file,
	 * and saves the state of individual applications to their respective
	 * application XML files.
	 * 
	 * This method should ONLY be called on application shutdown, because
	 * writing to files on the classpath during execution time will cause the
	 * application to restart. This will cause logging exceptions and
	 * inconsistencies on the large display.
	 * 
	 * @param sc
	 *            servlet context of servlet that initiates the shutdown
	 * @throws URISyntaxException
	 *             if the URL of the file is malformed
	 * @throws IOException
	 *             if writing to files fails
	 */
	public void saveState(ServletContext sc) throws URISyntaxException, IOException {
		
		try {
			// Set up JAXB marshaller and information objects
			JAXBContext jc = JAXBContext.newInstance(Application.class.getPackage().getName());
			Marshaller m = jc.createMarshaller();
			ObjectFactory of = new ObjectFactory();
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			
			// Create a common list of all the appliactions to save, from all
			// the different categories
			List<Application> savingapps = new ArrayList<Application>();
			savingapps.addAll(allApps);
			savingapps.addAll(applications);
			savingapps.add(defaultApp);
			
			for (Application a : savingapps) {
				// Find the file.
				URL fileUrl = this.getClass().getClassLoader().getResource(
				        CONFIG_PATH + a.getApplicationShortName() + ".xml");
				try {
					// Marshall the application to XML, and write to disc
					m.marshal(of.createPluginApp(a), new FileOutputStream(new File(fileUrl.toURI())));
				}
				catch (NullPointerException e) {
					// If the file does not exist, it's a new applicaiton.
					// Create the file and write to it.
					String fileName = sc.getRealPath("/WEB-INF/classes" + CONFIG_PATH);
					File f = new File(fileName + "/" + a.getApplicationShortName() + ".xml");
					f.createNewFile();
					m.marshal(of.createPluginApp(a), new FileOutputStream(f));
					
				}
				catch (JAXBException e) {
					log.error("Failure saving application", e);
				}
			}
			
			// If all applications saved properly, save the properties file.
			URL propFileUrl = this.getClass().getClassLoader().getResource(CONFIG_PATH + CONFIG_FILE);
			OutputStream os = new FileOutputStream(new File(propFileUrl.toURI()));
			prop.store(os, null);
		}
		catch (JAXBException e) {
			log.error("Failure saving application", e);
		}
	}
	
	/*
	 * (non-JavaDoc)
	 * This code unsubscribes all open subscriptions and cancels timers.
	 * This cleanup is required to ensure a smooth boot up the next time the
	 * applicationis started
	 */
	public void shutdown() {
		log.info("Shutting down...");
		
		//Unsubscribe all instances.		
		contextTimer.cancel();
		unsubscribe(prop.getProperty("coffeeShopMenuTopicName"));
		unsubscribe(context.getConnectionInfo().getTopic());
		unsubscribe(prop.getProperty("smsDefaultTopic"));
		osgiBroker.removeClient(osgiClient.getClientId());
		messageBoard.unsubscribe();

		log.info("Shutdown Complete");
	}
	
	/*
	 * (non-JavaDoc)
	 * Restart the application by un-subscribing everything, resetting
	 * configuration, and re-subscribing and starting timers. This should be
	 * done
	 * when a new configuration is available.
	 */
	private void restart() {
		try {
			log.info("Resetting configuration...");
			shutdown();
			startup();
		}
		catch (Exception e) {
			throw new ConfigurationException(e);
		}
	}
	
	/**
	 * Sets new configuration flag. If flag is true, restarts the appliction
	 * with new configuration.
	 * 
	 * @param flag
	 *            true if a new configuration is available, false if the
	 *            configuration reset has been acknowledged.
	 */
	public synchronized void setNewConfiguration(boolean flag) {
		if (flag) {
			this.newConfiguration = true;
			restart();
		}
		else {
			this.newConfiguration = false;
		}
	}
	
	/**
	 * Gets new configuration flag
	 * 
	 * @return true if there is a new configuration, else return false.
	 */
	public boolean isNewConfiguration() {
		return newConfiguration;
	}
	
	/*
	 * (non-Javadoc)
	 * The timer task runs when the context switch timer has expired
	 * When the timer expires, this task checks to see if any new messages have
	 * been sent to the current topic since the last time it checked. If there
	 * are new events, the application continues to run happily. Otherwise, a
	 * context switch is performed, and a new application is loaded.
	 */
	private class ContextSwitchTask extends TimerTask {
		
		@Override
		public void run() {
			try {
				boolean isDefault = context.getConnectionInfo().getConnectorClass().equals(
				        DefaultConnector.class.getName());
				TopicEvent[] events = osgiClient.subscriber().getEvents(context.getConnectionInfo().getTopic(), 1);
				if (isDefault && events.length == 0) {
					doContextSwitch();
				}
				else if (!isDefault && !customAppEvent) {
					customAppEvent = false;
					doContextSwitch();
				}
				else if (!isDefault) {
					customAppEvent = false;
				}
			}
			catch (OSGiBrokerException e) {
				log.warn("Could not check for events during context switch.", e);
				doContextSwitch();
			}
		}
		
	}
	
	/**
	 * Notifies the tracker that a custom event was recieved. Custom events are
	 * events on a topic for an application with a custom connector.
	 * 
	 * This method is already called on reciept of an event in the CoffeeShop
	 * servlet
	 */
	public void notifyCustomEvent() {
		customAppEvent = true;
	}
}
