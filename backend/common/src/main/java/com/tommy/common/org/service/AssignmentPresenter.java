package com.tommy.common.org.service;

import org.springframework.stereotype.Component;

import com.tommy.common.org.dto.AssignmentResponse;
import com.tommy.common.org.entity.Dept;
import com.tommy.common.org.entity.PersonAssignment;

@Component
public class AssignmentPresenter {

	public AssignmentResponse response(PersonAssignment assignment) {
		Dept dept = assignment.getDept();
		return new AssignmentResponse(assignment.getAssignmentPk(), assignment.getPerson().getPersonPk(),
				assignment.getCompany().getCompanyPk(), assignment.getCompany().getCompanyCode(),
				assignment.getCompany().getCompanyName(), dept == null ? null : dept.getDeptPk(),
				dept == null ? null : dept.getDeptCode(), dept == null ? null : dept.getDeptName(),
				assignment.getEmployeeNo(), assignment.getEmploymentTypeCode(), assignment.getPositionCode(),
				assignment.getJobTitleCode(), assignment.getAssignmentStatusCode(), assignment.getPrimaryYn(),
				assignment.getDeptHeadYn(), assignment.getStartDate(), assignment.getEndDate(), assignment.getDescription(),
				assignment.getUseYn(), assignment.getCreateDatetime(), assignment.getCreateUserPk(),
				assignment.getUpdateDatetime(), assignment.getUpdateUserPk());
	}

}
