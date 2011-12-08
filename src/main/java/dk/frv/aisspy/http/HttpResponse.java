package dk.frv.aisspy.http;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class HttpResponse {

	private static final String VERSION = "HTTP/1.0";
	private static final String CRLF = "\r\n";
	private static final String SERVER = "AisSpy";

	private Map<String, String> headerFields = new HashMap<String, String>();
	private String content = "";
	private int status;

	public HttpResponse() {
		// Fill in the default headers
		headerFields.put("Connection", "close");
		headerFields.put("Server", SERVER);
		headerFields.put("Content-Type", "text/plain");
		headerFields.put("Content-Length", "0");
		headerFields.put("Pragma", "no-cache");
		headerFields.put("Cache-Control", "no-cache");		
		status = HttpURLConnection.HTTP_OK;
	}

	public void setHeader(String header, String value) {
		headerFields.put(header, value);
	}

	public void setContent(String content) {
		this.content = content;
		headerFields.put("Content-Length", Integer.toString(content.length()));
	}

	public void setContentType(String contentType) {
		headerFields.put("Content-Type", contentType);
	}

	public void setBadRequest() {
		status = HttpURLConnection.HTTP_BAD_REQUEST;
	}

	public void setMethodNotAllowed() {
		status = HttpURLConnection.HTTP_BAD_METHOD;
	}

	public void setNotSupported() {
		status = HttpURLConnection.HTTP_VERSION;
	}

	public void setNotFound() {
		status = HttpURLConnection.HTTP_NOT_FOUND;
	}

	public void setInternalError() {
		status = HttpURLConnection.HTTP_INTERNAL_ERROR;
	}

	public int getStatus() {
		return this.status;
	}

	private String getReasonText() {
		switch (status) {
		case HttpURLConnection.HTTP_OK:
			return "OK";
		case HttpURLConnection.HTTP_VERSION:
			return "HTTP Version Not Supported";
		case HttpURLConnection.HTTP_BAD_METHOD:
			return "Method Not Allowed";
		case HttpURLConnection.HTTP_NOT_FOUND:
			return "Not Found";
		case HttpURLConnection.HTTP_BAD_REQUEST:
			return "Bad Request";
		case HttpURLConnection.HTTP_INTERNAL_ERROR:
			return "Internal Server Error";
		}
		return "";
	}

	public void makeResponse(Socket socket) {
		StringBuffer sb = new StringBuffer();
		sb.append(VERSION);
		sb.append(" " + Integer.toString(status));
		sb.append(" " + getReasonText() + CRLF);
		for (String header : headerFields.keySet()) {
			sb.append(header + ": " + headerFields.get(header) + CRLF);
		}
		sb.append(CRLF);
		sb.append(content);

		try {
			OutputStream out = socket.getOutputStream();
			out.write(sb.toString().getBytes());
		} catch (IOException e) {
		}
	}

}
