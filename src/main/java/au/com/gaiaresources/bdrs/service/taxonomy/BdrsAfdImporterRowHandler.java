package au.com.gaiaresources.bdrs.service.taxonomy;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;

import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;
import au.com.gaiaresources.bdrs.model.taxa.SpeciesProfileDAO;
import au.com.gaiaresources.bdrs.model.taxa.TaxaDAO;
import au.com.gaiaresources.bdrs.model.taxa.TaxonGroup;
import au.com.gaiaresources.bdrs.model.taxa.TaxonRank;
import au.com.gaiaresources.taxonlib.ITemporalContext;
import au.com.gaiaresources.taxonlib.ITaxonLibSession;
import au.com.gaiaresources.taxonlib.importer.afd.AfdImporter;
import au.com.gaiaresources.taxonlib.importer.afd.AfdImporterRowHandler;
import au.com.gaiaresources.taxonlib.importer.afd.AfdRow;
import au.com.gaiaresources.taxonlib.model.ITaxonConcept;
import au.com.gaiaresources.taxonlib.model.ITaxonName;

public class BdrsAfdImporterRowHandler implements AfdImporterRowHandler {

	private ITemporalContext temporalContext;
	private Session sesh;
	private TaxaDAO taxaDAO;
	private SpeciesProfileDAO spDAO;
	
	private Transaction tx;
	
	private TaxonGroup group;
	
	private Logger log = Logger.getLogger(getClass());
	
	public static final String AFD_GROUP_NAME = "AFD";
	
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
            
            // do teh funky cast....
            // There are 2 duplicate TaxonRank enums. One in the BDRS namespace and one in the TaxonLib namespace...
            TaxonRank rank = TaxonRank.valueOf(tn.getRank().toString());
            if (rank == null) {
            	throw new IllegalStateException("Something has gone wrong with our funky casting");
            }
            iSpecies.setTaxonRank(rank);
            
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
					List<ITaxonName> commonNames = temporalContext.getCommonNames(concept);
			    	if (commonNames != null && !commonNames.isEmpty()) {
			    		// grabs the first common name....
			    		String cn = commonNames.get(0).getName();
			    		iSpecies.setCommonName(cn);
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
