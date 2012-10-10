package au.com.gaiaresources.bdrs.file;

import au.com.gaiaresources.bdrs.db.Persistent;
import au.com.gaiaresources.bdrs.util.ImageUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.activation.FileDataSource;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Storage and retrieval service for files that are associated with Persistent
 * object. The files are stored under the folder declared by file.store.location
 * and then in sub folders that represent the package that the owning Persistent
 * object is in.
 * 
 * @author Tim Carpenter
 * 
 */
@Component
public class FileService {
    public static final String FILE_URL_TMPL = "className=%s&id=%d&fileName=%s";
        
    public static final String FILE_STORE_LOCATION_PREFERENCE_KEY = "file.store.location";

    /** The width (in pixels) of thumbnails produced by this service */
    public static final int DEFAULT_THUMBNAIL_WIDTH = 100;
    /** The height (in pixels) of thumbnails produced by this service */
    public static final int DEFAULT_THUMBNAIL_HEIGHT = 120;
    /** The prefix applied to the file name of generated thumbnails */
    public static final String THUMBNAIL_FILENAME_PREFIX = "thumb";

    public static final String THUMBNAIL_CONTENT_TYPE = "image/png";
    public static final String THUMBNAIL_FILE_EXTENSION = ".png";

	private File storageDirectory;
	private Map<Class<? extends Persistent>, File> persistentFolders;
	private Logger logger = Logger.getLogger(getClass());

	/**
	 * Constructor.
	 * 
	 * @throws IOException
	 *             If the configuration properties file cannot be read.
	 */
	public FileService() throws IOException {
		logger.info("Initialising FileService");
		Properties p = new Properties();
		p.load(getClass().getResource("file.properties").openStream());
		if (p.containsKey(FILE_STORE_LOCATION_PREFERENCE_KEY)) {
			String path = p.getProperty(FILE_STORE_LOCATION_PREFERENCE_KEY);
			this.storageDirectory = new File(path);
			if (!this.storageDirectory.exists()
					|| !this.storageDirectory.isDirectory()) {
				throw new IllegalStateException(
						"Property " + FILE_STORE_LOCATION_PREFERENCE_KEY+ " has an invalid value: "
								+ path);
			}
		} else {
			throw new IllegalStateException(
					"Property " + FILE_STORE_LOCATION_PREFERENCE_KEY + " is not set.");
		}
		this.persistentFolders = new HashMap<Class<? extends Persistent>, File>();
		logger.info("FileService content directory set to: "
				+ this.storageDirectory.getAbsolutePath());
	}
	
	public String getStorageDirectory() {
	    return storageDirectory.getAbsolutePath();
	}

	/**
	 * Saves a file to the storage.
	 * 
	 * @param p
	 *            <code>Persistent</code> that owns the file.
	 * @param file
	 *            <code>MultipartFile</code>.
	 * @throws IOException
	 *             If the file cannot be read or written.
	 */
	public void createFile(Persistent p, MultipartFile file) throws IOException {
		createFile(p.getClass(), p.getId(), file);
	}
	
	public void createFile(Persistent p, MultipartFile file, String filename) throws IOException {
            createFile(p.getClass(), p.getId(), file, filename);
        }


	/**
	 * The file is copied from source location.
	 * @param p
	 * @param file
	 * @throws IOException
	 */
	public void createFile(Persistent p, File file) throws IOException {
		createFile(p, file, file.getName());
	}
	
	public void createFile(Persistent p, File file, String filename) throws IOException {
	    File target = createTargetFile(p.getClass(), p.getId(), filename);
            FileUtils.deleteQuietly(target);
            FileUtils.copyFile(file, target);
	}

