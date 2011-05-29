import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.util.logging.Logger;

/**
 * Utility class which contains various useful methods, creates responses, deals
 * with files, vhosts, MIME types, etc.
 * 
 * @author Laurynas Sukys
 * 
 */
public class ServerCore {

	/**
	 * Configuration
	 */
	private Properties properties;

	/**
	 * Vhosts Properties object
	 */
	private Properties vhostsProperties;

	/**
	 * Vhosts
	 */
	private Map<String, File> vhosts;

	/**
	 * Registered Servlets
	 */
	private ArrayList<String> servlets;

	/**
	 * Logger
	 */
	private Logger logger;

	/**
	 * Root folder of a server
	 */
	private File serverRoot;

	/**
	 * Key - file extension, value - application type
	 */
	private HashMap<String, String> mimeTypes;

	/**
	 * Server name
	 */
	public static String serverName = "Simple Java Web Server by Laurynas Sukys";

	/**
	 * Constructor
	 * 
	 * @param logger
	 *            logger object
	 */
	public ServerCore(Logger logger) {
		this.logger = logger;
		properties = new Properties();
		vhostsProperties = new Properties();

		// Detect server root folder
		URL classesDirectory = getClass().getProtectionDomain().getCodeSource().getLocation();
		File classFolder = new File(classesDirectory.getFile());

		if (classFolder.getAbsolutePath().endsWith("bin")) {
			// Class is in bin folder, use parent folder as a server root
			serverRoot = new File(new File(classesDirectory.getFile()).getParent());
		} else {
			// Assumed that classes are in server root folder
			serverRoot = new File(classesDirectory.getFile());
		}

		try {
			// Read configuration
			properties.load(getClass().getResourceAsStream("server.config"));

			// Read vhosts
			vhostsProperties.load(getClass().getResourceAsStream("vhosts.config"));
			parseVhosts();
			parseServlets();

			// Read MIME types from local /resources/mime.types
			getMimeTypes();
		} catch (Exception e) {
			logger.severe("Failed parsing configuration, server exited.");
			e.printStackTrace();
			System.exit(1);
		}

	}

	/**
	 * Extract vhosts from vhost Properties object
	 * 
	 * @throws ServerException
	 *             if no vhosts are given
	 */
	private void parseVhosts() throws ServerException {
		vhosts = new HashMap<String, File>();
		Enumeration<?> e = vhostsProperties.propertyNames();
		while (e.hasMoreElements()) {
			String key = (String) e.nextElement();
			String value = vhostsProperties.getProperty(key);
			File file = null;
			if (value.startsWith("/")) {
				// absolute path
				file = new File(value);
			} else {
				// path, relative to the server
				file = new File(serverRoot, value);
			}
			if (!file.exists() || !file.isDirectory()) {
				logger.severe("Wrong vhost directory: " + key + " = " + value);
			} else {
				vhosts.put(key.toLowerCase(), file);
			}
		}
		if (vhosts.size() == 0) {
			throw new ServerException(500, "No vhosts in configuration");
		}
	}

