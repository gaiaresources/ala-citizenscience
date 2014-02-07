package au.com.gaiaresources.bdrs.controller.record;

import au.com.gaiaresources.bdrs.controller.AbstractControllerTest;
import au.com.gaiaresources.bdrs.geometry.GeometryBuilder;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.survey.BdrsCoordReferenceSystem;
import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;
import au.com.gaiaresources.bdrs.model.taxa.TaxonGroup;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.util.StringUtils;
import com.vividsolutions.jts.geom.Polygon;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.encoding.Md5PasswordEncoder;
import org.springframework.security.authentication.encoding.PasswordEncoder;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Created with IntelliJ IDEA.
 * User: serge
 * Date: 17/01/14
 * Time: 10:50 AM
 */
public class ReclassifyControllerTest extends AbstractControllerTest {

    private IndicatorSpecies dropBear;
    private IndicatorSpecies nyanCat;
    private IndicatorSpecies hoopSnake;
    private IndicatorSpecies surfingBird;

    private static String[][] testUsers = {
            {"RoleRoot", "Hughes", Role.ROOT},
            {"RoleAdmin", "Magnusson", Role.ADMIN},
            {"RoleSupervisor", "Jansson", Role.SUPERVISOR},
            {"RolePower", "Pettersson", Role.POWERUSER},
            {"RoleUser", "Carlsson", Role.USER},
    };

    @Before
    public void setUp() throws Exception {
        //the order of execution is crucial
        createIndicatorSpecies();
        createUsers();
    }

    private IndicatorSpecies[] createIndicatorSpecies() {
        TaxonGroup g1 = new TaxonGroup();
        g1.setName("fictionus animus");
        taxaDAO.save(g1);
        dropBear = createIndicatorSpeciesDB(g1, "dropus bearus", "drop bear", "jimmy ricard", "guid1231239a8d");
        nyanCat = createIndicatorSpeciesDB(g1, "nyanatic catup", "nyan cat", null, "lsid:sdklsdff:s39er:sdksdf:");
        hoopSnake = createIndicatorSpeciesDB(g1, "circulom reptile", "hoop snake", null, null);
        surfingBird = createIndicatorSpeciesDB(g1, "orthonological waverider", "surfing bird", "a guy who names stuff", null);
        return new IndicatorSpecies[]{dropBear, nyanCat, hoopSnake, surfingBird};
    }

    private List<User> createUsers() {
        List<User> result = new ArrayList<User>(testUsers.length);
        // shamelessly copied from UserDAOImplTest
        PasswordEncoder passwordEncoder = new Md5PasswordEncoder();
        String emailAddr = "abigail.ambrose@example.com";
        String encodedPassword = passwordEncoder.encodePassword("password", null);
        String registrationKey = passwordEncoder.encodePassword(StringUtils.generateRandomString(10, 50), emailAddr);

        for (String[] name : testUsers) {
            String first = name[0];
            String last = name[1];
            String role = name[2];
            User user = userDAO.createUser(
                    first,
                    first, last,
                    first + "@" + last + ".com",
                    encodedPassword, registrationKey,
                    role);
            result.add(user);
        }
        return result;
    }

    private IndicatorSpecies createIndicatorSpeciesDB(TaxonGroup group, String sciName, String commonName, String author, String guid) {
        IndicatorSpecies species = new IndicatorSpecies();
        species.setTaxonGroup(group);
        species.setScientificName(sciName);
        species.setCommonName(commonName);
        if (author != null) {
            species.setScientificNameAndAuthor(sciName + " - " + author);
        }
        if (guid != null) {
            species.setSourceId(guid);
        }
        species.setRunThreshold(false);
        return taxaDAO.save(species);
    }

