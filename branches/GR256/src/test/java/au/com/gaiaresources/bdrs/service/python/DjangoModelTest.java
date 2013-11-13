package au.com.gaiaresources.bdrs.service.python;

import au.com.gaiaresources.bdrs.controller.AbstractGridControllerTest;
import au.com.gaiaresources.bdrs.controller.report.ReportController;
import au.com.gaiaresources.bdrs.controller.report.ReportTestUtil;
import au.com.gaiaresources.bdrs.db.impl.PersistentImpl;
import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.report.Report;
import au.com.gaiaresources.bdrs.model.report.ReportDAO;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.util.SpatialUtil;
import au.com.gaiaresources.bdrs.util.SpatialUtilFactory;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTWriter;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.type.Type;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartHttpServletRequest;

import javax.persistence.*;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Aims to test the integration between the BDRS and Django.
 *
 * Note that this test currently does not test table relationships. This is something to needs to be tested
 * in the future. For future developers please consider the various kinds of relationships, many to many, one to many,
 * one to one and any variant that involves a position argument (for ordered lists or arrays). Also consider that
 * Hibernate stores the "many" side of a one to many relationship on the "many" side of the relation while Django
 * stores the relation on the "one" side. That is to say, in Hibernate, an IndicatorSpecies has many SpeciesProfiles
 * where in Django a SpeciesProfile is a fk to an IndicatorSpecies.
 */
public class DjangoModelTest extends AbstractGridControllerTest {

    public static final String DJANGO_INTEGRATION_TEST_REPORT_NAME = "Django Integration Test Report";

    @Autowired
    private ReportDAO reportDAO;

    private Report report;

    @Before
    public void setup() throws Exception {
        super.requestDropDatabase();
        super.abstractGridControllerTestSetup();

        login("admin", "password", new String[]{Role.ADMIN});

        File reportDir;
        URL url = PythonService.class.getResource("DjangoIntegrationTestReport");
        try {
            reportDir = new File(url.toURI());
        } catch (URISyntaxException e) {
            reportDir = new File(url.getPath());
        }
        addReport(reportDir, DJANGO_INTEGRATION_TEST_REPORT_NAME);

        // Commit changes (so they are visible to the django side) and create a new transaction.
        sessionFactory.getCurrentSession().getTransaction().commit();
        sessionFactory.getCurrentSession().beginTransaction();
        getRequestContext().setHibernate(sessionFactory.getCurrentSession());

        login("admin", "password", new String[]{Role.ADMIN});
        report = ReportTestUtil.getReportByName(reportDAO, DJANGO_INTEGRATION_TEST_REPORT_NAME);
    }

    /**
     * Adds the report at the specified location to the system.
     *
     * @param reportDir      the directory containing the report to be added.
     * @param testReportName the filename of the report to be uploaded.
     * @throws Exception thrown if an error has occurred.
     */
    protected void addReport(File reportDir, String testReportName) throws Exception {
        // Upload the Report
        request = createMockHttpServletRequest();
        login("admin", "password", new String[]{Role.ADMIN});

        request.setMethod("POST");
        request.setRequestURI(ReportController.REPORT_ADD_URL);

        MockMultipartHttpServletRequest req = (MockMultipartHttpServletRequest) request;
        req.addFile(ReportTestUtil.getTestReport(reportDir, testReportName));

        handle(request, response);
        junit.framework.Assert.assertFalse(reportDAO.getReports().isEmpty());

        junit.framework.Assert.assertEquals(1, getRequestContext().getMessageContents().size());
    }

    /**
     * Tests the database table structure of each model and the data content of each instance.
     */
    @Test
    public void testDjangoIntegration() throws Exception {
        Session sesh = getSession();
        DjangoModelFacadeFactory facadeFactory = new DjangoModelFacadeFactory(sesh, new DjangoModelFacadeInitHandler() {
            @Override
            public JSONObject getModelData(String entityName) throws Exception {
                return getDjangoModel(entityName);
            }
        });

        Map<String, ClassMetadata> classMetadataMap = sesh.getSessionFactory().getAllClassMetadata();
        for (Map.Entry<String, ClassMetadata> entry : classMetadataMap.entrySet()) {
            ClassMetadata md = entry.getValue();
            validateColumns(facadeFactory, md);
            validateData(facadeFactory, md);
        }
    }

