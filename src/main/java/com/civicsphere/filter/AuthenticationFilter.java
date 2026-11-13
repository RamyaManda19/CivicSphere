package com.civicsphere.filter;

import com.civicsphere.util.JsonUtil;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Filter enforcing session authentication and role-based access control.
 */
@WebFilter(filterName = "AuthenticationFilter", urlPatterns = {"/*"})
public class AuthenticationFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String path = req.getRequestURI();
        String contextPath = req.getContextPath();
        String relativePath = path.substring(contextPath.length());

        // Normalize
        if (relativePath.isEmpty()) {
            relativePath = "/";
        }

        // Public resources
        if (isPublicPath(relativePath)) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = req.getSession(false);
        Integer userId = (session != null) ? (Integer) session.getAttribute("USER_ID") : null;
        String role = (session != null) ? (String) session.getAttribute("USER_ROLE") : null;

        // Check authentication
        if (userId == null || role == null) {
            if (relativePath.startsWith("/api/")) {
                JsonUtil.sendErrorResponse(resp, HttpServletResponse.SC_UNAUTHORIZED, "Authentication required. Please log in.");
            } else {
                resp.sendRedirect(contextPath + "/login.html");
            }
            return;
        }

        // Check ADMIN authorization for admin endpoints
        if (isAdminPath(relativePath) && !"ADMIN".equalsIgnoreCase(role)) {
            if (relativePath.startsWith("/api/")) {
                JsonUtil.sendErrorResponse(resp, HttpServletResponse.SC_FORBIDDEN, "Access denied. Administrator privileges required.");
            } else {
                resp.sendRedirect(contextPath + "/citizen/dashboard.html");
            }
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isPublicPath(String path) {
        return path.equals("/") ||
               path.equals("/index.html") ||
               path.equals("/login.html") ||
               path.equals("/register.html") ||
               path.startsWith("/css/") ||
               path.startsWith("/js/") ||
               path.startsWith("/assets/") ||
               path.startsWith("/uploads/") ||
               path.equals("/api/auth/login") ||
               path.equals("/api/auth/register") ||
               path.equals("/api/auth/me") ||
               path.equals("/api/categories");
    }

    private boolean isAdminPath(String path) {
        return path.startsWith("/admin/") ||
               path.startsWith("/api/admin/") ||
               (path.startsWith("/api/departments") && !path.equals("/api/departments")) ||
               (path.startsWith("/api/recurrence") && path.endsWith("/verify"));
    }

    @Override
    public void destroy() {}
}

