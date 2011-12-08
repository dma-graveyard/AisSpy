package dk.frv.aisspy.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.lang.StringUtils;

public class HttpRequestHandler {
	
	private static final String CRLF = "\r\n";

	private Socket socket;
	private HttpResponse errorResponse = new HttpResponse();
	private String requestLine;
	private String requestURI;
	private Map<String, String> headerFields = new HashMap<String, String>();
	private HttpParams params = null;

	public HttpRequestHandler(Socket socket) {
		this.socket = socket;
	}

	public void parse() throws IOException {
		BufferedReader reader;

		try {
			reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

			// Read the request line
			String headerLine = reader.readLine();
			if (headerLine == null || headerLine.equals(CRLF) || headerLine.equals("")) {
				errorResponse.setBadRequest();
				throw new IOException("Empty header line");				
			}
			StringTokenizer tokenizer = new StringTokenizer(headerLine, " ");
			if (tokenizer.countTokens() != 3) {
				errorResponse.setBadRequest();
				throw new IOException("Wrong header line: " + headerLine);
			}
			String method = tokenizer.nextToken();
			requestLine = tokenizer.nextToken();
			String version = tokenizer.nextToken();

			if (!method.equals("GET")) {
				errorResponse.setMethodNotAllowed();
				throw new IOException("Method " + method + " not allowed");
			}
			if (!version.equals("HTTP/1.0") && !version.equals("HTTP/1.1")) {
				errorResponse.setNotSupported();
				throw new IOException("Unsupported HTTP version: " + version);
			}

			parseRequestLine();

			while (true) {
				headerLine = reader.readLine();
				if (headerLine.equals(CRLF) || headerLine.equals("") || headerLine.equals("\n"))
					break;
				tokenizer = new StringTokenizer(headerLine, ":");
				if (tokenizer.countTokens() > 1) {
					headerFields.put(tokenizer.nextToken(), tokenizer.nextToken());
				}

			}

		} catch (IOException e) {
			errorResponse.setBadRequest();
			throw e;
		}

	}

	public void parseRequestLine() {
		String queryString = "";
		StringTokenizer tokenizer = new StringTokenizer(requestLine, "?");
		requestURI = tokenizer.nextToken();
		if (tokenizer.countTokens() > 0) {
			queryString = tokenizer.nextToken();
		}
		params = new HttpParams(queryString);
	}

	public String getRequestURI() {
		return requestURI;
	}

	public HttpParams getHttpParams() {
		return params;
	}

	public HttpResponse getErrorResponse() {
		return errorResponse;
	}

	public Socket getSocket() {
		return socket;
	}

	public String getRequestCommand() {
		String uri = getRequestURI();
		if (uri == null) {
			return "";
		}
		String[] parts = StringUtils.split(uri, '/');
		if (parts.length == 0) {
			return "";
		}
		return parts[parts.length - 1];
	}

}
