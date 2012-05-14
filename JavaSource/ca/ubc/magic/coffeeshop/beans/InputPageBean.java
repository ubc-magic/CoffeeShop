package ca.ubc.magic.coffeeshop.beans;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIData;
import javax.faces.component.html.HtmlPanelGrid;
import javax.faces.context.FacesContext;

import org.apache.log4j.Logger;

import ca.ubc.magic.coffeeshop.classes.CoffeeShop;
import ca.ubc.magic.coffeeshop.exceptions.ConfigurationException;
import ca.ubc.magic.coffeeshop.jaxb.Application;
import ca.ubc.magic.osgibroker.OSGiBrokerException;

/*
 * (non-JavaDoc)
 * 
 * Author: Jay Wakefield
 * Version: 1.0
 * 
 * InputPageBean
 * 
 * This class is implemented as a JSF server side bean.  Its use and name is defined in WEB-INF/faces-config.xml
 * 
 * The purpose of this bean is to perform actions and maintain state for the input page of the application.  It contains variables that represent
 * all the form inputs on these pages, as well as keeps references to the loaded applications and properties of the 
 * Coffee Shop application.
 * 
 * This bean is used to control and dynamically update the input page to show application information to the user.  
 * It also contains references to the applications, to that the page is automatically updated when the configuration changes.
 */
public class InputPageBean {
	
	private final Logger log = Logger.getLogger(InputPageBean.class);
	
	// CoffeeShop instance
	private CoffeeShop coffeeshop;
	
	// UI objects. Note that in V1.0 the paramTable is not used. This should be
	// extended at some point.
	private HtmlPanelGrid paramTable;
	private UIData dataTable;
	
	// Application whose contents is displayed in the main part of the page
	private Application applicationVisible;
	
	// Form fields for message board.
	private String messageName;
	private String message;
	private String messageStatus = "";
	private boolean messageSuccess = true;
	
	// Name of the currently running application.
	private String currentAppName = "";
	
	// Flag used when switching context of applications
	private boolean viewMenuSelect;
	
	// CSS file to load
	private String cssFile;
	
	/*
	 * (non-JavaDoc)
	 * 
	 * Constructor. This is called by JSF Servlet when a new bean needs
	 * to be created for current scope, as defined in the faces-config file.
	 */
	public InputPageBean() {
		initBean();
	}
	
	/*
	 * (non-JavaDoc)
	 * 
	 * Initialization. Establishes references to coffeeshop assets, and sets up
	 * page.
	 */
	public void initBean() {
		try {
			coffeeshop = CoffeeShop.getInstance();
			applicationVisible = coffeeshop.getCurrentApplication();
			clearMessageBoard();
			browserDetect();
		}
		catch (FileNotFoundException e) {
			log.error("Failed to load bean.  Error finding files during configuration.", e);
			throw new ConfigurationException();
		}
		catch (IOException e) {
			log.error("Failed to load bean.  Error reading files during configuration.", e);
			throw new ConfigurationException();
		}
	}
	
	/*
	 * (non-JavaDoc)
	 * 
	 * Detects user agent, and loads keeps variable for appropriate css file to
	 * be loaded on page.
	 */
	private void browserDetect() {
		FacesContext fc = FacesContext.getCurrentInstance();
		String browser = fc.getExternalContext().getRequestHeaderMap().get("User-Agent");
		if (browser.contains("Chrome")) {
			cssFile = "../styles/style-chrome.css";
		}
		else if (browser.contains("Firefox")) {
			cssFile = "../styles/style-firefox.css";
		}
		else {
			cssFile = "../styles/style-ie.css";
		}
	}
	
	/*
	 * (non-JavaDoc)
	 * 
	 * Clears message board form fields.
	 */
	private void clearMessageBoard() {
		messageName = "";
		message = "";
	}
	
