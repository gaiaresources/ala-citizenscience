package au.com.gaiaresources.bdrs.controller.record;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import au.com.gaiaresources.bdrs.deserialization.record.RecordEntry;

import com.vividsolutions.jts.geom.Geometry;

public class TrackerFormToRecordEntryTransformer {
    
    public List<RecordEntry> httpRequestParamToRecordMap(Map<String, String[]> paramMap, Map<String, MultipartFile> fileMap) {
        
        List<RecordEntry> result = new ArrayList<RecordEntry>();
        RecordEntry entry = new RecordEntry(paramMap, fileMap);
        result.add(entry);
        return result;
    }
}
