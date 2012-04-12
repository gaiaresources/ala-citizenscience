package au.com.gaiaresources.bdrs.service.taxonomy;

/**
 * Error initialising TaxonLib
 *
 */
public class BdrsTaxonLibException extends Exception {

	/**
	 * Default serial version ID
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Create a new exception.
	 * @param message Exception message.
	 */
	public BdrsTaxonLibException(String message) {
		super(message);
	}
	
	/**
	 * Create a new exception.
	 * @param message Exception message.
	 * @param e Inner throwable.
	 */
	public BdrsTaxonLibException(String message, Throwable e) {
		super(message, e);
	}
}