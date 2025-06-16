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
	
	
	// do not touch
	public ConfigPayload() {
		this(new ArrayList<String>());
	}
	
	// configure if you'd like
	public ConfigPayload(ArrayList<String> hosts) {
		super();
		this.hosts = hosts.toArray(new String[0]);
	}
}
