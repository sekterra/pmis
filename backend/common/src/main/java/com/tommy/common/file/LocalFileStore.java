package com.tommy.common.file;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.springframework.stereotype.Component;

import com.tommy.common.global.exception.ApiException;

@Component
public class LocalFileStore {

	public void write(Path target, byte[] bytes) {
		Path part = target.resolveSibling(target.getFileName().toString() + ".uploading");
		try {
			Files.createDirectories(target.getParent());
			Files.write(part, bytes);
			try {
				Files.move(part, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			}
			catch (AtomicMoveNotSupportedException ex) {
				Files.move(part, target, StandardCopyOption.REPLACE_EXISTING);
			}
		}
		catch (IOException ex) {
			deleteQuiet(part);
			throw new ApiException(500, "COMMON.INTERNAL", "파일을 저장하지 못했습니다.", 2);
		}
	}

	public byte[] read(Path target) {
		try {
			return Files.readAllBytes(target);
		}
		catch (IOException ex) {
			return null;
		}
	}

	public boolean exists(Path target) {
		return Files.isRegularFile(target);
	}

	public boolean delete(Path target) {
		try {
			if (!Files.exists(target)) {
				return true;
			}
			Files.delete(target);
			return true;
		}
		catch (IOException ex) {
			return false;
		}
	}

	public void deleteQuiet(Path target) {
		if (target != null) {
			delete(target);
		}
	}

}
