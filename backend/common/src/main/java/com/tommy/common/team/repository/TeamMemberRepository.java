package com.tommy.common.team.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.tommy.common.team.entity.TeamMember;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long>, JpaSpecificationExecutor<TeamMember> {

	boolean existsByTeam_TeamPkAndPerson_PersonPkAndEndDateIsNullAndUseYn(Long teamPk, Long personPk, String useYn);

	boolean existsByTeam_TeamPkAndEndDateIsNullAndUseYn(Long teamPk, String useYn);

}
