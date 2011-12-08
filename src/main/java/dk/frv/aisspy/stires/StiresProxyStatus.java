package dk.frv.aisspy.stires;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import dk.frv.ais.country.Country;

public class StiresProxyStatus {
	
	private static final Logger LOG = Logger.getLogger(StiresProxyStatus.class);

	// Proxy settings
	private Country country;
	private String server;
	private int port;
	private String system;

	// Basic status, is the proxy running
	private boolean running = false;
	private Date lastCheck = null;
	
	// Stires Proxy properties
	private boolean enabled = false;
	private String stiresRemoteAddr;
	private boolean conntectedToStires = false;
	private double flow = 0;
	
	// Provider status
	private boolean providerConnected = false;
	private boolean providerDelivering = false;

	public StiresProxyStatus() {
	}

	public synchronized boolean isRunning() {
		return running;
	}

	public synchronized void setRunning(boolean running) {
		this.running = running;
	}

	public synchronized boolean isProviderConnected() {
		return providerConnected;
	}

	public synchronized void setProviderConnected(boolean providerConnected) {
		this.providerConnected = providerConnected;
	}

	public Country getCountry() {
		return country;
	}

	public void setCountry(Country country) {
		this.country = country;
	}

	public String getServer() {
		return server;
	}

	public void setServer(String server) {
		this.server = server;
	}

	public synchronized double getFlow() {
		return flow;
	}

	public synchronized void setFlow(double flow) {
		this.flow = flow;
	}

	public String getSystem() {
		return system;
	}

