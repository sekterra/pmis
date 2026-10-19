package com.tommy.common.file;

import java.time.LocalDate;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.tommy.common.file.dto.FileDtos.CleanupResponse;
import com.tommy.common.file.dto.FileDtos.Download;
import com.tommy.common.file.dto.FileDtos.FileResponse;
import com.tommy.common.global.paging.PageRequests;
import com.tommy.common.global.response.ApiResponse;
import com.tommy.common.global.response.PageResponse;
import com.tommy.common.global.security.RequirePermission;

@RestController
@RequestMapping("/api/mdm/v1/files")
public class FileController {

	private static final Set<String> SORTS = Set.of("uploadDatetime", "originalFileName", "fileSize", "filePk");

	private final FileService files;

	public FileController(FileService files) {
		this.files = files;
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@RequirePermission(menu = "FILE", action = "CREATE")
	public ApiResponse<FileResponse> upload(@RequestParam("file") MultipartFile file,
			@RequestParam(required = false) String storageCode) {
		return ApiResponse.ok(files.upload(file, storageCode));
	}

	@GetMapping
	@RequirePermission(menu = "FILE", action = "READ")
	public ApiResponse<PageResponse<FileResponse>> list(
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) String fileStatusCode,
			@RequestParam(required = false) Long uploadUserPk,
			@RequestParam(required = false) LocalDate from,
			@RequestParam(required = false) LocalDate to,
			@RequestParam(required = false) String useYn,
			@RequestParam(required = false) Integer page,
			@RequestParam(required = false) Integer size,
			@RequestParam(required = false) List<String> sort) {
		return ApiResponse.ok(files.list(keyword, fileStatusCode, uploadUserPk, from, to, useYn,
				PageRequests.parse(page, size, sort, SORTS, List.of(Sort.Order.desc("uploadDatetime")), "filePk")));
	}

	@GetMapping("/{filePk:[0-9]+}")
	@RequirePermission(menu = "FILE", action = "READ")
	public ApiResponse<FileResponse> get(@PathVariable long filePk) {
		return ApiResponse.ok(files.get(filePk));
	}

	@GetMapping("/{filePk:[0-9]+}/download")
	public ResponseEntity<byte[]> download(@PathVariable long filePk) {
		return body(files.download(filePk));
	}

	@DeleteMapping("/{filePk:[0-9]+}")
	@RequirePermission(menu = "FILE", action = "DELETE")
	public ApiResponse<Void> delete(@PathVariable long filePk) {
		files.delete(filePk);
		return ApiResponse.ok(null);
	}

	@PostMapping("/{filePk:[0-9]+}/restore")
	@RequirePermission(menu = "FILE", action = "UPDATE")
	public ApiResponse<FileResponse> restore(@PathVariable long filePk) {
		return ApiResponse.ok(files.restore(filePk));
	}

	@PostMapping("/cleanup")
	@RequirePermission(menu = "FILE", action = "DELETE")
	public ApiResponse<CleanupResponse> cleanup() {
		return ApiResponse.ok(files.cleanup());
	}

	static ResponseEntity<byte[]> body(Download download) {
		MediaType mediaType;
		try {
			mediaType = MediaType.parseMediaType(download.mimeType());
		}
		catch (RuntimeException ex) {
			mediaType = MediaType.APPLICATION_OCTET_STREAM;
		}
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, download.disposition())
				.header("X-Content-Type-Options", "nosniff")
				.header(HttpHeaders.CACHE_CONTROL, "private, no-store")
				.contentType(mediaType)
				.body(download.bytes());
	}

}
