package ca.ubc.magic.coffeeshop.servlets;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import ca.ubc.magic.coffeeshop.classes.CoffeeShop;
import ca.ubc.magic.coffeeshop.classes.MessageBoard;
import ca.ubc.magic.coffeeshop.exceptions.ConfigurationException;
import ca.ubc.magic.osgibroker.OSGiBrokerException;
import ca.ubc.magic.osgibroker.workgroups.TopicEvent;

/**
 * MessageBoardServlet
 * 
 * @author Jay Wakefield
 * @version 1.0
 * 
 *          This servlet reponds with message board events. The GUI would poll
 *          this URL for any messages intended for the message board. The
 *          servlet responds with the messages available, or an empty response
 *          if there are no messages available.
 * 
 *          Polling for messages should be done with a GET request. If someone
 *          would like to send an event to the message board with this servlet,
 *          it can be done using a POST request, with the parameters "name" and
 *          "message".
 * 
 */
public class MessageBoardServlet extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
	private final Logger log = Logger.getLogger(MessageBoardServlet.class);
	private MessageBoard board;
	private CoffeeShop coffeeshop;
	@Override
	public void init() throws ServletException {
		super.init();
		
		try {
			// Subscribe to the message board, since this is the first access to
			// the servlet.
			this.coffeeshop = CoffeeShop.getInstance();
			this.board = coffeeshop.getMessageBoard();
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
	public void destroy() {
		// unsubscribe from the messageboard
		super.destroy();
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// Check for new messages
		List<TopicEvent> list = board.getNewMessages();
		// Send message to GUI
		String response = ServletUtil.createMessageBoardXML(list);
		ServletUtil.createHeaders(resp);
		resp.getWriter().write(response);
		log.debug("SENT:\n" + response);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// Send
		HashMap<String, String> params = new HashMap<String, String>();
		Enumeration<String> names = req.getParameterNames();
		
		// Write the "name" and "message" parameters to the OSGiBroker
		while (names.hasMoreElements()) {
			String name = names.nextElement();
			params.put(name, req.getParameter(name));
		}
		
		String response = "Result: ";
		try {
			board.pushEvent(params);
			response += "Success";
		}
		catch (OSGiBrokerException e) {
			response += "Failure: " + e.getMessage();
		}
		
		resp.getWriter().write(response);
		log.debug("SENT:\n" + response);
	}
	
}
