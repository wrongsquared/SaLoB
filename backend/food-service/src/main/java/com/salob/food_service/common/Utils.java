package com.salob.food_service.common;

import jakarta.servlet.http.HttpServletRequest;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.salob.food_service.api.onemap.OneMapClient.POSTAL_CODE_PATTERN;

public final class Utils {

	public static String stripPostalCode(String address) {
		return POSTAL_CODE_PATTERN.matcher(address).replaceAll("").trim();
	}

	// Trim, lowercase, replace space with underscore, and remove weird chars
	public static String prepareAddressForObjKey(String address) {
		return address.replace(" ", "_").trim().toLowerCase().replaceAll("[^a-z0-9]", "");
	}

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
