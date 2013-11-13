package au.com.gaiaresources.bdrs.db.impl;

import java.util.*;

import org.apache.log4j.Logger;

/**
 * A predicate is the part of the HQL sentence following the " from ClassName "
 * sentence Example usage: HqlQuery query = new HqlQuery( "from MyClass obj" );
 * query.and( Predicate.eq( "property", value ) .and(
 * Predicate.ilike("property", value+"%" ) .and( Predicate.eq("prop1", value
 * ).or( Predicate.eq( "prop2", value )));
 * 
 * @author Davide Alberto Molin (davide.molin@gmail.com)
 * 
 *         adapted from
 *         http://developme.wordpress.com/2010/03/03/creating-a-criteria
 *         -like-api-to-build-hql-queries/
 */
public class Predicate {
    private static Logger log = Logger.getLogger(Predicate.class);
    private String predicate;
    private List<Object> paramValues;
    private Map<String, Object> namedArgs;

    public Predicate() {
        this.predicate = "";
        paramValues = new ArrayList<Object>();
        namedArgs = new HashMap<String, Object>();
    }

    public Predicate(String predicate) {
        this();
        if (predicate != null) {
            this.predicate = predicate;
        }
    }

    public Predicate(String predicate, Object... paramValues) {
        this();
        if (predicate != null) {
            this.predicate = predicate;
        }
        this.paramValues.addAll(Arrays.asList(paramValues));
    }

    public Predicate(String predicate, Object value, String name) {
        this();
        if (predicate != null) {
            this.predicate = predicate;
        }
        this.namedArgs.put(name, value);
    }

    private void copyArgs(Predicate pg) {
        this.paramValues.addAll(Arrays.asList(pg.getParametersValue()));
        this.namedArgs.putAll(pg.getNamedArgs());
    }

    public Predicate and(Predicate pg) {
        if (predicate.length() == 0) {
            this.predicate = " WHERE " + enclose(pg.getQueryString());
        } else {
            this.predicate += " AND " + enclose(pg.getQueryString());
        }
        copyArgs(pg);
        return this;
    }

    public Predicate or(Predicate pg) {
        if (predicate.length() == 0) {
            this.predicate = " WHERE " + enclose(pg.getQueryString());
        } else {
            this.predicate += " OR " + enclose(pg.getQueryString());
        }
        copyArgs(pg);
        return this;
    }

    private String enclose(String src) {
        return "(" + src + ")";
    }

    public static Predicate enclose(Predicate pg) {
        return new Predicate("(" + pg.getQueryString() + ")",
                pg.getParametersValue());
    }

    /**
     * Create a new predicate with any expression.
     * e.g. pred.expr("foo.number >= :testNumber", 2, "testNumber");
     *
     * @param expression hql expression
     * @param value value of the named argument
     * @param name name of the named argument
     * @return Newly created predicate
     */
    public static Predicate expr(String expression, Object value, String name) {
        return new Predicate(expression, value, name);
    }

    public static Predicate eq(String expression, Object value, String name) {
        return new Predicate(expression + " = :"+name, value, name);
    }

    public static Predicate eq(String expression, Object value) {
        return new Predicate(expression + " = ?", value);
    }

    public static Predicate neq(String expression, Object value) {
        return new Predicate(expression + " != ?", value);
    }

    public static Predicate like(String expression, String value) {
        if (value == null)
            value = "";
        return new Predicate(expression + " like ?", value);
    }

    public static Predicate ilike(String expression, String value) {
        if (value == null)
            value = "";
        return new Predicate("upper(" + expression + ") like ?",
                value.toUpperCase());
    }

    /**
     * 'in' predicate
     * Passes the collection parameter directly to the hibernate query
     *
     * @param expression hql expression
     * @param c collection argument for 'in' predicate
     * @param name name of named argument
     * @return Newly created predicate
     */
    public static Predicate in(String expression, Collection c, String name) {
        StringBuilder cond = new StringBuilder(expression);
        cond.append(" in ");
        cond.append("(");
        cond.append(":");
        cond.append(name);
        cond.append(")");
        return new Predicate(cond.toString(), c, name);
    }

    /**
     * 'in' predicate
     * Unrolls the array of objects before passing to the hibernate query
     *
     * @param expression hql expression
     * @param values array of objects, argument for the 'in' predicate
     * @return Newly created predicate
     */
    public static Predicate in(String expression, Object[] values) {
        StringBuilder cond = new StringBuilder(expression + " in (");
        for (Object o : values) {
            cond.append(cond.charAt(cond.length()-1) == '(' ? "?" : ",?");
        }
        cond.append(")");
        return new Predicate(cond.toString(), values);
    }

    public static Predicate inElements(String expression, Object value) {
        StringBuilder cond = new StringBuilder();
        cond.append("?");
        cond.append(" in ");
        cond.append(expression);
        return new Predicate(cond.toString(), value);
    }
    
    public static Predicate notInElements(String expression, Object value) {
        StringBuilder cond = new StringBuilder();
        cond.append("?");
        cond.append(" not in ");
        cond.append(expression);
        return new Predicate(cond.toString(), value);
    }

    @Override
    public String toString() {
        return getQueryString();
    }

    public String getQueryString() {
        return predicate;
    }

    public Object[] getParametersValue() {
        return paramValues.toArray();
    }

    public Map<String, Object> getNamedArgs() {
        return Collections.unmodifiableMap(this.namedArgs);
    }
}
