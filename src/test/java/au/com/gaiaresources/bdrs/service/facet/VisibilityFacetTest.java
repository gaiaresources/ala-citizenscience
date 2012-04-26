package au.com.gaiaresources.bdrs.service.facet;

import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.facet.FacetDAO;
import au.com.gaiaresources.bdrs.model.record.RecordDAO;
import au.com.gaiaresources.bdrs.model.record.RecordVisibility;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.service.facet.option.FacetOption;
import au.com.gaiaresources.bdrs.util.Pair;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests the VisibilityFacet class.
 */
@RunWith(JMock.class)
public class VisibilityFacetTest {

    private Mockery context = new JUnit4Mockery();
    private FacetDAO recordDAO = context.mock(RecordDAO.class);

    private JSONObject configuration;
    
    @Before
    public void setUp() {
        configuration = new JSONObject();
        configuration.put(VisibilityFacet.JSON_ENABLE_FOR_ROLES_KEY, Role.ADMIN+","+Role.POWERUSER);
    }


    /**
     * Tests the facet will not be visible for anonymous users.
     */
    @Test
    public void testConfigurationNotActiveForAnonymousUser() {

        User anonymous = null;
        VisibilityFacet facet = new VisibilityFacet("Visibility", recordDAO, new HashMap<String, String[]>(), anonymous, configuration);

        Assert.assertEquals(false, facet.isActive());
    }

    /**
     * Tests the facet is visible to admins only if the enabledForRoles configuration is not specified.
     */
    @Test
    public void testConfigurationDefaults() {

        context.checking(new Expectations(){
            {oneOf(recordDAO).getDistinctRecordVisibilities();
            returnValue(new ArrayList()); }
        });

        User admin = new User();
        admin.setRoles(new String[]{Role.ADMIN});
        VisibilityFacet facet = new VisibilityFacet("Visibility", recordDAO, new HashMap<String, String[]>(), admin, new JSONObject());

        Assert.assertEquals(true, facet.isActive());

    }

    /**
     * Tests the facet is visible if the user has a configured role.
     */
    @Test
    public void testConfigurationUserHasRole() {

        context.checking(new Expectations(){
            {oneOf(recordDAO).getDistinctRecordVisibilities();
                returnValue(new ArrayList()); }
        });

        User admin = new User();
        admin.setRoles(new String[]{Role.POWERUSER});
        VisibilityFacet facet = new VisibilityFacet("Visibility", recordDAO, new HashMap<String, String[]>(), admin, configuration);

        Assert.assertEquals(true, facet.isActive());

    }

    /**
     * Tests the facet is not visible if the user does not have a configured role.
     */
    @Test
    public void testConfigurationUserDoesNotHaveRole() {

        User admin = new User();
        admin.setRoles(new String[]{Role.USER});
        VisibilityFacet facet = new VisibilityFacet("Visibility", recordDAO, new HashMap<String, String[]>(), admin, configuration);

        Assert.assertEquals(false, facet.isActive());

    }

    /**
     * Tests the facet options produced when no visibility facet has been selected.
     */
    @Test
    public void testOptionsNoOptionsSelected() {

        configureRecordDAO();

        User admin = new User();
        admin.setRoles(new String[]{Role.ADMIN});
        VisibilityFacet facet = new VisibilityFacet("Visibility", recordDAO, new HashMap<String, String[]>(), admin, new JSONObject());

        Assert.assertEquals(true, facet.isActive());
        List<FacetOption> facetOptions = facet.getFacetOptions();
        Assert.assertEquals(3, facetOptions.size());

        FacetOption option = facetOptions.get(0);
        checkOption(facetOptions.get(0), RecordVisibility.CONTROLLED.getDescription(), 2, false);
        checkOption(facetOptions.get(1), RecordVisibility.OWNER_ONLY.getDescription(), 1, false);
        checkOption(facetOptions.get(2), RecordVisibility.PUBLIC.getDescription(), 10, false);

    }

    /**
     * Tests the facet options produced when a facet option has been selected.
     */
    @Test
    public void testOptionsPublicOptionSelected() {

        configureRecordDAO();

        Map<String, String[]> params = new HashMap<String, String[]>();
        params.put("_"+VisibilityFacet.QUERY_PARAM_NAME+"_option", new String[]{Integer.toString(RecordVisibility.PUBLIC.ordinal())});

        User admin = new User();
        admin.setRoles(new String[]{Role.ADMIN});
        VisibilityFacet facet = new VisibilityFacet("Visibility", recordDAO, params, admin, new JSONObject());

        Assert.assertEquals(true, facet.isActive());
        List<FacetOption> facetOptions = facet.getFacetOptions();
        Assert.assertEquals(3, facetOptions.size());

        FacetOption option = facetOptions.get(0);
        checkOption(facetOptions.get(0), RecordVisibility.CONTROLLED.getDescription(), 2, false);
        checkOption(facetOptions.get(1), RecordVisibility.OWNER_ONLY.getDescription(), 1, false);
        checkOption(facetOptions.get(2), RecordVisibility.PUBLIC.getDescription(), 10, true);

    }

    /**
     * Sets up an expectation that returns a known set of visibility values.
     */
    private void configureRecordDAO() {
        final List<Pair<RecordVisibility, Long>> results = new ArrayList<Pair<RecordVisibility, Long>>();

        results.add(new Pair<RecordVisibility, Long>(RecordVisibility.CONTROLLED, 2L));
        results.add(new Pair<RecordVisibility, Long>(RecordVisibility.OWNER_ONLY, 1L));
        results.add(new Pair<RecordVisibility, Long>(RecordVisibility.PUBLIC, 10L));

        context.checking(new Expectations(){
            {oneOf(recordDAO).getDistinctRecordVisibilities();
                will(returnValue(results)); }
        });
    }

    private void checkOption(FacetOption option, String expectedDescription, long expectedCount, boolean expectedSelected) {
        Assert.assertEquals(expectedDescription, option.getDisplayName());
        Assert.assertEquals(Long.valueOf(expectedCount), option.getCount());
        Assert.assertEquals(expectedSelected, option.isSelected());
    }


}