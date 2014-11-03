package dk.frv.aisspy;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import dk.dma.enav.model.Country;

public class Settings {

	private static final Logger LOG = Logger.getLogger(Settings.class);

	private Properties props;
	private List<SystemHandler> systemHandlers = new ArrayList<SystemHandler>();
	private int httpServerPort = 8088;
	private int minEmailInterval;
	private String alertEmail;
	private String emailFrom;
	private String smtpServer;
	private Map<String, ProxyAppStarter> proxyAppStarters = new HashMap<String, ProxyAppStarter>();

	public Settings() {

	}

	public void load(String filename) throws IOException {
		props = new Properties();
		URL url = ClassLoader.getSystemResource(filename);
		if (url == null) {
			throw new IOException("Could not find properties file: " + filename);
		}
		props.load(url.openStream());

		// Iterate through system
		String systemsStr = props.getProperty("systems", "");
		for (String name : StringUtils.split(systemsStr, ",")) {
			SystemHandler systemHandler = new SystemHandler(name);
			getProxySettings(systemHandler);
			getCountries(systemHandler);
			getBasestations(systemHandler);
			getRegions(systemHandler);

			systemHandler.setSystemReceiveFailTime(getInt("system_receive_fail_time." + systemHandler.getName(), "60"));
			systemHandler.setSystemReceiveRateMinimum(getDouble("system_rate_minimum." + systemHandler.getName(), "0.0"));
			systemHandler.setCountryReceiveFailTime(getInt("country_receive_fail_time." + systemHandler.getName(), "60"));
			systemHandler.setBaseStationTimeout(getInt("base_station_timeout." + systemHandler.getName(), "300"));
			systemHandler.setRegionTimeout(getInt("regions_timeout." + systemHandler.getName(), "300"));
			systemHandler.setOldDataTolerance(getInt("old_data_tolerance." + systemHandler.getName(), "0"));
			systemHandler.setCountryReceiveRateMinimum(getDouble("country_rate_minimum." + systemHandler.getName(), "0.0"));
			
			String proxyStartup = props.getProperty("system_proxy_startup." + systemHandler.getName());
			if (proxyStartup != null) {
				ProxyAppStarter proxyAppStarter = new ProxyAppStarter(proxyStartup);
				proxyAppStarter.start();
				proxyAppStarters.put(name, proxyAppStarter);
			}

			systemHandlers.add(systemHandler);
		}

		String httpPortStr = props.getProperty("http_server_port", "8088");		
		httpServerPort = Integer.parseInt(httpPortStr);
		minEmailInterval = Integer.parseInt(props.getProperty("email_min_interval", "5"));
		alertEmail = props.getProperty("alert_email", null);
		emailFrom = props.getProperty("email_from", null);
		smtpServer = props.getProperty("smtp_server", "localhost");

	}

	public List<SystemHandler> getSystemHandlers() {
		return systemHandlers;
	}

	private double getDouble(String key, String defaultValue) {
		String val = props.getProperty(key, defaultValue);
		return Double.parseDouble(val);
	}

	private int getInt(String key, String defaultValue) {
		String val = props.getProperty(key, defaultValue);
		return Integer.parseInt(val);
	}

	private void getProxySettings(SystemHandler systemHandler) {
		String systemProxy = props.getProperty("system_proxy." + systemHandler.getName(), "localhost:4001");
		String[] parts = StringUtils.split(systemProxy, ":");
		systemHandler.setProxyHost(parts[0]);
		systemHandler.setProxyPort(Integer.parseInt(parts[1]));
	}

	private void getCountries(SystemHandler systemHandler) {
		String countriesStr = props.getProperty("countries." + systemHandler.getName(), "");
		for (String countryStr : StringUtils.split(countriesStr, ",")) {
			Country country = Country.getByCode(countryStr);
			if (country == null) {
				LOG.error("Unknown country " + countryStr);
				continue;
			}
			systemHandler.addCountry(country);
		}
	}
	
	private void getBasestations(SystemHandler systemHandler) {
		String baseStr = props.getProperty("base_stations." + systemHandler.getName(), "");
		for (String baseSt : StringUtils.split(baseStr, ",")) {
			String[] elems = StringUtils.split(baseSt, "|");
			BaseStation baseStation = new BaseStation(Integer.parseInt(elems[0]), elems[1]);
			systemHandler.getBaseStations().add(baseStation);
		}
	}
	
	private void getRegions(SystemHandler systemHandler) {
		String regionsStr = props.getProperty("regions." + systemHandler.getName(), "");
		for (String regionStr : StringUtils.split(regionsStr, ",")) {
			String[] elems = StringUtils.split(regionStr, "|");
			Region region = new Region(Integer.parseInt(elems[0]), elems[1]);
			systemHandler.getRegions().add(region);
		}
	}

	public int getHttpServerPort() {
		return httpServerPort;
	}
	
	public int getMinEmailInterval() {
		return minEmailInterval;
	}
	
	public String getAlertEmail() {
		return alertEmail;
	}
	
	public Map<String, ProxyAppStarter> getProxyAppStarters() {
		return proxyAppStarters;
	}
	
	public String getEmailFrom() {
		return emailFrom;
	}
	
	public String getSmtpServer() {
		return smtpServer;
	}

}
