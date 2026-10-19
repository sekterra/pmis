package com.tommy.common.access.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tommy.common.access.entity.UserPasswordHistory;

public interface UserPasswordHistoryRepository extends JpaRepository<UserPasswordHistory, Long> {
}
