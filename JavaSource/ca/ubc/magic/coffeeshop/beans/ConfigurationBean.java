package ca.ubc.magic.coffeeshop.beans;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.faces.component.html.HtmlDataTable;
import javax.faces.context.FacesContext;

import org.apache.log4j.Logger;
import org.richfaces.event.DropEvent;

import ca.ubc.magic.coffeeshop.classes.CoffeeShop;
import ca.ubc.magic.coffeeshop.exceptions.ConfigurationException;
import ca.ubc.magic.coffeeshop.jaxb.Application;
import ca.ubc.magic.coffeeshop.jaxb.Connection;
import ca.ubc.magic.coffeeshop.jaxb.ObjectFactory;
import ca.ubc.magic.coffeeshop.jaxb.Parameters;

/*
 * (non-JavaDoc)
 * 
 * Author: Jay Wakefield
 * Version: 1.0
 * 
 * ConfigurationBean
 * 
 * This class is implemented as a JSF server side bean.  Its use and name is defined in WEB-INF/faces-config.xml
 * 
 * The purpose of this bean is to perform actions and maintain state for the configuration pages of the application, 
 * including the main configuration page, and the view/edit/new application pages.  It contains variables that represent
 * all the form inputs on these pages, as well as keeps references to the loaded applications and properties of the 
 * Coffee Shop application.
 * 
 * It is important to note that all of the references to applications and properties in this file are the same references
 * throughout the application.  This is done to maintain state on all parts of the app, whether is is the large screen,
 * the selection page, or the configuration page.  Therefore, changes to applications and properties done in this bean
 * are reflected in the large screen and selection pages as well.
 */
public class ConfigurationBean implements NavigationConstants {
	
	private static Logger log = Logger.getLogger(ConfigurationBean.class);
	
	// CoffeeShop instance, applications instances and properties
	// All of these variables are shallow copies of those from
	// the CoffeeShop instance, in order to maintain state on all part of the
	// application
	private CoffeeShop cs;
	private Properties prop;
	private List<Application> allApps;
	private List<Application> configuredApps;
	private Application defaultApp;
	
	// Data Table references
	private HtmlDataTable configuredTable;
	private HtmlDataTable allAppTable;
	
	// View/Edit/New Page form variables
	private Application editingApp;
	private String appName;
	private String appShortName;
	private String appImageUrl;
	private int appIdleTime;
	private String appDescription;
	private String appInstructions;
	private String appType;
	private boolean appFullscreen;
	private String appTopicName;
	private String appDisplayUrl;
	private String appConnectorClass;
	
	// Page Flag, since View/Edit and Create New pages share the same page.
	// This flag is used for rendering appropriate content.
	private boolean createNew;
	
	// Used for dynamic css loading, depending on browser
	private String cssFile;
	
	/*
	 * (non-JavaDoc)
	 * 
	 * Constructor. This is called by JSF Servlet when a new bean needs
	 * to be created for current scope, as defined in the faces-config file.
	 */
	public ConfigurationBean() {
		initBean();
	}
	
	/*
	 * (non-JavaDoc)
	 * 
	 * Initialization. Obtain references to coffee shop assets.
	 */
	private void initBean() {
		try {
			cs = CoffeeShop.getInstance();
			prop = cs.getProperties();
			configuredApps = cs.getApplications();
			defaultApp = cs.getDefaultApplication();
			allApps = cs.getAllApplications();
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
			setCssFile("../styles/style-chrome.css");
		}
		else if (browser.contains("Firefox")) {
			setCssFile("../styles/style-firefox.css");
		}
		else {
			setCssFile("../styles/style-ie.css");
		}
	}
	
	/*
	 * (non-JavaDoc)
	 * 
	 * This method changes state of applications being dropped into the
	 * configured table of the configuration page. It removes the appliation
	 * from the "all available" applications, and
	 * inserts it into the "configured to run" list.
	 */
	public void doProcessConfigDrop(DropEvent arg0) {
		Application a = (Application) arg0.getDragValue();
		if (allApps.remove(a)) {
			configuredApps.add(a);
		}
	}
	
