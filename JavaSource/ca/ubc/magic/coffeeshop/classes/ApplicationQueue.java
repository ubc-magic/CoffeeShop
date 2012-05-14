package ca.ubc.magic.coffeeshop.classes;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * ApplicationQueue
 * 
 * @author Jay Wakefield
 * @version 1.0
 * 
 *          This class is a simple queue implementation. It has been written to
 *          support the application queue of the CoffeeShop application.
 *          The queue acts similarly to a set, where if an object of the same
 *          type is already in the queue, it is not added again.
 * 
 * 
 * @param <E>
 *            The parameterized class to be used with this queue.
 */
public class ApplicationQueue<E> {
	
	private LinkedList<E> queueImpl;
	
	/**
	 * Default constructor
	 */
	public ApplicationQueue() {
		queueImpl = new LinkedList<E>();
	}
	
	/**
	 * Adds an object E to the end of the queue. If the queue already contains
	 * this object, it will be not be added again.
	 * 
	 * @param e
	 * @return true if the object was successfully added to the queue or already
	 *         exists in the queue, else returns false
	 */
	public boolean enqueue(E e) {
		if (queueImpl.contains(e)) {
			return true;
		}
		else {
			return queueImpl.add(e);
		}
	}
	
	/**
	 * Returns the element at the head of the queue, or throws
	 * NoSuchElementException if the queue is empty
	 * 
	 * @return object E from the head of the queue
	 * @throws NoSuchElementException
	 *             if the queue is empty
	 */
	public E dequeue() throws NoSuchElementException {
		return queueImpl.remove();
	}
	
	/**
	 * Returns list which represents the state of the queue.
	 * 
	 * @return a List object which is a shallow copy of the queue
	 *         implementation.
	 */
	public List<E> getOrderedList() {
		return new ArrayList<E>(queueImpl);
	}
}
