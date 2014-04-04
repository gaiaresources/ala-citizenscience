package au.com.gaiaresources.bdrs.controller;

/**
 * Created by aaron on 4/04/2014.
 */
public class BadWebParameterException extends Exception {

    public BadWebParameterException(String msg, Throwable e) {
        super(msg, e);
    }
}
