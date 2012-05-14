package ca.ubc.magic.coffeeshop.classes;

import java.util.Comparator;

import ca.ubc.magic.coffeeshop.jaxb.Parameter;

/**
 * ParameterComparator
 * 
 * @author Jay Wakefield
 * @version 1.0
 * 
 *          This class has been created to sort all parameters for an
 *          application based on the order as defined in the application XML
 *          file. Each parameter must list an order in which they should be
 *          listed. This sorts them accordingly.
 */
public class ParameterComparator implements Comparator<Parameter> {
	
	@Override
	public int compare(Parameter o1, Parameter o2) {
		if (o1.getOrder() == o2.getOrder()) {
			return 0;
		}
		else if (o1.getOrder() < o2.getOrder()) {
			return -1;
		}
		else {
			return 1;
		}
	}
}
