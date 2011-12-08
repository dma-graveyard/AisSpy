package dk.frv.aisspy.http;

import org.apache.commons.lang.StringUtils;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Class to hold HTTP parameters
 */
public class HttpParams extends HashMap<String, String[]> implements Map<String, String[]> {

	private static final long serialVersionUID = -6112117400237282207L;

	/**
	 * Empty constructor
	 */
	public HttpParams() {
		super();
	}

	/**
	 * Constructor with query string as argument
	 * 
	 * @param queryString
	 *            query
	 */
	public HttpParams(String queryString) {
		super();
		addParams(queryString);
	}

	/**
	 * Contructor with HashMap<String, String[]> as argument
	 * 
	 * @param params
	 *            parameters
	 */
	public HttpParams(Map<String, String[]> params) {
		super();
		for (Map.Entry<String, String[]> entry : params.entrySet()) {
			put(entry.getKey(), entry.getValue().clone());
		}
	}

	/**
	 * Get the value of parameter key as a number
	 * 
	 * @param key
	 *            key
	 * @return value
	 */
	public Integer getNumeric(String key) {
		return getNumericTrim(key, false);
	}

	/**
	 * Get the value of the parameter key as a number. All whitespace characters
	 * are removed.
	 * 
	 * @param key
	 *            key
	 * @param trim
	 *            if true trim
	 * @return value
	 */
	public Integer getNumericTrim(String key, boolean trim) {
		if (!containsKey(key)) {
			return null;
		}
		String[] values = get(key);
		try {
			if (values.length > 0) {
				if (trim) {
					return Integer.parseInt(values[0].replaceAll("\\s+", ""));
				} else {
					return Integer.parseInt(values[0]);
				}
			} else {
				return null;
			}
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/**
	 * Get the value of the (first) parameter as boolean
	 * 
	 * @param key
	 *            key
	 * @return value
	 */
	public boolean getBool(String key) {
		if (!containsKey(key)) {
			return false;
		} else
			return Boolean.parseBoolean(getFirst(key));
	}

	/**
	 * Get the first string value of parameter key
	 * 
	 * @param key
	 *            key
	 * @return value or null if not present
	 */
	public String getFirst(String key) {
		if (!containsKey(key)) {
			return null;
		}
		String[] values = get(key);
		if (values.length > 0) {
			return values[0];
		}
		return "";
	}

	public String getExisting(String key) {
		String first = getFirst(key);
		if (first == null || first.length() == 0) {
			return null;
		}
		return first;
	}

	public boolean exists(String key) {
		return (containsKey(key) && getFirst(key).length() > 0);
	}

	/**
	 * Inserts a key value pair
	 * 
	 * @param key
	 *            key
	 * @param value
	 *            value
	 */
	public void put(String key, String value) {
		String[] values = new String[1];
		values[0] = value;
		put(key, values);
	}

	/**
	 * Make a string with form key1=value1&key2=value2&....keyN=valueN
	 * 
	 * @param encode
	 *            URL encode values or not
	 * @return new query string with all values
	 */
	public String makeQueryString(boolean encode) {
		Set<Map.Entry<String, String[]>> entries = entrySet();
		String[] keyAndValues = new String[entries.size()];
		int keyIndex = 0;
		try {
			Iterator<Map.Entry<String, String[]>> it = entries.iterator();
			while (it.hasNext()) {
				Map.Entry<String, String[]> entry = it.next();
				String[] values = new String[entry.getValue().length];
				for (int valueIndex = 0; valueIndex < entry.getValue().length; valueIndex++) {
					String value = Array.get(entry.getValue(), valueIndex).toString();
					Array.set(values, valueIndex, (encode) ? URLEncoder.encode(value, "UTF-8") : value);
				}
				Array.set(keyAndValues, keyIndex++, entry.getKey() + "=" + StringUtils.join(values, ","));
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}

		return StringUtils.join(keyAndValues, "&");
	}

	/**
	 * Make a string with form key1=value1&key2=value2&....keyN=valueN. The
	 * values are URL encoded.
	 * 
	 * @return new queryString
	 */
	public String makeQueryString() {
		return makeQueryString(true);
	}

	/**
	 * Add parameters from a query string
	 * 
	 * @param queryString
	 *            queryString
	 */
	public void addParams(String queryString) {
		if (queryString == null) {
			return;
		}
		String[] pairs = StringUtils.split(queryString, '&');
		for (String pair : pairs) {
			String[] keyValue = StringUtils.split(pair, '=');
			String[] values = new String[0];
			if (keyValue.length > 1) {
				values = StringUtils.split(keyValue[1], ',');
			}
			for (int j = 0; j < values.length; j++) {
				try {
					values[j] = URLDecoder.decode(values[j], "UTF-8");
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}
			if (keyValue.length > 1) {
				put(keyValue[0], values);
			}
		}
	}

}
