package au.com.gaiaresources.bdrs.model.taxa;

import org.hibernate.Session;

import au.com.gaiaresources.bdrs.db.TransactionDAO;

import java.util.List;

public interface SpeciesProfileDAO extends TransactionDAO {

    /**
     * Get species profile by primary key
     * @param id primary key
     * @return species profile is exists, otherwise null.
     */
    SpeciesProfile getById(Integer id);

    SpeciesProfile createSpeciesProfile(String header, String content,
            String type);

    SpeciesProfile createSpeciesProfile(String header, String description,
            String content, String type);

    List<SpeciesProfile> getSpeciesProfileForSpecies(int id);

    SpeciesProfile save(SpeciesProfile profile);

    SpeciesProfile save(Session sesh, SpeciesProfile profile);

    SpeciesProfile getSpeciesProfileBySourceDataId(Session sesh,
            IndicatorSpecies species, String sourceDataIdKey,
            String sourceDataId);

    void delete(SpeciesProfile delProf);

    List<SpeciesProfile> getSpeciesProfileByType(String[] types);

    List<SpeciesProfile> getSpeciesProfileByTypeAndContent(String type,
            String content);
    
}
