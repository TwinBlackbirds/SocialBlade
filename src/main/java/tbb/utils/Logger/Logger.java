// Name: Michael Amyotte
// Date: 6/13/25
// Purpose: Generic logging utility library

package tbb.utils.Logger;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class Logger implements AutoCloseable {
	private LogLevel minLevel = null;
	
	private ArrayList<String> Stack = new ArrayList<String>();
	
	public Logger() { }
	
	public Logger(LogLevel minLevel) {
		this.minLevel = minLevel;
	}

	// generic logging function
	public void Write(LogLevel severity, String msg) {
		String ldt = LocalDateTime.now()
				.format(
						DateTimeFormatter.ofPattern("HH:mm:ss")
				);
		String severityMsg = "";
		switch (severity) {
		case DBG:
			severityMsg = "DEBUG"; break;
		case INFO:
			severityMsg = "INFO"; break;
		case WARN:
			severityMsg = "WARN"; break;
		default:
			severityMsg = "ERROR"; break;
		}
		String log = String.format("[ %s ] (%s): %s", ldt, severityMsg, msg);
		if (severity.compareTo(minLevel) >= 0) { // only print messages at or above minLevel
			System.out.println(log);
		}
		Stack.add(log);
	}
	
	// dump stack trace to file 
		public void Dump() {
			Path pa = Paths.get("log.txt");
			if (Files.exists(pa)) {
				try {
					Files.delete(pa);	
				} catch (Exception ex) { /* swallow */ };
			}
			StringBuilder s = new StringBuilder();
			for (String el : Stack) {
				s.append(el.toString()).append("\n");
			}
			try {
				Files.writeString(pa, s.toString());
			} catch (Exception ex) { /* swallow */}
		}
		
	// dump stack trace to file (for severe failures)
	public void Dump(Exception e) {
		Path pa = Paths.get("exception_log.txt");
		if (Files.exists(pa)) {
			try {
				Files.delete(pa);	
			} catch (Exception ex) { /* swallow */ };
		}
		StringBuilder s = new StringBuilder();
		
		s.append(e.getMessage()).append("\n"); // msg
		for (StackTraceElement el : e.getStackTrace()) { // trace
			s.append(el.toString()).append("\n");
		}
		
		try {
			Files.writeString(pa, s.toString());
		} catch (Exception ex) { /* swallow */}
	}

	@Override
	public void close() {
		Path log = Paths.get("log.txt");
		Path exLog = Paths.get("exception_log.txt");
		if (Files.exists(log)) {
			try {
				Files.delete(log);
			} catch (IOException e) {}
		}
		if (Files.exists(exLog)) {
			try {
				Files.delete(exLog);
			} catch (IOException e) {}
		}
		Write(LogLevel.DBG, "Dumping log to file");
		this.Dump();
	}
}
