package com.tommy.common.team;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tommy.common.global.exception.ApiException;
import com.tommy.common.global.query.LikeEscape;
import com.tommy.common.global.response.PageResponse;
import com.tommy.common.global.security.CurrentTenantUser;
import com.tommy.common.global.time.TenantClock;
import com.tommy.common.org.entity.Person;
import com.tommy.common.org.repository.PersonRepository;
import com.tommy.common.org.service.EntityTextService;
import com.tommy.common.org.service.OrgCodes;
import com.tommy.common.org.support.OrgExceptions;
import com.tommy.common.team.dto.TeamDtos.MemberEndRequest;
import com.tommy.common.team.dto.TeamDtos.MemberRequest;
import com.tommy.common.team.dto.TeamDtos.MemberResponse;
import com.tommy.common.team.dto.TeamDtos.MemberUpdateRequest;
import com.tommy.common.team.dto.TeamDtos.TeamRequest;
import com.tommy.common.team.dto.TeamDtos.TeamResponse;
import com.tommy.common.team.dto.TeamDtos.TeamUpdateRequest;
import com.tommy.common.team.entity.Team;
import com.tommy.common.team.entity.TeamMember;
import com.tommy.common.team.repository.TeamMemberRepository;
import com.tommy.common.team.repository.TeamRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;

@Service
public class TeamService {

	private final TeamRepository teams;

	private final TeamMemberRepository members;

	private final PersonRepository people;

	private final EntityTextService texts;

	private final OrgCodes codes;

	private final CurrentTenantUser currentUser;

	private final TenantClock clock;

	private final EntityManager entityManager;

	public TeamService(TeamRepository teams, TeamMemberRepository members, PersonRepository people, EntityTextService texts,
			OrgCodes codes, CurrentTenantUser currentUser, TenantClock clock, EntityManager entityManager) {
		this.teams = teams;
		this.members = members;
		this.people = people;
		this.texts = texts;
		this.codes = codes;
		this.currentUser = currentUser;
		this.clock = clock;
		this.entityManager = entityManager;
	}

	@Transactional(readOnly = true)
	public PageResponse<TeamResponse> list(String keyword, String teamTypeCode, Boolean active, String useYn, Pageable pageable) {
		Page<Team> page = teams.findAll(filter(keyword, teamTypeCode, active, useYn), pageable);
		Map<Long, Long> counts = memberCounts(page.getContent());
		List<TeamResponse> items = page.getContent().stream().map(team -> response(team, false, counts.getOrDefault(team.getTeamPk(), 0L)))
				.toList();
		return new PageResponse<>(items, page.getTotalElements(), pageable.getPageNumber() + 1, pageable.getPageSize());
	}

	@Transactional(readOnly = true)
	public TeamResponse get(long teamPk) {
		Team team = require(teamPk);
		return response(team, true, countCurrent(teamPk));
	}

	@Transactional
	public TeamResponse create(TeamRequest request) {
		String code = request.teamCode().trim();
		FileRulesCode(code);
		dates(request.startDate(), request.endDate());
		codes.require("TEAM_TYPE", request.teamTypeCode().trim(), "teamTypeCode");
		Long actor = currentUser.pkOrNull();
		Team team = Team.create(code, request.teamName().trim(), request.teamTypeCode().trim(), actor);
		team.change(request.teamName().trim(), request.teamTypeCode().trim(), request.startDate(), request.endDate(), request.sortOrder(),
				OrgExceptions.trimToNull(request.description()), actor);
		try {
			teams.saveAndFlush(team);
		}
		catch (DataIntegrityViolationException ex) {
			throw new ApiException(409, "TEAM.DUPLICATE", "이미 있는 팀 코드입니다.", 2);
		}
		texts.upsert(EntityTextService.TEAM, EntityTextService.TEAM_NAME, team.getTeamPk(), request.translations());
		return response(team, true, 0);
	}

	@Transactional
	public TeamResponse update(long teamPk, TeamUpdateRequest request) {
		Team team = requireActive(teamPk);
		dates(request.startDate(), request.endDate());
		codes.require("TEAM_TYPE", request.teamTypeCode().trim(), "teamTypeCode");
		team.change(request.teamName().trim(), request.teamTypeCode().trim(), request.startDate(), request.endDate(), request.sortOrder(),
				OrgExceptions.trimToNull(request.description()), currentUser.pkOrNull());
		texts.upsert(EntityTextService.TEAM, EntityTextService.TEAM_NAME, team.getTeamPk(), request.translations());
		return response(team, true, countCurrent(teamPk));
	}

	@Transactional
	public void delete(long teamPk) {
		Team team = requireActive(teamPk);
		if (members.existsByTeam_TeamPkAndEndDateIsNullAndUseYn(teamPk, "Y")) {
			throw new ApiException(409, "TEAM.HAS_ACTIVE_MEMBER", "현재 팀원이 있어 삭제할 수 없습니다.", 2);
		}
		team.deactivate(currentUser.pkOrNull());
	}

