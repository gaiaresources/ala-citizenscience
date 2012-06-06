package au.com.gaiaresources.bdrs.service.taxonomy;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import au.com.gaiaresources.bdrs.model.taxa.*;
import au.com.gaiaresources.bdrs.service.taxonomy.afd.SpeciesProfileBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;

import au.com.gaiaresources.taxonlib.ITemporalContext;
import au.com.gaiaresources.taxonlib.ITaxonLibSession;
import au.com.gaiaresources.taxonlib.importer.afd.AfdImporter;
import au.com.gaiaresources.taxonlib.importer.afd.AfdImporterRowHandler;
import au.com.gaiaresources.taxonlib.importer.afd.AfdRow;
import au.com.gaiaresources.taxonlib.model.ITaxonConcept;
import au.com.gaiaresources.taxonlib.model.ITaxonName;

public class BdrsAfdImporterRowHandler implements AfdImporterRowHandler {
    /**
     * Taxon group name
     */
    public static final String AFD_GROUP_NAME = "AFD";
    static final SpeciesProfileBuilder[] PROFILE_BUILDER = new SpeciesProfileBuilder[] {

        new SpeciesProfileBuilder(AfdRow.ColumnName.AUTHOR, "Author"),
        new SpeciesProfileBuilder(AfdRow.ColumnName.YEAR, "Year"),
        new SpeciesProfileBuilder(AfdRow.ColumnName.QUALIFICATION, "Qualification or Comments"),
        new SpeciesProfileBuilder(AfdRow.ColumnName.PUB_PUB_FORMATTED, "Publication"),

//        new SpeciesProfileBuilder(AfdRow.ColumnName.ORIG_COMBINATION, "Whether this is an original combination, either 'Y', 'N' or empty when not applicable."),
//        new SpeciesProfileBuilder(AfdRow.ColumnName.PUB_PUB_AUTHOR, "Publication Author"),
//        new SpeciesProfileBuilder(AfdRow.ColumnName.PUB_PUB_YEAR, "Publication Year"),
//        new SpeciesProfileBuilder(AfdRow.ColumnName.PUB_PUB_TITLE, "Publication Title"),
//        new SpeciesProfileBuilder(AfdRow.ColumnName.PUB_PUB_PAGES, "Publication Page Reference"),
//        new SpeciesProfileBuilder(AfdRow.ColumnName.PUB_PUB_PARENT_BOOK_TITLE, "Publication Book Title"),
//        new SpeciesProfileBuilder(AfdRow.ColumnName.PUB_PUB_PARENT_JOURNAL_TITLE, "Publication Journal Title"),
//        new SpeciesProfileBuilder(AfdRow.ColumnName.PUB_PUB_PARENT_ARTICLE_TITLE, "Publication Article Title"),
//        new SpeciesProfileBuilder(AfdRow.ColumnName.PUB_PUB_PUBLICATION_DATE, "Publication Date"),
//        new SpeciesProfileBuilder(AfdRow.ColumnName.PUB_PUB_PUBLISHER, "Publication Publisher"),
//        new SpeciesProfileBuilder(AfdRow.ColumnName.PUB_PUB_QUALIFICATION, "Publication Qualification and Comments"),
//        new SpeciesProfileBuilder(AfdRow.ColumnName.PUB_PUB_TYPE, "Publication Type"),
    };

    private Logger log = Logger.getLogger(getClass());
	private ITemporalContext temporalContext;
	private Session sesh;
	private TaxaDAO taxaDAO;
    private SpeciesProfileDAO spDAO;
	
	private Transaction tx;
	private TaxonGroup group;

