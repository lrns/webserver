
import java.io.PrintWriter;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: nrjs
 * Date: Oct 27, 2005
 * Time: 6:33:18 AM
 * To change this template use File | Settings | File Templates.
 */
public class Example implements Servlet
{
    public void service(Request request, Response response)
    {
        PrintWriter out = response.getWriter();
        response.setResponseType("text/plain");
        response.sendResponseHeader();

        out.println("Hello from my Web Server");
        out.println("Today's date is " + new Date());
        out.close();
    }
}