	/**
	 * Read MIME types from local /resources/mime.types file
	 * 
	 */
	private void getMimeTypes() {

		mimeTypes = new HashMap<String, String>();
		try {

			DataInputStream in = new DataInputStream(getClass().getResourceAsStream(
					"/resources/mime.types"));

			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			String line;

			// Each line is in format:
			// application-type ext1 etx2 ext3
			while ((line = reader.readLine()) != null) {
				String[] parts = line.split("\\s+");

				// There are some extensions for that type given
				if (parts.length > 1) {
					for (int i = 1; i < parts.length; i++) {
						mimeTypes.put(parts[i].toLowerCase(), parts[0]);
					}
				}
			}

			in.close();
		} catch (Exception e) {
			logger.severe("Failed to read MIME types: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Get MIME type from file extension
	 * 
	 * @param uri
	 *            path to file
	 * @return MIME type
	 */
	public String getFileType(String uri) {
		return getMimeType(getFileExtension(uri));
	}

	/**
	 * Get extension of file name
	 * 
	 * @param name
	 *            file name with extension
	 * @return extension
	 */
	public static String getFileExtension(String name) {
		int pos = name.lastIndexOf(".");
		String extension = "";
		if (pos >= 0) {
			extension = name.substring(pos + 1).toLowerCase();
		}
		return extension;
	}

	/**
	 * Check if local file was modified
	 * 
	 * @param sinceString
	 *            was file modified after this time
	 * @param file
	 *            file to check
	 * @return true if file was modified
	 */
	public boolean wasModifiedSince(String sinceString, File file) {
		long lastModified = file.lastModified();
		SimpleDateFormat dateFormat = new SimpleDateFormat("d MMM yyyy HH:mm:ss z");
		Date since = null;
		try {
			since = dateFormat.parse(sinceString);

		} catch (ParseException e) {
			logger.severe("Exception: " + e.getMessage());
			e.printStackTrace();
			return true;
		}
		return since.getTime() < lastModified;

	}

	/**
	 * Parse the list of registered servlets from the config file
	 */
	private void parseServlets() {
		servlets = new ArrayList<String>();
		if (properties.getProperty("servlets") != null) {
			String[] list = properties.getProperty("servlets").split(",");
			for (int i = 0; i < list.length; i++) {
				String className = list[i].trim();
				try {
					// Exception is thrown if such class cannot be found
					// Only existing servlets are 'registered'
					Class.forName(className);
					servlets.add(className.toLowerCase());
				} catch (Exception e) {
					logger.severe("Could not find servlet '" + className + "'");
				}

			}
		}
	}

	/**
	 * Check if it is a registered servlet
	 * 
	 * @param name
	 *            servlet's name
	 * @return true if such servlet is registered
	 */
	public boolean isServlet(String name) {
		return servlets.contains(name.toLowerCase());
	}

	/**
	 * Get value of the property
	 * 
	 * @param name
	 *            name of property
	 * @return value
	 */
	public String getProperty(String name) {
		return properties.getProperty(name);
	}

	/**
	 * Get MIME type of an extension
	 * 
	 * @param extension
	 *            file extension
	 * @return MIME type, text/plain if no type is found
	 */
	public String getMimeType(String extension) {
		// Return plain text if extension is not found
		return mimeTypes.containsKey(extension) ? mimeTypes.get(extension) : "text/plain";
	}

	/**
	 * Format date to GMT format
	 * 
	 * @param date
	 *            date to format
	 * @return date in GMT format
	 */
	public static String getGMTDate(Date date) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("d MMM yyyy HH:mm:ss z");
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		return dateFormat.format(date);
	}

	/**
	 * Examine HTTP request and create a response Currently works only with GET
	 * requests
	 * 
	 * @param request
	 *            request
	 * @param outputStream
	 *            socket output stream
	 * @return response object
	 * @throws ServerException
	 */
	public HTTPResponse createResponse(HTTPRequest request, OutputStream outputStream)
			throws ServerException {
		// Check what file/servlet is requested and create appropriate response

		Servlet servlet = null;
		Service service = null;

		HTTPResponse response = new HTTPResponse(outputStream, request, serverRoot);
		try {

			String URI = request.getRequestURI();
			if (!URI.startsWith("/")) {
				// Bad Request, Error code 400
				logger.info("RESPONSE: Error 400 Bad Request " + URI);
				service = new Error(400, URI);
			} else {
				// Extract the first token from request /*/....
				int pos = URI.indexOf("/", 1);
				String name;
				if (pos > 1) {
					name = URI.substring(1, pos);
				} else {
					name = URI.substring(1);
				}

				// Is it a servlet?
				if (isServlet(name)) {
					// Make sure class name is lowercase with the first letter
					// uppercase
					String servletName = name.toLowerCase().substring(0, 1).toUpperCase()
							+ name.toLowerCase().substring(1);
					servlet = (Servlet) Class.forName(servletName).newInstance();
					logger.info("RESPONSE: Servlet " + servletName);
				} else {
					// Local resource
					// Works with resources in JAR file as well
					InputStream resource = resourceLocator(URI);
					if (resource != null) {
						// Send resource
						response.setResponseType(getFileType(URI));
						logger.info("RESPONSE: Resource " + URI);
						service = new FileService(resource);
					} else {

						// Is it a file
						File file = fileLocator(URI, request.getHeader("Host"));
						if (file.exists()) {
							if (file.canRead()) {
								if (file.isDirectory()) {
									// A directory
									// Redirect if it does not end with /
									if (!URI.endsWith("/")) {
										// Redirect
										logger.info("RESPONSE: Redirect to " + URI + "/");
										response.setStatusCode("302 Found");
										response.setRedirect(URI + "/");
									} else {
										// Directory

										// Check if index.html exists
										File indexFile = new File(file.getAbsoluteFile(),
												"index.html");
										if (indexFile.exists() && indexFile.isFile()
												&& indexFile.canRead()) {
											// Is index.html cached
											if (request.getHeader("If-Modified-Since") != null
													&& !wasModifiedSince(
															request.getHeader("If-Modified-Since"),
															file)) {
												response.setStatusCode("304 Not Modified");
												response.setNoContent(true);
												logger.info("RESPONSE: 304 Not Modified "
														+ indexFile.getAbsolutePath());
											} else {
												// Show index file
												response.setResponseType("text/html");

												service = new FileService(indexFile);
												logger.info("RESPONSE: File "
														+ indexFile.getAbsolutePath());
											}

										} else {
											// Directory listing
											logger.info("RESPONSE: Directory "
													+ file.getAbsolutePath());

											service = new Directory(file);
										}
									}
								} else {
									// File
									if (request.getHeader("If-Modified-Since") != null
											&& !wasModifiedSince(
													request.getHeader("If-Modified-Since"), file)) {
										// File is cached by client and it
										// wasn't
										// modified
										response.setStatusCode("304 Not Modified");
										response.setNoContent(true);
										logger.info("RESPONSE: 304 Not Modified "
												+ file.getAbsolutePath());
									} else {
										// Send file
										logger.info("RESPONSE: File " + file.getAbsolutePath());
										response.setResponseType(getFileType(file.getName()));
										service = new FileService(file);
									}
								}

							} else {
								// Forbidden - 403
								logger.info("RESPONSE: Error 403 Forbidden " + URI);
								service = new Error(403, URI);
							}
						} else {
							// File Not Found - 404
							logger.info("RESPONSE: Error 404 Not Found " + URI);
							service = new Error(404, URI);
						}
					}
				}
			}
		} catch (Exception e) {
			throw new ServerException(500, e.getMessage());
		}

		response.setServlet(servlet);
		response.setService(service);
		return response;
	}

	/**
	 * Find local resource in resources folder
	 * 
	 * Can work with resources in JAR file
	 * 
	 * @param URI
	 *            relative address of file
	 * @return InputStream object of a file
	 */
	public InputStream resourceLocator(String URI) {

		if (!URI.endsWith("/")) {
			return getClass().getResourceAsStream(
					"/resources" + (URI.startsWith("/") ? "" : "/") + URI);
		}
		return null;
	}

	/**
	 * Find local files using virtual hosts
	 * 
	 * @param URI
	 *            address of file
	 * @param host
	 *            vhost to use
	 * @return File object
	 */
	public File fileLocator(String URI, String hostName) {
		// remove port form host name
		String[] parts = hostName.split(":");
		String host = parts[0].toLowerCase();		

		// Use vhosts for a file
		File vhostRoot = null;
		if (vhosts.containsKey(host)) {
			// vhost found
			vhostRoot = vhosts.get(host);
		} else if (vhosts.containsKey("default_host")) {
			// default host used
			vhostRoot = vhosts.get("default_host");
		} else {
			// use the first vhost
			vhostRoot = vhosts.values().iterator().next();
		}

		return new File(vhostRoot, URI);

	}

	/**
	 * Convert newlines '\n' to html line breaks
	 * 
	 * @param s
	 *            some string
	 * @return string with newlines converted
	 */
	public static String newlineToBreak(String s) {
		return s.replace("\n", "<br>\n");
	}

}
