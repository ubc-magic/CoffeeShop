package ca.ubc.magic.coffeeshop.exceptions;

/**
 * ConfigurationException
 * 
 * @author Jay Wakefield
 * @version 1.0
 * 
 *          This exception should thrown whenever an incorrect coffee shop
 *          configuration file has caused the loading process to fail.
 *          In this case, this exception is caught by the container, and the
 *          appropriate error page is displayed to the user. Hence, this is a
 *          RuntimeException
 * 
 */
public class ConfigurationException extends RuntimeException {
	
	private static final long serialVersionUID = 1L;
	
	/* Upper exception */
	private Throwable exception;
	
	/**
	 * Default Constructor
	 */
	public ConfigurationException() {
		// create exception
	}
	
	/**
	 * Constructor which allows wrapping from higher level exception that was
	 * caught before throwing a ConfigurationException.
	 * 
	 * @param e
	 *            the exception caught from higher up the stack.
	 */
	public ConfigurationException(Throwable e) {
		exception = e;
	}
	
	@Override
	public String getMessage() {
		return "There was a problem configuring the application.  See administrator for details.";
	}
	
	@Override
	public Throwable getCause() {
		if (exception != null) {
			return exception;
		}
		else
			return super.getCause();
	}
}