	public void createFile(Class<? extends Persistent> clazz, Integer id,
			MultipartFile file) throws IOException {
		String originalFileName = file.getOriginalFilename();
		// Some browsers include the complete path when uploading a file. We
		// need to strip that out.
		if (originalFileName.indexOf("/") >= 0) {
			originalFileName = stripPath(originalFileName, "/");
		} else if (originalFileName.indexOf("\\") >= 0) {
			originalFileName = stripPath(originalFileName, "\\");
		}
		createFile(clazz, id, file, originalFileName);
	}
	
	void createFile(Class<? extends Persistent> clazz, Integer id,
                MultipartFile file, String filenameToUse) throws IOException {
            File writeToFile = createTargetFile(clazz, id, filenameToUse);
            writeToFile(file.getInputStream(), writeToFile);
        } 

	/**
	 * 
	 * @param id
	 * @param file
	 * @return unique identifier for file.
	 * @throws IOException
	 */
	public String createTempFile(String id, MultipartFile file) throws IOException {
		String originalFileName = file.getOriginalFilename();
		// Some browsers include the complete path when uploading a file. We
		// need to strip that out.
		if (originalFileName.indexOf("/") >= 0) {
			originalFileName = stripPath(originalFileName, "/");
		} else if (originalFileName.indexOf("\\") >= 0) {
			originalFileName = stripPath(originalFileName, "\\");
		}

		File writeToFile = createTempFile(id, originalFileName);
		writeToFile(file.getInputStream(), writeToFile);
		return writeToFile.getName();
	}

	/**
	 * 
	 * @param id
	 * @return
	 * @throws IOException
	 *             if the file doesn't exist.
	 */
	public File getTempFile(String id, MultipartFile fileObj) throws IOException {
		String originalFileName = fileObj.getOriginalFilename();
		// Some browsers include the complete path when uploading a file. We
		// need to strip that out.
		if (originalFileName.indexOf("/") >= 0) {
			originalFileName = stripPath(originalFileName, "/");
		} else if (originalFileName.indexOf("\\") >= 0) {
			originalFileName = stripPath(originalFileName, "\\");
		}
		
		File file = new File(this.storageDirectory.getAbsolutePath()
				+ File.separator + "Temp" + File.separator + id + "-" + originalFileName);
		logger.info("Looking for file: " + file.getAbsolutePath());
		if (file.exists())
			return file;
		throw new FileNotFoundException("Temp file does not exist : " + file.getName());
	}

	public void createFile(Class<? extends Persistent> clazz, Integer id,
			String name, byte[] content) throws IOException {
		File writeToFile = createTargetFile(clazz, id, name);
		ByteArrayInputStream is = new ByteArrayInputStream(content);
		writeToFile(is, writeToFile);
	}

	public File createTargetFile(Class<? extends Persistent> clazz, Integer id,
			String name) throws IOException {
	    File instanceDir = getPersistentInstanceFolder(clazz, id);
	    File writeToFile = new File(instanceDir, name);
	    logger.info("attempting to create new file : " + writeToFile.getAbsolutePath());
	    if (writeToFile.createNewFile()) {
	        return writeToFile;
	    } else {
	        logger.error("File already exists : " + writeToFile.getAbsolutePath());
	    }
            return writeToFile;	
	}
	
	public File getTargetDirectory(Persistent p, String name, boolean createIfMissing) throws IOException {
	    return this.getTargetDirectory(p.getClass(), p.getId(), name, createIfMissing);
	}
	
        public File getTargetDirectory(Class<? extends Persistent> clazz, Integer id, String name, boolean createIfMissing) throws IOException {
            File instanceDir = getPersistentInstanceFolder(clazz, id);
            File targetDir = new File(instanceDir, name);
            if(!targetDir.exists() && createIfMissing) {
                boolean dirCreateSuccess = targetDir.mkdirs();
                if(!dirCreateSuccess) {
                    throw new IOException("Unable to create directory path: "+targetDir.getAbsolutePath());
                }
            }
            return targetDir;
        }	       
	
