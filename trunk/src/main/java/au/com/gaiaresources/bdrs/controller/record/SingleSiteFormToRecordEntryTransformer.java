package au.com.gaiaresources.bdrs.controller.record;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import au.com.gaiaresources.bdrs.deserialization.record.RecordEntry;

import com.vividsolutions.jts.geom.Geometry;

public class SingleSiteFormToRecordEntryTransformer {
    
    public List<RecordEntry> httpRequestParamToRecordMap(Map<String, String[]> paramMap, Map<String, MultipartFile> fileMap, String[] rowIds) {
        
        List<RecordEntry> result = new ArrayList<RecordEntry>();
        for (String recordPrefix : rowIds) {
            RecordEntry entry = new RecordEntry(paramMap, fileMap, recordPrefix);
            result.add(entry);
        }
        return result;
    }
}
