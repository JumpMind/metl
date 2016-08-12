package org.jumpmind.metl;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;

public class HttpMethodFilter implements Filter {

    private Set<String> allowedMethods = new HashSet<String>();
    private Set<String> disallowedMethods = new HashSet<String>();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        String allowMethodsConfig = filterConfig.getInitParameter("server.allow.http.methods");
        loadMethods(allowMethodsConfig, allowedMethods);
        String disallowMethodsConfig = filterConfig
                .getInitParameter("server.disallow.http.methods");
        loadMethods(disallowMethodsConfig, disallowedMethods);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String method = httpRequest.getMethod().toUpperCase();

        if (disallowedMethods.contains(method)) {
            forbid(method, request, response);
        } else if (!allowedMethods.isEmpty() && !allowedMethods.contains(method)) {
            forbid(method, request, response);
        } else {
            filterChain.doFilter(request, response);
        }
    }

    protected void forbid(String method, ServletRequest request, ServletResponse response)
            throws IOException {
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN,
                "Method " + method + " is not allowed.");
    }

    protected void loadMethods(String configuredValue, Set<String> methods) {
        if (!StringUtils.isEmpty(configuredValue)) {
            String[] methodsSplit = configuredValue.split(",");
            for (String method : methodsSplit) {
                if (!StringUtils.isEmpty(method)) {
                    methods.add(method.toUpperCase());
                }
            }
        }
    }

    @Override
    public void destroy() {
        // Empty.
    }

}