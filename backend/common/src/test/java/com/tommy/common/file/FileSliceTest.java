package com.tommy.common.file;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.tommy.common.file.dto.FileDtos.Download;
import com.tommy.common.global.exception.ApiException;
import com.tommy.common.global.exception.ApiExceptionHandler;
import com.tommy.common.global.paging.PageRequests;

@ExtendWith(MockitoExtension.class)
class FileSliceTest {

	@Mock
	private FileService files;

	@Test
	void downloadHeadersAndErrorMapping() throws Exception {
		when(files.download(7L)).thenReturn(new Download("hi".getBytes(), "text/plain", FileRules.disposition("보고서.png")));
		when(files.download(8L)).thenThrow(new ApiException(403, "AUTH.FORBIDDEN", "이 기능을 사용할 권한이 없습니다.", 2));
		when(files.download(9L)).thenThrow(new ApiException(401, "AUTH.UNAUTHORIZED", "인증이 필요합니다.", 2));
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new FileController(files)).setControllerAdvice(new ApiExceptionHandler()).build();
		mockMvc.perform(get("/api/mdm/v1/files/7/download"))
				.andExpect(status().isOk())
				.andExpect(header().string("X-Content-Type-Options", "nosniff"))
				.andExpect(header().string("Cache-Control", "private, no-store"))
				.andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("filename*=UTF-8''")));
		mockMvc.perform(get("/api/mdm/v1/files/8/download")).andExpect(status().isForbidden())
				.andExpect(jsonPath("$.errorCode").value("AUTH.FORBIDDEN"));
		mockMvc.perform(get("/api/mdm/v1/files/9/download")).andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errorCode").value("AUTH.UNAUTHORIZED"));
		org.junit.jupiter.api.Assertions.assertThrows(ApiException.class,
				() -> PageRequests.parse(1, 20, java.util.List.of("secret"), java.util.Set.of("filePk"), java.util.List.of(), "filePk"));
	}

}