	@Transactional
	public TeamResponse restore(long teamPk) {
		Team team = require(teamPk);
		team.activate(currentUser.pkOrNull());
		return response(team, true, countCurrent(teamPk));
	}

	@Transactional(readOnly = true)
	public PageResponse<MemberResponse> members(long teamPk, String useYn, Boolean current, Pageable pageable) {
		require(teamPk);
		Page<TeamMember> page = members.findAll(memberFilter(teamPk, useYn, current), pageable);
		return new PageResponse<>(page.getContent().stream().map(this::member).toList(), page.getTotalElements(),
				pageable.getPageNumber() + 1, pageable.getPageSize());
	}

	@Transactional
	public MemberResponse addMember(long teamPk, MemberRequest request) {
		Team team = requireActive(teamPk);
		dates(request.startDate(), request.endDate());
		codes.require("TEAM_ROLE", request.teamRoleCode().trim(), "teamRoleCode");
		Person person = person(request.personPk());
		if (request.endDate() == null
				&& members.existsByTeam_TeamPkAndPerson_PersonPkAndEndDateIsNullAndUseYn(teamPk, person.getPersonPk(), "Y")) {
			throw new ApiException(409, "TEAM.MEMBER_DUPLICATE", "이미 현재 팀원입니다.", 2);
		}
		TeamMember member = TeamMember.create(team, person, request.teamRoleCode().trim(), request.startDate(), currentUser.pkOrNull());
		member.change(request.teamRoleCode().trim(), request.startDate(), request.endDate(),
				OrgExceptions.trimToNull(request.description()), currentUser.pkOrNull());
		try {
			members.saveAndFlush(member);
		}
		catch (DataIntegrityViolationException ex) {
			throw new ApiException(409, "TEAM.MEMBER_DUPLICATE", "이미 현재 팀원입니다.", 2);
		}
		return member(member);
	}

	@Transactional
	public MemberResponse updateMember(long teamMemberPk, MemberUpdateRequest request) {
		TeamMember member = requireMember(teamMemberPk);
		if (!member.inUse()) {
			throw new ApiException(409, "TEAM.MEMBER_INACTIVE", "사용 중이 아닌 팀원입니다.", 2);
		}
		dates(request.startDate(), request.endDate());
		codes.require("TEAM_ROLE", request.teamRoleCode().trim(), "teamRoleCode");
		if (request.endDate() == null && member.getEndDate() != null
				&& members.existsByTeam_TeamPkAndPerson_PersonPkAndEndDateIsNullAndUseYn(member.getTeam().getTeamPk(),
						member.getPerson().getPersonPk(), "Y")) {
			throw new ApiException(409, "TEAM.MEMBER_DUPLICATE", "이미 현재 팀원입니다.", 2);
		}
		member.change(request.teamRoleCode().trim(), request.startDate(), request.endDate(),
				OrgExceptions.trimToNull(request.description()), currentUser.pkOrNull());
		return member(member);
	}

	@Transactional
	public MemberResponse endMember(long teamMemberPk, MemberEndRequest request) {
		TeamMember member = requireMember(teamMemberPk);
		if (!member.inUse()) {
			throw new ApiException(409, "TEAM.MEMBER_INACTIVE", "사용 중이 아닌 팀원입니다.", 2);
		}
		if (member.getEndDate() != null) {
			throw new ApiException(409, "TEAM.MEMBER_ALREADY_ENDED", "이미 종료된 팀원입니다.", 2);
		}
		LocalDate end = request == null || request.endDate() == null ? clock.today() : request.endDate();
		dates(member.getStartDate(), end);
		member.end(end, currentUser.pkOrNull());
		return member(member);
	}

	@Transactional
	public void deleteMember(long teamMemberPk) {
		TeamMember member = requireMember(teamMemberPk);
		if (!member.inUse()) {
			throw new ApiException(409, "TEAM.MEMBER_INACTIVE", "사용 중이 아닌 팀원입니다.", 2);
		}
		member.deactivate(currentUser.pkOrNull());
	}

	@Transactional
	public MemberResponse restoreMember(long teamMemberPk) {
		TeamMember member = requireMember(teamMemberPk);
		if (member.getEndDate() == null && members.existsByTeam_TeamPkAndPerson_PersonPkAndEndDateIsNullAndUseYn(
				member.getTeam().getTeamPk(), member.getPerson().getPersonPk(), "Y")) {
			throw new ApiException(409, "TEAM.MEMBER_DUPLICATE", "이미 현재 팀원입니다.", 2);
		}
		member.activate(currentUser.pkOrNull());
		return member(member);
	}

	private void FileRulesCode(String code) {
		if (!com.tommy.common.file.FileRules.CODE.matcher(code).matches()) {
			throw OrgExceptions.field("COMMON.VALIDATION_FAILED", "팀 코드 형식이 올바르지 않습니다.", "teamCode");
		}
	}

	private static void dates(LocalDate start, LocalDate end) {
		OrgExceptions.requireDateOrder(start, end, "TEAM.INVALID_DATE", "시작일은 종료일보다 늦을 수 없습니다.");
	}