	/*
	 * (non-JavaDoc)
	 * 
	 * Similar to doProcessConfigDrop, this method removes an application from
	 * the confgured list, and adds it to the all
	 * available list when dragged from the appropriate tbable.
	 */
	public void doProcessAllDrop(DropEvent arg0) {
		Application a = (Application) arg0.getDragValue();
		if (configuredApps.remove(a)) {
			allApps.add(a);
		}
	}
	
	/*
	 * (non-JavaDoc)
	 * 
	 * Changes default application when dragged to default on the configuration
	 * page. Similar to processAllDrop() and processConfigDrop()
	 */
	public void doProcessDefaultDrop(DropEvent arg0) {
		Application a = (Application) arg0.getDragValue();
		configuredApps.add(defaultApp);
		configuredApps.remove(a);
		defaultApp = a;
	}
	
	/*
	 * (non-JavaDoc)
	 * 
	 * This method is invoked when save has been clicked on the main
	 * configuration page.
	 * 
	 * Its main purpose is to save the state of all the applications in the
	 * properties list.
	 * 
	 * This means adding new applicaitons, re-writing the list of applications,
	 * configured application, and the default application.
	 * These properties values are saved in memory, and is only written to file
	 * when the applicaiton has been safely shut down.
	 */
	public String doSaveAction() {
		String appNames = "";
		Iterator<Application> it = configuredApps.iterator();
		
		// Build the list of configured applications
		while (it.hasNext()) {
			Application a = it.next();
			appNames += a.getApplicationShortName();
			if (it.hasNext()) {
				appNames += ",";
			}
		}
		// Add default application to the list of configured
		appNames += "," + defaultApp.getApplicationShortName();
		
		// Write configured applications to the properties file
		// This change is propagated throughout the application
		prop.put("applicationsToRun", appNames);
		
		// Add the remaining applications to the list
		for (Application a : allApps) {
			appNames += "," + a.getApplicationShortName();
		}
		
		// Save default app and all apps
		prop.put("allApplications", appNames);
		prop.put("defaultApplication", defaultApp.getApplicationShortName());
		
		// Tell the front end that there is a new configuration to load, and set
		// the new default app
		cs.setNewConfiguration(true);
		cs.setDefaultApplication(defaultApp);
		
		// Navigate to the same page.
		return NAV_HERE;
	}
	
	/*
	 * (non-JavaDoc)
	 * 
	 * Navigate to the view/edit page for the selected application in the
	 * configured list
	 */
	public String doViewEditConfiguredAction() {
		Application a = configuredApps.get(configuredTable.getRowIndex());
		loadAppForView(a);
		return NAV_VIEWEDIT;
	}
	
	/*
	 * (non-JavaDoc)
	 * 
	 * Navigate to the view/edit page for the selected application in the all
	 * apps list
	 */
	public String doViewEditAllAction() {
		Application a = allApps.get(allAppTable.getRowIndex());
		loadAppForView(a);
		return NAV_VIEWEDIT;
	}
	
	/*
	 * (non-JavaDoc)
	 * 
	 * Loads the selected application parameters into the view/edit page form
	 */
	private void loadAppForView(Application a) {
		appConnectorClass = a.getConnectionInfo().getConnectorClass();
		appDescription = a.getApplicationDescription();
		appDisplayUrl = a.getConnectionInfo().getDisplayURL();
		appFullscreen = a.getConnectionInfo().getUseFullScreen().equals("true") ? true : false;
		appIdleTime = a.getMinumumIdleTime();
		appImageUrl = a.getApplicationImageURL();
		appInstructions = a.getInteractionInstructions();
		appName = a.getApplicationName();
		appShortName = a.getApplicationShortName();
		appTopicName = a.getConnectionInfo().getTopic();
		appType = a.getConnectionInfo().getApplicationType();
		editingApp = a;
		createNew = false;
	}
	
