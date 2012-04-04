package au.com.gaiaresources.bdrs.service.taxonomy;

import java.util.Collection;
import java.util.List;

import junit.framework.Assert;

import au.com.gaiaresources.bdrs.model.taxa.SpeciesProfile;
import au.com.gaiaresources.taxonlib.ITemporalContext;
import au.com.gaiaresources.taxonlib.model.ITaxonConcept;
import au.com.gaiaresources.taxonlib.model.ITaxonConceptRelation;
import au.com.gaiaresources.taxonlib.model.ITaxonName;
import au.com.gaiaresources.taxonlib.model.StringSearchType;
import au.com.gaiaresources.taxonlib.model.TaxonConcept;
import au.com.gaiaresources.taxonlib.model.TaxonConceptRelation;
import au.com.gaiaresources.taxonlib.model.TaxonName;

/**
 * Helpers for testing. Most of these operate to extract a particular object out
 * of a list.
 *
 */
public class TaxonTestUtils {
	
	public static SpeciesProfile getProfileItemByType(List<SpeciesProfile> spList,
            String type) {
        for (SpeciesProfile sp : spList) {
            if (sp.getType().equals(type)) {
                return sp;
            }
        }
        return null;
    }

    public static void assertSpeciesProfileValue(List<SpeciesProfile> spList,
            String type, String expectedValue) {
        SpeciesProfile sp = getProfileItemByType(spList, type);
        Assert.assertNotNull("SpeciesProfile cannot be null", sp);
        Assert.assertEquals("wrong value", expectedValue, sp.getContent());
    }

    public static ITaxonConcept getTaxonConceptById(
            Collection<ITaxonConcept> tcList, Integer id) {
        nullIdCheck(id);
        for (ITaxonConcept tc : tcList) {
            if (tc.getId() != null) {
                if (tc.getId().intValue() == id) {
                    return tc;
                }
            }
        }
        return null;
    }

    public static ITaxonConcept getTaxonConceptBySourceId(
            Collection<ITaxonConcept> tcList, String sourceId) {
        nullIdCheck(sourceId);
        for (ITaxonConcept tc : tcList) {
            if (tc.getId() != null) {
                if (tc.getSourceId().equals(sourceId)) {
                    return tc;
                }
            }
        }
        return null;
    }

    public static ITaxonConceptRelation getTaxonConceptRelationById(
            Collection<ITaxonConceptRelation> tcrList, Integer id) {
        nullIdCheck(id);
        for (ITaxonConceptRelation tcr : tcrList) {
            if (tcr.getId() != null) {
                if (tcr.getId().intValue() == id) {
                    return tcr;
                }
            }
        }
        return null;
    }

    public static ITaxonName getTaxonNameById(Collection<ITaxonName> list,
            Integer id) {
        nullIdCheck(id);
        for (ITaxonName name : list) {
            if (name.getId() != null) {
                if (name.getId().intValue() == id) {
                    return name;
                }
            }
        }
        return null;
    }

    private static void nullIdCheck(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("Cannot search for a null id");
        }
    }

    private static void nullIdCheck(String id) {
        if (id == null) {
            throw new IllegalArgumentException(
                    "Cannot search for a null string id");
        }
    }
    
    public static ITaxonConcept searchUniqueByDisplayName(ITemporalContext ctxt, String displayName) {
        List<ITaxonConcept> list = ctxt.searchByDisplayName(displayName, null, StringSearchType.EXACT, null, null);
        if (list == null) {
            return null;
        }
        if (list.size() > 1) {
            throw new IllegalStateException("more than one returned");
        } else if (list.isEmpty()) {
            return null;
        } else {
            return list.get(0);
        }
    }
    
    public static ITaxonName getUniqueCommonName(ITemporalContext ctxt, ITaxonConcept tc) {
        List<ITaxonName> list = ctxt.getCommonNames(tc);
        if (list == null) {
            return null;
        }
        if (list.size() > 1) {
            throw new IllegalStateException("more than one returned");
        } else if (list.isEmpty()) {
            return null;
        } else {
            return list.get(0);
        }
    }
}
