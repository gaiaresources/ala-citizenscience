package au.com.gaiaresources.bdrs.python.model;

import java.util.List;
import java.util.Map;

import au.com.gaiaresources.bdrs.json.JSONArray;
import au.com.gaiaresources.bdrs.model.taxa.SpeciesProfile;
import au.com.gaiaresources.bdrs.model.taxa.SpeciesProfileDAO;

public class PySpeciesProfileDAO extends AbstractPyDAO {

    private SpeciesProfileDAO spDAO;
    
    /**
     * Create a new wrapped DAO
     * @param spDAO
     */
    public PySpeciesProfileDAO(SpeciesProfileDAO spDAO) {
        this.spDAO = spDAO;
    }
    
    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.python.model.AbstractPyDAO#getById(int)
     */
    @Override
    public String getById(int pk) {
        return super.getById(spDAO, SpeciesProfile.class, pk);
    }
    
    /**
     * Get all of the species profiles for a indicator species pk.
     * @param pk indicator species primary key.
     * @return JSON list of species profile items.
     */
    public String getForSpecies(int pk) {
        List<SpeciesProfile> profileList = spDAO.getSpeciesProfileForSpecies(pk);
        JSONArray result = new JSONArray();
        for (SpeciesProfile sp : profileList) {
            Map<String, Object> spFlatten = sp.flatten();
            result.add(spFlatten);
        }
        return result.toString();
    }
}
