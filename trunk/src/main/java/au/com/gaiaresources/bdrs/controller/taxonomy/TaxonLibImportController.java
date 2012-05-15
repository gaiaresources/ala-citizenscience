package au.com.gaiaresources.bdrs.controller.taxonomy;

import au.com.gaiaresources.bdrs.controller.AbstractController;
import au.com.gaiaresources.bdrs.db.SessionFactory;
import au.com.gaiaresources.bdrs.email.EmailService;
import au.com.gaiaresources.bdrs.model.taxa.SpeciesProfileDAO;
import au.com.gaiaresources.bdrs.model.taxa.TaxaDAO;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.service.mode.ApplicationModeService;
import au.com.gaiaresources.bdrs.service.mode.TaxonomyImportMode;
import au.com.gaiaresources.bdrs.service.taxonomy.*;
import au.com.gaiaresources.taxonlib.ITaxonLibSession;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for handling TaxonLib importing.
 *
 */
@Controller
public class TaxonLibImportController extends AbstractController {
	
	public static final String TAXON_LIB_IMPORT_URL = "/bdrs/admin/taxonomy/taxonLibImport.htm";
	
	public static final String TAXON_LIB_SELECT_IMPORT_VIEW = "taxonLibSelectImport";
	
	public static final String NSW_FLORA_IMPORT_URL = "/bdrs/admin/taxonomy/nswFloraImport.htm";
	public static final String MAX_IMPORT_URL = "/bdrs/admin/taxonomy/maxImport.htm";
	public static final String AFD_IMPORT_URL = "/bdrs/admin/taxonomy/afdImport.htm";
	
	public static final String NSW_IMPORT_VIEW = "taxonLibNswFloraImport";
	public static final String MAX_IMPORT_VIEW = "taxonLibMaxImport";
	public static final String AFD_IMPORT_VIEW = "taxonLibAfdImport";

	@Autowired
	private TaxaDAO taxaDAO;
	@Autowired
	private SpeciesProfileDAO spDAO;
	@Autowired
    private SessionFactory sessionFactory;
	@Autowired
	private TaxonLibSessionFactory taxonLibSessionFactory;
	@Autowired
	private EmailService emailService;
    @Autowired
    private ApplicationModeService applicationModeService;
	
	private Logger log = Logger.getLogger(getClass());
	
	public enum TaxonLibImportSource {
		NSW_FLORA,
		MAX,
		AFD
	}

	/**
	 * Renders the taxon lib importer page.
	 * 
	 * @param request Request.
	 * @param response Response.
	 * @return ModelAndView to render the page.
	 */
	@RolesAllowed({Role.ROOT})
	@RequestMapping(value=TAXON_LIB_IMPORT_URL, method = RequestMethod.GET)
	public ModelAndView renderSelectImportPage(HttpServletRequest request, HttpServletResponse response) {
		return new ModelAndView(TAXON_LIB_SELECT_IMPORT_VIEW);
	}
	
	/**
	 * Renders the NSW Flora importer page.
	 * 
	 * @param request Request.
	 * @param response Response.
	 * @return ModelAndView to render the page.
	 */
	@RolesAllowed({Role.ROOT})
	@RequestMapping(value=NSW_FLORA_IMPORT_URL, method = RequestMethod.GET)
	public ModelAndView renderNswFloraImport(HttpServletRequest request, HttpServletResponse response) {
		return new ModelAndView(NSW_IMPORT_VIEW);
	}
	
	/**
	 * Renders the MAX importer page.
	 * 
	 * @param request Request.
	 * @param response Response.
	 * @return ModelAndView to render the page.
	 */
	@RolesAllowed({Role.ROOT})
	@RequestMapping(value=MAX_IMPORT_URL, method = RequestMethod.GET)
	public ModelAndView renderMaxImportPage(HttpServletRequest request, HttpServletResponse response) {
		return new ModelAndView(MAX_IMPORT_VIEW);
	}
	
	/**
	 * Renders the AFD importer page.
	 * 
	 * @param request Request.
	 * @param response Response.
	 * @return ModelAndView to render the page.
	 */
	@RolesAllowed({Role.ROOT})
	@RequestMapping(value=AFD_IMPORT_URL, method = RequestMethod.GET)
	public ModelAndView renderAfdImportPage(HttpServletRequest request, HttpServletResponse response) {
		return new ModelAndView(AFD_IMPORT_VIEW);
	}
	
