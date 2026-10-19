package com.tommy.common.file;

import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.tommy.common.file.dto.FileDtos.CleanupResponse;
import com.tommy.common.file.dto.FileDtos.Download;
import com.tommy.common.file.dto.FileDtos.FileResponse;
import com.tommy.common.file.dto.FileDtos.StorageRequest;
import com.tommy.common.file.dto.FileDtos.StorageResponse;
import com.tommy.common.file.dto.FileDtos.StorageUpdateRequest;
import com.tommy.common.global.paging.PageRequests;
import com.tommy.common.global.response.ApiResponse;
import com.tommy.common.global.response.PageResponse;
import com.tommy.common.global.security.RequirePermission;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/mdm/v1/file-storages")
public class FileStorageController {

	private static final Set<String> SORTS = Set.of("storageCode", "storageName", "createDatetime", "storagePk");

	private final FileStorageService storages;

	public FileStorageController(FileStorageService storages) {
		this.storages = storages;
	}

	@GetMapping
	@RequirePermission(menu = "FILE_STORAGE", action = "READ")
	public ApiResponse<PageResponse<StorageResponse>> list(
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) String storageTypeCode,
			@RequestParam(required = false) String useYn,
			@RequestParam(required = false) Integer page,
			@RequestParam(required = false) Integer size,
			@RequestParam(required = false) List<String> sort) {
		return ApiResponse.ok(storages.list(keyword, storageTypeCode, useYn,
				PageRequests.parse(page, size, sort, SORTS, List.of(Sort.Order.asc("storageCode")), "storagePk")));
	}

	@GetMapping("/{storagePk:[0-9]+}")
	@RequirePermission(menu = "FILE_STORAGE", action = "READ")
	public ApiResponse<StorageResponse> get(@PathVariable long storagePk) {
		return ApiResponse.ok(storages.get(storagePk));
	}

	@PostMapping
	@RequirePermission(menu = "FILE_STORAGE", action = "CREATE")
	public ApiResponse<StorageResponse> create(@Valid @RequestBody StorageRequest request) {
		return ApiResponse.ok(storages.create(request));
	}

	@PutMapping("/{storagePk:[0-9]+}")
	@RequirePermission(menu = "FILE_STORAGE", action = "UPDATE")
	public ApiResponse<StorageResponse> update(@PathVariable long storagePk, @Valid @RequestBody StorageUpdateRequest request) {
		return ApiResponse.ok(storages.update(storagePk, request));
	}

	@DeleteMapping("/{storagePk:[0-9]+}")
	@RequirePermission(menu = "FILE_STORAGE", action = "DELETE")
	public ApiResponse<Void> delete(@PathVariable long storagePk) {
		storages.delete(storagePk);
		return ApiResponse.ok(null);
	}

	@PostMapping("/{storagePk:[0-9]+}/restore")
	@RequirePermission(menu = "FILE_STORAGE", action = "UPDATE")
	public ApiResponse<StorageResponse> restore(@PathVariable long storagePk) {
		return ApiResponse.ok(storages.restore(storagePk));
	}

}
