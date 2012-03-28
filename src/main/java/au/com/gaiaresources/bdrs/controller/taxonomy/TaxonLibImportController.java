package au.com.gaiaresources.bdrs.controller.taxonomy;

import java.io.IOException;
import java.util.Date;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import au.com.gaiaresources.bdrs.controller.AbstractController;
import au.com.gaiaresources.bdrs.db.SessionFactory;
import au.com.gaiaresources.bdrs.model.preference.Preference;
import au.com.gaiaresources.bdrs.model.preference.PreferenceDAO;
import au.com.gaiaresources.bdrs.model.taxa.SpeciesProfileDAO;
import au.com.gaiaresources.bdrs.model.taxa.TaxaDAO;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.service.taxonomy.BdrsAfdImporter;
import au.com.gaiaresources.bdrs.service.taxonomy.BdrsMaxImporter;
import au.com.gaiaresources.bdrs.service.taxonomy.NswFloraImporter;
import au.com.gaiaresources.bdrs.service.taxonomy.TaxonLibSessionFactory;
import au.com.gaiaresources.taxonlib.TaxonLibSession;

@Controller
public class TaxonLibImportController extends AbstractController {
	
	public static final String TAXON_LIB_IMPORT_URL = "/bdrs/admin/taxonomy/taxonLibImport.htm";
	
	public static final String TAXON_LIB_SELECT_IMPORT_VIEW = "taxonLibSelectImport";
	
	@Autowired
	private PreferenceDAO prefDAO;
	@Autowired
	private TaxaDAO taxaDAO;
	@Autowired
	private SpeciesProfileDAO spDAO;
	@Autowired
    private SessionFactory sessionFactory;
	
	private Logger log = Logger.getLogger(getClass());
	
	private static final String TAXON_LIB_DB_URL_KEY = "taxonlib.database.url";
	private static final String TAXON_LIB_DB_USER_KEY = "taxonlib.database.username";
	private static final String TAXON_LIB_DB_PASS_KEY = "taxonlib.database.password";
	
	public enum TaxonLibImportSource {
		NSW_FLORA,
		MAX,
		AFD
	}

	@RolesAllowed({Role.ADMIN})
	@RequestMapping(value=TAXON_LIB_IMPORT_URL, method = RequestMethod.GET)
	public ModelAndView renderSelectImportPage(HttpServletRequest request, HttpServletResponse response) {
		ModelAndView mv = new ModelAndView(TAXON_LIB_SELECT_IMPORT_VIEW);
		return mv;
	}
	
	// hacked up the front end....
	@RolesAllowed({Role.ADMIN})
	@RequestMapping(value=TAXON_LIB_IMPORT_URL, method = RequestMethod.POST)
	public ModelAndView runImport(MultipartHttpServletRequest request,
            HttpServletResponse response) {
		
		ModelAndView mv = this.redirect(TAXON_LIB_IMPORT_URL);
		
		Preference urlPref = prefDAO.getPreferenceByKey(TAXON_LIB_DB_URL_KEY);
		Preference userPref = prefDAO.getPreferenceByKey(TAXON_LIB_DB_USER_KEY);
		Preference passPref = prefDAO.getPreferenceByKey(TAXON_LIB_DB_PASS_KEY);
		
		if (urlPref == null || userPref == null || passPref == null) {
			getRequestContext().addMessage("taxonlib.badTaxonLibConfig");
			return mv;
		}
		if (!StringUtils.hasLength(urlPref.getValue().trim()) || !StringUtils.hasLength(userPref.getValue().trim()) 
				|| !StringUtils.hasLength(passPref.getValue().trim())) {
			getRequestContext().addMessage("taxonlib.badTaxonLibConfig");
			return mv;
		}
			
		// looks ok, lets go!
		
		String url = urlPref.getValue().trim();
		String username = userPref.getValue().trim();
		String password = userPref.getValue().trim();
		
		log.debug("TAXONOMY IMPORT START");
		try {
			TaxonLibSession taxonLibSession = TaxonLibSessionFactory.getSession(url, username, password);
			
			TaxonLibImportSource importSource = TaxonLibImportSource.valueOf(request.getParameter("importSource"));
			
			switch (importSource) {
			case NSW_FLORA:
				runNswFloraImport(request, taxonLibSession);
				break;
			case MAX:
				runMaxImport(request, taxonLibSession);
				break;
			case AFD:
				runAfdImport(request, taxonLibSession);
				break;
				default:
					throw new IllegalStateException("case not handled : " + importSource);
			}
			// commit!
			taxonLibSession.commit();
			
			getRequestContext().addMessage("taxonlib.importSuccess", new Object[] { importSource.toString() });
		} catch (Exception e) {
			getRequestContext().addMessage("taxonlib.importError", new Object[] { e.getMessage() });
			log.error("Error during taxon lib import : ", e);
		}
		log.debug("TAXONOMY IMPORT END");
		
		return mv;
	}
	
	private void runNswFloraImport(MultipartHttpServletRequest request, TaxonLibSession tls) throws IOException, Exception {
		MultipartFile file = request.getFile("taxonomyFile");
		NswFloraImporter importer = new NswFloraImporter(tls, new Date(), taxaDAO, spDAO);
		importer.runImport(file.getInputStream());
	}
	
	private void runMaxImport(MultipartHttpServletRequest request, TaxonLibSession tls) throws IOException, Exception {
		MultipartFile familyFile = request.getFile("maxFamilyFile");
		MultipartFile generaFile = request.getFile("maxGeneraFile");
		MultipartFile nameFile = request.getFile("maxNameFile");
		MultipartFile xrefFile = request.getFile("maxXrefFile");
		
		BdrsMaxImporter importer = new BdrsMaxImporter(tls, new Date(), taxaDAO, spDAO);
		importer.runImport(familyFile.getInputStream(), generaFile.getInputStream(), nameFile.getInputStream(), xrefFile.getInputStream());
	}
	
	private void runAfdImport(MultipartHttpServletRequest request, TaxonLibSession tls) throws IOException, Exception {
		MultipartFile file = request.getFile("taxonomyFile");
		Session sesh = null;
		try {
			sesh = sessionFactory.openSession();
			BdrsAfdImporter importer = new BdrsAfdImporter(tls, new Date(), sesh, taxaDAO, spDAO);
			importer.runImport(file.getInputStream());
		} finally {
			if (sesh != null) {
				sesh.getTransaction().commit();
				sesh.close();
			}
		}
	}
}