    /**
     * Basic test of reclassification by admin as user and as owner of records
     * Also check that the controller forward the request to it's default redirection URL
     * (AdvancedReview controller)
     *
     * @throws Exception
     */
    @Test
    public void testSimpleReclassifyAdmin() throws Exception {
        // user admin always exists
        User admin = userDAO.getUser("admin");
        assertNotNull(admin);
        //create a record for each species
        IndicatorSpecies[] mySpecies = new IndicatorSpecies[]{dropBear, nyanCat, hoopSnake, surfingBird};
        List<Record> records = createRecords(admin, mySpecies);

        //check records
        for (int i = 0; i < records.size(); i++) {
            assertEquals(mySpecies[i].getId(), records.get(i).getSpecies().getId());
        }

        //reclassify all into hoopSnake
        ModelAndView mv = new RequestBuilder("POST").
                setSpecies(hoopSnake).
                setRecords(records).
                execute(admin);

        for (Record record : records) {
            assertEquals(hoopSnake.getId(), recDAO.getRecord(record.getId()).getSpecies().getId());
        }

        //check the forward
        assertEquals("Should forward to the advancedReview", "forward:" + ReclassifyController.DEFAULT_REDIRECT_URL, mv.getViewName());

    }

    /**
     * Create records from a non admin user and check that it can't reclassify but that admin can
     * Note: the reclassify is expected to throw a org.springframework.security.access.AccessDeniedException
     *
     * @throws Exception
     */
    @Test
    public void testReclassifyRootAndAdminOnly() throws Exception {

        // Grab the non admin users
        List<User> notAdminUsers = new ArrayList<User>(3);
        // Role.User
        User basicUser = userDAO.getUser("RoleUser");
        assertNotNull(basicUser);
        assertEquals(Role.USER, basicUser.getRoles()[0]);
        notAdminUsers.add(basicUser);

        // Role.Power
        User powerUser = userDAO.getUser("RolePower");
        assertNotNull(powerUser);
        assertEquals(Role.POWERUSER, powerUser.getRoles()[0]);
        notAdminUsers.add(powerUser);

        // Role.Power
        User supervisorUser = userDAO.getUser("RoleSupervisor");
        assertNotNull(supervisorUser);
        assertEquals(Role.SUPERVISOR, supervisorUser.getRoles()[0]);
        notAdminUsers.add(supervisorUser);

        List<Record> allRecords = new ArrayList<Record>();
        // try to reclassify
        for (User user : notAdminUsers) {
            //create a 3 dropBear records
            IndicatorSpecies[] mySpecies = new IndicatorSpecies[]{dropBear, dropBear, dropBear};
            List<Record> records = createRecords(user, mySpecies);
            //check records
            for (int i = 0; i < records.size(); i++) {
                assertEquals(mySpecies[i].getId(), records.get(i).getSpecies().getId());
            }
            // request reclassify all into hoopSnake and check that an AccessDeniedException is thrown
            try {
                new RequestBuilder("POST").
                        setSpecies(hoopSnake).
                        setRecords(records).
                        execute(user);
                fail("Non admin user should not be allowed to reclassify");
            } catch (AccessDeniedException e) {
                allRecords.addAll(records);
            }
        }

        // now admin and root should be able to reclassify even if they are not the records owners
        User admin = userDAO.getUser("RoleAdmin");
        assertNotNull(admin);
        new RequestBuilder("POST").
                setSpecies(hoopSnake).
                setRecords(allRecords).
                execute(admin);
        //verify
        for (Record record : allRecords) {
            assertEquals(hoopSnake.getId(), recDAO.getRecord(record.getId()).getSpecies().getId());
        }

        User root = userDAO.getUser("RoleRoot");
        assertNotNull(root);
        new RequestBuilder("POST").
                setSpecies(surfingBird).
                setRecords(allRecords).
                execute(root);
        //verify
        for (Record record : allRecords) {
            assertEquals(surfingBird.getId(), recDAO.getRecord(record.getId()).getSpecies().getId());
        }
    }

