package au.com.gaiaresources.bdrs.model.taxa.impl;

import au.com.gaiaresources.bdrs.db.impl.AbstractDAOImpl;
import au.com.gaiaresources.bdrs.model.taxa.AttributeOptionDAO;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Repository;

/**
 * Performs the retrieval of AttributeOptions from the database.
 */
@Repository
public class AttributeOptionDAOImpl extends AbstractDAOImpl implements AttributeOptionDAO {

    @SuppressWarnings("unused")
    private Logger log = Logger.getLogger(getClass());


}