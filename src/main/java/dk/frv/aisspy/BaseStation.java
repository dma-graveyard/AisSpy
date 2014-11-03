package dk.frv.aisspy;

public class BaseStation {
	
	private int mmsi;
	private String name;
	
	public BaseStation(int mmsi, String name) {
		this.mmsi = mmsi;
		this.name = name;
	}
	
	public int getMmsi() {
		return mmsi;
	}
	
	public void setMmsi(int mmsi) {
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
		return mmsi;
	}
	
	@Override
	public String toString() {
		return "(" + mmsi + "," + name + ")";
	}

}
