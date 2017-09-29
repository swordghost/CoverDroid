package cn.edu.pku.apiminier.web.trace;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class TraceCache<T> {
	Map<String, T> traces;
	Map<String, Long> lastOptime = new HashMap<String, Long>();
	Thread gcThread;
	long gcTime;

	private Thread getGCThread() {
		Thread t = new Thread() {
			public void run() {
				for (;;) {
					try {
						Thread.sleep(gcTime);
					} catch (Exception e) {

					}
					Set<String> toRemove = new HashSet<String>();
					Long curr = System.currentTimeMillis() - gcTime;
					for (String str : lastOptime.keySet()) {
						if (lastOptime.get(str) <= curr) {
							toRemove.add(str);
						}
					}
					synchronized (traces) {
						for (String key : toRemove) {
							traces.remove(key);
							lastOptime.remove(key);
						}
					}
				}
			}
		};
		t.start();
		return t;
	}

	public TraceCache(long time) {
		traces = new HashMap<String, T>();
		gcTime = time;
		lastOptime = new HashMap<String, Long>();
		gcThread = getGCThread();
	}

	public T get(String key) {
		return traces.get(key);
	}

	public boolean containsKey(String fileName) {
		return traces.containsKey(fileName);
	}

	public void put(String key, T t) {
		traces.put(key, t);
	}

	public void remove(String key) {
		traces.remove(key);
	}

	public void updateOp(String key) {
		if (lastOptime.containsKey(key)) {
			lastOptime.remove(key);
		}
		lastOptime.put(key, System.currentTimeMillis());
	}

	public void clear() {
		traces.clear();
	}

	public abstract T getInstance(String key);

	public boolean loadIfNotExist(String fileName) {
		updateOp(fileName);
		if (traces.containsKey(fileName))
			return true;
		T trace = getInstance(fileName);
		if (trace != null)
			try {
				traces.put(fileName, trace);
			} catch (Exception e) {
				e.printStackTrace();
			}
		return traces.containsKey(fileName);
	}
}