	/*
	 * (non-JavaDoc)
	 * 
	 * Establishes which application has been selected to show in the main part
	 * of the page. This is invoked when the application image has been
	 * clicked.
	 */
	public void doShowApplication() {
		setApplicationVisible(coffeeshop.getApplications().get(dataTable.getRowIndex()));
		viewMenuSelect = true;
	}
	
	/*
	 * (non-JavaDoc)
	 * 
	 * Cancels viewing an application to queue. This is clicked instead of
	 * queueing the application for use.
	 */
	public void doCancelAction() {
		viewMenuSelect = false;
	}
	
	/*
	 * (non-JavaDoc)
	 * 
	 * When clicked, queues the application for use in the large display.
	 */
	public void doQueueApplication() {
		coffeeshop.queueApplication(coffeeshop.getApplications().indexOf(applicationVisible));
		viewMenuSelect = false;
	}
	
	/*
	 * (non-JavaDoc)
	 * 
	 * Submits a message to be displayed on the message board. This is invoked
	 * when user submits their message.
	 */
	public void doSubmitMessage() {
		
		// Validate the message, output error if incorrect.
		if (message.equals("") || message == null) {
			messageStatus = "Please enter a message!";
			messageSuccess = false;
			return;
		}
		
		// Construct message for OSGiBroker
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("name", (messageName.equals("") || messageName == null) ? "Anonymous" : messageName);
		map.put("message", message);
		
		try {
			// Push the message to the OSGiBroker, via the MessageBoard object
			coffeeshop.getMessageBoard().pushEvent(map);
			messageStatus = "Message successfully sent at "
			        + DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date()) + ".";
			messageSuccess = true;
			// Success, so clear message board and show success message.
			clearMessageBoard();
		}
		catch (OSGiBrokerException e) {
			// Something bad happened, so tell the user it didn't work.
			messageStatus = "Message unsuccessfully sent at "
			        + DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date()) + ". Please try again later.";
			messageSuccess = false;
		}
	}
	
	/*
	 * (non-JavaDoc)
	 * 
	 * The remaining methods in this bean are the appropriate getters and
	 * setters for form fields, and other member variables needed elsewhere.
	 */

	public List<Application> getApplicationList() {
		return coffeeshop.getApplications();
	}
	
	public Application getCurrentApplication() {
		Application currentApp = coffeeshop.getCurrentApplication();
		if (!currentApp.getApplicationName().equals(currentAppName)) {
			currentAppName = currentApp.getApplicationName();
		}
		return currentApp;
	}
	
	public boolean getPollEnabled() {
		return !viewMenuSelect;
	}
	
	public void setDataTable(UIData dataTable) {
		this.dataTable = dataTable;
	}
	
	public UIData getDataTable() {
		return dataTable;
	}
	
	public void setApplicationVisible(Application applicationVisible) {
		this.applicationVisible = applicationVisible;
	}
	
	public Application getApplicationVisible() {
		return applicationVisible;
	}
	
	public void setViewMenuSelect(boolean viewMenuSelect) {
		this.viewMenuSelect = viewMenuSelect;
	}
	
	public boolean isViewMenuSelect() {
		return viewMenuSelect;
	}
	
	public void setMessageName(String messageName) {
		this.messageName = messageName;
	}
	
	public String getMessageName() {
		return messageName;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}
	
	public String getMessage() {
		return message;
	}
	
	public void setMessageStatus(String messageStatus) {
		this.messageStatus = messageStatus;
	}
	
	public String getMessageStatus() {
		return messageStatus;
	}
	
	public void setMessageSuccess(boolean messageSuccess) {
		this.messageSuccess = messageSuccess;
	}
	
	public boolean isMessageSuccess() {
		return messageSuccess;
	}
	
	public HtmlPanelGrid getParamTable() {
		return paramTable;
	}
	
	public void setParamTable(HtmlPanelGrid paramTable) {
		this.paramTable = paramTable;
	}
	
	public void setCssFile(String cssFile) {
		this.cssFile = cssFile;
	}
	
	public String getCssFile() {
		return cssFile;
	}
	
}
