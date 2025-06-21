// Name: Michael Amyotte
// Date: 6/13/25
// Purpose: Generic configuration lobby to read settings from JSON file


package tbb.utils.Config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;

import tbb.utils.Logger.LogLevel;
import tbb.utils.Logger.Logger;


public class Configurator {
	// private
	private final String filepathStr = "./config.json";
	private final Path filepath = Paths.get(filepathStr);
	private Logger log = null;
	private ConfigPayload data;
	
	
	public Configurator(Logger log) throws IOException {
		this.log = log;
		ObjectMapper om = new ObjectMapper();
		if (!Files.exists(filepath)) {
			makeDefaultFile(om);
		}
		try {
			String fStr = Files.readString(filepath);
			this.data = om.readValue(fStr, ConfigPayload.class); // read as json 
		} catch (Exception e) {
			log.Write(LogLevel.ERROR, "Could not read configuration file!");
		}
	}
	
	void makeDefaultFile(ObjectMapper om) {
		log.Write(LogLevel.WARN, "Falling back to default configuration file");
		try {
			Files.deleteIfExists(filepath);
			Files.writeString(filepath, "{\"hosts\": [\"https://youtube.com\"], \"headless\": true}");
		} catch (Exception e) {
			log.Write(LogLevel.ERROR, "Could not make default config.json file!");
		}
	}

	public ConfigPayload getData() {
		return data;
	}
}