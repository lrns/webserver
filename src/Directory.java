import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;

/**
 * Directory Listing
 * 
 * @author Laurynas Sukys
 * 
 */
public class Directory implements Service {

	/**
	 * Directory to list
	 */
	File dir = null;

	/**
	 * Constructor
	 * 
	 * @param dir
	 *            Directory to list
	 */
	public Directory(File dir) {
		this.dir = dir;
	}

	@Override
	public void service(HTTPRequest request, HTTPResponse response) {

		response.setResponseType("text/html");
		response.setChunkedEncoding(true);
		response.setKeepAlive(true);

		String uri = request.getRequestURI();
		try {

			StringBuffer text = new StringBuffer();

			response.bufferResource("header.html", "Index of " + uri + "");

			text.append("<div id=\"header\"><h1>Index of " + uri + "</h1></div>\n");

			File[] list = dir.listFiles();

			/**
			 * Sorting set by passing parameters: column: name, time, size
			 * order: asc, desc
			 * 
			 * By default, sorted by name asc.
			 */
			String sortColumn = request.getParameter("column");

			boolean sortAsc = true;
			String sortOrder = request.getParameter("order");
			if (sortOrder != null && sortOrder.equals("desc")) {
				sortAsc = false;
			}

			String nameSortAnchor = uri + "?column=name&order=asc";
			String timeSortAnchor = uri + "?column=time&order=asc";
			String sizeSortAnchor = uri + "?column=size&order=asc";

			String nameClass = "";
			String timeClass = "";
			String sizeClass = "";

			if (sortColumn == null) {
				sortColumn = "name";
			}

			if (sortColumn.equals("name")) {
				Arrays.sort(list, new FileNameComparator(sortAsc));
				if (sortAsc) {
					nameSortAnchor = uri + "?column=name&order=desc";
					nameClass = "asc";
				} else {
					nameClass = "desc";
				}
			} else if (sortColumn.equals("time")) {
				Arrays.sort(list, new ModificationTimeComparator(sortAsc));
				if (sortAsc) {
					timeSortAnchor = uri + "?column=time&order=desc";
					timeClass = "asc";
				} else {
					timeClass = "desc";
				}
			} else if (sortColumn.equals("size")) {
				Arrays.sort(list, new FileSizeComparator(sortAsc));
				if (sortAsc) {
					sizeSortAnchor = uri + "?column=size&order=desc";
					sizeClass = "asc";
				} else {
					sizeClass = "desc";
				}
			}

			text.append("<div id=\"content\"><table id=\"directory\" cellspacing=\"0\" cellpadding=\"0\"><thead><tr>");

			// Table format: Icon Name Time Size
			text.append("<th>&nbsp;</th><th>");
			text.append("<a href=\"" + nameSortAnchor + "\" class=\"" + nameClass + "\">Name</a>");
			text.append("</th><th>");
			text.append("<a href=\"" + timeSortAnchor + "\" class=\"" + timeClass
					+ "\">Modification Time</a>");
			text.append("</th><th>");
			text.append("<a href=\"" + sizeSortAnchor + "\" class=\"" + sizeClass + "\">Size</a>");
			text.append("</th></tr></thead>");
			text.append("<tbody>");

			// Show link to parent directory
			if (!uri.equals("/")) {
				text.append("<tr><td>&nbsp;</td><td class=\"file_name\"><a href=\"..\">Parent Directory</a></td>"
						+ "<td>&nbsp;</td><td>&nbsp;</td>");
			}
			response.bufferLine(text.toString());

			// Go trough directory content
			for (int i = 0; i < list.length; i++) {

				// Clear text string buffer
				text.delete(0, text.length());

				File file = list[i];

				// Don't display hidden files
				if (!file.isHidden()) {

					String link = file.getName();
					String image;
					String extension;

					// File icon
					if (file.isDirectory()) {
						extension = "dir";
						link += "/";
						image = "/icons/dir.png";
					} else {
						extension = ServerCore.getFileExtension(file.getName());
						image = "/icons/" + extension + ".png";
						if (getClass().getResourceAsStream("/resources" + image) == null) {
							image = "/icons/_blank.png";
						}
					}

					text.append("<tr class=\"" + (i % 2 == 0 ? "even" : "odd") + "\">");
					text.append("<td><img src=\"" + image + "\" alt=\"" + extension + "\"/></td>");
					text.append("<td class=\"file_name\"><a href=\"" + link + "\">"
							+ file.getName() + "</a></td>");

					// Show size for files
					String sizeString;
					if (file.isFile()) {
						DecimalFormat format = new DecimalFormat("#.#");
						long bytes = file.length();

						if (bytes < 1024) {
							// bytes
							sizeString = "" + bytes;
						} else {
							double size = ((double) bytes) / 1024;
							if (size < 1024) {
								// KB
								sizeString = format.format(size) + "K";
							} else {
								size = size / 1024;
								if (size < 1024) {
									// MB
									sizeString = format.format(size) + "M";
								} else {
									// GB
									size = size / 1024;
									sizeString = format.format(size) + "G";
								}
							}

						}

					} else {
						sizeString = "-";
					}

					// Modification date
					SimpleDateFormat dateFormat = new SimpleDateFormat("d-M-yyyy HH:mm");
					String lastModified = dateFormat.format(new Date(file.lastModified()));
					text.append("<td>" + lastModified + "</td>");
					text.append("<td>" + sizeString + "</td>");
					response.bufferLine(text.toString());
				}
			}
			response.bufferLine("</tbody></table></div>");
			response.bufferResource("footer.html", null);

			response.printHeadersAndContent();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}

/**
 * Compare by file names (case insensitive)
 * 
 */

class FileNameComparator implements Comparator<File> {
	private boolean ascending = true;

	public FileNameComparator() {
	}

	public FileNameComparator(boolean ascending) {
		this.ascending = ascending;
	}

	public int compare(File f1, File f2) {

		return (ascending ? 1 : -1)
				* f1.getName().toLowerCase().compareTo(f2.getName().toLowerCase());
	}
}

/**
 * Compare by file modification date
 * 
 */
class ModificationTimeComparator implements Comparator<File> {
	private boolean ascending = true;

	public ModificationTimeComparator() {
	}

	public ModificationTimeComparator(boolean ascending) {
		this.ascending = ascending;
	}

	public int compare(File f1, File f2) {

		return (ascending ? 1 : -1)
				* new Long(f1.lastModified()).compareTo(new Long(f2.lastModified()));
	}
}

/**
 * Compare by file size
 * 
 * Directory < File
 */
class FileSizeComparator implements Comparator<File> {
	private boolean ascending = true;

	public FileSizeComparator() {
	}

	public FileSizeComparator(boolean ascending) {
		this.ascending = ascending;
	}

	public int compare(File f1, File f2) {

		if (f1.isDirectory() && f2.isDirectory()) {
			// Both directories
			return (ascending ? 1 : -1)
					* f1.getName().toLowerCase().compareTo(f2.getName().toLowerCase());
		} else if (f1.isDirectory() && f2.isFile()) {
			// Directory and a file
			return (ascending ? 1 : -1) * -1;
		} else if (f1.isFile() && f2.isDirectory()) {
			// File and a directory
			return (ascending ? 1 : -1) * 1;
		} else if (f1.length() == f2.length()) {
			// Same size, compare names
			return (ascending ? 1 : -1)
					* f1.getName().toLowerCase().compareTo(f2.getName().toLowerCase());
		} else {
			// Compare sizes
			return (ascending ? 1 : -1) * new Long(f1.length()).compareTo(new Long(f2.length()));
		}
	}
}