	public void setSystem(String system) {
		this.system = system;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getStatusUrl() {
		return "http://" + server + ":" + port + "/status.asp";
	}
	
	public String getCredentialsStatusUrl() {
		return "http://admin:safeseanet@" + server + ":" + port + "/status.asp";
	}
	
	public synchronized boolean isConntectedToStires() {
		return conntectedToStires;
	}
	
	public synchronized void setConntectedToStires(boolean conntectedToStires) {
		this.conntectedToStires = conntectedToStires;
	}
	
	public synchronized String getStiresRemoteAddr() {
		return stiresRemoteAddr;
	}
	
	public synchronized void setStiresRemoteAddr(String stiresRemoteAddr) {
		this.stiresRemoteAddr = stiresRemoteAddr;
	}

	public synchronized void parseProxyHtml(String content) {
		setLastCheck(new Date());
		setEnabled(false);
		
		if (content == null) {
			setRunning(false);
			return;
		}
		setRunning(true);

		// Stires connection part
		if (!parseStiresConnectionPart(content)) {
			LOG.error("Failed to parse content: " + content + " for proxy: " + this.toString());
			setRunning(false);
			return;
		}
		if (!parseStiresProviderPart(content)) {
			LOG.error("Failed to parse content: " + content + " for proxy: " + this.toString());
			setRunning(false);
			return;
			
		}
	}
	
	private boolean parseStiresProviderPart(String content) {
		String part = getPart(content, "CONNECTION TO DATA PROVIDER");
		if (part == null) {			
			return false;
		}
		// Check if connected to Gatehouse subscriber proxy
		this.providerConnected = (part.indexOf("Connected (Client mode)") > 0);
		// Check if data is received from subscriber proxy
		this.providerDelivering = (part.indexOf("Data transmission from provider OK") > 0);
		return true;
	}
	
	private boolean parseStiresConnectionPart(String content) {
		String part = getPart(content, "CONNECTION TO STIRES");
		if (part == null) {			
			return false;
		}
		// Determine if connected to stires
		this.conntectedToStires = (part.indexOf("<TD>Connected</TD>") > 0);
		// Determine if enabled
		this.enabled = (part.indexOf("Data transmission to STIRES enabled</TD>") > 0);
		// Get stires remote address
		//Pattern pattern = Pattern.compile("(\\d+\\.\\d+.\\d+.\\d+:.\\d+)");
		Pattern pattern = Pattern.compile("(\\w+\\.\\w+.\\w+.\\w+:.\\d+)");
		Matcher m = pattern.matcher(part);
		if (m.find()) {
			this.stiresRemoteAddr = m.group(0);			
		} else {
			LOG.warn("Could not find Stires remote address");
		}
		// Get flow
		// Do some truncating
		int index = part.indexOf("10s: ");
		String truncPart = part.substring(index + 3);
		pattern = Pattern.compile("10s: \\d+ - (\\S+) msg/s");
		m = pattern.matcher(truncPart);
		if (m.find()) {
			try {
				this.flow = Double.parseDouble(m.group(1));
			} catch (Exception e) {
				LOG.warn("Could not find Stires flow: " + e.getMessage());
			}
		} else {
			flow = 0;
			LOG.warn("Could not find Stires flow for: " + this.toString() + "\npart: " + part);
		}
		
		return true;
	}
	
	private static String getPart(String content, String headline) {
		Pattern pattern = Pattern.compile(headline + "(.*?)</div>");
		Matcher m = pattern.matcher(content);
		if (m.find()) {
			return m.group(0);
		}		
		LOG.error("Could not find headline: " + headline);
		return null;
	}
	
	public synchronized boolean isEnabled() {
		return enabled;
	}
	
	public synchronized void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	
	public synchronized boolean isProviderDelivering() {
		return providerDelivering;
	}
	
	public synchronized void setProviderDelivering(boolean providerDelivering) {
		this.providerDelivering = providerDelivering;
	}
	
	public synchronized Date getLastCheck() {
		return lastCheck;
	}
	public synchronized void setLastCheck(Date lastCheck) {
		this.lastCheck = lastCheck;
	}
	
	public synchronized boolean isError() {
		return (!isRunning() || !isConntectedToStires() || !isProviderConnected());
	}
	
	public synchronized boolean isWarning() {
		return (!isProviderDelivering());
	}
	
	public synchronized String getStatus() {
		if (isError()) return "ERROR";
		if (isWarning()) return "WARNING";
		return "OK";
	}

	@Override
	public synchronized String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("StiresProxyStatus [conntectedToStires=");
		builder.append(conntectedToStires);
		builder.append(", country=");
		builder.append((country != null) ? country.getTwoLetter() : "null");
		builder.append(", enabled=");
		builder.append(enabled);
		builder.append(", flow=");
		builder.append(flow);
		builder.append(", port=");
		builder.append(port);
		builder.append(", providerConnected=");
		builder.append(providerConnected);
		builder.append(", providerDelivering=");
		builder.append(providerDelivering);
		builder.append(", running=");
		builder.append(running);
		builder.append(", server=");
		builder.append(server);
		builder.append(", stiresRemoteAddr=");
		builder.append(stiresRemoteAddr);
		builder.append(", system=");
		builder.append(system);
		builder.append(", lastCheck=");
		builder.append(lastCheck);
		builder.append("]");
		return builder.toString();
	}
	
	
	public synchronized String getHttpReply() {
		StringBuilder builder = new StringBuilder();
		builder.append("status=" + getStatus());
		builder.append("&country=");
		builder.append((country != null) ? country.getTwoLetter() : "null");
		builder.append("&system=");
		builder.append(system);
		builder.append("&server=");
		builder.append(server);
		builder.append("&running=");
		builder.append(running);
		builder.append("&conntectedToStires=");
		builder.append(conntectedToStires);		
		builder.append("&enabled=");
		builder.append(enabled);
		builder.append("&flow=");
		builder.append(flow);
		builder.append("&providerConnected=");
		builder.append(providerConnected);
		builder.append("&providerDelivering=");
		builder.append(providerDelivering);		
		builder.append("&lastCheck=");
		builder.append(lastCheck);		
		builder.append("&stiresRemoteAddr=");
		builder.append(stiresRemoteAddr);			
		builder.append("&port=");
		builder.append(port);		
		return builder.toString();
	}

}
