package tbb.db.Driver;

import java.util.ArrayList;

public class ChannelCache {
	private ArrayList<String> cache = new ArrayList<String>();

	public boolean contains(String ID) {
		return cache.contains(ID);
	}

	public void add(String toCache) {
		if (cache.size() >= 100) {
			cache.remove(0);
		}
		cache.add(toCache);
	}
}
