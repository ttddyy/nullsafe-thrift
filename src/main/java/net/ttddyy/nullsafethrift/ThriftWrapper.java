package net.ttddyy.nullsafethrift;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * @author Tadaya Tsuyukubo
 */
public class ThriftWrapper {

    /**
     * Returns null-safe collection proxy for thrift generated class.
     *
     * @param source thrift generated instance
     * @param <T>
     * @return a wrapped proxy
     */
    public static <T> T w(final T source) {

        final Class<?> sourceClass = source.getClass();
        final Set<Field> initiallyNullListFields = new HashSet<Field>();
        final Set<Field> initiallyNullSetFields = new HashSet<Field>();
        final Set<Field> initiallyNullMapFields = new HashSet<Field>();
        final Method thriftWriteMethod = getThriftWriteMethod(sourceClass);

        boolean containsCollection = false;
        for (Field field : sourceClass.getDeclaredFields()) {

            if (Modifier.isStatic(field.getModifiers())) {
                continue;  // ignore static attributes
            }

            final Class<?> fieldType = field.getType();

            field.setAccessible(true);
            final Object value = getFieldValue(field, source);

            // TODO: these can be cached
            final boolean isThriftClass = isThriftClass(fieldType);
            final boolean isList = fieldType.isAssignableFrom(List.class);
            final boolean isSet = fieldType.isAssignableFrom(Set.class);
            final boolean isMap = fieldType.isAssignableFrom(Map.class);
            final boolean isCollection = isList || isSet || isMap;

            if (isThriftClass) {
                if (value != null) {
                    // traverse all non-null thrift class attributes
                    setFieldValue(field, source, w(value));
                }
            } else if (isCollection) {
                containsCollection = true;
                if (value == null) {
                    if (isList) {
                        initiallyNullListFields.add(field);
                        setFieldValue(field, source, new ArrayList<Object>());
                    } else if (isSet) {
                        initiallyNullSetFields.add(field);
                        setFieldValue(field, source, new HashSet<Object>());
                    } else {
                        initiallyNullMapFields.add(field);
                        setFieldValue(field, source, new HashMap<Object, Object>());
                    }
                }
            }

        }

        if (!containsCollection) {
            return source;  // doesn't make proxy
        }


        // make proxy
        final Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(sourceClass);
        enhancer.setInterfaces(sourceClass.getInterfaces());
        enhancer.setCallback(new MethodInterceptor() {
            @Override
            public Object intercept(Object obj, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {

                if (method.equals(thriftWriteMethod)) {
                    // when collection was initially null and still is an empty collection, then set back to null
                    // to omit thrift transmission.
                    for (Field field : initiallyNullListFields) {
                        if (((List) field.get(source)).size() == 0) {
                            field.set(source, null);
                        }
                    }
                    for (Field field : initiallyNullSetFields) {
                        if (((Set) field.get(source)).size() == 0) {
                            field.set(source, null);
                        }
                    }
                    for (Field field : initiallyNullMapFields) {
                        if (((Map) field.get(source)).size() == 0) {
                            field.set(source, null);
                        }
                    }
                }
                return methodProxy.invoke(source, args);

            }
        });
        return (T) enhancer.create();

    }

    private static Object getFieldValue(Field field, Object source) {
        try {
            return field.get(source);
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException("Could not access method: " + ex.getMessage());
        }
    }

    private static void setFieldValue(Field field, Object source, Object value) {
        try {
            field.set(source, value);
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException("Could not access method: " + ex.getMessage());
        }
    }

    private static Method getThriftWriteMethod(Class<?> clazz) {
        for (Method method : clazz.getMethods()) {
            // to work with in-lined thrift classes, finding method by name.
            // ideally this should compare with {@link org.apache.thrift.TBase#write}.
            if ("write".equals(method.getName())) {
                return method;
            }
        }
        return null;
    }

    private static boolean isThriftClass(Class<?> clazz) {
        for (Class<?> interfaceClass : clazz.getInterfaces()) {
            // ideally it should compare with {@link org.apache.thrift.TBase}. But for inlined Thrift,
            // doing string comparison for now.
            if (interfaceClass.getSimpleName().equals("TBase")) {
                return true;
            }
        }
        return false;
    }


}
