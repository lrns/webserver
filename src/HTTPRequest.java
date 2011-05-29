import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Implementation of HTTP Request
 * 
 * @author Laurynas Sukys
 * 
 */
public class HTTPRequest implements Request {


	/**
	 * Request headers
	 */
	HashMap<String, String> headers;
	/**
	 * Request parameters
	 */
	HashMap<String, String> parameters;
	/**
	 * Request method, currently only GET is implemented
	 */
	String method;
	/**
	 * Requested resource
	 */
	String requestURI;
	/**
	 * Full request query string
	 */
	String queryString;
	/**
	 * Request protocol, both HTTP/1.0 and HTTP/1.1 are accepted
	 */
	String protocol;

	/**
	 * Constructor
	 */
	public HTTPRequest() {
		headers = new HashMap<String, String>();
		parameters = new HashMap<String, String>();
	}

	/**
	 * Parse request and extract data
	 * @param lines request lines
	 * @throws ServerException
	 */
	public void parseFullRequest(ArrayList<String> lines) throws ServerException {

		String request = null;
		
		// Request not empty
		if (lines.size() > 0) {
			// Request must be the first line
			request = lines.get(0);
			lines.remove(0);

			// Extract headers
			for (String line : lines) {
				// Check if line is a header
				int pos = line.indexOf(":");
				if (pos > 0) {
					headers.put(line.substring(0, pos), line.substring(pos + 2)
							.trim());
				}
			}
			
			// Parse main request line
			parseRequestString(request);
			
			// HTTP/1.1 request must send 'Host' header
			if (protocol.equals("HTTP/1.1") && !headers.containsKey("Host")) {
				throw new ServerException(400, "Host header is required");
			}

		} else {
			throw new ServerException(400, "Empty request");
		}
	}

	/**
	 * Parse main request line, e.g. GET /
	 * 
	 * Currently supports only GET requests
	 * 
	 * @param request
	 */
	public void parseRequestString(String request) throws ServerException {
		String[] req = request.split(" ");
		
			// GET request
			if (req[0].equals("GET")) {
				try {
				method = "GET";
				protocol = req[2];

				// Parameters separated by '?' or ';'
				if (req[1].indexOf("?") > 0 || req[1].indexOf(";") > 0) {
					// Extract URI and query string
					// Expected input: URI?p1=1&p2=2
					String[] query = req[1].split("\\?");
					queryString = query[1];
					requestURI = query[0];
					extractParameters(queryString);
				} else {
					// No parameters
					queryString = "";
					requestURI = req[1];
				}

				queryString = req[1];
				} catch (Exception e) {
					e.printStackTrace();
					throw new ServerException(400, e.getMessage());
				}
				
			} else {
				throw new ServerException(501, "Request method '" +  req[0] + "' is not implemented");
			}

	}

	/**
	 * Extract parameters from query Expected input in format: p1=1&p2=2&p3=3
	 * 
	 * @param query
	 */
	private void extractParameters(String query) {
		// Extract parameter key=value pairs
		String[] params;
		if (query.indexOf("&") > 0) {
			// Separated by '&'
			params = query.split("&");
		} else { 
			// Separated by ';'
			params = query.split(";");
		}
		for (int i = 0; i < params.length; i++) {
			// Check if it is a key=value pair, i.e. contains '='
			if (params[i].indexOf("=") > 0) {
				// Extract key and value from each pair
				String[] parameter = params[i].split("=");

				// Use empty value if only 'param=' is given
				parameters.put(parameter[0],
						parameter.length == 2 ? parameter[1] : "");
			} else {
				// Only key specified, no '=' in parameter
				parameters.put(params[i], "");
			}

		}

	}

	@Override
	public String getParameter(String name) {
		return parameters.get(name);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Iterator getParameterNames() {
		return parameters.keySet().iterator();
	}

	@Override
	public String getProtocol() {
		return protocol;
	}

	@Override
	public String getHost() {
		return headers.containsKey("Host") ? headers.get("Host") : "";
	}

	@Override
	public String getHeader(String name) {
		return headers.get(name);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Iterator getHeaderNames() {
		return headers.keySet().iterator();
	}

	@Override
	public String getQueryString() {
		return queryString;
	}

	@Override
	public String getRequestURI() {
		return requestURI;
	}

	@Override
	public String getMethod() {
		return method;
	}

	/**
	 * Print request nicely
	 */
	public String toString() {
		String s = "HTTP Request" + "\n";
		s += "Method = " + getMethod() + "\n";
		s += "Request URI = " + getRequestURI() + "\n";
		s += "Protocol Version = " + getProtocol() + "\n";
		s += "\n";

		s += "HTTP Headers:" + "\n";
		@SuppressWarnings("rawtypes")
		Iterator i = getHeaderNames();
		while (i.hasNext()) {
			String header = (String) i.next();
			s += header + " = " + getHeader(header) + "\n";
		}

		s += "\nParameters:" + "\n";
		i = getParameterNames();
		while (i.hasNext()) {
			String parameter = (String) i.next();
			s += parameter + " = " + getParameter(parameter) + "\n";
		}
		return s;
	}

	/**
	 * Print request nicely in HTML
	 */
	public String toHTML() {
		String s = "<h1>HTTP Request" + "</h2>\n";
		s += "<p>Method = " + getMethod() + "</p>\n";
		s += "<p>Request URI = " + getRequestURI() + "</p>\n";
		s += "<p>Protocol Version = " + getProtocol() + "</p>\n";
		s += "\n";

		s += "<h2>HTTP Headers:</h2>" + "\n";
		@SuppressWarnings("rawtypes")
		Iterator i = getHeaderNames();
		while (i.hasNext()) {
			String header = (String) i.next();
			s += "<p>" + header + " = " + getHeader(header) + "</p>\n";
		}

		s += "\n<h2>Parameters:</h2>" + "\n";
		i = getParameterNames();
		while (i.hasNext()) {
			String parameter = (String) i.next();
			s += "<p>" + parameter + " = " + getParameter(parameter) + "</p>\n";
		}
		return s;
	}

}
