package dk.frv.aisspy;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dk.dma.ais.message.AisMessage4;
import dk.dma.enav.model.Country;
import dk.frv.aisspy.status.BaseStationStatus;

public class SystemStatistics {

	private FlowStatEntry flowStat = new FlowStatEntry();
	private Date startTime = new Date();
	private Map<String, FlowStatEntry> countryOrigin = new ConcurrentHashMap<String, FlowStatEntry>();
	private Map<Integer, FlowStatEntry> baseStationOrigin = new ConcurrentHashMap<Integer, FlowStatEntry>();
	private Map<Integer, FlowStatEntry> baseStationReport = new ConcurrentHashMap<Integer, FlowStatEntry>();
	private Map<Integer, FlowStatEntry> regionOrigin = new ConcurrentHashMap<Integer, FlowStatEntry>();
	private Map<Integer, BaseStationStatus> baseStationStatus = new ConcurrentHashMap<Integer, BaseStationStatus>();
	private Map<Integer, String> baseStationCountry = new ConcurrentHashMap<Integer, String>();

	public SystemStatistics() {

	}

	public void markOrigin(String country) {
		if (country == null)
			return;
		FlowStatEntry entry = countryOrigin.get(country);
		if (entry == null) {
			entry = new FlowStatEntry();
			countryOrigin.put(country, entry);
		}
		entry.received();
	}

	public void markBaseStationOrigin(Integer baseMmsi) {
		if (baseMmsi == null) {
			return;
		}
		FlowStatEntry entry = baseStationOrigin.get(baseMmsi);
		if (entry == null) {
			entry = new FlowStatEntry();
			baseStationOrigin.put(baseMmsi, entry);
		}
		entry.received();
	}

	public void markBaseStationReport(AisMessage4 message) {
		FlowStatEntry entry = baseStationReport.get(message.getUserId());
		if (entry == null) {
			entry = new FlowStatEntry();
			baseStationReport.put(message.getUserId(), entry);
		}
		entry.received();

		String country = null;
		if (message.getSourceTag() != null && message.getSourceTag().getCountry() != null) {
			country = message.getSourceTag().getCountry().getTwoLetter();
		}
		if (country != null) {
			baseStationCountry.put(message.getUserId(), country);			
		}
		
		// Determine country for base station by MMSI
		String midPart = Integer.toString(message.getUserId());
		if (midPart.length() > 3) {
			midPart = midPart.substring(0, 3);
		}
		Country midCountry = Country.getByMid(Integer.parseInt(midPart));
	
		if (midCountry != null) {
			BaseStationStatus bsStatus = baseStationStatus.get(message.getUserId());
			if (bsStatus == null) {
				bsStatus = new BaseStationStatus();
				bsStatus.setCountry(midCountry.getTwoLetter());
				bsStatus.setMmsi(message.getUserId());
				baseStationStatus.put(message.getUserId(), bsStatus);
			}
			bsStatus.setLastReveived(new Date());
			bsStatus.setPos(message.getPos().getGeoLocation());
		}
		
	}
	
	public void markRegionOrigin(String strId) {
		if (strId == null || strId.length() == 0) return;
		int id = 0;
		try {
			id = Integer.parseInt(strId);
		} catch (NumberFormatException e) {
			return;
		}		
		FlowStatEntry entry = regionOrigin.get(id);
		if (entry == null) {
			entry = new FlowStatEntry();
			regionOrigin.put(id, entry);
		}
		entry.received();
	}

	public FlowStatEntry getFlowStat() {
		return flowStat;
	}

	public Date getStartTime() {
		return startTime;
	}

	public Map<String, FlowStatEntry> getCountryOrigin() {
		return countryOrigin;
	}

	public Map<Integer, FlowStatEntry> getBaseStationOrigin() {
		return baseStationOrigin;
	}

	public Map<Integer, FlowStatEntry> getBaseStationReport() {
		return baseStationReport;
	}

	public Map<Integer, String> getBaseStationCountry() {
		return baseStationCountry;
	}
	
	public Map<Integer, BaseStationStatus> getBaseStationStatus() {
		return baseStationStatus;
	}
	
	public Map<Integer, FlowStatEntry> getRegionOrigin() {
		return regionOrigin;
	}

}