	public File createTempFile(String id, String name) throws IOException {
		File instanceDir = new File(this.storageDirectory.getAbsolutePath()
				+ File.separator + "Temp");
		if (!instanceDir.exists()) {
		    if (!instanceDir.mkdirs()) {
		        logger.error("Could not make directory");
		    }
		}
		File writeToFile = new File(instanceDir.getAbsolutePath()
				+ File.separator + id + "-" + name);
		if (writeToFile.createNewFile()) {
		    return writeToFile;
		} else {
	        logger.error("file already exists");
	    }
        return writeToFile;
	}

	public void writeToFile(InputStream input, File outputFile)
			throws IOException {
		FileOutputStream fo = new FileOutputStream(outputFile);
		try {
			IOUtils.copy(input, fo);
		} finally {
			// Close the streams.
			try {
				fo.close();
			} catch (IOException ioe) {
				logger.error("Failed to close output stream.", ioe);
			}
			try {
				input.close();
			} catch (IOException ioe) {
				logger.error("Failed to close input stream.", ioe);
			}
		}
	}

	/**
	 * Get all of the files that are associated with the given Persistent
	 * instance.
	 * 
	 * @param p
	 *            <code>Persistent</code>.
	 * @return <code>List</code> of <code>javax.activation.FileDataSource</code>
	 *         .
	 */
	public List<FileDataSource> getFiles(Persistent p) {
		return getFiles(p.getClass(), p.getId());
	}

	/**
	 * Get all of the files that are associated with the given Persistent
	 * instance.
	 * 
	 * @param clazz
	 *            The <code>Class</code> of the <code>Persistent</code>.
	 * @param id
	 *            The id of the owning <code>Persistent</code>.
	 * @return <code>List</code> of <code>javax.activation.FileDataSource</code>
	 *         .
	 */
	public List<FileDataSource> getFiles(Class<? extends Persistent> clazz,
			Integer id) {
		File dir = getPersistentInstanceFolder(clazz, id);
		List<FileDataSource> files = new ArrayList<FileDataSource>();
		if (dir.exists()) {
			for (File f : dir.listFiles()) {
				files.add(new FileDataSource(f));
			}
		}
		return files;
	}

	/**
	 * Get a single file that is owned by a given <code>Persistent</code>.
	 * 
	 * @param p
	 *            <code>Persistent</code>.
	 * @param fileName
	 *            The name of the file.
	 * @return <code>FileDataSource</code>.
	 */
	public FileDataSource getFile(Persistent p, String fileName) {
		return getFile(p.getClass(), p.getId(), fileName);
	}

	public FileDataSource getFile(Class<? extends Persistent> clazz,
			Integer id, String fileName) {
		File f = new File(getPersistentInstanceFolder(clazz, id), fileName);
		if (f.exists()) {
			return new FileDataSource(f);
		}
		throw new IllegalArgumentException("File " + f.getAbsolutePath()
				+ " does not exist!");
	}

    /**
     * Returns the thumbnail of the file identified by the supplied class, id and file name.  If no thumbnail
     * exists, the original file will be returned.
     * There is an assumption here that the file being requested
     * is an image file.  If it is not,
     * @param clazz the type of persistent object the file is associated with.
     * @param id the id of the persistent object the file is associated with.
     * @param fileName the name of the file to return.
     * @param width (optional) the width of the desired thumbnail.
     *              (Default: {@link #DEFAULT_THUMBNAIL_WIDTH})
     * @param height (optional) the height of the desired thumbnail.
     *               (Default: {@link #DEFAULT_THUMBNAIL_HEIGHT})
     * @param clipped (optional) true if the original image should be clipped to fix the thumbnail aspect ratio.
     * @return a FileDataSource containing the contents of the requested file.
     */
    public FileDataSource getFileThumbnail(Class<? extends Persistent> clazz,
                                           Integer id, String fileName, Integer width, Integer height, Boolean clipped) throws IOException {

        if (width == null || width <= 0) {
            width = DEFAULT_THUMBNAIL_WIDTH;
        }
        if (height == null || height <= 0) {
            height = DEFAULT_THUMBNAIL_HEIGHT;
        }
        if (clipped == null) {
            clipped = true;
        }
        File folder = getPersistentInstanceFolder(clazz, id);
        File original = new File(folder, fileName);
        String thumbFileName = thumbnailName(original, width, height, clipped);

        File thumb = new File(folder, thumbFileName);
        if (!thumb.exists()) {
            // Make sure the image file exists.

            if (!original.exists()) {
                throw new IllegalArgumentException("File " + original.getAbsolutePath() + " does not exist!");
            }

            thumb = createThumbnail(original, width, height, clipped);
        }
        return new FileDataSource(thumb);
    }

