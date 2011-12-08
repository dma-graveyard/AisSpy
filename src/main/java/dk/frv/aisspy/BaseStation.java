package dk.frv.aisspy;

public class BaseStation {
	
	private long mmsi;
	private String name;
	
	public BaseStation(long mmsi, String name) {
		this.mmsi = mmsi;
		this.name = name;
	}
	
	public long getMmsi() {
		return mmsi;
	}
	
	public void setMmsi(long mmsi) {
		this.mmsi = mmsi;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	public boolean equals(Object obj) {
		return (((BaseStation)obj).getMmsi() == mmsi);
	}
	
	@Override
	public int hashCode() {
		return (int)mmsi;
	}
	
	@Override
	public String toString() {
		return "(" + mmsi + "," + name + ")";
	}

}
