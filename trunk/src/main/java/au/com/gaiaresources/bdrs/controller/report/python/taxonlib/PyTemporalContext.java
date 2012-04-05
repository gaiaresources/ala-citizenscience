package au.com.gaiaresources.bdrs.controller.report.python.taxonlib;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import au.com.gaiaresources.bdrs.db.impl.PersistentImpl;
import au.com.gaiaresources.bdrs.json.JSON;
import au.com.gaiaresources.bdrs.json.JSONArray;
import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.util.BeanUtils;
import au.com.gaiaresources.taxonlib.ITemporalContext;
import au.com.gaiaresources.taxonlib.PagedQueryResult;
import au.com.gaiaresources.taxonlib.PaginationFilter;
import au.com.gaiaresources.taxonlib.model.IPersistent;
import au.com.gaiaresources.taxonlib.model.ITaxonConcept;
import au.com.gaiaresources.taxonlib.model.ITaxonConceptJunction;

public class PyTemporalContext {

	private ITemporalContext context;
	
	public PyTemporalContext(ITemporalContext context) {
		this.context = context;
	}
	
	/**
	 * Get ITaxonConcept by Id
	 * @param conceptId
	 * @return Json representation of ITaxonConcept object
	 */
	public String getConceptById(int conceptId) {
		ITaxonConcept result = context.selectConcept(conceptId);
		return toJSON(result, 1).toString();
	}
	
	/**
	 * Selects junction by date
	 * 
	 * @param startDate start date as an ISO date string
	 * @param sndDate end date as an ISO date string
	 * @param offset offset to start pagination
	 * @param limit max number of paginated results
	 * @return Json array of ITaxonConceptJunction objects
	 * @throws ParseException 
	 */
	public String getJunctionByDate(String startDateString, String endDateString, int offset, int limit) throws ParseException {
		SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd");
		Date startDate = isoFormat.parse(startDateString);
		Date endDate = isoFormat.parse(endDateString);
		//List<ITaxonConceptJunction> resultList = context.selectJunctionByDateRange(startDate, endDate);
		PaginationFilter filter = new PaginationFilter(offset, limit);
		PagedQueryResult<ITaxonConceptJunction> pagedResult = context.selectJunctionByDateRange(startDate, endDate, filter);
		return toJSON(pagedResult, 2).toString();
	}
	
	/*
	 * Unused at the moment. Will be required if we ever jsonify collections.
	private static JSONArray toJSON(Collection list, int depth) {
		if (list instanceof Map) {
			// only list, arrays, sets are supported
			throw new IllegalStateException("Collection cannot be an instance of Map");
		}
        JSONArray array = new JSONArray();
        if(list != null) {
            for(Object item : list) {
                if(item != null) {
                    array.add(BeanUtils.flatten(IPersistent.class, item, depth));
                }
            }
        }
        return array;
    }
    */

    /**
     * JSON serializes a single {@link PersistentImpl} to a {@link JSONObject}.
     * 
     * @param obj the item to be serialized. 
     * @return a {@link JSONObject} representing the specified item.
     */
    private static JSON toJSON(Object obj, int depth) {
        if(obj == null) {
            return new JSONObject();
        } else {
            return JSONObject.fromMapToJSONObject(BeanUtils.flatten(IPersistent.class, obj, depth));
        }
    }
}
