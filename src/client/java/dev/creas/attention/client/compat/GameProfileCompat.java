package dev.creas.attention.client.compat;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class GameProfileCompat {
	private static final ConcurrentMap<Class<?>, Method> NAME_METHODS = new ConcurrentHashMap<>();
	private static final ConcurrentMap<Class<?>, Method> ID_METHODS = new ConcurrentHashMap<>();

	private GameProfileCompat() {
	}

	public static String name(Object profile) {
		Object value = invoke(profile, NAME_METHODS, "name", "getName");
		return value instanceof String name ? name : "";
	}

	public static UUID id(Object profile) {
		Object value = invoke(profile, ID_METHODS, "id", "getId");
		return value instanceof UUID id ? id : null;
	}

	private static Object invoke(Object target, ConcurrentMap<Class<?>, Method> cache, String modernName, String legacyName) {
		if (target == null) {
			return null;
		}

		try {
			Method method = cache.computeIfAbsent(target.getClass(), type -> findMethod(type, modernName, legacyName));
			return method.invoke(target);
		} catch (ReflectiveOperationException exception) {
			throw new IllegalStateException("Failed to read game profile", exception);
		}
	}

	private static Method findMethod(Class<?> type, String modernName, String legacyName) {
		Method method = findNoArgMethod(type, modernName);
		if (method == null) {
			method = findNoArgMethod(type, legacyName);
		}
		if (method == null) {
			throw new IllegalStateException("Unsupported game profile type: " + type.getName());
		}
		method.setAccessible(true);
		return method;
	}

	private static Method findNoArgMethod(Class<?> type, String name) {
		try {
			return type.getMethod(name);
		} catch (NoSuchMethodException ignored) {
			return null;
		}
	}
}
