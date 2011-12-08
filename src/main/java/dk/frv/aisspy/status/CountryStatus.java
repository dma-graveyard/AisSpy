package dk.frv.aisspy.status;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import dk.frv.ais.country.MidCountry;

public class CountryStatus {
	
	private static final Logger LOG = Logger.getLogger(CountryStatus.class);

	private Status status;
	private MidCountry country;
	private Date lastReceived;
	private double rate;
	private SystemStatus systemStatus;
	private List<BaseStationStatus> baseStations = new ArrayList<BaseStationStatus>();

	public CountryStatus() {
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public MidCountry getCountry() {
		return country;
	}

	public void setCountry(MidCountry country) {
		this.country = country;
	}

	public Date getLastReceived() {
		return lastReceived;
	}

	public void setLastReceived(Date lastReceived) {
		this.lastReceived = lastReceived;
	}

	public double getRate() {
		return rate;
	}

	public void setRate(double rate) {
		this.rate = rate;
	}

	public SystemStatus getSystemStatus() {
		return systemStatus;
	}

	public void setSystemStatus(SystemStatus systemStatus) {
		this.systemStatus = systemStatus;
	}
	
	public List<BaseStationStatus> getBaseStations() {
		return baseStations;
	}
	
	@Override
	public String toString() {
		return String.format("%4s %8s %20s %8s msg/min\n", country.getTwoLetter(), status.name(), Formatter
				.dateFormat(lastReceived), Formatter.rateFormat(rate));
	}

	public String getHttpReply() {
		Status statStatus = status;
		if (!systemStatus.isProxyConnected()) {
			statStatus = Status.UNKNOWN;
		}
		String httpReply = String.format("status=%s&last_received=%s&rate=%s", statStatus.name(), Formatter.dateFormat(lastReceived), Formatter
				.rateFormat(rate));
		if (status == Status.FAIL) {
			LOG.info("CountryStatus HTTP reply indicates error: " + httpReply);
		}

		return httpReply;
	}

}
