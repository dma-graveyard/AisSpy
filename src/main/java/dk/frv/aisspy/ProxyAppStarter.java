package dk.frv.aisspy;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

public class ProxyAppStarter extends Thread {

	private static final Logger LOG = Logger.getLogger(ProxyAppStarter.class);

	private String app = "";
	private String dir = "";
	private String args = "";
	private Process p;

	public ProxyAppStarter(String appStr) {
		String[] elems = StringUtils.split(appStr, "|");
		if (elems.length == 0) {
			LOG.error("Wrong proxy app str");			
		}
		if (elems.length > 0) {
			this.dir = elems[0];
		}
		if (elems.length > 1) {
			this.app = elems[1];
			this.app = dir + "\\" + this.app;
		}
		if (elems.length > 2) {
			this.args = elems[2];
		}
	}
	
	public void stopApp() {
		if (p != null) {
			p.destroy();
		}
	}

	@Override
	public void run() {
		while (true) {
			executeApp();
			
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {}			
		}
	}
	
	private void executeApp() {
		ArrayList<String> appArgs = new ArrayList<String>();
		appArgs.add(app);
		for (String arg : StringUtils.split(args, " ")) {
			appArgs.add(arg);
		}		
		ProcessBuilder pb = new ProcessBuilder(appArgs);
		File appDir = new File(dir);
		if (!appDir.exists() || !appDir.isDirectory()) {
			LOG.error("Proxy app dir " + dir + " does not exists");
			return;
		}
		pb.directory(appDir);
		pb.redirectErrorStream(true);
		String cmd = StringUtils.join(pb.command(), " ");
		LOG.info("Starting proxy app: " + cmd);
		try {
			p = pb.start();
			BufferedReader pIn = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line;
			while ((line = pIn.readLine()) != null) {
				LOG.info("Proxy app: " + line);
			}
			p.waitFor();
			LOG.info("Proxy app exited with exit value: " + p.exitValue());
		} catch (Exception e) {
			LOG.error("Failed to start proxy app: " + cmd + ": " + e.getMessage());
		}
	}

}
