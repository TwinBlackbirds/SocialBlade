package tbb.apps.SocialBlade;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {
	private boolean enableDebugMessages = false;
	
	public Logger(boolean debugMode) {
		this.enableDebugMessages = debugMode;
	}

	// generic logging function
	void Write(LogLevel severity, String msg) {
		String ldt = LocalDateTime.now()
				.format(
						DateTimeFormatter.ofPattern("HH:mm:ss")
				);
		String severityMsg = "";
		switch (severity) {
		case DBG:
			if (enableDebugMessages) { severityMsg = "DEBUG"; } break;
		case INFO:
			severityMsg = "INFO"; break;
		case WARN:
			severityMsg = "WARN"; break;
		default:
			severityMsg = "ERROR"; break;
		}
		if (severityMsg != "") { // skip debug messages if they are not enabled
			System.out.println(String.format("[ %s ] (%s): %s", ldt, severityMsg, msg));
		    	
		}
	}
	
	// dump stack trace to file
	void Dump(Exception e) {
		Path pa = Paths.get("log.txt");
		if (Files.exists(pa)) {
			try {
				Files.delete(pa);	
			} catch (Exception ex) { /* swallow */ };
		}
		StringBuilder s = new StringBuilder();
		for (StackTraceElement el : e.getStackTrace()) {
			s.append(el.toString()).append("\n");
		}
		try {
			Files.writeString(pa, s.toString());
		} catch (Exception ex) { /* swallow */}
	}
}