	/*
	 * (non-JavaDoc)
	 * 
	 * This method is invoked when the save button is clicked on the view/edit
	 * page, for the given application.
	 * 
	 * This saves the state of the configured application throughout the coffee
	 * shop app.
	 */
	public String doSaveViewEditAction() {
		
		// Put all information back into the application object.
		Application a = editingApp;
		editingApp = null;
		a.setApplicationDescription(appDescription);
		a.setApplicationImageURL(appImageUrl);
		a.setApplicationName(appName);
		a.setApplicationShortName(appShortName);
		a.setInteractionInstructions(appInstructions);
		a.setMinumumIdleTime(appIdleTime);
		Connection c = a.getConnectionInfo();
		c.setApplicationType(appType);
		c.setConnectorClass(appConnectorClass);
		c.setDisplayURL(appDisplayUrl);
		c.setTopic(appTopicName);
		c.setUseFullScreen(appFullscreen ? "true" : "false");
		Parameters p = a.getParameters();
		a.setConnectionInfo(c);
		a.setParameters(p);
		
		// If the app is confugured, notify the large screen there is a new
		// configuration
		if (configuredApps.contains(a)) {
			cs.setNewConfiguration(true);
		}
		
		// Navigate back to the main configuration page.
		return NAV_CONFIG;
	}
	
	/*
	 * (non-JavaDoc)
	 * 
	 * This method is invoked when the cancel button is clicked in the view/edit
	 * page.
	 * 
	 * No changes are saved when this is invoked.
	 */
	public String doCancelAction() {
		return NAV_CONFIG;
	}
	
	/*
	 * (non-JavaDoc)
	 * 
	 * Moves the selected application up in the order of the list of configured
	 * applications.
	 */
	public void doMoveUpAction() {
		int index = configuredTable.getRowIndex();
		Application a = configuredApps.remove(index);
		configuredApps.add(index - 1, a);
	}
	
	/*
	 * (non-JavaDoc)
	 * 
	 * Moves the selected application down in the order of the list of
	 * configured applications
	 */
	public void doMoveDownAction() {
		int index = configuredTable.getRowIndex();
		Application a = configuredApps.remove(index);
		configuredApps.add(index + 1, a);
	}
	
	/*
	 * (non-JavaDoc)
	 * 
	 * This method is invoked when the user wishes to create a new application.
	 * 
	 * Sets up default values for all form fields.
	 */
	public String doNewAppAction() {
		appConnectorClass = "ca.ubc.magic.coffeeshop.connectors.DefaultConnector";
		appDescription = "";
		appDisplayUrl = "";
		appFullscreen = false;
		appIdleTime = 0;
		appImageUrl = "";
		appInstructions = "";
		appName = "";
		appShortName = "";
		appTopicName = "";
		appType = "web";
		createNew = true;
		return NAV_VIEWEDIT;
	}
	
	/*
	 * (non-JavaDoc)
	 * 
	 * This method is invokes when a new application is to be created. It takes
	 * all form fields from the new application page and
	 * constructs a JAXB Application object which is stored in memory until the
	 * coffee shop is shut down.
	 * 
	 * It is important that the new application object that is constructed come
	 * from the JAXB library, as is must be compatible to save to XML at the
	 * appropriate time. This is of course done through JAXB.
	 */
	public String doCreateApplicationAction() {
		
		// Create JAXB objects.
		ObjectFactory of = new ObjectFactory();
		Application a = of.createApplication();
		a.setApplicationDescription(appDescription);
		a.setApplicationImageURL(appImageUrl);
		a.setApplicationName(appName);
		a.setApplicationShortName(appShortName);
		a.setInteractionInstructions(appInstructions);
		a.setMinumumIdleTime(appIdleTime);
		Connection c = of.createConnection();
		c.setApplicationType(appType);
		c.setConnectorClass(appConnectorClass);
		c.setDisplayURL(appDisplayUrl);
		c.setTopic(appTopicName);
		c.setUseFullScreen(appFullscreen ? "true" : "false");
		Parameters p = of.createParameters();
		a.setConnectionInfo(c);
		a.setParameters(p);
		
		// Add to the list of applications.
		allApps.add(a);
		
		return NAV_CONFIG;
	}
	