    /**
     * The reclassify controller has a get method but it should
     * not do anything but forwarding the request to the advancedReview controller.
     */
    @Test
    public void testGetMethodDoNothing() throws Exception {
        User admin = userDAO.getUser("admin");
        assertNotNull(admin);
        //create a record for each species
        IndicatorSpecies[] mySpecies = new IndicatorSpecies[]{dropBear, nyanCat, hoopSnake, surfingBird};
        List<Record> records = createRecords(admin, mySpecies);

        //check records
        for (int i = 0; i < records.size(); i++) {
            assertEquals(mySpecies[i].getId(), records.get(i).getSpecies().getId());
        }

        //reclassify all into hoopSnake
        ModelAndView mv = new RequestBuilder("GET").
                setSpecies(hoopSnake).
                setRecords(records).
                execute(admin);

        // check they haven't changed
        for (int i = 0; i < records.size(); i++) {
            assertEquals(mySpecies[i].getId(), records.get(i).getSpecies().getId());
        }

        //check the forward
        assertEquals("Should forward to the advancedReview", "forward:" + ReclassifyController.DEFAULT_REDIRECT_URL, mv.getViewName());

    }

    /**
     * If the flag massReclassify is set in the request and no facet filter
     * then all the records are reclassified
     */
    @Test
    public void testMassReclassifyWithNoFacet() throws Exception {
        // user admin always exists
        User admin = userDAO.getUser("admin");
        assertNotNull(admin);
        User basicUser = userDAO.getUser("RoleUser");
        assertNotNull(basicUser);
        //create 10 records
        IndicatorSpecies[] mySpecies = new IndicatorSpecies[]
                {nyanCat, hoopSnake, surfingBird, dropBear,
                        nyanCat, hoopSnake, nyanCat, surfingBird, surfingBird};
        List<Record> records = createRecords(basicUser, mySpecies);
        IndicatorSpecies newSpecies = dropBear;
        ModelAndView mv = new RequestBuilder("POST").
                setSpecies(newSpecies).
                setMassReclassify(null).// set the mass reclassify flag with no facet params!!
                execute(admin);
        //check
        for (Record record : records) {
            assertEquals(newSpecies.getId(), recDAO.getRecord(record.getId()).getSpecies().getId());
        }
        //check the forward
        assertEquals("Should forward to the advancedReview", "forward:" + ReclassifyController.DEFAULT_REDIRECT_URL, mv.getViewName());

    }

    /**
     * Test mass reclassify with within area facet on
     */
    @Test
    public void testMassReclassifyWithinArea() throws Exception {
        User admin = userDAO.getUser("admin");
        assertNotNull(admin);

        // build a square area
        GeometryBuilder geometryBuilder = new GeometryBuilder(BdrsCoordReferenceSystem.DEFAULT_SRID);
        double originLong = 115.0;
        double originLat = -32.0;
        double areaWidth = 5;
        Polygon area = geometryBuilder.createSquare(originLong, originLat, areaWidth);
        // create a bunch of surfingBirds records within the area
        IndicatorSpecies[] surfingBirds = new IndicatorSpecies[]
                {surfingBird, surfingBird, surfingBird, surfingBird, surfingBird,
                        surfingBird, surfingBird, surfingBird, surfingBird, surfingBird};
        List<Record> surfingBirdsWithin = createRecords(admin, surfingBirds);
        Random rand = new Random();

        // create a bunch of surfingBirds records within the area
        for (Record record : surfingBirdsWithin) {
            int withinOffset = rand.nextInt((int) areaWidth - 2) + 1;  // 0 < offset < area width
            record.setGeometry(geometryBuilder.createPoint(originLong + withinOffset, originLat + withinOffset));
            recDAO.saveRecord(record);
        }

        // create a bunch of surfingBirds records outside the area
        List<Record> surfingBirdsOutside = createRecords(admin, surfingBirds);
        for (Record record : surfingBirdsOutside) {
            int outsideOffset = (int) areaWidth + 1 + rand.nextInt(10); // areaWidth <  offset < areaWidth + 10
            record.setGeometry(geometryBuilder.createPoint(originLong + outsideOffset, originLat + outsideOffset));
            recDAO.saveRecord(record);
        }

        //now reclassify the within surfingBirds into dropBears by asking a mass reclassify with the facet
        // within area on
        Map<String, String[]> params = new HashMap<String, String[]>();
        String areaValue = area.toText();  // this should be enough to convert the area in WKT 4326
        params.put("within", new String[]{areaValue});
        new RequestBuilder("POST").
                setSpecies(dropBear).
                setMassReclassify(params).
                execute(admin);

        //check records
        // within surfingBirds should be dropBears now
        IndicatorSpecies expectedSpecies = dropBear;
        List<Record> records = surfingBirdsWithin;
        for (Record record : records) {
            assertEquals("records within area haven't been reclassified", expectedSpecies.getId(), recDAO.getRecord(record.getId()).getSpecies().getId());
        }

        // outside surfingBirds should still be surfingBirds
        expectedSpecies = surfingBird;
        records = surfingBirdsOutside;
        for (Record record : records) {
            assertEquals("records outside area shouldn't be reclassified", expectedSpecies.getId(), recDAO.getRecord(record.getId()).getSpecies().getId());
        }
    }

