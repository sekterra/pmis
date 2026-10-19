package com.tommy.common.file.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.tommy.common.file.entity.StoredFile;

public interface StoredFileRepository extends JpaRepository<StoredFile, Long>, JpaSpecificationExecutor<StoredFile> {

	boolean existsByStorage_StoragePkAndUseYnAndFileStatusCodeNot(Long storagePk, String useYn, String fileStatusCode);

	boolean existsByFilePk(Long filePk);

}
