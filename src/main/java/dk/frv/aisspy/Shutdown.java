package dk.frv.aisspy;

import org.apache.log4j.Logger;

public class Shutdown extends Thread {
	
	private static final Logger LOG = Logger.getLogger(Shutdown.class);
	
	@Override
	public void run() {
		LOG.info("Shutting down");
		for (String system : AisSpy.getSettings().getProxyAppStarters().keySet()) {
			LOG.info("Shutting down proxy for " + system);
			ProxyAppStarter proxyAppStarter = AisSpy.getSettings().getProxyAppStarters().get(system);
			proxyAppStarter.stopApp();
		}
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {e.printStackTrace();}
	    
	}

}
