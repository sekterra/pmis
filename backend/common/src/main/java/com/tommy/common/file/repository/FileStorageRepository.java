package com.tommy.common.file.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.tommy.common.file.entity.FileStorage;

public interface FileStorageRepository extends JpaRepository<FileStorage, Long>, JpaSpecificationExecutor<FileStorage> {

	Optional<FileStorage> findByStorageCode(String storageCode);

	Optional<FileStorage> findFirstByDefaultYnAndUseYn(String defaultYn, String useYn);

	List<FileStorage> findByDefaultYnAndUseYn(String defaultYn, String useYn);

	long countByUseYn(String useYn);

}
