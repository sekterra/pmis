package com.tommy.common.i18n.controller;

import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tommy.common.global.paging.PageRequests;
import com.tommy.common.global.response.ApiResponse;
import com.tommy.common.global.response.PageResponse;
import com.tommy.common.global.security.RequirePermission;
import com.tommy.common.i18n.dto.MessageRequest;
import com.tommy.common.i18n.dto.MessageResponse;
import com.tommy.common.i18n.service.I18nMessageService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/mdm/v1/i18n-messages")
public class I18nMessageController {

	private static final Set<String> SORTS = Set.of("messageKey", "messageTypeCode", "messagePk");

	private static final List<Sort.Order> DEFAULT_SORT = List.of(Sort.Order.asc("messageKey"), Sort.Order.asc("messagePk"));

	private final I18nMessageService messages;

	public I18nMessageController(I18nMessageService messages) {
		this.messages = messages;
	}

	@GetMapping
	@RequirePermission(menu = "I18N", action = "READ")
	public ApiResponse<PageResponse<MessageResponse>> list(
			@RequestParam(required = false) String namespaceCode,
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) String messageTypeCode,
			@RequestParam(required = false) String useYn,
			@RequestParam(required = false) String translationStatusCode,
			@RequestParam(required = false) Integer page,
			@RequestParam(required = false) Integer size,
			@RequestParam(required = false) List<String> sort) {
		return ApiResponse.ok(messages.list(namespaceCode, keyword, messageTypeCode, useYn, translationStatusCode,
				PageRequests.parse(page, size, sort, SORTS, DEFAULT_SORT, "messagePk")));
	}

	@GetMapping("/{messagePk}")
	@RequirePermission(menu = "I18N", action = "READ")
	public ApiResponse<MessageResponse> get(@PathVariable long messagePk) {
		return ApiResponse.ok(messages.get(messagePk));
	}

	@PostMapping
	@RequirePermission(menu = "I18N", action = "CREATE")
	public ApiResponse<MessageResponse> create(@Valid @RequestBody MessageRequest request) {
		return ApiResponse.ok(messages.create(request));
	}

	@PutMapping("/{messagePk}")
	@RequirePermission(menu = "I18N", action = "UPDATE")
	public ApiResponse<MessageResponse> update(@PathVariable long messagePk, @Valid @RequestBody MessageRequest request) {
		return ApiResponse.ok(messages.update(messagePk, request));
	}

	@DeleteMapping("/{messagePk}")
	@RequirePermission(menu = "I18N", action = "DELETE")
	public ApiResponse<Void> deactivate(@PathVariable long messagePk) {
		messages.deactivate(messagePk);
		return ApiResponse.ok(null);
	}

	@PostMapping("/{messagePk}/restore")
	@RequirePermission(menu = "I18N", action = "UPDATE")
	public ApiResponse<MessageResponse> restore(@PathVariable long messagePk) {
		return ApiResponse.ok(messages.restore(messagePk));
	}

}
