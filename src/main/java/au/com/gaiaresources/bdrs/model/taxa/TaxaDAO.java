package au.com.gaiaresources.bdrs.model.taxa;

import au.com.gaiaresources.bdrs.db.ScrollableResults;
import au.com.gaiaresources.bdrs.db.TransactionDAO;
import au.com.gaiaresources.bdrs.db.impl.PagedQueryResult;
import au.com.gaiaresources.bdrs.db.impl.PaginationFilter;
import au.com.gaiaresources.bdrs.model.region.Region;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.util.Pair;
import org.apache.lucene.queryParser.ParseException;
import org.hibernate.Session;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface TaxaDAO extends TransactionDAO {

    /** Allows the client to qualify the desired behaviour when searching by taxon group */
    static enum TaxonGroupSearchType {PRIMARY, SECONDARY, PRIMARY_OR_SECONDARY};

    TaxonGroup createTaxonGroup(String name, boolean includeBehaviour, boolean includeFirstAppearance,
                                boolean includeLastAppearance, boolean includeHabitat, boolean includeWeather,
                                boolean includeNumber);

    TaxonGroup createTaxonGroup(String name, boolean includeBehaviour,
            boolean includeFirstAppearance, boolean includeLastAppearance,
            boolean includeHabitat, boolean includeWeather,
            boolean includeNumber, String image, String thumbNail);

    TaxonGroup updateTaxonGroup(Integer id, String name, boolean includeBehaviour, boolean includeFirstAppearance,
                                boolean includeLastAppearance, boolean includeHabitat, boolean includeWeather,
                                boolean includeNumber);
    TaxonGroup updateTaxonGroup(Integer id, String name, boolean includeBehaviour, boolean includeFirstAppearance,
            boolean includeLastAppearance, boolean includeHabitat, boolean includeWeather,
            boolean includeNumber, String image, String thumbnail);

    TaxonGroup getTaxonGroup(String name);

    TaxonGroup getTaxonGroup(Integer id);

    List<TaxonGroup> getTaxonGroup(Survey survey);

    /**
     * Returns all taxon groups in alphabetical order.
     * Will exclude the 'Field Names' taxon group.
     * @return
     */
    List<? extends TaxonGroup> getTaxonGroups();

    /**
     * Create a <code>TaxonGroupAttribute</code>.
     * @param group <code>TaxonGroup</code> to which the attribute will belong.
     * @param name The name of the attribute.
     * @param type The data type of the attribute.
     * @param required Is this attribute required.
     * @return <code>TaxonGroupAttribute</code>.
     */
    Attribute createAttribute(TaxonGroup group, String name, AttributeType type, boolean required);
    Attribute createAttribute(TaxonGroup group, String name, AttributeType type, boolean required, boolean isTag);
    Attribute createAttribute(TaxonGroup group, String name, String description, AttributeType type, boolean required, boolean isTag);

    /**
     * Update a <code>TaxonGroupAttribute</code>.
     * @param id The id if the attribute to update.
     * @param name The new name of the attribute.
     * @param type The new data type of the attribute.
     * @param required Is this attribute required.
     * @return <code>TaxonGroupAttribute</code>.
     */
    Attribute updateAttribute(Integer id, String name, AttributeType type, boolean required);
    /**
     * Update a <code>TaxonGroupAttribute</code>.
     * @param id The id if the attribute to update.
     * @param name The new name of the attribute.
     * @param description The new description of the attribute.
     * @param type The new data type of the attribute.
     * @param required Is this attribute required.
     * @return <code>TaxonGroupAttribute</code>.
     */
    Attribute updateAttribute(Integer id, String name, String description, AttributeType type, boolean required);

    /**
     * Create a TaxonGroupAttributeOption, usually an option for type string_with_valid_options
     * @param attribute the attribute to attach this option to
     * @param option the value for the option
     * @return <code>TaxonGroupAttributeOption</code>
     */
    AttributeOption createAttributeOption(Attribute attribute, String option);

    TypedAttributeValue createAttributeValue(IndicatorSpecies species, Attribute attr, String value);
    TypedAttributeValue createAttributeValue(IndicatorSpecies species, Attribute attr, String value, String desc);

    /**
     * Deletes a given option.
     * @param id the id of the <code>TaxonGroupAttributeOption</code>
     */
    void deleteTaxonGroupAttributeOption(Integer id);

    AttributeOption getOption(Integer id);

    /**
     * Get a taxon group attribute by id.
     * @param id <code>Integer</code>.
     * @return <code>TaxonGroupAttribute</code>.
     */
    Attribute getAttribute(Integer id);
    
    /**
     * Get a taxon group attribute by its values
     * @param taxonGroup
     * @param name
     * @param isTag
     * @return first attribute matching the parameters
     */
    Attribute getAttribute(TaxonGroup taxonGroup, String name, boolean isTag);

    IndicatorSpecies createIndicatorSpecies(String scientificName, String commonName, TaxonGroup taxonGroup,
                                            Collection<Region> regions, List<SpeciesProfile> infoItems);

    IndicatorSpecies updateIndicatorSpecies(Integer id,
            String scientificName, String commonName, TaxonGroup taxonGroup,
            Collection<Region> regions, List<SpeciesProfile> infoItems);
    
    IndicatorSpecies updateIndicatorSpecies(Integer id,
            String scientificName, String commonName, TaxonGroup taxonGroup,
            Collection<Region> regions, List<SpeciesProfile> infoItems, Set<AttributeValue> attributes);

    /**
     * Returns all of the species in the database
     * @return - List<IndicatorSpecies>
     */
    List<IndicatorSpecies> getIndicatorSpecies();

    List<IndicatorSpecies> getIndicatorSpeciesById(Integer[] pks);
    
    List<IndicatorSpecies> getIndicatorSpeciesBySpeciesProfileItem(String type, String content);

    List<IndicatorSpecies> getIndicatorSpecies(Region region);

    List<IndicatorSpecies> getIndicatorSpecies(TaxonGroup group);

    /**
     * Search for the indicator species by scientific name or common name. 
     * 
     * Search is case insensitive.
     * 
     * The name is split by spaces and has
     * wild cards added. e.g. 'tib crac' will resolve to 'tibius cracticus'.
     * 
     * Multiple entries may be returned.
     * 
     * @param name Name to search for.
     * @param includeFieldSpecies If true will return the special 'Field Species' in the query.
     * @return List of indicator species that match the search string.
     */
    List<IndicatorSpecies> getIndicatorSpeciesByNameSearch(String name, boolean includeFieldSpecies);
    
    /**
	 * Search for the indicator species by scientific name or common name. 
     * 
     * Search is case insensitive.
     * 
     * The name is split by spaces and has
     * wild cards added. e.g. 'tib crac' will resolve to 'tibius cracticus'.
     * 
     * Multiple entries may be returned.
     * 
     * @param name Name to search for.
     * @return List of indicator species that match the search string.
     */
    List<IndicatorSpecies> getIndicatorSpeciesByNameSearchExact(String name);
    
    /**
     * Queries taxa with a query build up using a taxongroup id or query parameter 'AND' a sub-query parameter if available.
     * 
     * @param groupId the id of a taxongroup
     * @param searchInGroups the query parameter
     * @param searchInResult the sub-query parameter
     * @param filter the PaginationFilter
     * @return a PagedQueryResult of IndicatorSpecies
     * 
     * @deprecated Use searchTaxa() instead.
     */
    PagedQueryResult<IndicatorSpecies> getIndicatorSpeciesByQueryString(Integer groupId, String searchInGroups, String searchInResult, PaginationFilter filter);

    /**
     * Gets indicator species by id.
     * @param id ID of the species.
     * @return The IndicatorSpecies result.
     */
    IndicatorSpecies getIndicatorSpecies(Integer id);
    
    /**
     * Gets species profile by id
     * @param id ID of the species profile.
     * @return The SpeciesProfile result. 
     */
    SpeciesProfile getSpeciesProfileById(Integer id);
    
    /**
     * Returns the indicator species with the given GUID, or null if one could
     * not be found.
     * @param guid
     * @return
     */
    IndicatorSpecies getIndicatorSpeciesByGuid(String guid);

    /**
     * Gets IndicatorSpecies by common name.
     * @param commonName Common name to match on. This method does not add wildcards.
     * @return List of matching IndicatorSpecies.
     */
    List<IndicatorSpecies> getIndicatorSpeciesByCommonName(String commonName);

    /**
     * Performs a case insensitive exact match on the indicator species within one of the surveys with the
     * specified common name using the session provided.
     *
     * @param sesh the session to be used to perform the query.
     * @param surveys the candidate surveys containing the species.
     * @param commonName the common name of the indicator species to be retrieved.
     * @return the list of matching indicator species.
     */
    List<IndicatorSpecies> getIndicatorSpeciesByCommonName(Session sesh, Collection<Survey> surveys, String commonName);

    /**
     * Performs a case insensitive exact match on the indicator species within one of the surveys named with the
     * specified scientific name using the session provided.
     *
     * @param sesh the session to be used to perform the query.
     * @param surveys the candidate surveys containing the species.
     * @param scientificName the scientific name of the indicator species to be retrieved.
     * @return the list of matching indicator species.
     */
    List<IndicatorSpecies> getIndicatorSpeciesByScientificName(Session sesh, Collection<Survey> surveys, String scientificName);

    /**
     * Refreshes the IndicatorSpecies object
     * @param s IndicatorSpecies to refresh.
     * @return Refreshed IndicatorSpecies
     */
    IndicatorSpecies refresh(IndicatorSpecies s);

    /**
     * Get IndicatorSpecies by taxon group ID's.
     * @param taxonGroupIds TaxonGroup ID's to match on.
     * @return List of matching IndicatorSpecies.
     */
    List<IndicatorSpecies> getIndicatorSpecies(
            Integer[] taxonGroupIds);
    
    /**
     * Count IndicatorSpecies belonging to the specified TaxonGroups
     * @param taxonGroupIds TaxonGroup ID's to match on.
     * @return Count of matching IndicatorSpecies.
     */
    Integer countIndicatorSpecies(Integer[] taxonGroupIds);

    /**
     * Count all IndicatorSpecies.
     * @return Count of all IndicatorSpecies.
     */
    Integer countAllSpecies();

    /**
     * Get IndicatorSpecies by scientific name. Search is case insensitive.
     * Method does not add wildcards.
     * @param scientificName Scientific name to search for. 
     * @return IndicatorSpecies result.
     */
    IndicatorSpecies getIndicatorSpeciesByScientificName(String scientificName);
    
    /**
     * Get IndicatorSpecies by scientific name and other parameters. Search is case insensitive.
     * Method does not add wildcards.
     * @param sesh Hibernate session.
     * @param source Source of IndicatorSpecies.
     * @param scientificName Scientific name of IndicatorSpecies.
     * @param rank Rank of IndicatorSpecies.
     * @param parentId ID of the parent of the IndicatorSpecies.
     * @return IndicatorSpecies result.
     */
    IndicatorSpecies getIndicatorSpeciesByScientificNameAndParent(Session sesh, String source, String scientificName, TaxonRank rank, Integer parentId);

    /**
     * Get IndicatorSpecies by scientific name. Search is case insensitive.
     * Method does not add wildcards.
     * @param sesh Hibernate session.
     * @param scientificName Scientific name to search for.
     * @return IndicatorSpecies result.
     */
    IndicatorSpecies getIndicatorSpeciesByScientificName(Session sesh,
            String scientificName);
    
    /**
     * Get IndicatorSpecies by scientific name and rank. Search is case insensitive.
     * Method does not add wildcards.
     * @param scientificName Scientific name to search for.
     * @param rank TaxonRank to search for.
     * @return IndicatorSpecies result.
     */
    IndicatorSpecies getIndicatorSpeciesByScientificNameAndRank(String scientificName, TaxonRank rank);
    
    /**
     * Get IndicatorSpecies by scientific name and rank. Search is case insensitive.
     * Method does not add wildcards.
     * @param sesh Hibernate session.
     * @param scientificName Scientific name to search for.
     * @param rank TaxonRank to search for.
     * @return IndicatorSpecies result.
     */
    IndicatorSpecies getIndicatorSpeciesByScientificNameAndRank(Session sesh,
            String scientificName, TaxonRank rank);

    /**
     * Get taxon groups by name. Search is case insensitive.
     * Method adds wildcards on the beginning and end of nameFragment.
     * @param nameFragment Name fragment to search for.
     * @return List of matching TaxonGroups
     */
    List<TaxonGroup> getTaxonGroupSearch(String nameFragment);

    /**
     * Save an attribute.
     * @param attribute Attribute to save.
     * @return Saved Attribute.
     */
    Attribute save(Attribute attribute);
    
    /**
     * Save an IndicatorSpecies.
     * @param species IndicatorSpecies to save.
     * @return Saved IndicatorSpecies.
     */
    IndicatorSpecies save(IndicatorSpecies species);
    
    /**
     * Save an IndicatorSpecies.
     * @param sesh Hibernate session.
     * @param species IndicatorSpecies to save.
     * @return Saved IndicatorSpecies.
     */
    IndicatorSpecies save(Session sesh, IndicatorSpecies species);

    /**
     * Save an AttributeOption.
     * @param opt AttributeOption to save.
     * @return Saved AttributeOption.
     */
    AttributeOption save(AttributeOption opt);
    
    /**
     * Save a TaxonGroup.
     * @param taxongroup TaxonGroup to save.
     * @return Saved TaxonGroup.
     */
    TaxonGroup save(TaxonGroup taxongroup);

    /**
     * Update an IndicatorSpecies.
     * @param is IndicatorSpecies to update.
     * @return Updated IndicatorSpecies.
     */
    IndicatorSpecies updateIndicatorSpecies(IndicatorSpecies is);

    /**
     * Count the species assigned to a survey. Will return the same as
     * survey.getSpecies().size()
     * @param survey Survey to search for.
     * @return Count of species assigned to the survey.
     */
    int countSpeciesForSurvey(Survey survey);

    /**
     * Count the species associated with a survey. Takes into account the fact that
     * when survey.getSpecies().size() = 0, the survey actually has all species.
     * @param survey Survey to search for.
     * @param notTheseSurveys Exclude species from these surveys in the count. Nullable.
     * @return Count of species assigned to the survey.
     */
    int countActualSpeciesForSurvey(Survey survey, List<Survey> notTheseSurveys);
    
    /**
     * Get IndicatorSpecies by common name. Search is case sensitive.
     * Wild cards not allowed. Search requires exact match.
     * @param sesh Hibernate session.
     * @param commonName Common name to search for.
     * @return
     */
    IndicatorSpecies getIndicatorSpeciesByCommonName(Session sesh,
            String commonName);

    /**
     * Get taxon group by name. Search is case sensitive.
     * Wildcards not allowed. Search requires exact match.
     * @param sesh Hibernate session.
     * @param name TaxonGroup name to match.
     * @return TaxonGroup result.
     */
    TaxonGroup getTaxonGroup(Session sesh, String name);

    /**
     * Save a TaxonGroup.
     * @param sesh Hibernate session.
     * @param taxongroup TaxonGroup to save.
     * @return Saved TaxonGroup.
     */
    TaxonGroup save(Session sesh, TaxonGroup taxongroup);

    /**
     * Gets a list of IndicatorSpecies by source data id.
     * @param sesh Hibernate session.
     * @param source Data source.
     * @param sourceDataIdList the source data ids of the indicator species to be returned.
     * @return the matching list of indicator species.
     */
    List<IndicatorSpecies> getIndicatorSpeciesBySourceDataID(Session sesh, String source, List<String> sourceDataIdList);

    /**
     * Get IndicatorSpecies by source data id.
     * @param sesh Hibernate session.
     * @param sourceDataId Source data id.
     * @return IndicatorSpecies result.
     */
    IndicatorSpecies getIndicatorSpeciesBySourceDataID(Session sesh, String sourceDataId);
    
    /**
     * Get IndicatorSpecies by source data id.
     * @param sesh Hibernate session.
     * @param source Data source.
     * @param sourceDataId Source data id.
     * @return IndicatorSpecies result.
     */
    IndicatorSpecies getIndicatorSpeciesBySourceDataID(Session sesh, String source, String sourceDataId);
    
    /**
     * Returns the indicator species for the specified survey. If the survey
     * does not have any attached indicator species, this function will return
     * all indicator species.
     * @param sesh the session to use to retrieve the IndicatorSpecies
     * @param survey the survey associated with the indicator species.
     * @param start the first indicator species.
     * @param maxSize the maximum number of indicator species to return.
     * @return the indicator species associated with the specified survey.
     */
    List<IndicatorSpecies> getIndicatorSpeciesBySurvey(Session sesh, Survey survey, int start, int maxSize);
    
    /**
     * Returns the indicator species for the specified survey. If the survey
     * does not have any attached indicator species, this function will return
     * all indicator species.
     * @param sesh the session to use to retrieve the IndicatorSpecies
     * @param survey the survey associated with the indicator species.
     * @param start the first indicator species.
     * @param maxSize the maximum number of indicator species to return.
     * @param excludeSpeciesInSurveys exclude all species in the list of surveys, can be null.
     * @return the indicator species associated with the specified survey.
     */
    List<IndicatorSpecies> getIndicatorSpeciesBySurvey(Session sesh, Survey survey, int start, int maxSize, List<Survey> excludeSpeciesInSurveys);

    /**
     * Get IndicatorSpecies by scientific name. Case sensitive.
     * Wild cards not allowed. Requires exact match.
     * @param scientificName Scientific name to match.
     * @return List of matching IndicatorSpecies.
     */
    List<IndicatorSpecies> getIndicatorSpeciesListByScientificName(
            String scientificName);
    
    /**
     * Gets the most frequently used IndicatorSpecies in records.
     * @param userPk Records belong to this user ID.
     * @param limit Number of pair objects to return.
     * @return List of the top occuring IndicatorSpecies and the amount of times they occur.
     */
    List<Pair<IndicatorSpecies, Integer>> getTopSpecies(int userPk, int limit);

    /**
     * Get all taxon groups sorted by name.
     * Will exclude the 'Field Names' taxon group.
     * @return List of taxon groups sorted by name.
     */
    List<TaxonGroup> getTaxonGroupsSortedByName();

    /**
     * Get Paginated IndicatorSpecies
     * @param taxonGroup TaxonGroup to filter by.
     * @param filter Pagination details.
     * @return Paginated search result.
     */
    PagedQueryResult<IndicatorSpecies> getIndicatorSpecies(
            TaxonGroup taxonGroup, PaginationFilter filter);

    /**
     * Get the child taxa for an IndicatorSpecies.
     * @param taxon Parent IndicatorSpecies.
     * @return All child IndicatorSpecies.
     */
    List<IndicatorSpecies> getChildTaxa(IndicatorSpecies taxon);

    /**
     * Delete an IndicatorSpecies.
     * @param taxon IndicatorSpecies to delete.
     */
    void delete(IndicatorSpecies taxon);

    /**
     * Dlete a TaxonGroup.
     * @param taxonGroup TaxonGroup to delete.
     */
    void delete(TaxonGroup taxonGroup);
    
    /**
     * Returns a unique list of taxa that have been recorded in the specified 
     * survey.
     * @param surveyId the survey containing the records with the taxa to be
     * returned.
     * @return a list of unique taxa that have been recoreded in the specified
     * survey
     */
    List<IndicatorSpecies> getDistinctRecordedTaxaForSurvey(int surveyId);

    /**
     * Returns a List of IndicatorSpecies that:
     * <ol>
     *     <li>are in a Taxon Group that has a name containing the string supplied in the groupName parameter.</li>
     *     <li>have a common name or scientific name containing the sting supplied in the taxonName parameter.</li>
     * </ol>
     * The search is case insensitive.
     * @param groupName the search string to match against the Taxon Group name.
     * @param taxonName the search string to match against the Taxon scientific or common name.
     * @return a List of IndicatorSpecies that are in a group matching the supplied <code>groupName</code>
     */
    List<IndicatorSpecies> searchIndicatorSpeciesByGroupName(String groupName, String taxonName);

    /**
     * Returns a {@link PagedQueryResult} of {@link IndicatorSpecies} for the {@link TaxonGroup}
     * with groupId and matching the search terms: searchInGroups and searchInResult
     * @param groupId (optional) id of the {@link TaxonGroup} to search in.
     * @param groupSearchType Specifies how the groupId parameter is to be used.  Valid values are:
     *                        PRIMARY : matches taxa in the primary group = groupId.
     *                        SECONDARY :  matches taxa with groupId as one of the configured secondary groups.
     *                        PRIMARY_OR_SECONDARY : matches taxa in the the primary or secondary group specified by groupId
     * @param searchInGroups (optional) a string to search for
     * @param searchInResult (optional) a second string to search for once the results have been narrowed by searchInGroups
     * @param filter A {@link PaginationFilter} to apply to the query to implement paging
     * @return a {@link PagedQueryResult} of {@link IndicatorSpecies} for the {@link TaxonGroup}
     * with groupId and matching the search terms: searchInGroups and searchInResult
     * @throws ParseException if there is an error in the search terms
     */
    PagedQueryResult<IndicatorSpecies> searchTaxa(
            Integer groupId, TaxonGroupSearchType groupSearchType, String searchInGroups, String searchInResult, PaginationFilter filter) throws ParseException;

    /**
     * Updates the primary group of the taxa identified by the supplied list of ids.
     * Additionally, the TaxonGroup will no longer be a secondary group of any of the supplied taxa.
     * @param taxonids Identifies the IndicatorSpecies to update.
     * @param group The new primary TaxonGroup for the identified species.
     */
    void bulkUpdatePrimaryGroup(List<Integer> taxonids, TaxonGroup group);

    /**
     * Removes the supplied TaxonGroup from the list of configured secondary groups of the IndicatorSpecies identified
     * by the supplied list of ids.
     *
     * @param taxonIds Identifies the IndicatorSpecies to update.
     * @param group The TaxonGroup that will be removed as a secondary group from all of the IndicatorSpecies.
     */
    void bulkRemoveSecondaryGroup(List<Integer> taxonIds, TaxonGroup group);

    /**
     * Adds the supplied TaxonGroup to the list of configured secondary groups of the IndicatorSpecies identified
     * by the supplied list of ids.
     * IndicatorSpecies that have the supplied TaxonGroup as their primary group will not be updated.
     * @param taxonIds Identifies the IndicatorSpecies to update.
     * @param group The TaxonGroup that will be added as a secondary group to all of the IndicatorSpecies.
     */
    void bulkAssignSecondaryGroup(List<Integer> taxonIds, TaxonGroup group);
    
    /**
     * Get all of the species contained in a record. This includes the species record property
     * and any populated attribute values containing species. 
     * 
     * @param sesh session to execute query in. can be null.
     * @param recId id of the record to search inside.
     * @return distinct list of indicator species contained in the record.
     */
    List<IndicatorSpecies> getSpeciesForRecord(Session sesh, Integer recId);
    
    /**
     * Get all the species in the database as a scrollable results
     *
     * @param sesh Hibernate session to use. Can be null. 
     * @return Scrollable Results
     */
    ScrollableResults<IndicatorSpecies> getAll(Session sesh);
    
    /**
     * Return a single IndicatorSpecies that exactly matches a scientific name
     * and that the Survey is allowed to assign.
     * 
     * @param sesh Hibernate session. Nullable.
     * @param s Survey. cannot be null.
     * @param sciName Sci name, cannot be null, cannot be empty.
     * @return A single species that matches the search parameters.
     */
    IndicatorSpecies getSpeciesForSurvey(Session sesh, Survey s, String sciName);

    /**
     * Gets all primary AND secondary groups for a survey
     * @param survey survey to search in
     * @return list of unique taxon groups
     */
    List<TaxonGroup> getAllTaxonGroups(Survey survey);
}
