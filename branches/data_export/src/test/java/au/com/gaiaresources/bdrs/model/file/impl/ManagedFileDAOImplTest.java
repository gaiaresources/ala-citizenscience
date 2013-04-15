package au.com.gaiaresources.bdrs.model.file.impl;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import au.com.gaiaresources.bdrs.db.impl.PaginationFilter;
import au.com.gaiaresources.bdrs.model.file.ManagedFile;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.model.user.UserDAO;
import au.com.gaiaresources.bdrs.security.UserDetails;
import au.com.gaiaresources.bdrs.servlet.RequestContext;
import au.com.gaiaresources.bdrs.servlet.RequestContextHolder;
import au.com.gaiaresources.bdrs.test.AbstractTransactionalTest;

/**
 * Tests the ManagedFileDAOImpl class.  
 * Please note that these tests are integration tests and connect to a database.
 */
public class ManagedFileDAOImplTest  extends AbstractTransactionalTest {

	/** The class we are testing */
	@Autowired
	private ManagedFileDAOImpl managedFileDAO;
	@Autowired
	private UserDAO userDAO;
	
	/** Tracks the files we create for our tests so we can clean up afterwards */
	private List<ManagedFile> testFiles;
	
	/** Our user for testing purposes */
	private User admin;
	
	/**
	 * Creates some files we can search for.
	 */
	@Before
	public void setUp() {
		
		configureRequestContext();
		
		testFiles = new ArrayList<ManagedFile>();
		
		ManagedFile file = createFile("/fake/path/1.txt", "Description", "text/plain");
		managedFileDAO.save(file);
		testFiles.add(file);
		
		// I'm flushing here to force the timestamps for the two files to be updated separately so we can
		// test our default ordering.
		managedFileDAO.getSessionFactory().getCurrentSession().flush();
		
		file = createFile("/fake/path/2.jpg", "Picture", "image/jpg");
		managedFileDAO.save(file);
		testFiles.add(file);
		managedFileDAO.getSessionFactory().getCurrentSession().flush();
	}


	/**
	 * Deletes the files we created in setUp.
	 */
	@After
	public void tearDown() {
		for (ManagedFile file : testFiles) {
			managedFileDAO.delete(file);
		}
	}
	
	/**
	 * Tests that a search for all managed files returns the correct results.
	 */
	@Test
	public void testSearchAll() {
		
		PaginationFilter filter = new PaginationFilter(0, 10);
		String searchText = "";
		String typeFilter = "";
		String userFilter = "";
		
		List<Object[]> files = managedFileDAO.search(filter, searchText, typeFilter, userFilter).getList();
		
		assertEquals(2, files.size());
		
		// We are testing the default sort order here, should be most recently modified first.
		assertEquals(testFiles.get(1), files.get(0)[0]);
		assertEquals(testFiles.get(0), files.get(1)[0]);
		assertEquals(admin, files.get(0)[1]);
		assertEquals(admin, files.get(0)[2]);
		
	}
	
	
	@Test
	public void testSearchByUser() {
		
		PaginationFilter filter = new PaginationFilter(0, 10);
		String searchText = "";
		String typeFilter = "";
		String userFilter = "admin";
		
		List<Object[]> files = managedFileDAO.search(filter, searchText, typeFilter, userFilter).getList();
		
		assertEquals(2, files.size());
		
		userFilter = "root";
        files = managedFileDAO.search(filter, searchText, typeFilter, userFilter).getList();
		assertEquals(0, files.size());	
	}
	
	
	@Test
	public void testSearchFilenameOrDescription() {
		
		PaginationFilter filter = new PaginationFilter(0, 10);
		// lower case p to make sure the search is case insenstive.
		String searchText = "picture";
		String typeFilter = "";
		String userFilter = "";
		
		List<Object[]> files = managedFileDAO.search(filter, searchText, typeFilter, userFilter).getList();
		
		assertEquals(1, files.size());
		assertEquals(testFiles.get(1), files.get(0)[0]);
		
		
		searchText = "1.txt";
        files = managedFileDAO.search(filter, searchText, typeFilter, userFilter).getList();
		assertEquals(1, files.size());	
		
		assertEquals(testFiles.get(0), files.get(0)[0]);
	}
	
	
	@Test
	public void testSearchByContentType() {
		
		PaginationFilter filter = new PaginationFilter(0, 10);
		String searchText = "";
		String typeFilter = ManagedFile.IMAGE_CONTENT_TYPE_PREFIX;
		String userFilter = "";
		
		List<Object[]> files = managedFileDAO.search(filter, searchText, typeFilter, userFilter).getList();
		
		assertEquals(1, files.size());
		assertEquals(testFiles.get(1), files.get(0)[0]);
		
		typeFilter = "text";
        files = managedFileDAO.search(filter, searchText, typeFilter, userFilter).getList();
        assertEquals(1, files.size());
		assertEquals(testFiles.get(0), files.get(0)[0]);
	}

	/**
	 * Factory method that creates a new ManagedFile.
	 */
	private ManagedFile createFile(String fileName, String description, String contentType) {
		ManagedFile file = new ManagedFile();
		
		file.setContentType(contentType);
		file.setCredit("None");
		file.setDescription(description);
		file.setFilename(fileName);
		file.setLicense("creative commons");
	
		return file;
	}
	
	
	/**
	 * Associates the admin user with the request context so the managed files we create have non-null
	 * valid createdBy/updatedBy properties.
	 */
	private void configureRequestContext() {
		admin = userDAO.getUser("admin");
		RequestContext ctx = RequestContextHolder.getContext();
		ctx.setUserDetails(new UserDetails(admin));
	}
	
}
