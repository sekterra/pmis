package com.tommy.common.code.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.tommy.common.code.entity.CommonCode;

public interface CommonCodeRepository extends JpaRepository<CommonCode, Long>, JpaSpecificationExecutor<CommonCode> {

	Optional<CommonCode> findByCodeGroupCodeGroupPkAndCodeValue(Long codeGroupPk, String codeValue);

	Optional<CommonCode> findByCodeGroupCodeGroupPkAndCodeValueAndUseYn(Long codeGroupPk, String codeValue, String useYn);

	List<CommonCode> findByCodeGroupCodeGroupPk(Long codeGroupPk);

	boolean existsByCodeGroupCodeGroupPkAndUseYn(Long codeGroupPk, String useYn);

	boolean existsByCodeGroupCodeGroupPkAndCodeValueAndUseYnAndCodePkNot(Long codeGroupPk, String codeValue, String useYn,
			Long codePk);

}
