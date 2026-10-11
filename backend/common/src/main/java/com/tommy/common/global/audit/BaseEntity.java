package com.tommy.common.global.audit;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

	@CreatedDate
	@Column(name = "create_datetime", nullable = false, updatable = false)
	private LocalDateTime createDatetime;

	@CreatedBy
	@Column(name = "create_user_pk", updatable = false)
	private Long createUserPk;

	@LastModifiedDate
	@Column(name = "update_datetime")
	private LocalDateTime updateDatetime;

	@LastModifiedBy
	@Column(name = "update_user_pk")
	private Long updateUserPk;

}