    /**
     * Validates the content of each instance.
     * @param facadeFactory maintains a mapping of each django model to the hibernate entity.
     * @param md the hibernate entity to be tested
     */
    private void validateData(DjangoModelFacadeFactory facadeFactory, ClassMetadata md) throws Exception {
        Query query = getSession().createQuery(String.format("from %s", md.getEntityName()));
        Class<?> mappedClass = md.getMappedClass(getSession().getEntityMode());
        DjangoModelFacade djangoModelFacade = facadeFactory.getDjangoModelFacade(mappedClass);
        BeanPropertyUtil propertyDescriptorMapping = new BeanPropertyUtil(mappedClass);
        SpatialUtil spatialUtil = new SpatialUtilFactory().getLocationUtil();
        WKTWriter wktWriter = new WKTWriter();
        for(Object obj : query.list()) {
            // Identifier
            PropertyDescriptor pd = propertyDescriptorMapping.getPropertyDescriptor(md.getIdentifierPropertyName());
            Integer pk = (Integer)pd.getReadMethod().invoke(obj);
            JSONObject djangoObject = djangoModelFacade.getObject(pk.toString());
            Assert.assertNotNull(djangoObject);

            // All other persistent properties
            String[] propertyNameArray = md.getPropertyNames();
            for(int i=0; i<propertyNameArray.length; i++) {
                String propertyName = propertyNameArray[i];
                Type propertyType = md.getPropertyType(propertyName);
                pd = propertyDescriptorMapping.getPropertyDescriptor(propertyName);

                if(!propertyType.isCollectionType() && !isReverseOwnerOneToOne(pd)) {
                    Class<?> returnType = pd.getReadMethod().getReturnType();
                    String hibernateColumnName = getColumnName(mappedClass, pd).toLowerCase();

                    Object djangoValue = djangoObject.isNull(hibernateColumnName) ? null : djangoObject.get(hibernateColumnName);
                    Object hibernateValue = pd.getReadMethod().invoke(obj);

                    if(Date.class.equals(returnType) && djangoValue != null) {
                        djangoValue = new Date((Long) djangoValue);
                    } else if(Integer.class.equals(returnType) && (djangoValue instanceof Long) && (djangoValue != null)) {
                        djangoValue = new Integer(djangoValue.toString());
                    } else if((hibernateValue instanceof PersistentImpl) && (djangoValue instanceof Long)) {
                        hibernateValue = ((PersistentImpl)hibernateValue).getId();
                        djangoValue = new Integer(djangoValue.toString());
                    } else if(returnType.isEnum() && hibernateValue != null) {
                        hibernateValue = hibernateValue.toString();
                    } else if(hibernateValue instanceof BigDecimal) {
                        // Do a string comparison of decimal values to get the necessary precision
                        hibernateValue = hibernateValue.toString();
                    } else if(Geometry.class.isAssignableFrom(returnType) &&
                            (djangoValue != null) && (hibernateValue != null)) {
                        hibernateValue = wktWriter.write((Geometry)hibernateValue);
                        // We need to convert the django WKT to a geometry first to work around issues like
                        // the django outputting POINT(1.00000, 1.00000) that is not equal to POINT(1 1)
                        // It is not enough to merely leave them both as Geometries because the equals method
                        // in geometry has not been overridden and it will flag them as different. This is
                        // by design of the Vivid Solutions developer. See the javadocs for Geometry.
                        Geometry djangoGeom = spatialUtil.createGeometryFromWKT(djangoValue.toString());
                        djangoValue = wktWriter.write(djangoGeom);
                    }

                    Assert.assertEquals(djangoValue, hibernateValue);
                }
            }
        }
    }

