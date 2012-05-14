package ca.ubc.magic.coffeeshop.servlets;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import ca.ubc.magic.coffeeshop.classes.CoffeeShop;
import ca.ubc.magic.coffeeshop.exceptions.ConfigurationException;

/**
 * CommunicationServlet
 * 
 * @author Jay Wakefield
 * @version 1.0
 * 
 *          This class is a servlet that is used for communication between the
 *          server side code and the GUI for the coffee shop application.
 * 
 *          The GUI polls on this URL for information regarding the various
 *          components of its interface.
 * 
 *          Three types of messages can be sent:
 * 
 *          1) Configuration response = a message containing information about
 *          what applications are configured and their display, runtime, and
 *          descriptions. This message occurs when the user interface asks for
 *          the configuration message, or when the server side logic determines
 *          the configuration has changed since the last time the user interface
 *          asked for information. If the GUI does not ask for configuration but
 *          a new configuration is available, it overrides the request and sends
 *          the configuration message instead.
 * 
 *          2) Queued response = a message containing information about what
 *          applications are currently queued to run. This message also includes
 *          application information, so that it can be appropriately displayed
 *          on the user interface.
 * 
 *          3) Context message = a message containing information about the
 *          application that should be currently running in the main window of
 *          the user interface. The server side logic keeps track of what
 *          application should be running, and keeps the user interface up to
 *          date.
 * 
 *          All messages are polled on the same URL, but are differentiated via
 *          request paramaters that are sent with the URL. The parameter "type"
 *          may have a value of "configuration" or "queued". Otherwise, the
 *          servlet will respond with a context message.
 * 
 */
public class CommunicationServlet extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
	private final Logger log = Logger.getLogger(CommunicationServlet.class);
	private CoffeeShop coffeeShop;
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		String xmlResponse = null;
		// Get configuration and create config message
		if (coffeeShop == null || "configuration".equals(req.getParameter("type"))) {
			try {
				coffeeShop = CoffeeShop.getInstance();
			}
			catch (FileNotFoundException e) {
				log.fatal("Problem trying to find configuration file.", e);
				throw new ConfigurationException(e);
			}
			catch (IOException e) {
				log.fatal("Problem reading configuration file.", e);
				throw new ConfigurationException(e);
			}
			
			xmlResponse = ServletUtil.createConfigurationXML(coffeeShop.getApplications(), coffeeShop
			        .getDefaultApplication());
			log.info("Sending new configuration information");
		}
		else if (coffeeShop.isNewConfiguration()) {
			// There is a new configuration available, so send config message
			xmlResponse = ServletUtil.createConfigurationXML(coffeeShop.getApplications(), coffeeShop
			        .getDefaultApplication());
			coffeeShop.setNewConfiguration(false);
			log.info("Sending new configuration information");
		}
		else if ("queued".equals(req.getParameter("type"))) {
			// Send queue message
			xmlResponse = ServletUtil.createQeueudAppsXML(coffeeShop.getQueuedApplications());
			log.info("Sending queued applications");
		}
		else if (coffeeShop.isContextChange()) {
			// Send context change message
			xmlResponse = ServletUtil.createContextChangeXML(coffeeShop.getCurrentApplication());
			log.info("Sending new context change information");
		}
		else {
			// Send blank message - there is nothing new available
			xmlResponse = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<coffeeshop>\n</coffeeshop>";
		}
		
		// Remember to set the correct headers
		ServletUtil.createHeaders(resp);
		resp.getWriter().write(xmlResponse);
		log.debug("SENT: \n" + xmlResponse);
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doGet(req, resp);
	}
}
