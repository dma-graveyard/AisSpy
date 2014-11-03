package dk.frv.aisspy;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.log4j.Logger;

import dk.dma.ais.message.AisMessage;
import dk.dma.ais.message.AisMessage4;
import dk.dma.ais.packet.AisPacket;
import dk.dma.ais.proprietary.IProprietarySourceTag;
import dk.dma.ais.reader.AisTcpReader;
import dk.dma.enav.model.Country;
import dk.frv.aisspy.status.SystemStatus;

public class SystemHandler implements Consumer<AisPacket> {

	private static final Logger LOG = Logger.getLogger(SystemHandler.class);

	private String name;
	private List<Country> countries = new ArrayList<>();
	private String proxyHost = "localhost";
	private int proxyPort = 4001;
	private int countryReceiveFailTime = 30;
	private double countryReceiveRateMinimum = 0.0;
	private int systemReceiveFailTime = 30;
	private double systemReceiveRateMinimum = 0.0;
	private Set<BaseStation> baseStations = new HashSet<BaseStation>();
	private int baseStationTimeout = 300;
	private Set<Region> regions = new HashSet<Region>();
	private int regionTimeout = 300;
	private int oldDataTolerance = 0;
	private AisTcpReader aisReader = null;
	private Date lastEmail = new Date(0);
	private Date lastBsOldEmail = new Date(0);

	private SystemStatistics stats = new SystemStatistics();

	public SystemHandler(String name) {
		LOG.info("Created handler for " + name);
		this.name = name;
	}

	@Override
	public synchronized void accept(AisPacket packet) {
		AisMessage aisMessage = packet.tryGetAisMessage();
		if (aisMessage == null) {
			return;
		}
		
		// Update statistics
		stats.getFlowStat().received();
		// Update country
		IProprietarySourceTag tag = aisMessage.getSourceTag();		
		if (tag != null) {
			if (tag.getCountry() != null) {
				stats.markOrigin(tag.getCountry().getTwoLetter());
			}
			stats.markBaseStationOrigin(tag.getBaseMmsi());
			stats.markRegionOrigin(tag.getRegion());
			evaluateTimestamp(aisMessage);
		} else {
			LOG.warn("No source tag on message: " + aisMessage);
		}

		if (aisMessage.getMsgId() == 4) {			
			stats.markBaseStationReport((AisMessage4) aisMessage);
		}
	}
	
	private void evaluateTimestamp(AisMessage aisMessage) {
		if (oldDataTolerance == 0) {
			return;
		}
		
		IProprietarySourceTag tag = aisMessage.getSourceTag();
		Date now = new Date();
		
		// Check difference between now 
		if (aisMessage.getMsgId() == 4) {
			AisMessage4 msg4 = (AisMessage4)aisMessage;
			Date timestamp = msg4.getDate();
			long diff = Math.abs((now.getTime() - timestamp.getTime()) / 1000);
			if (diff > oldDataTolerance) {
//				String errorStr = "BS with old timestamp diff: " + diff + " system: " + name + " timestamp: " + timestamp + " now: " + now + "\r\n";
//				errorStr += tag + "\r\n";
//				errorStr += msg4 + "\r\n";
//				errorStr += aisMessage.getVdm().getOrgLinesJoined() + "\r\n";
//				LOG.error(errorStr);
				
				// Elapsed since last email
				long elapsed = (now.getTime() - lastBsOldEmail.getTime()) / 1000; 
				if (elapsed > AisSpy.getSettings().getMinEmailInterval() * 60) {
					//AisSpy.alertEmail("BS OLD TIMESTAMP", errorStr);
					lastBsOldEmail = new Date();
				}
			}
		}
		
		// Check difference between GH timestamp and now		
		if (tag != null) {
			Date timestamp = tag.getTimestamp();
			if (timestamp != null) {
				long diff = Math.abs((now.getTime() - timestamp.getTime()) / 1000);
				if (diff > oldDataTolerance) {
					String errorStr = "OLD DATA timestamp diff: " + diff + " system: " + name + " now: " + now + "\r\n";
					errorStr += tag + "\r\n";
					errorStr += aisMessage.getVdm().getOrgLinesJoined() + "\r\n";
					LOG.error(errorStr);
					
					// Elapsed since last email
					long elapsed = (now.getTime() - lastEmail.getTime()) / 1000; 
					if (elapsed > AisSpy.getSettings().getMinEmailInterval() * 60) {
						AisSpy.alertEmail("OLD AIS DATA", errorStr);
						lastEmail = new Date();
					}
					
				}
			}
		}

	}
	
	public synchronized SystemStatus getSystemStatus() {
		return new SystemStatus(this);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<Country> getCountries() {
		return countries;
	}

	public void setCountries(List<Country> countries) {
		this.countries = countries;
	}

	public void addCountry(Country country) {
		countries.add(country);
	}

	public String getProxyHost() {
		return proxyHost;
	}

	public void setProxyHost(String proxyHost) {
		this.proxyHost = proxyHost;
	}

	public int getProxyPort() {
		return proxyPort;
	}

	public void setProxyPort(int proxyPort) {
		this.proxyPort = proxyPort;
	}

	public Set<BaseStation> getBaseStations() {
		return baseStations;
	}
	
	public void setBaseStations(Set<BaseStation> baseStations) {
		this.baseStations = baseStations;
	}

	public int getBaseStationTimeout() {
		return baseStationTimeout;
	}

	public void setBaseStationTimeout(int baseStationTimeout) {
		this.baseStationTimeout = baseStationTimeout;
	}

	public void setAisReader(AisTcpReader aisReader) {
		this.aisReader = aisReader;
	}

	public SystemStatistics getStats() {
		return stats;
	}

	public AisTcpReader getAisReader() {
		return aisReader;
	}

	public int getCountryReceiveFailTime() {
		return countryReceiveFailTime;
	}

	public void setCountryReceiveFailTime(int countryReceiveFailTime) {
		this.countryReceiveFailTime = countryReceiveFailTime;
	}

	public double getCountryReceiveRateMinimum() {
		return countryReceiveRateMinimum;
	}

	public void setCountryReceiveRateMinimum(double countryReceiveRateMinimum) {
		this.countryReceiveRateMinimum = countryReceiveRateMinimum;
	}

	public int getSystemReceiveFailTime() {
		return systemReceiveFailTime;
	}

	public void setSystemReceiveFailTime(int systemReceiveFailTime) {
		this.systemReceiveFailTime = systemReceiveFailTime;
	}

	public double getSystemReceiveRateMinimum() {
		return systemReceiveRateMinimum;
	}

	public void setSystemReceiveRateMinimum(double systemReceiveRateMinimum) {
		this.systemReceiveRateMinimum = systemReceiveRateMinimum;
	}
	
	public int getOldDataTolerance() {
		return oldDataTolerance;
	}
	
	public void setOldDataTolerance(int oldDataTolerance) {
		this.oldDataTolerance = oldDataTolerance;
	}

	public Set<Region> getRegions() {
		return regions;
	}

	public void setRegions(Set<Region> regions) {
		this.regions = regions;
	}

	public int getRegionTimeout() {
		return regionTimeout;
	}
	
	public void setRegionTimeout(int regionTimeout) {
		this.regionTimeout = regionTimeout;
	}
	
}
