package ca.ubc.magic.coffeeshop.servlets;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import ca.ubc.magic.coffeeshop.jaxb.Application;
import ca.ubc.magic.osgibroker.workgroups.TopicEvent;

/**
 * ServletUtil
 * 
 * @author Jay Wakefield
 * @version 1.0
 * 
 *          This static class creates communication messages to send to the
 *          CoffeeShop GUI. It is used to format the messages appropriately, and
 *          build the communication protocol.
 */
public class ServletUtil {
	
	/**
	 * Creates appropriate HTTP headers for this communication protocol
	 * 
	 * @param resp
	 *            the response object to be sent for communication
	 */
	public static void createHeaders(HttpServletResponse resp) {
		// Set to expire far in the past.
		resp.setHeader("Expires", "Sat, 6 May 1995 12:00:00 GMT");
		// Set standard HTTP/1.1 no-cache headers.
		resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
		
		// Set IE extended HTTP/1.1 no-cache headers (use addHeader).
		resp.addHeader("Cache-Control", "post-check=0, pre-check=0");
		// Set standard HTTP/1.0 no-cache header.
		resp.setHeader("Pragma", "no-cache");
	}
	
	/**
	 * Creates configuration message for GUI. Includes all applications that are
	 * configured to run, plus the default application, and other information
	 * required by the protocol.
	 * 
	 * @param list
	 *            a list of application configured to run
	 * @param defaultApp
	 *            the configured default application
	 * @return the message to be sent to the GUI
	 */
	public static String createConfigurationXML(List<Application> list, Application defaultApp) {
		String ret = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<coffeeshop>\n";
		ret += "\t<mode>configuration</mode>\n";
		ret += "\t<numapps>" + list.size() + "</numapps>\n";
		ret += "\t<defaultURL>" + defaultApp.getConnectionInfo().getDisplayURL() + "</defaultURL>\n";
		ret += "\t<defaultType>" + defaultApp.getConnectionInfo().getApplicationType() + "</defaultType>\n";
		ret += "\t<defaultName>" + defaultApp.getApplicationName() + "</defaultName>\n";
		ret += "\t<applications>\n";
		for (Application a : list) {
			ret += "\t\t<application>\n";
			ret += "\t\t\t<name>" + a.getApplicationName() + "</name>\n";
			ret += "\t\t\t<img>" + a.getApplicationImageURL() + "</img>\n";
			ret += "\t\t</application>\n";
		}
		ret += "\t</applications>\n";
		ret += "</coffeeshop>";
		return ret;
	}
	
	/**
	 * Creates context message. It includes all information needed by the GUI
	 * when a new application is set to run.
	 * 
	 * @param a
	 *            the application involved in the context change
	 * @return the message to be sent to the GUI
	 */
	public static String createContextChangeXML(Application a) {
		String ret = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<coffeeshop>\n";
		ret += "\t<mode>context</mode>\n";
		ret += "\t<application>\n";
		ret += "\t\t<url>" + a.getConnectionInfo().getDisplayURL() + "</url>\n";
		ret += "\t\t<type>" + a.getConnectionInfo().getApplicationType() + "</type>\n";
		ret += "\t\t<name>" + a.getApplicationName() + "</name>\n";
		ret += "\t\t<img>" + a.getApplicationImageURL() + "</img>\n";
		ret += "\t\t<fullscreen>" + a.getConnectionInfo().getUseFullScreen() + "</fullscreen>\n";
		ret += "\t</application>\n";
		ret += "</coffeeshop>";
		return ret;
	}
	
	/**
	 * Creates a message that informs the GUI what applications are currently
	 * in the queue.
	 * 
	 * @param list
	 *            list of applications currently in the ready queue
	 * @return the message to be sent to the GUI
	 */
	public static String createQeueudAppsXML(List<Application> list) {
		String ret = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<coffeeshop>\n";
		if (list.size() > 0) {
			ret += "\t<mode>queued</mode>\n";
		}
		for (Application a : list) {
			ret += "\t<application>\n";
			ret += "\t\t<name>" + a.getApplicationName() + "</name>\n";
			ret += "\t\t<img>" + a.getApplicationImageURL() + "</img>\n";
			ret += "\t</application>\n";
		}
		ret += "</coffeeshop>";
		return ret;
	}
	
	/**
	 * Create a message that includes all messages sent to the message board
	 * since the last time the GUI asked.
	 * 
	 * @param list
	 *            of message board events from the OSGiBroker
	 * @return the message to be sent to the GUI
	 */
	public static String createMessageBoardXML(List<TopicEvent> list) {
		String ret = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<events>\n";
		for (TopicEvent e : list) {
			ret += "\t<event>\n";
			
			ret += "\t\t<timestamp>"
			        + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM).format(new Date())
			        + "</timestamp>\n";
			String[] attr = e.getNameArray();
			for (String s : attr) {
				ret += "\t\t<" + s + ">" + e.getAttribute(s) + "</" + s + ">\n";
			}
			ret += "\t</event>\n";
		}
		return ret + "</events>\n";
	}
}
