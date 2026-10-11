package com.tommy.common.code.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.tommy.common.code.entity.CommonCodeHistory;

public interface CommonCodeHistoryRepository extends JpaRepository<CommonCodeHistory, Long> {

	Page<CommonCodeHistory> findByCodePk(Long codePk, Pageable pageable);

}