    /**
     * Creates a thumbnail of the specified size from the supplied file.
     * @param file the file to create a thumbnail of.
     * @param width the desired width of the thumbnail.
     * @param height the desired height of the thumbnail.
     * @param clipped whether the original image should be clipped to fill the thumbnail (if false, the thumbnail
     *                will contain blank space unless the aspect ratio of the thumbnail matches that of the
     *                original).
     * @return the File containing the thumbnail image.
     * @throws IOException if there is an error reading or writing the file.
     */
    private File createThumbnail(File file, int width, int height, boolean clipped) throws IOException {
        BufferedImage thumb = ImageUtil.resizeImage(new FileInputStream(file), width, height, clipped);
        File thumbFile = new File(file.getParentFile(), thumbnailName(file, width, height, clipped));
        ImageUtil.saveImage(thumbFile, thumb, THUMBNAIL_CONTENT_TYPE);

        return thumbFile;
    }

    /**
     * Creates a standardized name for a thumbnail image based on it's properties.  The name takes the format:
     * thumb_{width}_{height}_clip_{original file name}.png where the _clip section is only present if the
     * clipped parameter is true.
     *
     * @param original the file to be made into a thumbnail.
     * @param width the width of the thumbnail.
     * @param height the height of the thumbnail.
     * @param clipped whether the original was clipped or not.
     * @return the name of the thumbnail.
     */
    private String thumbnailName(File original, int width, int height, boolean clipped) {
        StringBuilder name = new StringBuilder(THUMBNAIL_FILENAME_PREFIX);
        name.append("_").append(Integer.toString(width)).append("_").append(Integer.toString(height));
        if (clipped) {
            name.append("_").append("clip");
        }
        name.append("_").append(original.getName()).append(THUMBNAIL_FILE_EXTENSION);
        return name.toString();
    }

	private File getPersistentFolder(Class<? extends Persistent> clazz) {
		if (!this.persistentFolders.containsKey(clazz)) {
			String[] folders = clazz.getName().split("\\.");
			File parentFolder = this.storageDirectory;
			for (String f : folders) {
				File folder = new File(parentFolder, f);
				if (!folder.exists()) {
				    if (!folder.mkdir()) {
				        logger.error("Could not make directory");
				    }
				}
				parentFolder = folder;
			}
			this.persistentFolders.put(clazz, parentFolder);
		}
		return this.persistentFolders.get(clazz);
	}

	private File getPersistentInstanceFolder(Class<? extends Persistent> clazz,
			Integer id) {
		File instanceDir = new File(getPersistentFolder(clazz), id.toString());
		if (!instanceDir.exists()) {
		    if (!instanceDir.mkdir()) {
		        logger.error("Could not make directory");
		    }
		}
		return instanceDir;
	}

	private String stripPath(String filePath, String pathDelimitor) {
		return filePath.substring(filePath.lastIndexOf(pathDelimitor) + 1,
				filePath.length());
	}
    public void deleteFile (Persistent p, String fileName) {
        FileDataSource file = getFile(p, fileName);
        try{
            if(!file.getFile().delete()){
                logger.error("Failed to delete file: " + fileName);
            }
        } catch (SecurityException e){
            logger.error(e.getMessage(), e);
        }
    }
}
