package com.tommy.common.file;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tommy.common.file.dto.FileDtos.AttachmentRequest;
import com.tommy.common.file.dto.FileDtos.AttachmentResponse;
import com.tommy.common.global.response.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/mdm/v1/attachments")
public class AttachmentController {

	private final AttachmentService attachments;

	public AttachmentController(AttachmentService attachments) {
		this.attachments = attachments;
	}

	@PostMapping
	public ApiResponse<AttachmentResponse> create(@Valid @RequestBody AttachmentRequest request) {
		return ApiResponse.ok(attachments.create(request));
	}

	@GetMapping
	public ApiResponse<List<AttachmentResponse>> list(@RequestParam String targetTableName, @RequestParam String targetKey) {
		return ApiResponse.ok(attachments.list(targetTableName, targetKey));
	}

	@GetMapping("/{attachmentPk:[0-9]+}/download")
	public org.springframework.http.ResponseEntity<byte[]> download(@PathVariable long attachmentPk) {
		return FileController.body(attachments.download(attachmentPk));
	}

	@DeleteMapping("/{attachmentPk:[0-9]+}")
	public ApiResponse<Void> delete(@PathVariable long attachmentPk) {
		attachments.delete(attachmentPk);
		return ApiResponse.ok(null);
	}

}
