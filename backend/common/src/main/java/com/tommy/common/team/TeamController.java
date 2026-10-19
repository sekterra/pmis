package com.tommy.common.team;

import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tommy.common.global.paging.PageRequests;
import com.tommy.common.global.response.ApiResponse;
import com.tommy.common.global.response.PageResponse;
import com.tommy.common.global.security.RequirePermission;
import com.tommy.common.team.dto.TeamDtos.MemberEndRequest;
import com.tommy.common.team.dto.TeamDtos.MemberRequest;
import com.tommy.common.team.dto.TeamDtos.MemberResponse;
import com.tommy.common.team.dto.TeamDtos.MemberUpdateRequest;
import com.tommy.common.team.dto.TeamDtos.TeamRequest;
import com.tommy.common.team.dto.TeamDtos.TeamResponse;
import com.tommy.common.team.dto.TeamDtos.TeamUpdateRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/mdm/v1")
public class TeamController {

	private static final Set<String> TEAM_SORTS = Set.of("teamCode", "teamName", "sortOrder", "startDate", "teamPk");

	private static final Set<String> MEMBER_SORTS = Set.of("startDate", "teamMemberPk");

	private final TeamService teams;

	public TeamController(TeamService teams) {
		this.teams = teams;
	}

	@GetMapping("/teams")
	@RequirePermission(menu = "TEAM", action = "READ")
	public ApiResponse<PageResponse<TeamResponse>> list(
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) String teamTypeCode,
			@RequestParam(required = false) Boolean active,
			@RequestParam(required = false) String useYn,
			@RequestParam(required = false) Integer page,
			@RequestParam(required = false) Integer size,
			@RequestParam(required = false) List<String> sort) {
		return ApiResponse.ok(teams.list(keyword, teamTypeCode, active, useYn,
				PageRequests.parse(page, size, sort, TEAM_SORTS, List.of(Sort.Order.asc("teamCode")), "teamPk")));
	}

	@GetMapping("/teams/{teamPk:[0-9]+}")
	@RequirePermission(menu = "TEAM", action = "READ")
	public ApiResponse<TeamResponse> get(@PathVariable long teamPk) {
		return ApiResponse.ok(teams.get(teamPk));
	}

	@PostMapping("/teams")
	@RequirePermission(menu = "TEAM", action = "CREATE")
	public ApiResponse<TeamResponse> create(@Valid @RequestBody TeamRequest request) {
		return ApiResponse.ok(teams.create(request));
	}

	@PutMapping("/teams/{teamPk:[0-9]+}")
	@RequirePermission(menu = "TEAM", action = "UPDATE")
	public ApiResponse<TeamResponse> update(@PathVariable long teamPk, @Valid @RequestBody TeamUpdateRequest request) {
		return ApiResponse.ok(teams.update(teamPk, request));
	}

	@DeleteMapping("/teams/{teamPk:[0-9]+}")
	@RequirePermission(menu = "TEAM", action = "DELETE")
	public ApiResponse<Void> delete(@PathVariable long teamPk) {
		teams.delete(teamPk);
		return ApiResponse.ok(null);
	}

	@PostMapping("/teams/{teamPk:[0-9]+}/restore")
	@RequirePermission(menu = "TEAM", action = "UPDATE")
	public ApiResponse<TeamResponse> restore(@PathVariable long teamPk) {
		return ApiResponse.ok(teams.restore(teamPk));
	}

	@GetMapping("/teams/{teamPk:[0-9]+}/members")
	@RequirePermission(menu = "TEAM", action = "READ")
	public ApiResponse<PageResponse<MemberResponse>> members(
			@PathVariable long teamPk,
			@RequestParam(required = false) String useYn,
			@RequestParam(required = false) Boolean current,
			@RequestParam(required = false) Integer page,
			@RequestParam(required = false) Integer size,
			@RequestParam(required = false) List<String> sort) {
		return ApiResponse.ok(teams.members(teamPk, useYn, current,
				PageRequests.parse(page, size, sort, MEMBER_SORTS, List.of(Sort.Order.desc("startDate")), "teamMemberPk")));
	}

	@PostMapping("/teams/{teamPk:[0-9]+}/members")
	@RequirePermission(menu = "TEAM", action = "CREATE")
	public ApiResponse<MemberResponse> addMember(@PathVariable long teamPk, @Valid @RequestBody MemberRequest request) {
		return ApiResponse.ok(teams.addMember(teamPk, request));
	}

	@PutMapping("/team-members/{teamMemberPk:[0-9]+}")
	@RequirePermission(menu = "TEAM", action = "UPDATE")
	public ApiResponse<MemberResponse> updateMember(@PathVariable long teamMemberPk, @Valid @RequestBody MemberUpdateRequest request) {
		return ApiResponse.ok(teams.updateMember(teamMemberPk, request));
	}

	@PostMapping("/team-members/{teamMemberPk:[0-9]+}/end")
	@RequirePermission(menu = "TEAM", action = "UPDATE")
	public ApiResponse<MemberResponse> endMember(@PathVariable long teamMemberPk, @RequestBody(required = false) MemberEndRequest request) {
		return ApiResponse.ok(teams.endMember(teamMemberPk, request));
	}

	@DeleteMapping("/team-members/{teamMemberPk:[0-9]+}")
	@RequirePermission(menu = "TEAM", action = "DELETE")
	public ApiResponse<Void> deleteMember(@PathVariable long teamMemberPk) {
		teams.deleteMember(teamMemberPk);
		return ApiResponse.ok(null);
	}

	@PostMapping("/team-members/{teamMemberPk:[0-9]+}/restore")
	@RequirePermission(menu = "TEAM", action = "UPDATE")
	public ApiResponse<MemberResponse> restoreMember(@PathVariable long teamMemberPk) {
		return ApiResponse.ok(teams.restoreMember(teamMemberPk));
	}

}
