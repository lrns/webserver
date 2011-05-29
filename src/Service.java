/**
 * Interface similar to Servlet but more powerful, usign custom HTTPRequest and
 * HTTPResponse implementations
 * 
 * Used because of limited functionality of Response and Request interfaces
 * 
 * @author Laurynas Sukys
 * 
 */
public interface Service {
	void service(HTTPRequest request, HTTPResponse response);
}
