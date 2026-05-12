package com.salob.food_service.common;

import jakarta.servlet.http.HttpServletRequest;

public final class Utils {
    /**
     * Extract client IP from HTTP request.
     *
     * Handles proxies (X-Forwarded-For header) which is important in production
     * where requests may go through load balancers or reverse proxies.
     */
    public static String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For can have multiple IPs; take the first one
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
