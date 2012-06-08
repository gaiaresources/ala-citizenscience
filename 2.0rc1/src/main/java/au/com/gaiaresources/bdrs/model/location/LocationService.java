package au.com.gaiaresources.bdrs.model.location;

import java.math.BigDecimal;
import java.util.Set;

import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.user.User;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public interface LocationService {

    public GeometryFactory getGeometryFactory();

    public Point createPoint(BigDecimal locationLatitude,
            BigDecimal locationLongitude);

    public Point createPoint(double latitude, double longitude);

    public Geometry createGeometryFromWKT(String wktString);
    
    public Geometry convertToMultiGeom(Geometry geom);
    
    public double truncate(double x);

    /**
     * Returns the set of locations available for selection when recording data using the supplied survey.
     * The Locations configured for the Survey are combined with any User defined locations (if the survey allows it).
     * @param survey the Survey in question.
     * @param user the User that will be filling out / editing the Survey.
     * @return a Set of Location objects that are valid locations for the supplied Survey.
     */
    public Set<Location> locationsForSurvey(Survey survey, User user);
}
