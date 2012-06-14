package au.com.gaiaresources.bdrs.controller.file;

import au.com.gaiaresources.bdrs.db.Persistent;
import au.com.gaiaresources.bdrs.file.FileService;
import au.com.gaiaresources.bdrs.model.file.ManagedFile;
import au.com.gaiaresources.bdrs.model.file.ManagedFileDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.activation.FileDataSource;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.http.HTTPException;

@Controller
public class DownloadFileController extends AbstractDownloadFileController {
    
    public static final String FILE_DOWNLOAD_URL = "/files/download.htm";
    public static final String FILE_THUMBNAIL_DOWNLOAD_URL = "/files/downloadThumbnail.htm";
    
    public static final String CLASS_NAME_QUERY_PARAM = "className";
    public static final String INSTANCE_ID_QUERY_PARAM = "id";
    public static final String FILENAME_QUERY_PARAM = "fileName";
    public static final String WIDTH_QUERY_PARAM = "width";
    public static final String HEIGHT_QUERY_PARAM = "height";
    public static final String CLIPPED_QUERY_PARAM = "clipped";
    
    @Autowired
    private ManagedFileDAO managedFileDAO;
    
    @RequestMapping(value = FILE_DOWNLOAD_URL, method = RequestMethod.GET)
    public ModelAndView downloadFile(@RequestParam(CLASS_NAME_QUERY_PARAM) String className,
                                     @RequestParam(INSTANCE_ID_QUERY_PARAM) Integer id,
                                     @RequestParam(FILENAME_QUERY_PARAM) String fileName,
                                     HttpServletResponse response) {
        try {
            return super.downloadFile(className, id, fileName);
        }
        catch(HTTPException e) {
            response.setStatus(e.getStatusCode());
            return null;
        }
    }

    /**
     * Downloads a thumbnail image of a specified file, optionally specifying the desired size and clipping
     * behaviour.  If a thumbnail with the required properties exists, it will be returned, otherwise it will
     * be created then returned.
     * @param className identifies the persistent class the image is associated with.
     * @param id identifies the instance of the persistent class the image is associated with.
     * @param fileName the name of the original file a thumbnail is desired for.
     * @param width (optional) the width of the desired thumbnail.
     *              (Default: {@link au.com.gaiaresources.bdrs.file.FileService#DEFAULT_THUMBNAIL_WIDTH})
     * @param height (optional) the height of the desired thumbnail.
     *               (Default: {@link au.com.gaiaresources.bdrs.file.FileService#DEFAULT_THUMBNAIL_HEIGHT})
     * @param clipped (optional) true if the original image should be clipped to fix the thumbnail aspect ratio.
     * @param response the http response being produced.
     * @return a ModelAndView containing the thumbnail.
     */
    @RequestMapping(value = FILE_THUMBNAIL_DOWNLOAD_URL, method = RequestMethod.GET)
    public ModelAndView downloadFileThumbnail(@RequestParam(CLASS_NAME_QUERY_PARAM) String className,
                                              @RequestParam(INSTANCE_ID_QUERY_PARAM) Integer id,
                                              @RequestParam(FILENAME_QUERY_PARAM) String fileName,
                                              @RequestParam(value = WIDTH_QUERY_PARAM, required = false) Integer width,
                                              @RequestParam(value = HEIGHT_QUERY_PARAM, required = false) Integer height,
                                              @RequestParam(value = CLIPPED_QUERY_PARAM, required = false) Boolean clipped,
                                              HttpServletResponse response) {
        try {
            Class<? extends Persistent>  persistentClass = (Class<? extends Persistent>) Class.forName(className);
            FileDataSource file = fileService.getFileThumbnail(persistentClass, id, fileName, width, height, clipped);
            return downloadFile(fileName, FileService.THUMBNAIL_CONTENT_TYPE, false, file);

        } catch (Exception e) {
            log.error(String.format("Unable to download thumbnail for class: %s, id: %s, file: %s", className, id, fileName), e);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }
    }
    
    @RequestMapping(value="/files/downloadByUUID.htm", method=RequestMethod.GET)
    public ModelAndView downloadFileByUUID(HttpServletResponse response,
            @RequestParam(value="uuid", required=true) String uuid,
            @RequestParam(value="encode", required=false, defaultValue="false") boolean base64encode) {
        
    	log.debug("starting at file service " + System.currentTimeMillis() + " uuid=" + uuid);
        if(uuid == null) {
            response.setStatus(404);
            return null;
        }
        
        ManagedFile mf = managedFileDAO.getManagedFile(uuid);
        if(mf == null) {
            response.setStatus(404);
            return  null;
        }
        if(base64encode){
        	log.debug("returning from file service " + System.currentTimeMillis() + " uuid=" + uuid);
        	return super.downloadFile(mf.getClass().getName(), mf.getId(), mf.getFilename(), "application/javascript", base64encode);
        }else{
        	return super.downloadFile(mf.getClass().getName(), mf.getId(), mf.getFilename(), mf.getContentType());
        }
    }
}
