package dk.frv.aisspy.http;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import dk.frv.aisspy.AisSpy;
import dk.frv.aisspy.BaseStation;
import dk.frv.aisspy.FlowStatEntry;
import dk.frv.aisspy.Region;
import dk.frv.aisspy.SystemHandler;
import dk.frv.aisspy.status.BaseStationStatus;
import dk.frv.aisspy.status.CountryStatus;
import dk.frv.aisspy.status.Formatter;
import dk.frv.aisspy.status.Status;
import dk.frv.aisspy.status.SystemStatus;
import dk.frv.aisspy.stires.StiresProxyStatus;

public class HttpHandler extends Thread {

	private static final Logger LOG = Logger.getLogger(HttpHandler.class);

	private Socket socket;
	private HttpResponse httpResponse;
	private HttpRequestHandler httpRequestHandler;

	public HttpHandler(Socket socket) {
		this.socket = socket;
	}

	@Override
	public void run() {
		try {
			httpRequestHandler = new HttpRequestHandler(socket);
			// Parse request
			try {
				httpRequestHandler.parse();
			} catch (IOException e) {
				LOG.info("Failed to parse HTTP request: " + e.getMessage());
				httpResponse = httpRequestHandler.getErrorResponse();
				httpResponse.makeResponse(socket);
				socket.close();
				return;
			}
			
			httpResponse = new HttpResponse();
			String requestUri = httpRequestHandler.getRequestURI();

			if (requestUri.endsWith("full_status_text")) {
				fullStatusText();
			} else if (requestUri.endsWith("system_status")) {
				systemStatus();
			} else if (requestUri.endsWith("stires_status")) {
				fullStiresStatusText();
			} else if (requestUri.endsWith("stires_country_status")) {
				stiresCountryStatus();
			} else if (requestUri.endsWith("country_status")) {
				countryStatus();			
			} else if (requestUri.endsWith("region_status")) {
				regionStatus();
			} else if (requestUri.endsWith("base_station_status")) {
				baseStationStatus();
			} else if (requestUri.endsWith("full_status_js")) {
				fullStatusJS();
			} else if (requestUri.endsWith("bs_status_js")) {
				bsStatusJS();
			} else if (requestUri.endsWith("full_stires_status_js")) {
				fullStiresStatusJS();
			} else {
				httpResponse.setNotFound();
			}

			try {
				httpResponse.makeResponse(socket);
				socket.close();
			} catch (IOException e) {
				LOG.info("Failed to deliver reply: " + e.getMessage());
			} finally {
				socket.close();
			}
		} catch (IOException e) {
			LOG.info("Client socket failed: " + e.getMessage());
		}
	}
	
	private void baseStationStatus() {
		SystemHandler handler = getSystem();
		if (handler == null) {
			return;
		}
		String bsStr = getParam("bs");
		if (bsStr == null) {
			httpResponse.setBadRequest();
			httpResponse.setContent("Missing bs argument");
			return;
		}
		long bs;
		try {
			bs = Long.parseLong(bsStr);
		} catch (NumberFormatException e) {
			httpResponse.setBadRequest();
			httpResponse.setContent("Bad bs argument");
			return;
		}
		
		double rate = 0;
		Status status = Status.OK;
		Date last = new Date(0);
		
		FlowStatEntry bsFlow = handler.getStats().getBaseStationOrigin().get(bs);
		if (bsFlow == null) {
			status = Status.FAIL;
		} else {
			Date now = new Date();
			last = bsFlow.getLastReceived();
			rate = bsFlow.getRate();
			if (((now.getTime() - last.getTime()) / 1000) > handler.getBaseStationTimeout()) {
				status = Status.FAIL;
			}
		}
		
		String httpReply = String.format("status=%s&last_received=%s&rate=%s",status.name(), Formatter.dateFormat(last), Formatter
				.rateFormat(rate));
		httpResponse.setContent(httpReply);
	}
	
