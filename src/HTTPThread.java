import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

/**
 * Created by IntelliJ IDEA. User: nrjs Date: Oct 26, 2005 Time: 3:33:01 PM To
 * change this template use File | Settings | File Templates.
 */
public class HTTPThread implements Runnable {
	InputStream input;
	OutputStream output;

	HTTPThread(InputStream in, OutputStream out) {
		this.input = in;
		this.output = out;
	}

	public void run() {
		// IMPORTANT: class path must include root directory of a server		
		
		// Important for chunked transfer encoding, each response line must
		// terminate with CR+LF
		System.setProperty("line.separator", "\r\n");

		// Create logger
		Logger logger = null;
		Handler logHandler;
		try {
			// Create log directory
			File logDir = new File("logs");
			if (!logDir.exists()) {
				logDir.mkdirs();
			}
			// use file for log
			logHandler = new FileHandler("logs/access.log", true);
			logHandler.setFormatter(new SimpleFormatter());
			throw new Exception();
		} catch (Exception e) {
			// e.printStackTrace();
			// fall back to console
			logHandler = new StreamHandler(System.out, new SimpleFormatter());
		}
		logger = Logger.getLogger("ServerAccessLog");
		logger.addHandler(logHandler);

		// Start ServerCore which deals with file locations, MIME types,
		// configuration, vhosts, etc.
		ServerCore server = new ServerCore(logger);

		BufferedReader reader = new BufferedReader(new InputStreamReader(input));

		// Keep connection alive
		boolean alive = true;

		HTTPResponse response = null;
		HTTPRequest request = null;

		String line;
		ArrayList<String> requestLines;

		/*
		 * Continue reading requests and sending responses until connection is
		 * closed or error occurs
		 */

		try {
			while (alive) {
				try {
					// Skip empty lines
					line = reader.readLine();
					while (line != null && line.trim().length() == 0) {
						line = reader.readLine();
					}

					// Read request
					requestLines = new ArrayList<String>();
					while (line != null && line.trim().length() > 0) {
						requestLines.add(line);
						line = reader.readLine();
					}
				} catch (IOException e) {
					throw new ServerException(500, "I/O error");
				}

				// No request, close the connection
				if (line == null) {
					// End connection
					alive = false;

				}

				if (requestLines.size() > 0) {
					logger.info("Request: " + requestLines.get(0));
					// Parse request and headers
					request = new HTTPRequest();
					request.parseFullRequest(requestLines);

					// Create and send response
					response = server.createResponse(request, output);

					if (response.getService() != null) {
						// Run service
						response.runService();
					} else if (response.getServlet() != null) {
						// Run response
						response.runServlet();
					} else {
						// It is a redirect (304)
						response.sendResponseHeader();
						response.getWriter().flush();
					}
				}
			}
		} catch (ServerException e) {
			// Some kind of error
			// Run Error service
			logger.warning("RESPONSE: Exception " + e.getMessage());
			Service errorService = new Error(e);
			errorService.service(request, response);
			e.printStackTrace();
		}

		// Shutdown
		try {
			if (response != null) {
				response.getWriter().close();
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