    /**
     * Validates the database column structure (name) of each hibernate entity.
     * @param facadeFactory maintains a mapping of each django model to the hibernate entity.
     * @param md the hibernate entity to be tested
     */
    private void validateColumns(DjangoModelFacadeFactory facadeFactory, ClassMetadata md) throws Exception {
        Class mappedClass = md.getMappedClass(getSession().getEntityMode());
        DjangoModelFacade djangoModelFacade = facadeFactory.getDjangoModelFacade(mappedClass);
        List<String> djangoColumnNames = djangoModelFacade.getColumnNames();

        BeanPropertyUtil propertyDescriptorMapping = new BeanPropertyUtil(mappedClass);

        // The identifier
        PropertyDescriptor pd = propertyDescriptorMapping.getPropertyDescriptor(md.getIdentifierPropertyName());
        String hibernateColumnName = getColumnName(mappedClass, pd).toLowerCase();
        String message = String.format("Failed to find field with column name %s in class %s",
                hibernateColumnName, mappedClass.getName());
        Assert.assertTrue(message, djangoColumnNames.remove(hibernateColumnName));

        // All other persistent properties
        String[] propertyNameArray = md.getPropertyNames();
        for(int i=0; i<propertyNameArray.length; i++) {
            String propertyName = propertyNameArray[i];
            Type propertyType = md.getPropertyType(propertyName);
            pd = propertyDescriptorMapping.getPropertyDescriptor(propertyName);

            // System.out.println(String.format("Testing %s for property name %s", mappedClass.getName(), propertyName));
            if(!propertyType.isCollectionType() && !isReverseOwnerOneToOne(pd)) {
                String colName = getColumnName(mappedClass, pd);
                Assert.assertNotNull(String.format("colName null for prop name %s in class %s", propertyName,
                        mappedClass.getName()), colName);
                hibernateColumnName = colName.toLowerCase();
                message = String.format("Failed to find field with column name %s in class %s",
                        hibernateColumnName, mappedClass.getName());
                Assert.assertTrue(message, djangoColumnNames.remove(hibernateColumnName));
            }
        }

        // Since we are not checking collection types, there may be extra django columns in the list.
    }

    /**
     * Invokes the Django test report to retrieve a JSONObject containing the column names and a data dump of
     * each instance of that object.
     * @param djangoModelName the name of the model/entity (as seen by django) to be retrieved.
     * @return a JSONObject containing the column names and a data dump of
     * each instance of that object.
     */
    private JSONObject getDjangoModel(String djangoModelName) throws Exception {
        request = createStandardRequest();
        response = new MockHttpServletResponse();
        request.setMethod("GET");
        request.setRequestURI(ReportTestUtil.getReportRenderURL(report));
        request.setParameter("model_name", djangoModelName);

        handle(request, response);

        return JSONObject.fromStringToJSONObject(response.getContentAsString());
    }

    /**
     * Returns the column name for the specified property.
     *
     * @param entityClass the class that owns the property.
     * @param pd describes the property to be introspected.
     * @return the name of the column for the specified property.
     */
    private String getColumnName(Class<?> entityClass, PropertyDescriptor pd) throws Exception {
        if (pd == null) {
            return null;
        }

        AttributeOverrides attrOverrides = entityClass.getAnnotation(AttributeOverrides.class);
        if (attrOverrides != null) {
            for (AttributeOverride attrOverride : attrOverrides.value()) {
                if (attrOverride.name().equals(pd.getName())) {
                    return attrOverride.column().name().toLowerCase();
                }
            }
        }

        AttributeOverride attrOverride = entityClass.getAnnotation(AttributeOverride.class);
        if (attrOverride != null && attrOverride.name().equals(pd.getName())) {
            return attrOverride.column().name().toLowerCase();
        }

        Method readMethod = pd.getReadMethod();
        if (readMethod == null) {
            return null;
        }

        Column columnAnnotation = readMethod.getAnnotation(Column.class);
        if (columnAnnotation != null) {
            return columnAnnotation.name().toLowerCase();
        }

        JoinColumn joinColumnAnnotation = readMethod.getAnnotation(JoinColumn.class);
        if (joinColumnAnnotation != null) {
            return joinColumnAnnotation.name().toLowerCase();
        }

        OneToOne oneToOneAnnotation = readMethod.getAnnotation(OneToOne.class);
        // Mapped By must be null otherwise its owned by the other side of the relation
        if (oneToOneAnnotation != null && !isReverseOwnerOneToOne(pd)) {
            Class<?> otherType = readMethod.getReturnType();
            PropertyDescriptor otherPD = new BeanPropertyUtil(otherType).getPropertyDescriptor("id");
            return String.format("%s_%s", pd.getName().toLowerCase(), getColumnName(otherType, otherPD));
        }
        return null;
    }

    /**
     * True if this property is a one to one relationship where the owner of the relationship is the
     * other end of the relation, as specified by the presence of the mappedBy attribute.
     *
     * @param pd the descriptor of the property.
     * @return true if this is a one to one relationship owned by the other party.
     * @see javax.persistence.OneToOne#mappedBy()
     */
    private boolean isReverseOwnerOneToOne(PropertyDescriptor pd) {
        Method readMethod = pd.getReadMethod();
        OneToOne oneToOneAnnotation = readMethod.getAnnotation(OneToOne.class);
        return oneToOneAnnotation != null && oneToOneAnnotation.mappedBy() != null;
    }

    protected MockHttpServletRequest createMockHttpServletRequest() {
        return createUploadRequest();
    }
}
