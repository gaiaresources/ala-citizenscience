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
import au.com.gaiaresources.bdrs.util.ZipUtils;
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
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.sql.SQLException;
import java.util.*;
import java.util.zip.ZipInputStream;

/**
 * Controller for handling TaxonLib importing.
 *
 */
@Controller
public class TaxonLibImportController extends AbstractController {
	
	public static final String TAXON_LIB_IMPORT_URL = "/bdrs/admin/taxonomy/taxonLibImport.htm";
	
	public static final String TAXON_LIB_SELECT_IMPORT_VIEW = "taxonLibSelectImport";
	
	public static final String NSW_FLORA_IMPORT_URL = "/bdrs/admin/taxonomy/nswFloraImport.htm";
    public static final String NSW_FAUNA_IMPORT_URL = "/bdrs/admin/taxonomy/nswFaunaImport.htm";
	public static final String MAX_IMPORT_URL = "/bdrs/admin/taxonomy/maxImport.htm";
	public static final String AFD_IMPORT_URL = "/bdrs/admin/taxonomy/afdImport.htm";
	
	public static final String NSW_IMPORT_VIEW = "taxonLibNswFloraImport";
    public static final String NSW_FAUNA_IMPORT_VIEW = "taxonLibNswFaunaImport";
	public static final String MAX_IMPORT_VIEW = "taxonLibMaxImport";
	public static final String AFD_IMPORT_VIEW = "taxonLibAfdImport";

    public static final int AFD_DOWNLOAD_CONNECTION_TIMOUT = 5000;
    public static final int AFD_DOWNLOAD_READ_TIMEOUT = 30000;
    public static final int AFD_DOWNLOAD_RETRY_TIMEOUT = 30000;
    public static final int AFD_DOWNLOAD_RETRY_ATTEMPT = 10;

    static final String[] AFD_TAXONOMY_URLS = new String[] {
        "http://www.environment.gov.au/biodiversity/abrs/online-resources/fauna/afd-data/lowertaxa.zip",
        "http://www.environment.gov.au/biodiversity/abrs/online-resources/fauna/afd-data/highertaxa.zip",
        "http://www.environment.gov.au/biodiversity/abrs/online-resources/fauna/afd-data/chordata.zip",
        "http://www.environment.gov.au/biodiversity/abrs/online-resources/fauna/afd-data/mollusca.zip",
        "http://www.environment.gov.au/biodiversity/abrs/online-resources/fauna/afd-data/insecta.zip",
    };
    static final String[] AFD_FILE_INPUT_NAMES =
            new String[] { "lowerTaxa", "higherTaxa", "chordata", "insecta", "mollusca" };
    public static final String AFD_DOWNLOAD_FLAG_NAME = "download";

	@Autowired
	private TaxaDAO taxaDAO;
	@Autowired
	private SpeciesProfileDAO spDAO;
	@Autowired
    private SessionFactory sessionFactory;
	@Autowired
	private EmailService emailService;
    @Autowired
    private ApplicationModeService applicationModeService;
	
	private Logger log = Logger.getLogger(getClass());
	
