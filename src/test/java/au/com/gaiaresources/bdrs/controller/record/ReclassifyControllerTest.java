package au.com.gaiaresources.bdrs.controller.record;

import au.com.gaiaresources.bdrs.controller.AbstractControllerTest;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;
import au.com.gaiaresources.bdrs.model.taxa.TaxonGroup;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.util.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.encoding.Md5PasswordEncoder;
import org.springframework.security.authentication.encoding.PasswordEncoder;
import org.springframework.web.servlet.ModelAndView;

import java.util.ArrayList;
import java.util.List;

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
    public void testReclassifyRecordsAdmin() throws Exception {
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
        ModelAndView mv = requestReclassify(admin, hoopSnake, records);

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
    public void testReclassifyRecordsRootAndAdminOnly() throws Exception {

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
                requestReclassify(user, hoopSnake, records);
                fail("Non admin user should not be allowed to reclassify");
            } catch (AccessDeniedException e) {
                allRecords.addAll(records);
            }
        }

        // now admin and root should be able to reclassify even if they are not the records owners
        User admin = userDAO.getUser("RoleAdmin");
        assertNotNull(admin);
        requestReclassify(admin, hoopSnake, allRecords);
        //verify
        for (Record record : allRecords) {
            assertEquals(hoopSnake.getId(), recDAO.getRecord(record.getId()).getSpecies().getId());
        }

        User root = userDAO.getUser("RoleRoot");
        assertNotNull(root);
        requestReclassify(root, surfingBird, allRecords);
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
    public void getMethodNotReclassifying() throws Exception {
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
        ModelAndView mv = requestReclassify(admin, hoopSnake, records, "GET");

        // check they haven't changed
        for (int i = 0; i < records.size(); i++) {
            assertEquals(mySpecies[i].getId(), records.get(i).getSpecies().getId());
        }

        //check the forward
        assertEquals("Should forward to the advancedReview", "forward:" + ReclassifyController.DEFAULT_REDIRECT_URL, mv.getViewName());

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

    private ModelAndView requestReclassify(User user, IndicatorSpecies species, List<Record> records) throws Exception {
        return requestReclassify(user, species, records, "POST"); // the default
    }

    private ModelAndView requestReclassify(User user, IndicatorSpecies species, List<Record> records, String method) throws Exception {
        request.removeAllParameters();
        login(user.getName(), user.getPassword(), user.getRoles());
        String url = ReclassifyController.RECLASSIFY_URL;
        request.setRequestURI(url);
        request.setMethod(method);
        String paramSpeciesId = species.getId().toString();
        ArrayList<String> paramRecordIds = new ArrayList<String>(records.size());
        for (Record record : records) {
            paramRecordIds.add(record.getId().toString());
        }
        request.addParameter(ReclassifyController.PARAM_SPECIES_ID, paramSpeciesId);
        request.addParameter(ReclassifyController.PARAM_RECORD_ID, paramRecordIds.toArray(new String[paramRecordIds.size()]));
        return handle(request, response);
    }

}
