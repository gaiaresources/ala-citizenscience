package au.com.gaiaresources.bdrs.servlet;

import org.apache.log4j.Logger;



public class RequestContextHolder {
    @SuppressWarnings("unused")
    private static Logger log = Logger.getLogger(RequestContextHolder.class);
    private static ThreadLocal<RequestContext> context = new ThreadLocal<RequestContext>();
    
    public static RequestContext getContext() {
        if (context.get() == null) {
            context.set( new RequestContext());
        }
        return context.get();
    }
    
    public static void clear() {
        context.remove();
    }
    
    public static void set(RequestContext c) {
        context.set(c);
    }
}
