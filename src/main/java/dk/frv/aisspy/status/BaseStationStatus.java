package dk.frv.aisspy.status;

import java.util.Date;

import dk.dma.enav.model.geometry.Position;

public class BaseStationStatus implements Comparable<BaseStationStatus> {
		
	private int mmsi;
	private String country;
	private Date lastReveived;
	private Position pos;
	
	public BaseStationStatus() {
		
	}

	public int getMmsi() {
		return mmsi;
	}

	public void setMmsi(int mmsi) {
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

	public Position getPos() {
		return pos;
	}

	public void setPos(Position pos) {
		this.pos = pos;
	}
	
	@Override
	public boolean equals(Object obj) {
		return (((BaseStationStatus)obj).getMmsi() == mmsi);
	}

	@Override
	public int compareTo(BaseStationStatus bs) {
		return (new Integer(mmsi)).compareTo(new Integer(bs.mmsi));
	}
	
}
