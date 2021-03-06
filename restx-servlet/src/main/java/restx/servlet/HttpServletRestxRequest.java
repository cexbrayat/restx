package restx.servlet;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import restx.RestxRequest;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * User: xavierhanin
 * Date: 1/22/13
 * Time: 2:52 PM
 */
public class HttpServletRestxRequest implements RestxRequest {
    private final HttpServletRequest request;
    private BufferedInputStream bufferedInputStream;
    private ImmutableMap<String, ImmutableList<String>> queryParams;

    public HttpServletRestxRequest(HttpServletRequest request) {
        this.request = request;
    }

    @Override
    public int getPort() {
        return request.getServerPort();
    }

    @Override
    public String getBaseUri() {
        String url = request.getRequestURL().toString();
        return url.substring(0, url.lastIndexOf(getRestxPath()));
    }

    @Override
    public String getBaseNetworkPath() {
        return getBaseUri().replaceAll("^https?:", "");
    }

    @Override
    public String getRestxPath() {
        return request.getRequestURI().substring(
                (request.getContextPath() + request.getServletPath()).length());
    }

    @Override
    public String getRestxUri() {
        if (request.getQueryString() == null) {
            return getRestxPath();
        } else {
            return getRestxPath() + "?" + request.getQueryString();
        }
    }

    @Override
    public Optional<String> getQueryParam(String param) {
        return Optional.fromNullable(request.getParameter(param));
    }

    @Override
    public List<String> getQueryParams(String param) {
        return Lists.newArrayList(request.getParameterValues(param));
    }

    @Override
    public ImmutableMap<String, ImmutableList<String>> getQueryParams() {
        if (queryParams == null) {
            Map<String, String[]> parameterMap = request.getParameterMap();
            ImmutableMap.Builder<String, ImmutableList<String>> paramsBuilder = ImmutableMap.builder();
            for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
                paramsBuilder.put(entry.getKey(), ImmutableList.copyOf(entry.getValue()));
            }
            queryParams = paramsBuilder.build();
        }
        return queryParams;
    }

    @Override
    public InputStream getContentStream() throws IOException {
        /*
           maybe we could do this buffering only in dev mode?
           It is used to be able to read data again in case of json processing error.
         */
        if (bufferedInputStream == null) {
            bufferedInputStream = new BufferedInputStream(request.getInputStream()) {
                @Override
                public void close() throws IOException {
                    // NO OP, see #closeContentStream
                }
            };
            bufferedInputStream.mark(10 * 1024);
        }
        return bufferedInputStream;
    }

    @Override
    public void closeContentStream() throws IOException {
        bufferedInputStream.close();
    }

    @Override
    public String getHttpMethod() {
        return request.getMethod();
    }

    @Override
    public Map<String, String> getCookiesMap() {
        Map<String, String> cookies = Maps.newLinkedHashMap();
        Cookie[] requestCookies = request.getCookies();
        if (requestCookies != null) {
            for (int i = 0; i < requestCookies.length; i++) {
                Cookie cookie = requestCookies[i];
                cookies.put(cookie.getName(), cookie.getValue());
            }
        }
        return cookies;
    }

    @Override
    public String getCookieValue(String cookieName, String defaultValue) {
        return getCookieValue(request.getCookies(), cookieName, defaultValue);
    }

    @Override
    public boolean isPersistentCookie(String cookie) {
        Cookie c = getCookie(request.getCookies(), cookie);
        return c == null ? false : c.getMaxAge() > 0;
    }

    private static String getCookieValue(Cookie[] cookies,
                                    String cookieName,
                                    String defaultValue) {
        if (cookies == null) {
            return defaultValue;
        }
        for (int i = 0; i < cookies.length; i++) {
            Cookie cookie = cookies[i];
            if (cookieName.equals(cookie.getName()))
                return cookie.getValue();
        }
        return defaultValue;
    }

    static Cookie getCookie(Cookie[] cookies, String cookieName) {
        if (cookies == null) {
            return null;
        }
        for (int i = 0; i < cookies.length; i++) {
            Cookie cookie = cookies[i];
            if (cookieName.equals(cookie.getName()))
                return cookie;
        }
        return null;
    }

    @Override
    public Optional<String> getHeader(String headerName) {
        return Optional.fromNullable(request.getHeader(headerName));
    }

    @Override
    public String getContentType() {
        return request.getContentType();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[RESTX REQUEST] ");
        sb.append(getHttpMethod()).append(" ").append(getRestxPath());
        dumpParameters(sb);
        return sb.toString();
    }

    private void dumpParameters(StringBuilder sb) {
        if (request.getParameterMap().isEmpty()) {
            return;
        }
        sb.append(" ? ");
        for (Iterator iterator = request.getParameterMap().entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<String, String[]> entry = (Map.Entry<String, String[]>) iterator.next();
            sb.append(entry.getKey()).append("=").append(
                    entry.getValue().length == 1
                            ? entry.getValue()[0]
                            : Joiner.on("&" + entry.getKey() + "=").join(entry.getValue()));
            sb.append("&");
        }
        sb.setLength(sb.length() - 1);
    }
}
