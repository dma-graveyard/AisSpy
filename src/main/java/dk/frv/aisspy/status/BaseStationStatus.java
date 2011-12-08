package dk.frv.aisspy.status;

import java.util.Date;

import dk.frv.ais.geo.GeoLocation;

public class BaseStationStatus implements Comparable<BaseStationStatus> {
		
	private long mmsi;
	private String country;
	private Date lastReveived;
	private GeoLocation pos;
	
	public BaseStationStatus() {
		
	}

	public long getMmsi() {
		return mmsi;
	}

	public void setMmsi(long mmsi) {
		this.mmsi = mmsi;
	}
	
	public String getCountry() {
		return country;
	}
	
	public void setCountry(String country) {
		this.country = country;
	}

	public Date getLastReveived() {
		return lastReveived;
	}

	public void setLastReveived(Date lastReveived) {
		this.lastReveived = lastReveived;
	}

	public GeoLocation getPos() {
		return pos;
	}

	public void setPos(GeoLocation pos) {
		this.pos = pos;
	}
	
	@Override
	public boolean equals(Object obj) {
		return (((BaseStationStatus)obj).getMmsi() == mmsi);
	}

	@Override
	public int compareTo(BaseStationStatus bs) {
		return (new Long(mmsi)).compareTo(new Long(bs.mmsi));
	}
	
}
