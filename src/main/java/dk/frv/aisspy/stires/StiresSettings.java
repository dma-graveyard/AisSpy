package dk.frv.aisspy.stires;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;

import dk.dma.enav.model.Country;

public class StiresSettings {
	
	private Properties props;
	private String[] servers;
	private String[] systems;
	private List<StiresProxyStatus> proxyStatuses = new ArrayList<StiresProxyStatus>();
	private Map<String, String[]> systemCountries = new HashMap<String, String[]>();
	private Map<String, HashMap<String, ArrayList<StiresProxyStatus>>> systemCountryStatuses = new HashMap<String, HashMap<String,ArrayList<StiresProxyStatus>>>();
	private long interval;
	private String username;
	private String password;
	
	public void load() throws IOException {
		props = new Properties();
		URL url = ClassLoader.getSystemResource("stires_proxies.conf");
		if (url == null) {
			throw new IOException("Could not find properties file: stires_proxies.conf");
		}
		props.load(url.openStream());
			
		servers = StringUtils.split(props.getProperty("stires_servers", ""), ",");
		systems = StringUtils.split(props.getProperty("stires_systems", ""), ",");
		interval = Integer.parseInt(props.getProperty("interval", "10")) * 1000;
		username = props.getProperty("username", "");
		password = props.getProperty("password", "");
		
		for (String system : systems) {
			String[] countries = StringUtils.split(props.getProperty("stires_countries." + system, ""), ",");
			systemCountries.put(system, countries);
			
			HashMap<String, ArrayList<StiresProxyStatus>> countryMap = new HashMap<String, ArrayList<StiresProxyStatus>>();
			
			for (String country : countries) {
				Country c = Country.getByCode(country);
				
				ArrayList<StiresProxyStatus> serverProxyList = new ArrayList<StiresProxyStatus>();				
				
				for (String server : servers) {
					StiresProxyStatus proxyStatus = new StiresProxyStatus();
					proxyStatus.setCountry(c);
					proxyStatus.setSystem(system);
					proxyStatus.setServer(server);
					proxyStatus.setPort(Integer.parseInt(props.getProperty("stires_ports." + system + "." + country)));
					proxyStatuses.add(proxyStatus);
					serverProxyList.add(proxyStatus);
				}
								
				countryMap.put(country, serverProxyList);
				systemCountryStatuses.put(system, countryMap);				
			}
		}
		
	}
	
	public List<StiresProxyStatus> getProxyStatuses() {
		return proxyStatuses;
	}
	
	public long getInterval() {
		return interval;
	}
	
	public String getUsername() {
		return username;
	}
	
	public String getPassword() {
		return password;
	}
	
	public String[] getServers() {
		return servers;
	}
	
	public String[] getSystems() {
		return systems;
	}
	
	public Map<String, String[]> getSystemCountries() {
		return systemCountries;
	}
	
	public Map<String, HashMap<String, ArrayList<StiresProxyStatus>>> getSystemCountryStatuses() {
		return systemCountryStatuses;
	}
	
}
