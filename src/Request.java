
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: nrjs
 * Date: Oct 26, 2005
 * Time: 9:12:56 PM
 * To change this template use File | Settings | File Templates.
 */
public interface Request
{
    String getParameter(String name);
    Iterator getParameterNames();
    String getProtocol();
    String getHost();
    String getHeader(String name);
    Iterator getHeaderNames();
    String getQueryString();
    String getRequestURI();
    String getMethod();
}
