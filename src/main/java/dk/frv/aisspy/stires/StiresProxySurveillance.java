package dk.frv.aisspy.stires;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;

public class StiresProxySurveillance extends Thread {

	private static final Logger LOG = Logger.getLogger(StiresProxySurveillance.class);

	private StiresSettings stiresSettings;

	public StiresProxySurveillance(StiresSettings settings) {
		this.stiresSettings = settings;
	}

	@Override
	public void run() {

		while (true) {

			for (StiresProxyStatus proxyStatus : stiresSettings.getProxyStatuses()) {
				// HTTP call
				String response = httpGet(proxyStatus.getStatusUrl());
				// Evaluate response
				proxyStatus.parseProxyHtml(response);
			}

			// Wait
			try {
				Thread.sleep(stiresSettings.getInterval());
			} catch (InterruptedException e) {
			}

		}
	}

	private String httpGet(String url) {
		HttpClient client = new HttpClient();
		client.getHttpConnectionManager().getParams().setConnectionTimeout(2000);
		Credentials creds = new UsernamePasswordCredentials("admin", "safeseanet");
		client.getState().setCredentials(AuthScope.ANY, creds);
		HttpMethod method = new GetMethod(url);

		String responseBody = null;
		int resCode = 0;
		try {
			resCode = client.executeMethod(method);
			BufferedReader reader = new BufferedReader(new InputStreamReader(method.getResponseBodyAsStream()));
			String line;
			StringBuilder builder = new StringBuilder();
			while ((line = reader.readLine()) != null) {
				builder.append(line);
			}
			responseBody = builder.toString();
			method.releaseConnection();
		} catch (IOException e) {

			LOG.info("Failed to get URL=" + url + " " + e.getMessage());
			return null;
		}
		if (resCode != 200) {
			return null;
		}
		return responseBody;
	}

}
