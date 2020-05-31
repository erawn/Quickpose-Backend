package template.tool;

import java.io.File;

public class Utils {
	public static String getPath(String theFilename) {
		if (theFilename.startsWith("/")) {
			return theFilename;
		}
		return File.separator + "data" + File.separator + theFilename;
	}

}
