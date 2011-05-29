import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Date;

/**
 * Implementation of HTTP response
 * 
 * @author Laurynas Sukys
 * 
 */
public class HTTPResponse implements Response {

	/**
	 * Socket writer
	 */
	private PrintWriter writer;
	/**
	 * Content type
	 */
	private String type = "text/html";
	/**
	 * HTTP status code
	 */
	private String statusCode = "200 OK";
	/**
	 * Redirect to this URL
	 */
	private String redirect = null;
	/**
	 * Servlet handles response
	 */
	private Servlet servlet = null;

	/**
	 * Service handles response
	 */
	private Service service = null;

	/**
	 * Request for the response
	 */
	private HTTPRequest request;

	/**
	 * No content needed
	 */
	private boolean noContent = false;

	/**
	 * Socket output stream
	 */
	private OutputStream outputStream;

	/**
	 * Length of content
	 */
	private long contentLength = -1;

	/**
	 * Send content using chunked encoding
	 */
	private boolean chunkedEncoding = false;

	/**
	 * Send Keep-Alive header
	 */
	private boolean keepAlive = true;

	/**
	 * Buffer used to hold content.
	 * 
	 * Using buffer allows sending correct content length header
	 */
	private StringBuffer buffer = null;

	/**
	 * Send cache control header
	 */
	private String cacheControl = null;

	/**
	 * Root path of a server
	 */
	private File serverRoot;

	/**
	 * HTTP version
	 */
	private String version = "HTTP/1.1";

	/**
	 * Last modified header
	 */
	private String lastModified = null;
	/**
	 * Expires header
	 */
	private String expires = null;

	/**
	 * Constructor
	 * 
	 * @param outputStream
	 *            socket output stream
	 * @param request
	 *            request
	 * @param serverRoot
	 *            server root path
	 */
	public HTTPResponse(OutputStream outputStream, HTTPRequest request, File serverRoot) {
		this.outputStream = outputStream;
		this.writer = new PrintWriter(outputStream);
		this.request = request;
		this.serverRoot = serverRoot;
	}

	/**
	 * Set true if no content is given
	 * 
	 * @param noContent
	 */
	public void setNoContent(boolean noContent) {
		this.noContent = noContent;
	}

	/**
	 * Get root path of the server
	 * 
	 * @return
	 */
	public File getServerRoot() {
		return serverRoot;
	}

	/**
	 * Check what output is available.
	 * 
	 * E.g. HTTP 1.0 does not support chunked encoding
	 */
	public void detectAvailableOutput() {
		if (request.getProtocol().equals("HTTP/1.0")) {
			// No chunked encoding in HTTP 1.0
			chunkedEncoding = false;
		}
		// Assuming than Chunked TE is supported in all HTTP/1.1 clients

		// No Keep alive
		if (request.getHeader("Connection") == null
				|| request.getHeader("Connection").trim().toLowerCase().indexOf("keep-alive") < 0) {
			keepAlive = false;
		}
	}

	/**
	 * @param cacheControl
	 *            the cacheControl to set
	 */
	public void setCacheControl(String cacheControl) {
		this.cacheControl = cacheControl;
	}

	/**
	 * @param lastModified
	 *            the lastModified to set
	 */
	public void setLastModified(String lastModified) {
		this.lastModified = lastModified;
	}

	/**
	 * @param expires
	 *            the expires to set
	 */
	public void setExpires(String expires) {
		this.expires = expires;
	}

	/**
	 * @return the chunkedEncoding
	 */
	public boolean isChunkedEncoding() {
		return chunkedEncoding;
	}

	/**
	 * @param chunkedEncoding
	 *            the chunkedEncoding to set
	 */
	public void setChunkedEncoding(boolean chunkedEncoding) {
		this.chunkedEncoding = chunkedEncoding;
	}

	/**
	 * @return the keepAlive
	 */
	public boolean isKeepAlive() {
		return keepAlive;
	}

	/**
	 * @param keepAlive
	 *            the keepAlive to set
	 */
	public void setKeepAlive(boolean keepAlive) {
		this.keepAlive = keepAlive;
	}

