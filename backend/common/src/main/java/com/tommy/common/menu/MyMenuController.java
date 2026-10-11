package com.tommy.common.menu;

import java.util.List;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tommy.common.global.response.ApiResponse;

@RestController
@RequestMapping("/api/mdm/v1/menus")
public class MyMenuController {

	private final MyMenuService menus;

	public MyMenuController(MyMenuService menus) {
		this.menus = menus;
	}

	@GetMapping("/my")
	public ResponseEntity<ApiResponse<List<MyMenuItem>>> my(@RequestParam(required = false) String lang,
			@RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.ok(menus.my(lang, acceptLanguage)));
	}

}