	/**
	 * Create a new row handler
	 * 
	 * @param taxonLibSession TaxonLibSession.
	 * @param now Date the import occurs.
	 * @param sesh Hibernate session. Note that we do our own hibernate session management.
	 * @param taxaDAO TaxaDAO.
	 * @param spDAO SpeciesProfileDAO.
	 */
	public BdrsAfdImporterRowHandler(ITaxonLibSession taxonLibSession, Date now, Session sesh, TaxaDAO taxaDAO, SpeciesProfileDAO spDAO) {
		if (taxonLibSession == null) {
			throw new IllegalArgumentException("TaxonLibSession cannot be null");
		}
		if (now == null) {
			throw new IllegalArgumentException("Date cannot be null");
		}
		if (sesh == null) {
			throw new IllegalArgumentException("Hibernate Session cannot be null");
		}
		if (taxaDAO == null) {
			throw new IllegalArgumentException("TaxaDAO cannot be null");
		}
		if (spDAO == null) {
			throw new IllegalArgumentException("SpeciesProfileDAO cannot be null");
		}
		
		temporalContext = taxonLibSession.getTemporalContext(now);
		this.sesh = sesh;
		this.taxaDAO = taxaDAO;
        this.spDAO = spDAO;
		
		tx = sesh.beginTransaction();
		
		group = taxaDAO.getTaxonGroup(sesh, AFD_GROUP_NAME);
		if (group == null) {
			group = new TaxonGroup();
			group.setName(AFD_GROUP_NAME);
			group.setBehaviourIncluded(false);
			group.setLastAppearanceIncluded(false);
			group.setNumberIncluded(false);
			group.setWeatherIncluded(false);
			group.setHabitatIncluded(false);
			group = taxaDAO.save(sesh, group);
		}
	}
	
	@Override
	public void addRow(AfdRow row, ITaxonConcept concept) {
		ITaxonName tn = concept.getName();
		IndicatorSpecies iSpecies = taxaDAO.getIndicatorSpeciesBySourceDataID(sesh, AfdImporter.SOURCE, getSourceId(tn));
		if (iSpecies == null) {
			iSpecies = new IndicatorSpecies();
            iSpecies.setScientificName(tn.getDisplayName());
            
            iSpecies.setAuthor(tn.getAuthor());
            iSpecies.setTaxonGroup(group);
            iSpecies.setSource(AfdImporter.SOURCE);
            iSpecies.setSourceId(tn.getId().toString());
            iSpecies.setCommonName("");
            
            // There are 2 duplicate TaxonRank enums. One in the BDRS namespace and one in the TaxonLib namespace...
            TaxonRank rank = TaxonRank.valueOf(tn.getRank().toString());
            iSpecies.setTaxonRank(rank);

            List<SpeciesProfile> infoItems = new ArrayList<SpeciesProfile>();

            for(SpeciesProfileBuilder builder : PROFILE_BUILDER) {
                SpeciesProfile sp = builder.createProfile(row);
                if(sp != null) {
                    sp = spDAO.save(sesh, sp);
                    infoItems.add(sp);
                }
            }
            iSpecies.setInfoItems(infoItems);
            
            taxaDAO.save(sesh, iSpecies);
		}
	}

	@Override
	public void linkRow(AfdRow row, ITaxonConcept concept) {
		String nameType = row.getValue(AfdRow.ColumnName.NAME_TYPE);
		IndicatorSpecies iSpecies = taxaDAO.getIndicatorSpeciesBySourceDataID(sesh, AfdImporter.SOURCE, getSourceId(concept.getName()));
		
		if (iSpecies != null) {
			if (AfdImporter.COMMON.equals(nameType)) {
				if (StringUtils.isEmpty(iSpecies.getCommonName())) {
					ITaxonName commonName = temporalContext.getFirstCommonName(concept);
			    	if (commonName != null) {
			    		iSpecies.setCommonName(commonName.getName());
			    	} else {
			    		iSpecies.setCommonName("");
			    	}
				}
			} else if (AfdImporter.VALID.equals(nameType)) {
				if (concept.getParent() != null) {
		        	IndicatorSpecies iSpeciesParent = taxaDAO.getIndicatorSpeciesBySourceDataID(sesh, AfdImporter.SOURCE, getSourceId(concept.getParent().getName()));
		        	iSpecies.setParent(iSpeciesParent);
		        } else {
		        	iSpecies.setParent(null);
		        }
			} else if (AfdImporter.SYNONYM.equals(nameType) || AfdImporter.LIT_SYNONYM.equals(nameType)) {
				IndicatorSpecies species = taxaDAO.getIndicatorSpeciesBySourceDataID(sesh, AfdImporter.SOURCE, getSourceId(concept.getName()));
				if (species != null) {
					species.setCurrent(false);
				}
			}
		} else {
			throw new IllegalStateException("Can't find Indicator species with source id : " + getSourceId(concept.getName()));
		}
	}
	
	/**
	 * Get the source ID to assign to the matching IndicatorSpecies object.
	 * @param tn ITaxonName.
	 * @return Source ID
	 */
	private String getSourceId(ITaxonName tn) {
		return tn.getId().toString();
	}

	@Override
	public void commit() {
		sesh.flush();
		tx.commit();
		sesh.clear();
		tx = sesh.beginTransaction();
	}
}
