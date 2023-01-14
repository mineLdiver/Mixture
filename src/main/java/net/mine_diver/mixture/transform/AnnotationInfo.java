package net.mine_diver.mixture.transform;

import net.mine_diver.mixture.Mixtures;
import net.mine_diver.mixture.util.Identifier;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class AnnotationInfo {
	
	public static AnnotationInfo of(AnnotationNode node) {
		Map<String, Object> values = new HashMap<>();
		if (node.values != null) 
			for (int i = 0; i < node.values.size(); i+=2) {
				String key = (String) node.values.get(i);
				Object value = node.values.get(i + 1);
				if (value instanceof AnnotationNode)
					value = of((AnnotationNode) value);
				values.put(key, value);
			}
		return new AnnotationInfo(node, Collections.unmodifiableMap(values));
	}
	
	public final AnnotationNode node;
	public final Map<String, Object> values;
	
	private AnnotationInfo(AnnotationNode node, Map<String, Object> values) {
		this.node = node;
		this.values = values;
	}

	public <T> T get(String key) {
		if (values.containsKey(key))
			//noinspection unchecked
			return (T) values.get(key);
		throw new IllegalStateException("There's no value \"" + key + "\" in annotation info \"" + this + "\"!");
	}

	public <T> T get(String key, T defaultValue) {
		//noinspection unchecked
		return (T) values.getOrDefault(key, defaultValue);
	}

	public <T extends Enum<T>> T getEnum(String key, T defaultValue) {
		String[] rawValue = get(key, null);
		if (rawValue == null)
			return defaultValue;
		try {
			//noinspection unchecked
			return Enum.valueOf((Class<T>) Class.forName(Type.getType(rawValue[0]).getClassName()), rawValue[1]);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
	
	public String getReference(String key) {
		AnnotationInfo reference = (AnnotationInfo) values.get(key);
		String value = reference.get("value");
		List<String> overrides = reference.get("overrides", Collections.emptyList());
		if (overrides.size() > 0) {
			if ((overrides.size() & 1) == 1)
				throw new IllegalArgumentException("Override arrays can't be of odd size! " + overrides);
			for (int i = 0; i < overrides.size(); i += 2)
				if (Mixtures.PREDICATES.contains(Identifier.of(overrides.get(i))))
					value = overrides.get(i + 1);
		}
		return value;
	}
	
	@Override
	public String toString() {
		return node.desc + values;
	}
}