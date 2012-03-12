package au.com.gaiaresources.bdrs.controller.record;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import au.com.gaiaresources.bdrs.deserialization.record.RecordEntry;
import au.com.gaiaresources.bdrs.model.location.LocationService;

import com.vividsolutions.jts.geom.Geometry;

public class SingleSiteFormToRecordEntryTransformer {
    
    private LocationService locService;
    
    public SingleSiteFormToRecordEntryTransformer(LocationService locationService) {
        locService = locationService;
    }

    public List<RecordEntry> httpRequestParamToRecordMap(Map<String, String[]> paramMap, Map<String, MultipartFile> fileMap, String[] rowIds) {
        
        String[] wktParam = paramMap.get(TrackerController.PARAM_WKT);
        Geometry geom = null;
        if (wktParam != null && wktParam.length > 0 && StringUtils.hasLength(wktParam[0])) {
            geom = locService.createGeometryFromWKT(wktParam[0]);
        }
        
        List<RecordEntry> result = new ArrayList<RecordEntry>();
        for (String recordPrefix : rowIds) {
            if (StringUtils.hasLength(recordPrefix)) {
                RecordEntry entry = new RecordEntry(paramMap, fileMap, recordPrefix);
                entry.setGeometry(geom);
                result.add(entry);
            }
        }
        return result;
    }
}
