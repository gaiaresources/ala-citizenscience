package au.com.gaiaresources.bdrs.service.bulkdata;

/**
 * An exception that is thrown by the bulk upload if there has been an issue resolving the provided value (such as
 * scientific name) to a BDRS value (such as indicator species). This exception is geared towards having multiple
 * values resolved for teh given input as opposed to data that is {@link MissingDataException missing}.
 */
public class AmbiguousDataException extends Exception {
    private static final long serialVersionUID = 1L;

    public AmbiguousDataException() {
        super();
    }

    public AmbiguousDataException(String arg0, Throwable arg1) {
        super(arg0, arg1);
    }

    public AmbiguousDataException(String arg0) {
        super(arg0);
    }

    public AmbiguousDataException(Throwable arg0) {
        super(arg0);
    }
}
