package dk.frv.aisspy;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import dk.frv.ais.country.CountryMapper;
import dk.frv.ais.country.MidCountry;
import dk.frv.ais.message.AisMessage4;
import dk.frv.aisspy.status.BaseStationStatus;

public class SystemStatistics {

	private FlowStatEntry flowStat = new FlowStatEntry();
	private Date startTime = new Date();
	private Map<String, FlowStatEntry> countryOrigin = new HashMap<String, FlowStatEntry>();
	private Map<Long, FlowStatEntry> baseStationOrigin = new HashMap<Long, FlowStatEntry>();
	private Map<Long, FlowStatEntry> baseStationReport = new HashMap<Long, FlowStatEntry>();
	private Map<Long, FlowStatEntry> regionOrigin = new HashMap<Long, FlowStatEntry>();
	private Map<Long, BaseStationStatus> baseStationStatus = new HashMap<Long, BaseStationStatus>();
	private Map<Long, String> baseStationCountry = new HashMap<Long, String>();

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

	public void markBaseStationOrigin(Long baseMmsi) {
		if (baseMmsi == null)
			return;
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
		if (message.getSourceTag() != null) {
			country = message.getSourceTag().getCountry().getTwoLetter();
		}
		if (country != null) {
			baseStationCountry.put(message.getUserId(), country);			
		}
		
		// Determine country for base station by MMSI
		String midPart = Long.toString(message.getUserId());
		if (midPart.length() > 3) {
			midPart = midPart.substring(0, 3);
		}
		MidCountry midCountry = CountryMapper.getInstance().getByMid(Integer.parseInt(midPart));
	
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
		long id = 0;
		try {
			id = Long.parseLong(strId);
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

	public Map<Long, FlowStatEntry> getBaseStationOrigin() {
		return baseStationOrigin;
	}

	public Map<Long, FlowStatEntry> getBaseStationReport() {
		return baseStationReport;
	}

	public Map<Long, String> getBaseStationCountry() {
		return baseStationCountry;
	}
	
	public Map<Long, BaseStationStatus> getBaseStationStatus() {
		return baseStationStatus;
	}
	
	public Map<Long, FlowStatEntry> getRegionOrigin() {
		return regionOrigin;
	}

}
