package dk.frv.aisspy.status;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import dk.frv.ais.country.MidCountry;
import dk.frv.ais.reader.AisTcpReader;
import dk.frv.aisspy.BaseStation;
import dk.frv.aisspy.FlowStatEntry;
import dk.frv.aisspy.Region;
import dk.frv.aisspy.SystemHandler;
import dk.frv.aisspy.SystemStatistics;

public class SystemStatus {
	
	private static final Logger LOG = Logger.getLogger(SystemStatus.class);
	
	private static final long BS_TIMEOUT = 60 * 60 * 1000; // 1 hour

	private Status status;
	private String name;
	private Date startTime;
	private Date lastReceived;
	private boolean proxyConnected;
	private double rate;
	private List<CountryStatus> countryStatuses = new ArrayList<CountryStatus>();
	private Map<String, CountryStatus> countryStatusMap = new HashMap<String, CountryStatus>();
	private List<BaseStation> failingBaseStaions = new ArrayList<BaseStation>();
	private List<Region> failingRegions = new ArrayList<Region>();

	public SystemStatus(SystemHandler handler) {
		name = handler.getName();
		proxyConnected = (handler.getAisReader().getStatus() == AisTcpReader.Status.CONNECTED);
		SystemStatistics stats = handler.getStats();
		startTime = stats.getStartTime();
		rate = stats.getFlowStat().getRate();

		if (stats.getFlowStat().getLastReceived() != null) {
			lastReceived = new Date(stats.getFlowStat().getLastReceived().getTime());
		}

		// Determine status
		Date now = new Date();
		Date last = lastReceived;
		boolean hasReceived = true;
		if (last == null) {
			last = startTime;
			hasReceived = false;
		}
		if (((now.getTime() - last.getTime()) / 1000) > handler.getSystemReceiveFailTime()) {
			status = Status.FAIL;
		} else {
			status = Status.OK;
		}
		if (hasReceived && (getRate() < handler.getSystemReceiveRateMinimum())) {
			status = Status.FAIL;
		}

		// Handle countries
		for (MidCountry country : handler.getCountries()) {
			String countryCode = country.getTwoLetter();
			CountryStatus countryStatus = new CountryStatus();
			countryStatus.setSystemStatus(this);
			countryStatus.setCountry(country);
			if (stats.getCountryOrigin().get(countryCode) != null) {
				countryStatus.setLastReceived(new Date(stats.getCountryOrigin().get(countryCode).getLastReceived().getTime()));
				countryStatus.setRate(stats.getCountryOrigin().get(countryCode).getRate());
			}
			
			// Determine country status
			last = countryStatus.getLastReceived();
			hasReceived = true;
			if (last == null) {
				last = startTime;
				hasReceived = false;
			}
			if (((now.getTime() - last.getTime()) / 1000) > handler.getCountryReceiveFailTime()) {
				countryStatus.setStatus(Status.FAIL);
			} else {
				countryStatus.setStatus(Status.OK);
			}
			if (hasReceived && (countryStatus.getRate() < handler.getCountryReceiveRateMinimum())) {
				countryStatus.setStatus(Status.FAIL);
			}
			countryStatuses.add(countryStatus);
			countryStatusMap.put(countryCode, countryStatus);
		}
		
		// Handle base station statuses
		for (BaseStationStatus bsStatus : stats.getBaseStationStatus().values()) {
			// Only include not too old base stations
			if (now.getTime() - bsStatus.getLastReveived().getTime() > BS_TIMEOUT) continue;			
			// Get country status
			CountryStatus countryStatus = countryStatusMap.get(bsStatus.getCountry());
			if (countryStatus == null) continue;
			// Add base station
			countryStatus.getBaseStations().add(bsStatus);
		}
		
		// Handle base stations
		for (BaseStation baseStation : handler.getBaseStations()) {
			// Get report entry for base station
			FlowStatEntry baseFlow = stats.getBaseStationReport().get(baseStation.getMmsi());
			if (baseFlow == null) {
				failingBaseStaions.add(baseStation);
				continue;
			}
			last = baseFlow.getLastReceived();
			if (((now.getTime() - last.getTime()) / 1000) > handler.getBaseStationTimeout()) {
				failingBaseStaions.add(baseStation);
				continue;
			}
			
			// Get origin entry for base station
			baseFlow = stats.getBaseStationOrigin().get(baseStation.getMmsi());
			if (baseFlow == null) {
				failingBaseStaions.add(baseStation);
				continue;
			}
			last = baseFlow.getLastReceived();
			if (((now.getTime() - last.getTime()) / 1000) > handler.getBaseStationTimeout()) {
				failingBaseStaions.add(baseStation);
				continue;
			}
		}
		
		// Handle regions
		for (Region region : handler.getRegions()) {
			// Get report entry for region
			FlowStatEntry regionFlow = stats.getRegionOrigin().get(region.getId());
			if (regionFlow == null) {
				failingRegions.add(region);
				continue;
			}
			last = regionFlow.getLastReceived();
			if (((now.getTime() - last.getTime()) / 1000) > handler.getRegionTimeout()) {
				failingRegions.add(region);
				continue;
			}
		}
		
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append(name + "\n");
		buf.append(String.format("\tStart time     : %s\n", Formatter.dateFormat(startTime)));
		buf.append(String.format("\tStatus         : %s\n", status.name()));
		buf.append(String.format("\tProxy connected: %s\n", (proxyConnected) ? "YES" : "NO"));
		buf.append(String.format("\tLast received  : %s\n", Formatter.dateFormat(lastReceived)));
		buf.append(String.format("\tReceive rate   : %s msg/min\n", Formatter.rateFormat(rate)));
		if (!failingBaseStaions.isEmpty()) {
			buf.append(String.format("\tFailing PSS    : %s\n", StringUtils.join(failingBaseStaions.iterator(), " ")));
		}

		for (CountryStatus countryStatus : countryStatuses) {
			buf.append("\t\t" + countryStatus.toString());
		}		

		return buf.toString();
	}

	public String getHttpReply() {
		Status statStatus = status;
		if (!proxyConnected) {
			statStatus = Status.UNKNOWN;
		}
		String httpReply = String.format("status=%s&last_received=%s&rate=%s&proxy_connected=%s&start_time=%s", statStatus.name(),
				Formatter.dateFormat(lastReceived), Formatter.rateFormat(rate), (proxyConnected) ? "YES" : "NO", Formatter
						.dateFormat(startTime));
		if (status == Status.FAIL) {
			LOG.info("SystemStatus HTTP reply indicates error: " + httpReply);
		}
		return httpReply;
	}

	public CountryStatus getCountryStatus(String country) {
		return countryStatusMap.get(country);
	}

	public List<CountryStatus> getCountryStatuses() {
		return countryStatuses;
	}

	public Status getStatus() {
		return status;
	}

	public Date getStartTime() {
		return startTime;
	}

	public Date getLastReceived() {
		return lastReceived;
	}

	public boolean isProxyConnected() {
		return proxyConnected;
	}

	public double getRate() {
		return rate;
	}

	public String getName() {
		return name;
	}
	
	public List<BaseStation> getFailingBaseStaions() {
		return failingBaseStaions;
	}
	
	public List<Region> getFailingRegions() {
		return failingRegions;
	}

}
