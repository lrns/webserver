import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Service for displaying error messages
 * 
 * @author Laurynas Sukys
 * 
 */
public class Error implements Service {

	// Errors
	public final static int ERROR_400_BAD_REQUEST = 400;
	public final static int ERROR_403_FORBIDDEN = 403;
	public final static int ERROR_404_NOT_FOUND = 404;
	public final static int ERROR_500_INTERNAL = 500;
	public final static int ERROR_501_NOT_IMPLEMENTED = 501;
	public final static int ERROR_503_UNAVAILABLE = 503;
	public final static int ERROR_505_VERSION_NOT_SUPPORTED = 505;

	/**
	 * Description of an error
	 */
	private String description = null;

	/**
	 * Error code
	 */
	private int code;

	/**
	 * Exception which caused an error
	 */
	private Throwable exception = null;

	/**
	 * Extract error from ServerException
	 * 
	 * @param e
	 */
	public Error(ServerException e) {
		this(e.getCode(), e.getMessage(), e.getCause());
	}

	/**
	 * Error by code
	 * 
	 * @param code
	 */
	public Error(int code) {
		this(code, null, null);
	}

	/**
	 * Error by code and description
	 * 
	 * @param code
	 * @param description
	 */
	public Error(int code, String description) {
		this(code, description, null);
	}

	/**
	 * Code by code, description and cause exception
	 * 
	 * @param code
	 * @param description
	 * @param exception
	 *            cause
	 */
	public Error(int code, String description, Throwable exception) {
		this.description = description;
		this.code = code;
		this.exception = exception;
	}

	@Override
	public void service(HTTPRequest request, HTTPResponse response) {
		response.setResponseType("text/html");
		response.setStatusCode(getTitle(code));

		response.setChunkedEncoding(true);
		response.setKeepAlive(true);

		try {

			StringBuffer text = new StringBuffer();
			response.bufferResource("header.html", getTitle(code));

			// Title
			text.append("<div id=\"header\"><div id=\"error\"><h1>Error " + getTitle(code)
					+ "</h1>\n");

			// Short description
			text.append("<h2>" + getShortDescription(code) + "</h2>");

			// Custom description
			if (description != null) {
				text.append("<p class=\"error_description\">" + description + "</p>");
			}

			// Display exception stack trace
			if (exception != null) {
				text.append("<a href=\"#\" id=\"expand_div\" onclick=\"toggle_visibility('stack_trace');\">Exception: "
						+ exception.getMessage() + "</a>\n");

				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				exception.printStackTrace(pw);
				String stackTrace = ServerCore.newlineToBreak(sw.toString());
				text.append("<p id=\"stack_trace\">" + stackTrace + "</p>\n");
			}
			text.append("</div></div>\n");

			response.bufferLine(text.toString());
			response.bufferResource("footer.html", null);

			response.printHeadersAndContent();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * Get title for error code
	 * 
	 * @param code
	 * @return title
	 */
	public String getTitle(int code) {
		String title = "";

		switch (code) {
		case ERROR_400_BAD_REQUEST:
			title = "400 Bad Request";
			break;
		case ERROR_403_FORBIDDEN:
			title = "403 Forbidden";
			break;
		case ERROR_404_NOT_FOUND:
			title = "404 Not Found";
			break;
		case ERROR_500_INTERNAL:
			title = "500 Internal Server Error";
			break;
		case ERROR_501_NOT_IMPLEMENTED:
			title = "501 Not Implemented";
			break;
		case ERROR_503_UNAVAILABLE:
			title = "503 Service Unavailable";
			break;
		case ERROR_505_VERSION_NOT_SUPPORTED:
			title = "505 HTTP Version Not Supported";
			break;
		default:
			title = "500 Internal Server Error";
			break;
		}

		return title;
	}

	/**
	 * Get description for error code
	 * 
	 * @param code
	 * @return description
	 */
	public String getShortDescription(int code) {
		String description = "";

		switch (code) {
		case ERROR_400_BAD_REQUEST:
			description = "The request cannot be fulfilled due to bad syntax.";
			break;
		case ERROR_403_FORBIDDEN:
			description = "The access to the resource was forbidden.";
			break;
		case ERROR_404_NOT_FOUND:
			description = "The requested URL was not found on this server. If you entered the URL manually please check your spelling and try again.";
			break;
		case ERROR_500_INTERNAL:
			description = "Internal server error occured. Please try again later.";
			break;
		case ERROR_501_NOT_IMPLEMENTED:
			description = "This HTTP method is not implemented";
			break;
		case ERROR_503_UNAVAILABLE:
			description = "The server is currently unavailable. Please try again later.";
			break;
		case ERROR_505_VERSION_NOT_SUPPORTED:
			description = "The server does not support the HTTP protocol version used in the request.";
			break;
		default:
			description = "";
			break;
		}
		return description;
	}

}