	/**
	 * @return the outputStream
	 */
	public OutputStream getOutputStream() {
		return outputStream;
	}

	@Override
	public PrintWriter getWriter() {
		return writer;
	}

	@Override
	public void setResponseType(String type) {
		this.type = type;
	}

	@Override
	public void sendResponseHeader() {
		// Check if Chunked Encoding and Keep-Alive are supported
		detectAvailableOutput();

		// Send all headers

		writer.println(version + " " + statusCode);
		writer.println("Date: " + ServerCore.getGMTDate(new Date()));
		writer.println("Server: " + ServerCore.serverName);

		if (redirect != null) {
			// Redirect
			writer.println("Location: " + redirect);
		} else {
			if (!noContent) {
				writer.println("Content-Type: " + type);
			}
		}
		if (contentLength >= 0) {
			writer.println("Content-Length: " + contentLength);
		}
		if (chunkedEncoding) {
			writer.println("Transfer-Encoding: Chunked");
		}
		if (keepAlive) {
			writer.println("Connection: Keep-Alive");
		}
		if (lastModified != null) {
			writer.println("Last-Modified: " + lastModified);
		}
		if (cacheControl != null) {
			writer.println("Cache-Control: " + cacheControl);
		}
		if (expires != null) {
			writer.println("Expires: " + expires);
		}

		writer.println();
	}

	/**
	 * Buffer one line
	 * 
	 * @param line
	 */
	public void bufferLine(String line) {
		if (buffer == null) {
			// Buffer is empty, create new
			buffer = new StringBuffer();
			// Check if Chunked encoding is supported
			detectAvailableOutput();
		}
		String s = line + "\n";
		if (chunkedEncoding) {
			buffer.append(Integer.toHexString(s.getBytes().length) + "\n");
		}

		buffer.append(s + "\n");

	}

	public int getBufferLength() {
		return buffer == null ? 0 : buffer.length();
	}

	/**
	 * Output both headers and content
	 */
	public void printHeadersAndContent() {
		if (!chunkedEncoding) {
			contentLength = buffer.toString().getBytes().length;
		}
		sendResponseHeader();
		printContent();
	}

	/**
	 * Output buffered content
	 */
	public void printContent() {
		// End with 0 if chunked encoding is used
		if (chunkedEncoding) {
			buffer.append("0");
		}
		writer.println(buffer.toString());
		buffer = null;
		writer.println();
		writer.flush();
	}

	/**
	 * Buffer server resource file from ROOT/resources
	 * 
	 * Useful for loading html templates
	 * 
	 * @param file
	 *            file to buffer
	 * @param title
	 *            used for setting html page template title
	 */
	public void bufferResource(String file, String title) {
		try {

			DataInputStream in = new DataInputStream(getClass().getResourceAsStream("/resources/"+ file));

			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			String line;
			StringBuffer text = new StringBuffer();

			while ((line = reader.readLine()) != null) {
				if (title != null) {
					// Set page title
					line = line.replace(":title:", title);
				}
				text.append(line + "\n");
			}
			bufferLine(text.toString());

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @return the contentLength
	 */
	public long getContentLength() {
		return contentLength;
	}

	/**
	 * @param contentLength
	 *            the contentLength to set
	 */
	public void setContentLength(long contentLength) {
		this.contentLength = contentLength;
	}

	/**
	 * Run servlet's service(request, response) method
	 */
	public void runServlet() {
		if (servlet != null) {
			servlet.service(request, this);
		}
	}

	/**
	 * Run service's service(request, response) method
	 */
	public void runService() {
		if (service != null) {
			service.service(request, this);
		}
	}

	public void setStatusCode(String code) {
		this.statusCode = code;
	}

	public void setServlet(Servlet servlet) {
		this.servlet = servlet;
	}

	public void setService(Service service) {
		this.service = service;
	}

	public Servlet getServlet() {
		return servlet;
	}

	public Service getService() {
		return service;
	}

	public void setRedirect(String location) {
		this.redirect = location;
	}

	public void setContent(boolean content) {
		this.noContent = content;
	}

}
