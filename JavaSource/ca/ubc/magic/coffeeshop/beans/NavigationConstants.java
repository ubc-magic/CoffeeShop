package ca.ubc.magic.coffeeshop.beans;

/*
 * (non-JavaDoc)
 * 
 * Author: Jay Wakefield
 * Version: 1.0
 * 
 * NavigationContants
 * 
 * This interface only exists to maintain a list of navigation parameters as used by JSF.  These constants can be returned from 
 * action methods, which then tell JSF what page to navigate to.  This is again defined in the faces-config file.
 */
public interface NavigationConstants {
	
	public static String NAV_HERE = "NAV_HERE";
	public static String NAV_CONFIG = "NAV_CONFIG";
	public static String NAV_VIEWEDIT = "NAV_VIEWEDIT";
}
