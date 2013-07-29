package au.com.gaiaresources.bdrs.model.taxa.impl;

import au.com.gaiaresources.bdrs.controller.AbstractControllerTest;
import au.com.gaiaresources.bdrs.model.location.Location;
import au.com.gaiaresources.bdrs.model.location.LocationDAO;
import au.com.gaiaresources.bdrs.model.metadata.Metadata;
import au.com.gaiaresources.bdrs.model.metadata.MetadataDAO;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.survey.SurveyDAO;
import au.com.gaiaresources.bdrs.model.survey.SurveyFormRendererType;
import au.com.gaiaresources.bdrs.model.taxa.*;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.model.user.UserDAO;
import au.com.gaiaresources.bdrs.service.map.GeoMapService;
import au.com.gaiaresources.bdrs.service.survey.SurveyImportExportService;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.encoding.Md5PasswordEncoder;
import org.springframework.security.authentication.encoding.PasswordEncoder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: aaron
 * Date: 11/07/13
 * Time: 4:25 PM
 * To change this template use File | Settings | File Templates.
 */
public class TaxaDaoImplGetGroupsForSurveyTest extends AbstractControllerTest {


    private Logger log = Logger.getLogger(getClass());
    private Survey frogSurveyInDb, birdAndFrogSurveyInDb, allSurvey;

    private IndicatorSpecies frog1, frog2, bird1, bird2, bird3;

    private TaxonGroup frogTaxon, birdTaxon, allGroup;

    @Autowired
    private SurveyDAO surveyDAO;

    @Autowired
    private LocationDAO locationDAO;

    @Autowired
    private TaxaDAO taxaDAO;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private AttributeDAO attributeDAO;

    @Autowired
    private MetadataDAO metadataDAO;

    @Autowired
    private SurveyImportExportService surveyImportExportService;

    @Autowired
    private GeoMapService geoMapService;

    @Autowired
    private SpeciesProfileDAO speciesProfileDAO;