	private void regionStatus() {
		SystemHandler handler = getSystem();
		if (handler == null) {
			return;
		}
		String regionStr = getParam("region");
		if (regionStr == null) {
			httpResponse.setBadRequest();
			httpResponse.setContent("Missing region argument");
			return;
		}
		long region;
		try {
			region = Long.parseLong(regionStr);
		} catch (NumberFormatException e) {
			httpResponse.setBadRequest();
			httpResponse.setContent("Bad region argument");
			return;
		}
		
		double rate = 0;
		Status status = Status.OK;
		Date last = new Date(0);
		
		FlowStatEntry regionFlow = handler.getStats().getRegionOrigin().get(region);
		if (regionFlow == null) {
			status = Status.FAIL;
		} else {
			Date now = new Date();
			last = regionFlow.getLastReceived();
			rate = regionFlow.getRate();
			if (((now.getTime() - last.getTime()) / 1000) > handler.getRegionTimeout()) {
				status = Status.FAIL;
			}
		}
		
		String httpReply = String.format("status=%s&last_received=%s&rate=%s",status.name(), Formatter.dateFormat(last), Formatter
				.rateFormat(rate));
		httpResponse.setContent(httpReply);
	}

	private void countryStatus() {
		SystemHandler handler = getSystem();
		if (handler == null) {
			return;
		}
		SystemStatus systemStatus = handler.getSystemStatus();
		String country = getParam("country");
		if (country == null) {
			httpResponse.setBadRequest();
			httpResponse.setContent("Missing country argument");
			return;
		}
		CountryStatus countryStatus = systemStatus.getCountryStatus(country);
		if (countryStatus == null) {
			httpResponse.setBadRequest();
			httpResponse.setContent("Unknown country: " + country);
			return;
		}
		httpResponse.setContent(countryStatus.getHttpReply());
	}
	
	private void stiresCountryStatus() {
		String country = getParam("country");
		if (country == null) {
			httpResponse.setBadRequest();
			httpResponse.setContent("Missing country argument");
			return;
		}
		String system = getParam("system");
		if (system == null) {
			httpResponse.setBadRequest();
			httpResponse.setContent("Missing system argument");
			return;
		}
		// Get list of proxies for system,country
		HashMap<String, ArrayList<StiresProxyStatus>> countryMap = AisSpy.getStiresSettings().getSystemCountryStatuses().get(system);
		if (countryMap == null) {
			httpResponse.setBadRequest();
			httpResponse.setContent("Unknown system");
			return;
		}		
		List<StiresProxyStatus> proxies = countryMap.get(country);
		if (proxies == null || proxies.size() == 0) {
			httpResponse.setBadRequest();
			httpResponse.setContent("Unknown country for system");
			return;
		}
		
		for (StiresProxyStatus stiresProxyStatus : proxies) {
			if (stiresProxyStatus.isRunning() && stiresProxyStatus.isEnabled()) {
				httpResponse.setContent(stiresProxyStatus.getHttpReply());
				return;
			}
		}
		httpResponse.setContent(proxies.get(0).getHttpReply());
		
	}

	private void fullStiresStatusText() {
		StringBuilder buf = new StringBuilder();
		for (StiresProxyStatus proxyStatus : AisSpy.getStiresSettings().getProxyStatuses()) {
			buf.append(proxyStatus.getHttpReply() + "\n");
		}
		httpResponse.setContent(buf.toString());
	}

	private void fullStatusText() {
		StringBuilder buf = new StringBuilder();
		for (SystemHandler handler : AisSpy.getHandlers()) {
			SystemStatus systemStatus = handler.getSystemStatus();
			buf.append(systemStatus.toString());
		}
		httpResponse.setContent(buf.toString());
	}
	
