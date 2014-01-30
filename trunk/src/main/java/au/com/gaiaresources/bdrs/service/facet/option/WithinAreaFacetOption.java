package au.com.gaiaresources.bdrs.service.facet.option;

import au.com.gaiaresources.bdrs.db.impl.Predicate;
import au.com.gaiaresources.bdrs.model.survey.BdrsCoordReferenceSystem;
import au.com.gaiaresources.bdrs.util.SpatialUtil;
import au.com.gaiaresources.bdrs.util.SpatialUtilFactory;
import au.com.gaiaresources.bdrs.util.StringUtils;
import com.vividsolutions.jts.geom.Geometry;

/**
 * User: serge
 * Date: 23/12/13
 * Time: 9:18 AM
 */
public class WithinAreaFacetOption extends FacetOption {

    public static final WithinAreaFacetOption NO_AREA = new WithinAreaFacetOption("All", "", false);

    private final String area;

    /**
     * @param label                                                                                s
     * @param area should be a WKT encoded Geometry string in SRID 4326.
     */
    public WithinAreaFacetOption(String label, String area) {
        this(label, area, true);
    }

    public WithinAreaFacetOption(String label, String area, boolean visible) {
        super(label, area, 0L, false);
        super.setVisible(visible);
        this.area = area;
        this.id = "area";
    }

    @Override
    public Predicate getPredicate() {
        Predicate result = null;
        if (StringUtils.notEmpty(area)) {
            SpatialUtil spatialUtil = new SpatialUtilFactory().getLocationUtil();
            Geometry geometry = spatialUtil.createGeometryFromWKT(area);
            int srid = BdrsCoordReferenceSystem.DEFAULT_SRID;
            result = new Predicate("st_within(st_transform(record.geometry," + srid + "), ?) = True", geometry);
        }
        return result;
    }
}
