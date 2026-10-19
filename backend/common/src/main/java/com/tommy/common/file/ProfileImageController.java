package com.tommy.common.file;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tommy.common.file.dto.FileDtos.ProfileRequest;
import com.tommy.common.global.response.ApiResponse;
import com.tommy.common.global.security.RequirePermission;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/mdm/v1/people/{personPk:[0-9]+}/profile-image")
public class ProfileImageController {

	private final FileService files;

	public ProfileImageController(FileService files) {
		this.files = files;
	}

	@PutMapping
	@RequirePermission(menu = "PERSON", action = "UPDATE")
	public ApiResponse<Void> put(@PathVariable long personPk, @Valid @RequestBody ProfileRequest request) {
		files.assignProfile(personPk, request.fileId());
		return ApiResponse.ok(null);
	}

	@DeleteMapping
	@RequirePermission(menu = "PERSON", action = "UPDATE")
	public ApiResponse<Void> delete(@PathVariable long personPk) {
		files.clearProfile(personPk);
		return ApiResponse.ok(null);
	}

	@GetMapping
	public ResponseEntity<byte[]> get(@PathVariable long personPk) {
		return FileController.body(files.profile(personPk));
	}

}