    @Before
    public void setup() {

        //create a user
        PasswordEncoder passwordEncoder = new Md5PasswordEncoder();
        String emailAddr = "user@mailinator.com";
        String encodedPassword = passwordEncoder.encodePassword("password", null);
        User u = userDAO.createUser("user", "fn", "ln", emailAddr, encodedPassword, "usersIdent", new String[]{"ROLE_USER"});
        Set<User> users = new HashSet<User>();
        users.add(u);

        //create taxa for species
        frogTaxon = new TaxonGroup();
        birdTaxon = new TaxonGroup();
        allGroup = new TaxonGroup();
        frogTaxon.setName("frogs");
        birdTaxon.setName("birds");
        allGroup.setName("all");
        taxaDAO.save(frogTaxon);
        taxaDAO.save(birdTaxon);
        taxaDAO.save(allGroup);

        //create species
        Set<IndicatorSpecies> frogSet = new HashSet<IndicatorSpecies>();
        Set<IndicatorSpecies> birdSet = new HashSet<IndicatorSpecies>();
        frog1 = new IndicatorSpecies();
        frog2 = new IndicatorSpecies();
        bird1 = new IndicatorSpecies();
        bird2 = new IndicatorSpecies();
        bird3 = new IndicatorSpecies();
        frog1.setCommonName("commonNameFrog1");
        frog2.setCommonName("commonNameFrog2");
        bird1.setCommonName("commonNamebird1");
        bird2.setCommonName("commonNamebird2");
        bird3.setCommonName("commonNamebird3");
        frog1.setScientificName("scientificNameFrog1");
        frog2.setScientificName("scientificNameFrog2");
        bird1.setScientificName("scientificNamebird1");
        bird2.setScientificName("scientificNamebird2");
        bird3.setScientificName("scientificNamebird3");
        frog1.setTaxonGroup(taxaDAO.getTaxonGroup("frogs"));
        frog2.setTaxonGroup(taxaDAO.getTaxonGroup("frogs"));
        bird1.setTaxonGroup(taxaDAO.getTaxonGroup("birds"));
        bird2.setTaxonGroup(taxaDAO.getTaxonGroup("birds"));
        bird3.setTaxonGroup(taxaDAO.getTaxonGroup("birds"));
        frogSet.add(taxaDAO.save(frog1));
        frogSet.add(taxaDAO.save(frog2));
        birdSet.add(taxaDAO.save(bird1));
        birdSet.add(taxaDAO.save(bird2));
        birdSet.add(taxaDAO.save(bird3));

        frog1.addSecondaryGroup(allGroup);
        frog2.addSecondaryGroup(allGroup);
        bird1.addSecondaryGroup(allGroup);
        bird2.addSecondaryGroup(allGroup);
        bird3.addSecondaryGroup(allGroup);

        //create attributes for survey
        List<Attribute> attributes = new ArrayList<Attribute>();
        List<Attribute> birdAndFrogAttributes = new ArrayList<Attribute>();
        Attribute frogSurveyAttribute1 = new Attribute();
        Attribute frogSurveyAttribute2 = new Attribute();
        Attribute birdAndFrogSurveyAttribute1 = new Attribute();
        Attribute birdAndFrogSurveyAttribute2 = new Attribute();
        Attribute birdAndFrogSurveyAttribute3 = new Attribute();
        frogSurveyAttribute1.setName("surveyAttribute1");
        frogSurveyAttribute2.setName("surveyAttribute2");
        birdAndFrogSurveyAttribute1.setName("surveyAttribute1");
        birdAndFrogSurveyAttribute2.setName("surveyAttribute2");
        birdAndFrogSurveyAttribute3.setName("surveyAttribute3");
        frogSurveyAttribute1.setRequired(true);
        frogSurveyAttribute2.setRequired(true);
        birdAndFrogSurveyAttribute1.setRequired(true);
        birdAndFrogSurveyAttribute2.setRequired(true);
        birdAndFrogSurveyAttribute3.setRequired(true);
        frogSurveyAttribute1.setTypeCode("SV");
        frogSurveyAttribute2.setTypeCode("SV");
        birdAndFrogSurveyAttribute1.setTypeCode("SV");
        birdAndFrogSurveyAttribute2.setTypeCode("SV");
        birdAndFrogSurveyAttribute3.setTypeCode("SV");
        frogSurveyAttribute1.setTag(false);
        frogSurveyAttribute2.setTag(false);
        birdAndFrogSurveyAttribute1.setTag(false);
        birdAndFrogSurveyAttribute2.setTag(false);
        birdAndFrogSurveyAttribute3.setTag(false);
        attributes.add(attributeDAO.save(frogSurveyAttribute1));
        attributes.add(attributeDAO.save(frogSurveyAttribute2));
        birdAndFrogAttributes.add(attributeDAO.save(birdAndFrogSurveyAttribute1));
        birdAndFrogAttributes.add(attributeDAO.save(birdAndFrogSurveyAttribute2));
        birdAndFrogAttributes.add(attributeDAO.save(birdAndFrogSurveyAttribute3));

        //create locations for survey
        List<Location> locations = new ArrayList<Location>();
        List<Location> birdSurveyLocations = new ArrayList<Location>();
        Location l1 = new Location();
        Location l2 = new Location();
        Location birdSurveyLocation1 = new Location();
        Location birdSurveyLocation2 = new Location();
        Location birdSurveyLocation3 = new Location();
        l1.setName("location1");
        l2.setName("location2");
        birdSurveyLocation1.setName("location1");
        birdSurveyLocation2.setName("location2");
        birdSurveyLocation3.setName("location3");

        //create surveys
        Survey frogSurvey = new Survey();
        Survey birdAndFrogSurvey = new Survey();

        frogSurvey.setActive(true);
        birdAndFrogSurvey.setActive(true);
        frogSurvey.setName("frogSurvey");
        birdAndFrogSurvey.setName("birdAndFrogSurvey");
        frogSurvey.setDescription("This is a frog survey used for testing");
        birdAndFrogSurvey.setDescription("This is a birdsAndFrogs survey used for testing");
        frogSurvey.setUsers(users);
        birdAndFrogSurvey.setUsers(users);
        //add attributes
        frogSurvey.setAttributes(attributes);
        birdAndFrogSurvey.setAttributes(birdAndFrogAttributes);
        //add locations
        locations.add(locationDAO.save(l1));
        locations.add(locationDAO.save(l2));
        frogSurvey.setLocations(locations);
        birdSurveyLocations.add(locationDAO.save(birdSurveyLocation1));
        birdSurveyLocations.add(locationDAO.save(birdSurveyLocation2));
        birdSurveyLocations.add(locationDAO.save(birdSurveyLocation3));
        birdAndFrogSurvey.setLocations(birdSurveyLocations);
        //add species
        frogSurvey.setSpecies(frogSet);
        birdAndFrogSurvey.setSpecies(birdSet);
        birdAndFrogSurvey.getSpecies().addAll(frogSet);

        Metadata md = frogSurvey.setFormRendererType(SurveyFormRendererType.DEFAULT);
        metadataDAO.save(md);

        //save the survey
        frogSurveyInDb = surveyDAO.save(frogSurvey);
        birdAndFrogSurveyInDb = surveyDAO.save(birdAndFrogSurvey);

        geoMapService.getForSurvey(frogSurveyInDb);

        allSurvey = new Survey();
        allSurvey.setActive(true);
        allSurvey.setName("All survey");
        allSurvey.setDescription("This survey has all the species");
        surveyDAO.save(allSurvey);
    }

    @Test
    public void testGetGroupsForSurvey() {

        List<TaxonGroup> allGroups = taxaDAO.getAllTaxonGroups(allSurvey);
        Assert.assertEquals("wrong group count", 3, allGroups.size());
        Assert.assertTrue("missing group", allGroups.contains(birdTaxon));
        Assert.assertTrue("missing group", allGroups.contains(frogTaxon));
        Assert.assertTrue("missing group", allGroups.contains(allGroup));

        List<TaxonGroup> frogGroups = taxaDAO.getAllTaxonGroups(frogSurveyInDb);
        Assert.assertEquals("wrong group count", 2, frogGroups.size());
        Assert.assertTrue("missing group", frogGroups.contains(frogTaxon));
        Assert.assertTrue("missing group", frogGroups.contains(allGroup));
    }
}