	private void bsStatusJS() {
		List<String> lines = new ArrayList<String>();
		lines.add("var systems = new Array();");
		for (SystemHandler handler : AisSpy.getHandlers()) {
			SystemStatus stat = handler.getSystemStatus();
			String objName = "system" + stat.getName();
			lines.add("var " + objName + " = new Object();");
			lines.add(objName + ".name = '" + stat.getName() + "';");
			lines.add(objName + ".countries = new Array();");
			for (CountryStatus countryStatus : stat.getCountryStatuses()) {
				String countryName = countryStatus.getCountry().getTwoLetter();
				String cObjName = "country" + countryName;
				lines.add("var " + cObjName + " = new Object;");
				lines.add(cObjName + ".name = '" + countryName + "';");
				
				lines.add(cObjName + ".baseStations = new Array();");
				
				Collections.sort(countryStatus.getBaseStations());
				for (BaseStationStatus bsStatus : countryStatus.getBaseStations()) {
					String bsObjName = cObjName + "Bs" + bsStatus.getMmsi();
					lines.add("var " + bsObjName + " = new Object;");
					lines.add(bsObjName + ".mmsi = '" + bsStatus.getMmsi() + "';");
					lines.add(bsObjName + ".lastReceived = '" + Formatter.dateFormat(bsStatus.getLastReveived()) + "';");
					lines.add(bsObjName + ".lastReceivedUTC = '" + Formatter.getISO8601(bsStatus.getLastReveived()) + "';");
					String pos = String.format(Locale.US, "%.5f,%.5f", bsStatus.getPos().getLatitude(), bsStatus.getPos().getLongitude());
					lines.add(bsObjName + ".pos = '" + pos + "';");
					String lat = String.format(Locale.US, "%.5f", bsStatus.getPos().getLatitude());
					lines.add(bsObjName + ".lat = " + lat + ";");
					String lon = String.format(Locale.US, "%.5f", bsStatus.getPos().getLongitude());
					lines.add(bsObjName + ".lon = " + lon + ";");
					
					lines.add(cObjName + ".baseStations.push(" + bsObjName + ");");				
				}
				
				lines.add(objName + ".countries.push(" + cObjName + ");");
				
			}
			lines.add("systems.push(" + objName + ");");
		}
		httpResponse.setContentType("application/x-javascript;charset=UTF-8");
		httpResponse.setContent(StringUtils.join(lines, "\n"));
	}

	private void fullStatusJS() {
		List<String> lines = new ArrayList<String>();
		lines.add("var systems = new Array();");
		for (SystemHandler handler : AisSpy.getHandlers()) {
			SystemStatus stat = handler.getSystemStatus();

			String objName = "system" + stat.getName();
			lines.add("var " + objName + " = new Object();");
			lines.add(objName + ".name = '" + stat.getName() + "';");
			lines.add(objName + ".status = '" + stat.getStatus().name() + "';");
			lines.add(objName + ".lastReceived = '" + Formatter.dateFormat(stat.getLastReceived()) + "';");
			lines.add(objName + ".lastReceivedUTC = '" + Formatter.getISO8601(stat.getLastReceived()) + "';");
			lines.add(objName + ".rate = '" + Formatter.rateFormat(stat.getRate()) + " msg/min';");
			lines.add(objName + ".proxyConnected = '" + ((stat.isProxyConnected()) ? "YES" : "NO") + "';");
			lines.add(objName + ".countries = new Array();");

			for (CountryStatus countryStatus : stat.getCountryStatuses()) {
				String countryName = countryStatus.getCountry().getTwoLetter();
				String cObjName = "country" + countryName;
				lines.add("var " + cObjName + " = new Object;");
				lines.add(cObjName + ".name = '" + countryName + "';");
				lines.add(cObjName + ".status = '" + countryStatus.getStatus().name() + "';");
				lines.add(cObjName + ".lastReceived = '" + Formatter.dateFormat(countryStatus.getLastReceived()) + "';");
				lines.add(cObjName + ".lastReceivedUTC = '" + Formatter.getISO8601(countryStatus.getLastReceived()) + "';");
				lines.add(cObjName + ".rate = '" + Formatter.rateFormat(countryStatus.getRate()) + " msg/min';");
				
				lines.add(objName + ".countries.push(" + cObjName + ");");
			}
			
			lines.add(objName + ".failing_base_stations = new Array();");
			
			for (BaseStation baseStation : stat.getFailingBaseStaions()) {
				lines.add(objName + ".failing_base_stations.push('" + baseStation.toString() + "');");
			}
			
			lines.add(objName + ".failing_regions = new Array();");
			for (Region region : stat.getFailingRegions()) { 
				lines.add(objName + ".failing_regions.push('" + region.toString() + "');");
			}
			
			lines.add("systems.push(" + objName + ");");

		}
		
		httpResponse.setContentType("application/x-javascript;charset=UTF-8");
		httpResponse.setContent(StringUtils.join(lines, "\n"));
	}