    /**
     * If in the request there are recordIDs but also a massReclassify flag
     * only the records specified with their IDs are reclassify
     * the controller reclassify only the records
     *
     * @throws Exception
     */
    @Test
    public void testPrecedenceRecordIdsOverMassReclassify() throws Exception {
        User admin = userDAO.getUser("admin");
        assertNotNull(admin);
        // create some nyanCats
        IndicatorSpecies[] mySpecies = new IndicatorSpecies[]{nyanCat, nyanCat, nyanCat, nyanCat, nyanCat};
        List<Record> records = createRecords(admin, mySpecies);

        //check records
        for (int i = 0; i < records.size(); i++) {
            assertEquals(mySpecies[i].getId(), records.get(i).getSpecies().getId());
        }

        //reclassify the first two
        List<Record> toReclassify = records.subList(0, 2);
        new RequestBuilder("POST").
                setSpecies(dropBear).
                setRecords(toReclassify).
                setMassReclassify(null).   // set the mass reclassify flag!!
                execute(admin);

        //check that the first two are reclassified
        for (Record record : toReclassify) {
            assertEquals(dropBear.getId(), recDAO.getRecord(record.getId()).getSpecies().getId());
        }

        // but not the other ones
        for (Record record : records.subList(2, records.size())) {
            assertEquals(nyanCat.getId(), recDAO.getRecord(record.getId()).getSpecies().getId());
        }
    }


    private List<Record> createRecords(User user, IndicatorSpecies[] species) {
        List<Record> result = new ArrayList<Record>(species.length);
        for (IndicatorSpecies spec : species) {
            Record record = new Record();
            record.setUser(user);
            record.setSpecies(spec);
            record = recDAO.saveRecord(record);
            result.add(record);
        }
        return result;
    }

    class RequestBuilder {
        RequestBuilder(String method) {
            request.setRequestURI(ReclassifyController.RECLASSIFY_URL);
            request.setMethod(method);
            request.removeAllParameters();
        }

        public RequestBuilder setSpecies(IndicatorSpecies species) {
            request.addParameter(ReclassifyController.PARAM_SPECIES_ID, species.getId().toString());
            return this;
        }

        public RequestBuilder setRecords(List<Record> records) {
            ArrayList<String> paramRecordIds = new ArrayList<String>(records.size());
            for (Record record : records) {
                paramRecordIds.add(record.getId().toString());
            }
            request.addParameter(ReclassifyController.PARAM_RECORD_ID, paramRecordIds.toArray(new String[paramRecordIds.size()]));
            return this;
        }

        public RequestBuilder setMassReclassify(Map<String, String[]> facetParams) {
            request.addParameter(ReclassifyController.PARAM_MASS_RECLASSIFY, "true");
            if (facetParams != null) {
                request.addParameters(facetParams);
            }
            return this;
        }

        public ModelAndView execute(User user) throws Exception {
            login(user.getName(), user.getPassword(), user.getRoles());
            return handle(request, response);
        }

        public HttpServletRequest getRequest() {
            return request;
        }
    }
}
