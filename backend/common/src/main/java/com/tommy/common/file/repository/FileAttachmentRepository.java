package com.tommy.common.file.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tommy.common.file.entity.FileAttachment;

public interface FileAttachmentRepository extends JpaRepository<FileAttachment, Long> {

	Optional<FileAttachment> findByTargetTableNameAndTargetKeyAndFile_FilePk(String targetTableName, String targetKey, Long filePk);

	List<FileAttachment> findByTargetTableNameAndTargetKeyAndUseYn(String targetTableName, String targetKey, String useYn);

	List<FileAttachment> findByFile_FilePkAndUseYn(Long filePk, String useYn);

	boolean existsByFile_FilePkAndUseYn(Long filePk, String useYn);

	boolean existsByFile_FilePk(Long filePk);

}
