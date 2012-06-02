package au.com.gaiaresources.bdrs.controller.record.validator;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import au.com.gaiaresources.bdrs.service.property.PropertyService;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;
import au.com.gaiaresources.bdrs.model.taxa.TaxaDAO;

/**
 * Validates that the input exactly matches, including case, the scientific name
 * of a taxon.
 */
public class TaxonValidator extends AbstractValidator {

	/**
	 * Message for could not find unique match.
	 */
	public static final String TAXON_MESSAGE_KEY = "TaxonValidator.taxon";
	public static final String TAXON_MESSAGE = "Must exactly match a valid scientific name including case e.g Homo Sapien";

	/**
	 * Message for could not find unique match.
	 */
	public static final String TAXON_OR_BLANK_MESSAGE_KEY = "TaxonValidator.taxonOrBlank";
	public static final String TAXON_OR_BLANK_MESSAGE = "Must be a valid scientific name including case (e.g Homo sapien) or blank";

	/**
	 * Message for invalid taxon id - could not parse string to int.
	 */
	public static final String TAXON_INVALID_ID_MESSAGE_KEY = "TaxonValidator.invalidTaxonId";
	/**
	 * Message for could not find indicator_species in database for the given id.
	 */
	public static final String TAXON_COULD_NOT_RETRIEVE_MESSAGE_KEY =  "TaxonValidator.couldNotRetrieveSpeciesForId";
	
	/**
	 * Message for taxon is not allowed in survey.
	 */
	public static final String TAXON_INVALID_FOR_SURVEY_MESSAGE_KEY = "TaxonValidator.invalidForSurvey";

	private TaxaDAO taxaDAO;
	
	private Survey survey;

	private Logger log = Logger.getLogger(getClass());

	/**
	 * Creates a new <code>TaxonValidator</code>.
	 * 
	 * @param propertyService
	 *            used to access configurable messages displayed to the user.
	 * @param required
	 *            true if the input is mandatory, false otherwise.
	 * @param blank
	 *            true if the value can be an empty string, false otherwise.
	 * @param taxaDAO
	 *            used to access stored taxonomy.
	 * @param survey
	 *            survey (can be null) that defines what taxa are allowed.
	 */
	public TaxonValidator(PropertyService propertyService, boolean required,
			boolean blank, TaxaDAO taxaDAO, Survey survey) {
		super(propertyService, required, blank);
		this.taxaDAO = taxaDAO;
		this.survey = survey;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean validate(Map<String, String[]> parameterMap, String key,
			Attribute attribute, Map<String, String> errorMap) {
		boolean isValid = super
				.validate(parameterMap, key, attribute, errorMap);

		if (isValid) {
			String[] values = parameterMap.get(key);
			if (values != null) {
				// indicator_species_name should be in the param map at index 0
				// indicator_species_id has been included in the param map at index 1
				String speciesName = values[0];
				speciesName = speciesName != null ? speciesName.trim() : "";
				String speciesId = null;
				if (values.length > 1) {
					speciesId = values[1];
				}
				speciesId = speciesId != null ? speciesId.trim() : "";
				
				if (!speciesId.isEmpty()) {
					Integer id = null;
					try {
						id = Integer.valueOf(speciesId);
						IndicatorSpecies species = taxaDAO
								.getIndicatorSpecies(id);
						if (species == null) {
							errorMap.put(key, propertyService
									.getMessage(TAXON_COULD_NOT_RETRIEVE_MESSAGE_KEY));
						} else {
							if (!isValidSurveySpecies(species)) {
								errorMap.put(key, propertyService.getMessage(TAXON_INVALID_FOR_SURVEY_MESSAGE_KEY));
							}
						}
					} catch (NumberFormatException e) {
						errorMap.put(key, propertyService
								.getMessage(TAXON_INVALID_ID_MESSAGE_KEY));
					}
				} else if (!speciesName.isEmpty()) {
					List<IndicatorSpecies> taxaList = taxaDAO
							.getIndicatorSpeciesByNameSearchExact(speciesName);
					if (taxaList.isEmpty() || taxaList.size() > 1) {
						if (blank) {
							errorMap.put(key, propertyService.getMessage(
									TAXON_OR_BLANK_MESSAGE_KEY,
									TAXON_OR_BLANK_MESSAGE));
						} else {
							errorMap.put(key, propertyService.getMessage(
									TAXON_MESSAGE_KEY, TAXON_MESSAGE));
						}
					} else {
						if (!isValidSurveySpecies(taxaList.get(0))) {
							errorMap.put(key, propertyService.getMessage(TAXON_INVALID_FOR_SURVEY_MESSAGE_KEY));
						}
					}
				} 
			}
		}
		return isValid && !errorMap.containsKey(key);
	}
	
    private boolean isValidSurveySpecies(IndicatorSpecies species) {
    	return survey == null || survey.getSpecies().isEmpty() ||
    		survey.getSpecies().contains(species);
    }
}
