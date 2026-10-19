package com.tommy.common.org.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.tommy.common.org.entity.DeptHistory;

public interface DeptHistoryRepository extends JpaRepository<DeptHistory, Long>, JpaSpecificationExecutor<DeptHistory> {
}
