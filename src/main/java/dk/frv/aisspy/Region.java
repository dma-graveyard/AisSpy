package dk.frv.aisspy;

public class Region {
	
	private int id;
	private String name;
	
	public Region(int id, String name) {
		this.id = id;
		this.name = name;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	public boolean equals(Object obj) {
		return (((Region)obj).getId() == id);
	}
	
	@Override
	public int hashCode() {
		return (int)id;
	}
	
	@Override
	public String toString() {
		return "(" + id + "," + name + ")";
	}
	
}