	/**
	 * Handles the taxon lib upload.
	 * 
	 * @param request Request.
	 * @param response Response.
	 * @return ModelAndView to display result.
	 */
	@RolesAllowed({Role.ROOT})
	@RequestMapping(value=TAXON_LIB_IMPORT_URL, method = RequestMethod.POST)
	public void runImport(MultipartHttpServletRequest request,
            HttpServletResponse response) {

        TaxonomyImportMode mode = new TaxonomyImportMode();
        applicationModeService.addMode(mode);
        try {
            log.info("TAXONOMY IMPORT START");

            boolean rollback = false;

            ITaxonLibSession taxonLibSession = null;

            User currentUser = getRequestContext().getUser();
            sendStartEmail(currentUser);

            try {
                taxonLibSession = taxonLibSessionFactory.getSession();

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

                sendSuccessEmail(currentUser);
            } catch (MissingFileException e) {
                rollback = true;
                sendFailureEmail(currentUser, e.getMessage());
            } catch (BdrsTaxonLibException e) {
                log.error("Error during taxon lib import : ", e);
                rollback = true;
                sendFailureEmail(currentUser, e.getMessage());
            } catch (Exception e) {
                log.error("Error during taxon lib import : ", e);
                rollback = true;
                sendFailureEmail(currentUser, e.getMessage());
            } finally {
                if (taxonLibSession != null) {
                    if (rollback) {
                        requestRollback(request);
                        try {
                            taxonLibSession.rollback();
                        } catch (SQLException sqle) {
                            log.error("Error attempting taxon lib session rollback", sqle);
                        }

                    }
                    taxonLibSession.close();
                }
            }
            log.info("TAXONOMY IMPORT END");
        } finally {
            applicationModeService.removeMode(mode);
        }
	}
	
	/**
	 * Runs the NSW flora import.
	 * 
	 * @param request The request object that contains the uploaded files
	 * @param tls The TaxonLibSession.
	 * @throws IOException 
	 * @throws Exception
	 */
	private void runNswFloraImport(MultipartHttpServletRequest request, ITaxonLibSession tls) throws IOException, Exception {
		// Should run in a single transaction
		MultipartFile file = request.getFile("taxonomyFile");
		if (file == null) {
			throw new MissingFileException("NSW Flora");
		}
		BdrsNswFloraImporter importer = new BdrsNswFloraImporter(tls, new Date(), taxaDAO, spDAO);
		importer.runImport(file.getInputStream());
	}
	
	/**
	 * Runs the Max import
	 * 
	 * @param request The request object that contains the uploaded files
	 * @param tls The TaxonLibSession.
	 * @throws Exception
	 */
	private void runMaxImport(MultipartHttpServletRequest request, ITaxonLibSession tls) throws Exception {
		// Should run in a single transaction
		MultipartFile familyFile = request.getFile("maxFamilyFile");
		MultipartFile generaFile = request.getFile("maxGeneraFile");
		MultipartFile nameFile = request.getFile("maxNameFile");
		MultipartFile xrefFile = request.getFile("maxXrefFile");
		
		if (familyFile == null) {
			throw new MissingFileException("MAX Family");
		}
		if (generaFile == null) {
			throw new MissingFileException("MAX Genera");
		}
		if (nameFile == null) {
			throw new MissingFileException("MAX Name");
		}
		if (xrefFile == null) {
			throw new MissingFileException("MAX Xref");
		}

		BdrsMaxImporter importer = new BdrsMaxImporter(tls, new Date(), sessionFactory, taxaDAO, spDAO);
		importer.runImport(familyFile.getInputStream(), generaFile.getInputStream(), nameFile.getInputStream(), xrefFile.getInputStream());
	}
	
	/**
	 * Runs the AFD import
	 * 
	 * @param request The request object that contains the uploaded files
	 * @param tls The TaxonLibSession.
	 * @throws IOException 
	 * @throws Exception
	 */
	private void runAfdImport(MultipartHttpServletRequest request, ITaxonLibSession tls) throws IOException, Exception {
		MultipartFile file = request.getFile("taxonomyFile");
		
		if (file == null) {
			throw new MissingFileException("AFD file");
		}
		
		Session sesh = null;
		// The AFD dataset is too large to run the insert queries in a single transaction.
		// Because of this, we do manual hibernate session management.
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
	
	/**
	 * Send a start notification email
	 * @param user The user that started the import.
	 */
	private void sendStartEmail(User user) {
		if (user != null) {
			emailService.sendTemplateMessage(user.getEmailAddress(), 
					"hello@bdrs", "Taxonomy Import Started", "TaxonLibImportStart.vm", new HashMap<String, Object>());
		}
	}
	
	/**
	 * Send a success notification email.
	 * @param user The user that started the import.
	 */
	private void sendSuccessEmail(User user) {
		if (user != null) {
			emailService.sendTemplateMessage(user.getEmailAddress(), 
					"hello@bdrs", "Taxonomy Import Successful", "TaxonLibImportSuccess.vm", new HashMap<String, Object>());
		}
	}
	
	/**
	 * Send a failure notification email.
	 * @param user The user that started the import.
	 */
	private void sendFailureEmail(User user, String errorMsg) {
		if (user != null) {
			Map<String, Object> argMap = new HashMap<String, Object>();
			argMap.put("errorMsg", errorMsg);
			emailService.sendTemplateMessage(user.getEmailAddress(), 
					"hello@bdrs", "Taxonomy Import Failure", "TaxonLibImportFailure.vm", argMap);	
		}
	}
}
