/**
 * Custom exception for HTTP error
 * 
 * @author Laurynas Sukys
 * 
 */
public class ServerException extends Exception {

	private static final long serialVersionUID = 1L;

	/**
	 * Error code
	 */
	private int code;

	/**
	 * 
	 * @param code
	 *            error code
	 * @param message
	 *            error description
	 */
	public ServerException(int code, String message) {
		super(message);
		this.code = code;
	}

	/**
	 * 
	 * @param code
	 *            error code
	 * @param message
	 *            error description
	 * @param cause
	 *            cause of an error
	 */
	public ServerException(int code, String message, Exception cause) {
		super(message, cause);
		this.code = code;
	}

	/**
	 * Get error code
	 * 
	 * @return error code
	 */
	public int getCode() {
		return code;
	}

}
