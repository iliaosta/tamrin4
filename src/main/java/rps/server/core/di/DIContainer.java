package rps.server.core.di;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

public class DIContainer {

    private final Map<Class<?>, Class<?>> CLASS_REGISTRY = new HashMap<>();
    private final Map<Class<?>, Object> INSTANCE_CACHE = new HashMap<>();

    public <T> void register(Class<T> abstraction, Class<? extends T> implementation) {
        CLASS_REGISTRY.put(abstraction, implementation);
    }

    public <T> T resolve(Class<T> abstraction) {
        // return singleton if already built
        if (INSTANCE_CACHE.containsKey(abstraction)) {
            return abstraction.cast(INSTANCE_CACHE.get(abstraction));
        }

        Class<?> implementation = CLASS_REGISTRY.get(abstraction);
        if (implementation == null) {
            throw new RuntimeException("No implementation registered for: " + abstraction.getName());
        }

        try {
            Object instance = createInstance(implementation);
            // cache by abstraction type (singleton behavior)
            INSTANCE_CACHE.put(abstraction, instance);
            return abstraction.cast(instance);
        } catch (Exception e) {
            throw new RuntimeException("Failed to resolve: " + abstraction.getName(), e);
        }
    }

    private Object createInstance(Class<?> implementation) throws Exception {
        Constructor<?>[] constructors = implementation.getConstructors();

        // اگر چند کانستراکتور داشت، ساده‌ترین: اولین public constructor
        // (برای پروژه کافی است. بعداً اگر خواستیم بهترش می‌کنیم)
        if (constructors.length == 0) {
            Constructor<?> declared = implementation.getDeclaredConstructor();
            declared.setAccessible(true);
            return declared.newInstance();
        }

        Constructor<?> ctor = constructors[0];
        Class<?>[] paramTypes = ctor.getParameterTypes();

        Object[] params = new Object[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++) {
            params[i] = resolve(paramTypes[i]); // DI recursive
        }

        return ctor.newInstance(params);
    }
}