	public enum TaxonLibImportSource {
        NSW_FAUNA,
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
     * Renders the NSW Flora importer page.
     *
     * @param request Request.
     * @param response Response.
     * @return ModelAndView to render the page.
     */
    @RolesAllowed({Role.ROOT})
    @RequestMapping(value=NSW_FAUNA_IMPORT_URL, method = RequestMethod.GET)
    public ModelAndView renderNswFaunaImport(HttpServletRequest request, HttpServletResponse response) {
        return new ModelAndView(NSW_FAUNA_IMPORT_VIEW);
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
                taxonLibSession = getRequestContext().getTaxonLibSession();
                TaxonLibImportSource importSource = TaxonLibImportSource.valueOf(request.getParameter("importSource"));

                switch (importSource) {
                case NSW_FLORA:
                    runNswFloraImport(request, taxonLibSession);
                    break;
                case NSW_FAUNA:
                    runNswFaunaImport(request, taxonLibSession);
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
	 * @throws Exception
	 */
	private void runNswFloraImport(MultipartHttpServletRequest request, ITaxonLibSession tls) throws Exception {
		// Should run in a single transaction
		MultipartFile file = request.getFile("taxonomyFile");
		if (file == null) {
			throw new MissingFileException("NSW Flora");
		}
		BdrsNswFloraImporter importer = new BdrsNswFloraImporter(tls, new Date(), sessionFactory, taxaDAO, spDAO);
		importer.runImport(file.getInputStream());
	}

    /**
     * Runs the NSW fauna import.
     *
     * @param request The request object that contains the uploaded files
     * @param tls The TaxonLibSession.
     * @throws Exception
     */
    private void runNswFaunaImport(MultipartHttpServletRequest request, ITaxonLibSession tls) throws Exception {
        // Should run in a single transaction
        MultipartFile file = request.getFile("taxonomyFile");
        if (file == null) {
            throw new MissingFileException("NSW Fauna");
        }
        BdrsNswFaunaImporter importer = new BdrsNswFaunaImporter(tls, new Date(), sessionFactory, taxaDAO, spDAO);
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
	 * @throws Exception
	 */
	private void runAfdImport(MultipartHttpServletRequest request, ITaxonLibSession tls) throws Exception {
        boolean download_afd_data = request.getParameter(AFD_DOWNLOAD_FLAG_NAME) != null;
        if(download_afd_data) {
            downloadFromAFD(tls);
        } else {
            importFromZipFiles(request, tls);
        }
	}

    /**
     * Import AFD data based on the files uploaded in the request.
     * @param request the browser request
     * @param tls The TaxonLibSession.
     * @throws Exception
     */
    private void importFromZipFiles(MultipartHttpServletRequest request, ITaxonLibSession tls) throws Exception {
        for(String fileInputName : AFD_FILE_INPUT_NAMES) {
            MultipartFile file = request.getFile(fileInputName);
            if(file != null) {
                if(ZipUtils.ZIP_CONTENT_TYPE.equals(file.getContentType())) {
                    importAfd(new ZipInputStream(file.getInputStream()), tls);
                }
            } else  {
                // no file
                log.info("No file to load for " + fileInputName);
            }
        }
    }

    /**
     * Import AFD data by downloading data from the AFD website.
     * @param tls The TaxonLibSession.
     * @throws Exception
     */
    private void downloadFromAFD(ITaxonLibSession tls) throws Exception {
        log.info("Downloading AFD taxonomy");

        List<File> localAFDTaxaDownload = new ArrayList<File>(AFD_TAXONOMY_URLS.length);
        for(String urlStr : AFD_TAXONOMY_URLS) {
            localAFDTaxaDownload.add(downloadTaxonomyFile(new URL(urlStr)));
        }
        log.info("AFD Download completed. Starting import.");

        for(File f : localAFDTaxaDownload) {
            importAfd(new ZipInputStream(new FileInputStream(f)), tls);
        }
    }

    /**
     * Downloads the file specified by the URL accounting for retry attempts,
     * connection timeouts and read timeouts.
     *
     * @param url the location of the file to be downloaded.
     * @return a temporary file containing the data specified by the URL.
     * @throws IOException thrown if the attempt to retrieve data has failed.
     */
    private File downloadTaxonomyFile(URL url) throws IOException {
        log.info("Downloading from: " + url.toString());
        BufferedInputStream is = null;
        BufferedOutputStream os = null;
        byte[] buffer = new byte[4096];
        for(int retry=0; retry<AFD_DOWNLOAD_RETRY_ATTEMPT; retry++) {
            log.info(String.format("Attempt %d of %d", retry, AFD_DOWNLOAD_RETRY_ATTEMPT));
            try {
                URLConnection conn = url.openConnection();
                conn.setConnectTimeout(AFD_DOWNLOAD_CONNECTION_TIMOUT);
                conn.setReadTimeout(AFD_DOWNLOAD_READ_TIMEOUT);

                File target = File.createTempFile("afd", "zip");
                target.deleteOnExit();

                os = new BufferedOutputStream(new FileOutputStream(target));

                is = new BufferedInputStream(conn.getInputStream());
                for(int read = is.read(buffer, 0, buffer.length); read > -1; read = is.read(buffer, 0, buffer.length)) {
                    os.write(buffer, 0, read);
                }
                // flushing and closing happens in the finally block

                return target;

            } catch(IOException ioe) {
                // do nothing
                try {
                    Thread.sleep(AFD_DOWNLOAD_RETRY_TIMEOUT);
                } catch (InterruptedException e) {
                    // do nothing
                }
            } finally {
                try {
                    if(is != null) {
                        is.close();
                        is = null;
                    }

                    if(os != null) {
                        os.flush();
                        os.close();
                    }
                } catch(IOException ioex) {
                    log.error("Failed clean up io streams in finally block while downloading AFD data from: " + url.toString());
                }
            }
        }

        throw new IOException("Failed to download AFD data from: " + url.toString());
    }

    /**
     * Import AFD data from the specified zipped input stream.
     * @param inputStream the input stream containing the csv file with the data to be loaded.
     * @param tls The TaxonLibSession.
     * @throws Exception
     */
    private void importAfd(ZipInputStream inputStream, ITaxonLibSession tls) throws Exception {
        Session sesh = null;
        try {
            sesh = sessionFactory.openSession();
            BdrsAfdImporter importer = new BdrsAfdImporter(tls, new Date(), sesh, taxaDAO, spDAO);
            importer.runImport(inputStream);
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
            String subject = "Taxonomy Import Started";
            String templateName = "TaxonLibImportStart.vm";
            Map<String, Object> substitutionParams = new HashMap<String, Object>();
            emailService.sendMessage(user.getEmailAddress(), subject, templateName, substitutionParams);
		}
	}
	
	/**
	 * Send a success notification email.
	 * @param user The user that started the import.
	 */
	private void sendSuccessEmail(User user) {
		if (user != null) {
            String subject = "Taxonomy Import Successful";
            String templateName = "TaxonLibImportSuccess.vm";
            Map<String, Object> substitutionParams = new HashMap<String, Object>();
            emailService.sendMessage(user.getEmailAddress(), subject, templateName, substitutionParams);
		}
	}
	
	/**
	 * Send a failure notification email.
	 * @param user The user that started the import.
	 */
	private void sendFailureEmail(User user, String errorMsg) {
		if (user != null) {
            String subject = "Taxonomy Import Failure";
            String templateName = "TaxonLibImportFailure.vm";
			Map<String, Object> substitutionParams = new HashMap<String, Object>();
            substitutionParams.put("errorMsg", errorMsg);
            emailService.sendMessage(user.getEmailAddress(), subject, templateName, substitutionParams);
		}
	}
}
