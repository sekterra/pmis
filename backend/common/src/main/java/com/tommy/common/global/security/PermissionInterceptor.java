package com.tommy.common.global.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@ConditionalOnWebApplication
public class PermissionInterceptor implements HandlerInterceptor {

	private final PermissionChecker permissionChecker;

	public PermissionInterceptor(PermissionChecker permissionChecker) {
		this.permissionChecker = permissionChecker;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		if (handler instanceof HandlerMethod method) {
			RequirePermission permission = method.getMethodAnnotation(RequirePermission.class);
			if (permission != null) {
				permissionChecker.check(permission.menu(), permission.action());
			}
		}
		return true;
	}

}
