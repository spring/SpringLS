
package com.springrts.chanserv;


import com.springrts.chanserv.antispam.DefaultAntiSpamSystem;
import com.springrts.chanserv.antispam.SpamSettings;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Uses JAXB to save and load the ChanServ configuration to/from an XML file.
 * @author hoijui
 */
public class JAXBConfigStorage implements ConfigStorage {

	private static final Logger logger = LoggerFactory.getLogger(JAXBConfigStorage.class);

	private final Context context;
	private JAXBContext jaxbContext;

	public JAXBConfigStorage(Context context) {

		this.context = context;
		try {
			this.jaxbContext = JAXBContext.newInstance(Configuration.class);
		} catch (JAXBException ex) {
			logger.error("Failed creating a JAXB context (required for config file read/write", ex);
			context.getChanServ().closeAndExit(1);
		}
	}

	@Override
	public void loadConfig(String fileName) {

		try {
			Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
			InputStream in = new FileInputStream(fileName);

			// load
			context.setConfiguration((Configuration) unmarshaller.unmarshal(in));

			// post-process channels
			for (Channel channel : context.getConfiguration().channels) {
				// apply anti-spam settings:
				context.getAntiSpamSystem().setSpamSettingsForChannel(channel.getName(), channel.getAntiSpamSettings());
			}

			logger.info("Configuration loaded from file: {}", fileName);
		} catch (Exception ex) {
			logger.error("Failed loading configuration from file: " + fileName, ex);
			context.getChanServ().closeAndExit(1);
		}

	}

	@Override
	public void saveConfig(String fileName) {

		try {
			Marshaller marshaller = jaxbContext.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
			OutputStream out = new FileOutputStream(fileName);

			// persist
			marshaller.marshal(context.getConfiguration(), out);

			logger.debug("Configuration saved to file : {}", fileName);
		} catch (Exception ex) {
			logger.error("Failed to save configuration to file: " + fileName, ex);
		}
	}

	public static void main(String[] args) {

		if (args.length != 2) {
			throw new IllegalArgumentException("Require exactly 2 arguments: <oldConfigFile> <newConfigFile>");
		}

		String oldConfigFile = args[0];
		String newConfigFile = args[1];

		convertOldToJAXB(oldConfigFile, newConfigFile);
	}

	public static void convertOldToJAXB(String oldConfigFile, String newConfigFile) {

		// prepare environment
		Context context = new Context();
		Configuration configuration = new Configuration();
		configuration.remoteAccessPort = 12345;
		context.setConfiguration(configuration);
		ChanServ chanServ = new ChanServ();
		context.setRemoteAccessServer(new RemoteAccessServer(context, context.getConfiguration().remoteAccessPort));
		context.setChanServ(chanServ);
		context.setAntiSpamSystem(new DefaultAntiSpamSystem(context));

		// load old
		ConfigStorage configStorage_legacy = new LegacyConfigStorage(context);
		context.setConfigStorage(configStorage_legacy);
		configStorage_legacy.loadConfig(oldConfigFile);

		// save new
		ConfigStorage configStorage_jaxb = new JAXBConfigStorage(context);
		configStorage_jaxb.saveConfig(newConfigFile);

		// reload new, for validation
		configStorage_jaxb.loadConfig(newConfigFile);
	}
}