	private void fullStiresStatusJS() {
		List<String> lines = new ArrayList<String>();
		lines.add("var stiresSystems = new Array();");
		lines.add("var stiresSystemCountries = new Array();");
		lines.add("var stiresProxies = new Array();");
		for (String system : AisSpy.getStiresSettings().getSystems()) {
			lines.add("stiresSystems.push('" + system + "');");
			lines.add("stiresSystemCountries['" + system + "'] = new Array();");
			lines.add("stiresProxies['" + system + "'] = new Array();");
			for (String country : AisSpy.getStiresSettings().getSystemCountries().get(system)) {
				lines.add("stiresSystemCountries['" + system + "'].push('" + country + "');");
				lines.add("stiresProxies['" + system + "']['" + country + "'] = new Array();");
			}
		}
		lines.add("var stiresServers = new Array();");
		for (String server : AisSpy.getStiresSettings().getServers()) {
			lines.add("stiresServers.push('" + server + "');");
		}
		
		for (StiresProxyStatus proxyStatus : AisSpy.getStiresSettings().getProxyStatuses()) {
			String objName = String.format("stiresProxies['%s']['%s']['%s']", proxyStatus
					.getSystem(), proxyStatus.getCountry().getTwoLetter(), proxyStatus.getServer());
			lines.add(objName + " = new Object();");
			lines.add(objName + ".status = '" + proxyStatus.getStatus() + "';");
			lines.add(objName + ".lastCheck = '" + proxyStatus.getLastCheck() + "';");
			lines.add(objName + ".lastCheckUTC = '" + Formatter.getISO8601(proxyStatus.getLastCheck()) + "';");
			lines.add(objName + ".running = " + proxyStatus.isRunning() + ";");
			lines.add(objName + ".server = '" + proxyStatus.getServer() + "';");
			lines.add(objName + ".system = '" + proxyStatus.getSystem() + "';");
			lines.add(objName + ".url = '" + proxyStatus.getCredentialsStatusUrl() + "';");
			if (!proxyStatus.isRunning()) {
				continue;
			}
			lines.add(objName + ".enabled = " + proxyStatus.isEnabled() + ";");
			lines.add(objName + ".stiresRemoteAddr = '" + proxyStatus.getStiresRemoteAddr() + "';");
			lines.add(objName + ".connectedToStires = " + proxyStatus.isConntectedToStires() + ";");
			lines.add(objName + ".flow = '" + proxyStatus.getFlow() + " msg/s';");
			lines.add(objName + ".providerConnected = " + proxyStatus.isProviderConnected() + ";");
			lines.add(objName + ".providerDelivering = " + proxyStatus.isProviderDelivering() + ";");
		}

		httpResponse.setContentType("application/x-javascript");
		httpResponse.setContent(StringUtils.join(lines, "\n"));
	}

	private void systemStatus() {
		SystemHandler handler = getSystem();
		if (handler == null) {
			return;
		}
		SystemStatus systemStatus = handler.getSystemStatus();
		httpResponse.setContent(systemStatus.getHttpReply());
	}

	private SystemHandler getSystem() {
		String system = getParam("system");
		if (system == null) {
			httpResponse.setBadRequest();
			httpResponse.setContent("Missing system argument");
			return null;
		}
		SystemHandler handler = AisSpy.getHandler(system);
		if (handler == null) {
			httpResponse.setBadRequest();
			httpResponse.setContent("Unknown system: " + system);
			return null;
		}
		return handler;
	}

	private String getParam(String key, String defaultValue) {
		String val = httpRequestHandler.getHttpParams().getFirst(key);
		if (val == null) {
			val = defaultValue;
		}
		return val;
	}

	private String getParam(String key) {
		return getParam(key, null);
	}

}
