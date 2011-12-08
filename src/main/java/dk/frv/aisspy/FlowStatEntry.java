package dk.frv.aisspy;

import java.util.Date;
import java.util.LinkedList;

public class FlowStatEntry {

	private static final long backTrackTime = 2 * 60 * 1000; // Two minutes
	private Date lastReceived = null;
	// Timestamps for receives over the last five minutes
	private LinkedList<Long> receives = new LinkedList<Long>();

	public FlowStatEntry() {
	}

	public Date getLastReceived() {
		return lastReceived;
	}

	public double getRate() {
		truncateReceives();
		if (receives.size() == 0)
			return 0;
		long last = receives.getLast();
		long now = (new Date()).getTime();
		if (last == now)
			return 0;
		return ((double) receives.size() / ((double) (now - last) / 1000 / 60));
	}

	public void received() {
		lastReceived = new Date();
		receives.addFirst(lastReceived.getTime());
		truncateReceives();
	}

	private void truncateReceives() {
		long now = (new Date()).getTime();
		while (receives.size() > 0) {
			long last = receives.getLast();
			if ((now - last) > backTrackTime) {
				receives.removeLast();
			} else {
				break;
			}
		}
	}

}
