package com.example.cxfdemo.dao;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Hashtable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSM 測試用記憶體 InitialContextFactory，支援標準 JNDI lookup。
 */
public class MockInitialContextFactory implements InitialContextFactory {

    private static final ConcurrentHashMap<String, Object> BINDINGS = new ConcurrentHashMap<String, Object>();

    public static void bind(String name, Object obj) {
        BINDINGS.put(name, obj);
    }

    public static void clear() {
        BINDINGS.clear();
    }

    @Override
    public Context getInitialContext(Hashtable<?, ?> environment) throws NamingException {
        return createContext("");
    }

    public static Context createContext(final String basePrefix) {
        return (Context) Proxy.newProxyInstance(
                Context.class.getClassLoader(),
                new Class<?>[]{Context.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        if ("lookup".equals(method.getName())) {
                            String rawName = args[0] instanceof Name ? args[0].toString() : (String) args[0];
                            String fullName = (basePrefix.isEmpty() || rawName.startsWith("java:"))
                                    ? rawName
                                    : (basePrefix.endsWith("/") ? basePrefix + rawName : basePrefix + "/" + rawName);

                            if ("java:comp/env".equals(fullName) || "java:comp".equals(fullName) || "env".equals(fullName)) {
                                return createContext("java:comp/env/");
                            }

                            Object val = BINDINGS.get(fullName);
                            if (val == null && fullName.startsWith("java:comp/env/")) {
                                val = BINDINGS.get(fullName.substring("java:comp/env/".length()));
                            } else if (val == null && !fullName.startsWith("java:comp/env/")) {
                                val = BINDINGS.get("java:comp/env/" + fullName);
                            }
                            if (val == null) {
                                throw new NamingException("Name not bound in mock JNDI: " + rawName + " (resolved: " + fullName + ")");
                            }
                            return val;
                        }
                        if ("close".equals(method.getName())) {
                            return null;
                        }
                        return null;
                    }
                }
        );
    }
}
