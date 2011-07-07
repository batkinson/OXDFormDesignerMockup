package org.openxdata.modelutils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * An implementation of namespace context used to be able to use common oxd
 * xforms namespace prefixes.
 * 
 * @author brent
 * 
 */
public class NamespaceContext implements javax.xml.namespace.NamespaceContext {

	private static Map<String, String> mappings = new HashMap<String, String>();

	static {
		mappings.put("xf", "http://www.w3.org/2002/xforms");
		mappings.put("xsd", "http://www.w3.org/2001/XMLSchema");
	}

	@Override
	public String getNamespaceURI(String prefix) {
		String uri = null;
		if (mappings.containsKey(prefix))
			uri = mappings.get(prefix);
		return uri;
	}

	@Override
	public String getPrefix(String namespaceURI) {
		String prefix = null;
		for (Map.Entry<String, String> entry : mappings.entrySet())
			if (entry.getValue().equals(namespaceURI))
				prefix = entry.getKey();
		return prefix;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Iterator getPrefixes(String namespaceURI) {
		return mappings.keySet().iterator();
	}

}
