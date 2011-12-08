package dk.frv.aisspy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import dk.frv.ais.binary.SixbitExhaustedException;
import dk.frv.ais.message.AisMessageException;
import dk.frv.ais.proprietary.GatehouseFactory;
import dk.frv.ais.reader.AisReader;
import dk.frv.ais.reader.AisTcpReader;
import dk.frv.aisspy.http.HttpServer;
import dk.frv.aisspy.stires.StiresProxySurveillance;
import dk.frv.aisspy.stires.StiresSettings;

public class AisSpy {

	private static Logger LOG;

	private static Settings settings = new Settings();
	private static StiresSettings stiresSettings = new StiresSettings();
	private static StiresProxySurveillance stiresProxySurveillance;
	private static List<AisReader> readers = new ArrayList<AisReader>();
	private static HttpServer httpServer;
	private static Map<String, SystemHandler> handlers = new HashMap<String, SystemHandler>();

	public static void main(String[] args) throws InterruptedException, IOException, SixbitExhaustedException, AisMessageException {
		DOMConfigurator.configure("log4j.xml");
		LOG = Logger.getLogger(AisSpy.class);
		LOG.info("Starting AisSpy");
		
		// Load configuration
		String settingsFile = "settings.conf";
		if (args.length > 0) {
			settingsFile = args[0];
		}
		try {
			settings.load(settingsFile);
		} catch (IOException e) {
			LOG.error("Failed to load settings: " + e.getMessage());
			System.exit(-1);
		}
		
		// Registering shutdown hook
		Runtime.getRuntime().addShutdownHook(new Shutdown());

		// Make readers
		for (SystemHandler handler : settings.getSystemHandlers()) {
			handlers.put(handler.getName(), handler);
			AisTcpReader reader = new AisTcpReader(handler.getProxyHost(), handler.getProxyPort());
			reader.addProprietaryFactory(new GatehouseFactory());
			reader.registerHandler(handler);
			handler.setAisReader(reader);
			readers.add(reader);
			reader.start();
		}
		
		// Load stires settings
		try {
			stiresSettings.load();
		} catch (IOException e) {
			LOG.error("Failed to load stires settings: " + e.getMessage());
			System.exit(-1);
		}
		
		// Start surveillance thread
		stiresProxySurveillance = new StiresProxySurveillance(stiresSettings);
		stiresProxySurveillance.start();

		// Start HTTP server
		httpServer = new HttpServer(settings.getHttpServerPort());
		httpServer.start();

		// Maintanaince loop
		while (true) {
			Thread.sleep(10000);
		}
	}

	public static SystemHandler getHandler(String name) {
		return handlers.get(name);
	}

	public static List<SystemHandler> getHandlers() {
		return settings.getSystemHandlers();
	}
	
	public static StiresSettings getStiresSettings() {
		return stiresSettings;
	}
	
	public static Settings getSettings() {
		return settings;
	}
	
	public static void alertEmail(String subject, String content) {
		String to = getSettings().getAlertEmail();
		String from = getSettings().getEmailFrom();
		
		try {
			Properties props = new Properties();
		    props.put("mail.smtp.host", getSettings().getSmtpServer());
		    Session session = Session.getDefaultInstance(props, null);
		    session.setDebug(true);
		    Message msg = new MimeMessage(session);
		    InternetAddress addressFrom = new InternetAddress(from);
		    msg.setFrom(addressFrom);
		    InternetAddress addressTo[] = new InternetAddress[1];
		    addressTo[0] = new InternetAddress(to);
		    msg.setRecipients(Message.RecipientType.TO, addressTo);
		    msg.setSubject(subject);
		    msg.setContent(content, "text/plain");
		    Transport.send(msg);
		} catch (Exception e) {
			LOG.error("Failed to send alert email: " + e.getMessage());
		}
	    
	}

}
