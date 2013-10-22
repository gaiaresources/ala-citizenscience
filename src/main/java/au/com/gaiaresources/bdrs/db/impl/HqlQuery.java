package au.com.gaiaresources.bdrs.db.impl;

import au.com.gaiaresources.bdrs.json.JSONEnum;
import au.com.gaiaresources.bdrs.json.JSONEnumUtil;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Davide Alberto Molin (davide.molin@gmail.com)
 * 
 *         adapted from
 *         http://developme.wordpress.com/2010/03/03/creating-a-criteria
 *         -like-api-to-build-hql-queries/
 */
public class HqlQuery extends Predicate {
    String incipit;
    //following the incipit, usually contains joins
    String header = "";
    //closing the query: order by and other things
    String footer = "";
    //distinct predicate
    String distinct;
    boolean count = false;

    private Set<String> aliases = new HashSet<String>();

    public enum SortOrder implements JSONEnum {
        ASC, DESC;
        
        @Override
        public void writeJSONString(Writer out) throws IOException {
            JSONEnumUtil.writeJSONString(out, this);
        }

        @Override
        public String toJSONString() {
            return JSONEnumUtil.toJSONString(this);
        }
    };

    public HqlQuery(Class entityClass) {
        super();
        this.incipit = "from " + entityClass.getName() + " obj ";
    }

    /**
     * The incipit can start with "from  " or with the less common
     * "select  from  ..."
     */
    public HqlQuery(String incipit) {
        super();
        this.incipit = incipit;
    }

    /**
     * Inner Join
     * 
     * @param expression
     *            what to join with
     * @param alias
     *            Aliased name for this join
     * @return
     */
    public HqlQuery join(String expression, String alias) {
        checkAlias(alias);
        header += " join " + expression + " as " + alias;
        return this;
    }

    public HqlQuery leftJoin(String expression, String alias) {
        checkAlias(alias);
        header += " left join " + expression + " as " + alias;
        return this;
    }

    public HqlQuery rightJoin(String expression, String alias) {
        checkAlias(alias);
        header += " right join " + expression + " as " + alias;
        return this;
    }

    /**
     * Checks if an SQL alias has already been used.
     * @param alias the alias to check.
     */
    private void checkAlias(String alias) {
        if (aliases.contains(alias)) {
            throw new IllegalArgumentException("Alias "+alias+" is already used by this query");
        }
        aliases.add(alias);
    }

    /**
     * Returns true if this HqlQuery is already using the supplied alias.
     * @param alias the alias to check.
     * @return true if this HqlQuery is already using the supplied alias.
     */
    public boolean hasAlias(String alias) {
        return aliases.contains(alias);
    }

    public HqlQuery count() {
        this.count = true;
        return this;
    }

    public boolean hasCount() {
        return count;
    }

    /**
     * Useful when we want enable/disable counting
     * 
     * @param wantsCount
     *            true/false
     * @return this same query object, for chaining calls.
     */
    public HqlQuery count(boolean wantsCount) {
        this.count = wantsCount;
        return this;
    }

    private String getIncipit() {
        String ret = (incipit == null ? "" : incipit + " ");
        if (distinct == null || ret.trim().toUpperCase().startsWith("SELECT")) {
            //unable to apply distinct!.
            return ret;
        }
        return "select " + (count ? " count(" : "")
                + (" distinct " + distinct)
                + (count ? ")" : "") + " " + incipit;
    }

    @Override
    public String getQueryString() {
        return getIncipit() + header + super.getQueryString() + footer;
    }

    @Override
    public String toString() {
        return getQueryString();
    }

    public HqlQuery addOrderBy(String orderBy) {
        footer += (footer.length() == 0 ? " ORDER BY " : ",") + orderBy;
        return this;
    }

    public HqlQuery distinct() {
        return distinct(null);
    }

    /**
     * NOTE:distinct is applicable ONLY when the query is NOT already starting
     * with SELECT statement..
     * 
     * @param alias
     *            aliased name on which to apply the distinct as in
     *            "select distinct(<alias>) from MyTable <alias>"
     * @return
     */
    public HqlQuery distinct(String alias) {
        this.distinct = alias;
        return this;
    }

    /**
     * utility method to handle order by clauses.
     * 
     * @param sortProperty
     *            the property to apply the sorting to.
     * @param sortOrder
     *            see enum at class start
     * @param entityAlias
     *            alias to be used for table. defaults to 'obj'
     * @return
     */
    public HqlQuery order(String sortProperty, String sortOrder,
            String entityAlias) {
        //esamino ordinamento
        if (sortProperty == null)
            return this; //no ordering.

        String orderType = "ASC";

        //obj prefix, to avoid ambiguity in the query with joins
        sortProperty = (entityAlias != null ? (entityAlias + ".") : "")
                + sortProperty;

        orderType = "ASC".equalsIgnoreCase(sortOrder) ? "ASC" : "DESC";
        addOrderBy(sortProperty + " " + orderType);
        return this;
    }

    /**
     * utility method to handle order by clauses.
     * 
     * @param sortProperty
     *            the property to apply the sorting to.
     * @param sortOrder
     *            see enum at class start
     * @return
     */
    public HqlQuery order(String sortProperty, String sortOrder) {
        return order(sortProperty, sortOrder, "obj");
    }

    /**
     * Same as above, but using the Enum
     */
    public HqlQuery order(String sortProperty, SortOrder sortOrder) {
        return order(sortProperty, sortOrder.name());
    }

    public boolean hasOrder() {
        return footer.contains("ORDER BY");
    }

    /**
     * Applys only the named arguments to a hibernate query.
     * @param hibernateQuery hibernate query to modify
     */
    public void applyNamedArgsToQuery(Query hibernateQuery) {
        for (Map.Entry<String, Object> argPair : getNamedArgs().entrySet()) {
            if (argPair.getValue() instanceof Collection) {
                hibernateQuery.setParameterList(argPair.getKey(), (Collection) argPair.getValue());
            } else {
                hibernateQuery.setParameter(argPair.getKey(), argPair.getValue());
            }
        }
    }
}
