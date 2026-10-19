package com.tommy.common.global.multitenancy;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tommy.common.global.response.ApiResponse;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@ConditionalOnWebApplication
public class AccessContextController {

	private final AccessContextService contexts;

	public AccessContextController(AccessContextService contexts) {
		this.contexts = contexts;
	}

	@GetMapping("/api/mdm/v1/context")
	public ResponseEntity<ApiResponse<AccessContextService.ContextView>> context(HttpServletRequest request) {
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.ok(contexts.resolve(request)));
	}

}
