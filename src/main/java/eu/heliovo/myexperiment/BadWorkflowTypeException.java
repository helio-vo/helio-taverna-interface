package eu.heliovo.myexperiment;

/**
 * Exception thrown to indicate that an attempt to retrieve and launch a
 * non-supported workflow type was made. Taverna Server does not support
 * SCUFLv1.
 * 
 * @author Donal Fellows
 */
public class BadWorkflowTypeException extends Exception {
	BadWorkflowTypeException(String message) {
		super(message);
	}
}