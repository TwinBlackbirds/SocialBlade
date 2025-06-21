// Name: Michael Amyotte
// Date: 6/13/25
// Purpose: Configurator 'payload' object to marshal in and out of JSON form

package tbb.utils.Config;

import java.util.ArrayList;

import com.fasterxml.jackson.core.JsonFactory;

public class ConfigPayload extends JsonFactory {

	// do not remove (I don't know what it does but it is crucial)
	private static final long serialVersionUID = 1L;
	
	// your configuration parameters
	public String[] hosts;
	public boolean headless;
	
	// do not touch
	public ConfigPayload() {
		this(new String[0], true);
	}
	
	// configure if you'd like
	public ConfigPayload(String[] hosts, boolean headless) {
		super();
		this.hosts = hosts;
		this.headless = headless;
	}
	
}