	/*
	 * (non-JavaDoc)
	 * 
	 * The remaining methods in this bean are the appropriate getters and
	 * setters for form fields, and other member variables needed elsewhere.
	 */

	public void setAllApps(List<Application> allApps) {
		this.allApps = allApps;
	}
	
	public List<Application> getAllApps() {
		return allApps;
	}
	
	public List<Application> getConfiguredApps() {
		return configuredApps;
	}
	
	public void setConfiguredApps(List<Application> configuredApps) {
		this.configuredApps = configuredApps;
	}
	
	public void setConfiguredTable(HtmlDataTable configuredTable) {
		this.configuredTable = configuredTable;
	}
	
	public HtmlDataTable getConfiguredTable() {
		return configuredTable;
	}
	
	public HtmlDataTable getAllAppTable() {
		return allAppTable;
	}
	
	public void setAllAppTable(HtmlDataTable allAppTable) {
		this.allAppTable = allAppTable;
	}
	
	public String getAppName() {
		return appName;
	}
	
	public void setAppName(String appName) {
		this.appName = appName;
	}
	
	public String getAppShortName() {
		return appShortName;
	}
	
	public void setAppShortName(String appShortName) {
		this.appShortName = appShortName;
	}
	
	public String getAppImageUrl() {
		return appImageUrl;
	}
	
	public void setAppImageUrl(String appImageUrl) {
		this.appImageUrl = appImageUrl;
	}
	
	public int getAppIdleTime() {
		return appIdleTime;
	}
	
	public void setAppIdleTime(int appIdleTime) {
		this.appIdleTime = appIdleTime;
	}
	
	public String getAppDescription() {
		return appDescription;
	}
	
	public void setAppDescription(String appDescription) {
		this.appDescription = appDescription;
	}
	
	public String getAppInstructions() {
		return appInstructions;
	}
	
	public void setAppInstructions(String appInstructions) {
		this.appInstructions = appInstructions;
	}
	
	public String getAppType() {
		return appType;
	}
	
	public void setAppType(String appType) {
		this.appType = appType;
	}
	
	public boolean isAppFullscreen() {
		return appFullscreen;
	}
	
	public void setAppFullscreen(boolean appFullscreen) {
		this.appFullscreen = appFullscreen;
	}
	
	public String getAppTopicName() {
		return appTopicName;
	}
	
	public void setAppTopicName(String appTopicName) {
		this.appTopicName = appTopicName;
	}
	
	public String getAppDisplayUrl() {
		return appDisplayUrl;
	}
	
	public void setAppDisplayUrl(String appDisplayUrl) {
		this.appDisplayUrl = appDisplayUrl;
	}
	
	public String getAppConnectorClass() {
		return appConnectorClass;
	}
	
	public void setAppConnectorClass(String appConnectorClass) {
		this.appConnectorClass = appConnectorClass;
	}
	
	public void setCreateNew(boolean createNew) {
		this.createNew = createNew;
	}
	
	public boolean isCreateNew() {
		return createNew;
	}
	
	public void setDefaultApp(Application defaultApp) {
		this.defaultApp = defaultApp;
	}
	
	public Application getDefaultApp() {
		return defaultApp;
	}
	
	public void setCssFile(String cssFile) {
		this.cssFile = cssFile;
	}
	
	public String getCssFile() {
		return cssFile;
	}
	
}
