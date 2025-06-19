// Name: Michael Amyotte
// Date: 6/13/25
// Purpose: Generic configuration lobby to read settings from JSON file


package tbb.utils.Config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import com.fasterxml.jackson.databind.ObjectMapper;

import tbb.utils.Logger.LogLevel;
import tbb.utils.Logger.Logger;


public class Configurator<T extends ConfigPayload> {
	// private
	private final String filepathStr = "config.json";
	private final Path filepath = Paths.get(filepathStr);
	private Logger log = null;
	private T data;
	private Class<T> type;
	
	
	@SuppressWarnings("unchecked")
	public Configurator(Logger log, Class<T> type) throws IOException {
		this.log = log;
		this.type = type;
		ObjectMapper om = new ObjectMapper();
		if (!Files.exists(filepath)) {
			makeDefaultFile(om);
		}
		try {
			String fStr = Files.readString(filepath);
			this.data = om.readValue(fStr, type); // read as json 
		} catch (Exception e) {
			log.Write(LogLevel.ERROR, "Could not read configuration file!");
		}
	}
	
	void makeDefaultFile(ObjectMapper om) {
		log.Write(LogLevel.INFO, "Falling back to default configuration file");
		ConfigPayload defaultPayload = new ConfigPayload(); // default payload
		try {
			Files.createFile(filepath);
			String jsonStr = om.writeValueAsString(defaultPayload);
			Files.writeString(filepath, jsonStr);
		} catch (Exception e) {
			log.Write(LogLevel.ERROR, "Could not make default config.json file!");
		}
	}

	public T getData() {
		return data;
	}
}