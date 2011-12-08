package dk.frv.aisspy.status;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class Formatter {

	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

	public static String dateFormat(Date d) {
		if (d == null)
			return "-";
		return dateFormat.format(d);
	}

	public static String rateFormat(double rate) {
		return String.format("%.2f", rate);
	}
	
	public static String getISO8620(Date date) {
		SimpleDateFormat iso8601gmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		TimeZone tzGMT = TimeZone.getTimeZone("GMT+0000");
		iso8601gmt.setTimeZone(tzGMT);
		return iso8601gmt.format(date);
	}

	public static String getISO8601(Date date) {
		if (date == null) {
			return "";
		}
		SimpleDateFormat iso8601gmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss'Z'");
		TimeZone tzGMT = TimeZone.getTimeZone("GMT+0000");
		iso8601gmt.setTimeZone(tzGMT);
		return iso8601gmt.format(date);
	}
	
}