	private Team require(long teamPk) {
		return teams.findById(teamPk).orElseThrow(() -> new ApiException(404, "TEAM.NOT_FOUND", "팀을 찾을 수 없습니다.", 2));
	}

	private Team requireActive(long teamPk) {
		Team team = require(teamPk);
		if (!team.inUse()) {
			throw new ApiException(409, "TEAM.INACTIVE", "사용 중이 아닌 팀입니다.", 2);
		}
		return team;
	}

	private TeamMember requireMember(long teamMemberPk) {
		return members.findById(teamMemberPk)
				.orElseThrow(() -> new ApiException(404, "TEAM.MEMBER_NOT_FOUND", "팀원을 찾을 수 없습니다.", 2));
	}

	private Person person(long personPk) {
		Person person = people.findById(personPk)
				.orElseThrow(() -> OrgExceptions.of(404, "PERSON.NOT_FOUND", "사람을 찾을 수 없습니다."));
		if (!person.inUse()) {
			throw OrgExceptions.of(409, "PERSON.INACTIVE", "사용 중이 아닌 사람입니다.");
		}
		return person;
	}

	private long countCurrent(long teamPk) {
		Number count = (Number) entityManager.createNativeQuery("""
				SELECT count(*) FROM team_member WHERE team_pk = ? AND use_yn = 'Y' AND end_date IS NULL
				""").setParameter(1, teamPk).getSingleResult();
		return count.longValue();
	}

	private Map<Long, Long> memberCounts(List<Team> teams) {
		Map<Long, Long> counts = new HashMap<>();
		if (teams.isEmpty()) {
			return counts;
		}
		List<Long> ids = teams.stream().map(Team::getTeamPk).toList();
		@SuppressWarnings("unchecked")
		List<Object[]> rows = entityManager.createNativeQuery("""
				SELECT team_pk, count(*) FROM team_member
				WHERE use_yn = 'Y' AND end_date IS NULL AND team_pk IN (:ids)
				GROUP BY team_pk
				""").setParameter("ids", ids).getResultList();
		for (Object[] row : rows) {
			counts.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
		}
		return counts;
	}

	private TeamResponse response(Team team, boolean detail, long memberCount) {
		return new TeamResponse(team.getTeamPk(), team.getTeamCode(), team.getTeamName(), team.getTeamTypeCode(), team.getStartDate(),
				team.getEndDate(), team.getSortOrder(), team.getDescription(), team.getUseYn(), memberCount, team.getCreateDatetime(),
				team.getCreateUserPk(), team.getUpdateDatetime(), team.getUpdateUserPk(),
				detail ? texts.list(EntityTextService.TEAM, EntityTextService.TEAM_NAME, team.getTeamPk()) : null);
	}

	private MemberResponse member(TeamMember member) {
		return new MemberResponse(member.getTeamMemberPk(), member.getTeam().getTeamPk(), member.getPerson().getPersonPk(),
				member.getPerson().getPersonName(), member.getTeamRoleCode(), member.getStartDate(), member.getEndDate(),
				member.getDescription(), member.getUseYn(), member.current());
	}

	private Specification<Team> filter(String keyword, String teamTypeCode, Boolean active, String useYn) {
		return (root, query, builder) -> {
			List<Predicate> predicates = new ArrayList<>();
			String text = OrgExceptions.trimToNull(keyword);
			if (text != null) {
				predicates.add(builder.or(LikeEscape.contains(builder, root.get("teamCode"), text),
						LikeEscape.contains(builder, root.get("teamName"), text)));
			}
			if (OrgExceptions.trimToNull(teamTypeCode) != null) {
				predicates.add(builder.equal(root.get("teamTypeCode"), teamTypeCode.trim()));
			}
			if (Boolean.TRUE.equals(active)) {
				LocalDate today = clock.today();
				predicates.add(builder.or(builder.isNull(root.get("endDate")),
						builder.greaterThanOrEqualTo(root.get("endDate"), today)));
			}
			if (OrgExceptions.trimToNull(useYn) != null) {
				predicates.add(builder.equal(root.get("useYn"), useYn.trim()));
			}
			return predicates.isEmpty() ? builder.conjunction() : builder.and(predicates.toArray(Predicate[]::new));
		};
	}

	private static Specification<TeamMember> memberFilter(long teamPk, String useYn, Boolean current) {
		return (root, query, builder) -> {
			List<Predicate> predicates = new ArrayList<>();
			predicates.add(builder.equal(root.get("team").get("teamPk"), teamPk));
			if (OrgExceptions.trimToNull(useYn) != null) {
				predicates.add(builder.equal(root.get("useYn"), useYn.trim()));
			}
			if (Boolean.TRUE.equals(current)) {
				predicates.add(builder.and(builder.equal(root.get("useYn"), "Y"), builder.isNull(root.get("endDate"))));
			}
			return builder.and(predicates.toArray(Predicate[]::new));
		};
	}

}
