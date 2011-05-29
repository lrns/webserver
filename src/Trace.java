
import java.io.PrintWriter;
import java.util.Date;
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: nrjs
 * Date: Oct 27, 2005
 * Time: 7:22:55 AM
 * To change this template use File | Settings | File Templates.
 */
public class Trace implements Servlet
{
    public void service(Request request, Response response)
    {
        PrintWriter out = response.getWriter();
        response.setResponseType("text/html");
        response.sendResponseHeader();

        out.println("<html>");
        out.println("<title>HTTP Trace</title>");
        out.println();

        out.println("<H1>HTTP Request</H1>");
        out.println("<P>Method = " + request.getMethod() + "</P>");
        out.println("<P>Request URI = " + request.getRequestURI() + "</P>");
        out.println("<P>Protocol Version = " + request.getProtocol() + "</P>");
        out.println("<P>");

        out.println("<H1>HTTP Headers</H1>");
        Iterator i = request.getHeaderNames();
        while ( i.hasNext() )
        {
            String header = (String) i.next();
            out.println("<P>" + header + request.getHeader(header) + "</P>");
        }

        out.close();

    }
}
