import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Date;

/**
 * Send a file to a client
 * 
 * @author Laurynas Sukys
 * 
 */
public class FileService implements Service {

	/**
	 * File to send
	 */
	File file = null;

	/**
	 * Resource to send
	 */
	InputStream resource = null;

	/**
	 * Constructor
	 * 
	 * @param file
	 *            file to send
	 */
	public FileService(File file) {
		this.file = file;
	}

	/**
	 * Constructor
	 * 
	 * @param resource
	 *            InputStream of a resource
	 */
	public FileService(InputStream resource) {
		this.resource = resource;
	}

	public void service(HTTPRequest request, HTTPResponse response) {
		if (resource != null) {
			serviceResource(request, response);
			return;
		}

		// Show content length for all files
		response.setContentLength(file.length());

		// Last modified header
		long lastModified = file.lastModified();
		response.setLastModified(ServerCore.getGMTDate(new Date(lastModified)));

		// Expire after 1 week for files
		int expireTime = 7 * 24 * 3600;
		if (file.getName().endsWith(".html") || file.getName().endsWith(".htm")) {
			expireTime = 3600; // 1 hour for html documents
		}
		response.setExpires(ServerCore.getGMTDate(new Date(new Date().getTime() + expireTime * 1000)));

		response.sendResponseHeader();
		response.getWriter().flush();

		// Send file in binary format to ensure it's no affected by text
		// encoding and locales
		try {
			response.getOutputStream().flush();

			byte[] fileBytes = new byte[(int) file.length()];

			BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
			in.read(fileBytes, 0, fileBytes.length);

			response.getOutputStream().write(fileBytes, 0, fileBytes.length);
			response.getOutputStream().flush();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Send local resource instead of file
	 * 
	 * @param request
	 * @param response
	 */
	public void serviceResource(HTTPRequest request, HTTPResponse response) {
		try {
			// Show content length for all files
			response.setContentLength(resource.available());

			response.sendResponseHeader();
			response.getWriter().flush();

			// Send file in binary format to ensure it's no affected by text
			// encoding and locales

			response.getOutputStream().flush();

			byte[] fileBytes = new byte[resource.available()];

			BufferedInputStream in = new BufferedInputStream(resource);
			in.read(fileBytes, 0, fileBytes.length);

			response.getOutputStream().write(fileBytes, 0, fileBytes.length);
			response.getOutputStream().flush();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
