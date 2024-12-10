package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.*;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;

public class OkUrlFactoryClassReplacement extends ThirdPartyMethodReplacementClass {

    private static final OkUrlFactoryClassReplacement singleton = new OkUrlFactoryClassReplacement();

    @Override
    protected String getNameOfThirdPartyTargetClass() {
        return "com.squareup.okhttp.OkUrlFactory";
    }

    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = "okhttpclient_OkUrlFactory_open",
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.NET
    )
    public static HttpURLConnection open(Object caller, URL url) {
        if (caller == null) {
            throw new NullPointerException();
        }

        Method original = getOriginal(singleton, "okhttpclient_OkUrlFactory_open", caller);

        URL replaced = ExternalServiceUtils.getReplacedURL(url);
        try {
            return (HttpURLConnection) original.invoke(caller, replaced);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw (RuntimeException) e.getCause();
        }
    }

    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = "okhttpclient_OkUrlFactory_open_proxy",
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.NET
    )
    public static HttpURLConnection open(Object caller, URL url, Proxy proxy) {
        if (caller == null) {
            throw new NullPointerException();
        }

        Method original = getOriginal(singleton, "okhttpclient_OkUrlFactory_open_proxy", caller);

        URL replaced = ExternalServiceUtils.getReplacedURL(url);
        try {
            return (HttpURLConnection) original.invoke(caller, replaced, proxy);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw (RuntimeException) e.getCause();
        }
    }

}
