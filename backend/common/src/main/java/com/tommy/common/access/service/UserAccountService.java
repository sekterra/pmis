package com.tommy.common.access.service;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tommy.common.access.dto.IssuedCredential;
import com.tommy.common.access.dto.RoleGrantRequest;
import com.tommy.common.access.dto.UserCreateRequest;
import com.tommy.common.access.dto.UserResponse;
import com.tommy.common.access.dto.UserRoleItem;
import com.tommy.common.access.dto.UserStatusRequest;
import com.tommy.common.access.dto.UserUpdateRequest;
import com.tommy.common.access.entity.Role;
import com.tommy.common.access.entity.UserAccount;
import com.tommy.common.access.entity.UserPasswordHistory;
import com.tommy.common.access.entity.UserRole;
import com.tommy.common.access.repository.RoleRepository;
import com.tommy.common.access.repository.UserAccountRepository;
import com.tommy.common.access.repository.UserPasswordHistoryRepository;
import com.tommy.common.access.repository.UserRoleRepository;
import com.tommy.common.global.query.LikeEscape;
import com.tommy.common.global.response.PageResponse;
import com.tommy.common.global.security.CurrentTenantUser;
import com.tommy.common.global.time.TenantClock;
import com.tommy.common.i18n.entity.SupportLanguage;
import com.tommy.common.i18n.repository.SupportLanguageRepository;
import com.tommy.common.org.entity.Person;
import com.tommy.common.org.repository.PersonRepository;
import com.tommy.common.org.service.OrgCodes;
import com.tommy.common.org.support.OrgExceptions;
import com.tommy.common.tenant.provision.TemporaryPassword;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

@Service
public class UserAccountService {

	private static final Logger log = LoggerFactory.getLogger(UserAccountService.class);

	private static final Pattern LOGIN = Pattern.compile("^[a-z0-9._@-]{3,100}$");

	private static final Set<String> STATUSES = Set.of("ACTIVE", "LOCKED", "DORMANT");

	private final UserAccountRepository users;

	private final UserRoleRepository links;

	private final UserPasswordHistoryRepository histories;

	private final RoleRepository roles;

	private final PersonRepository people;

	private final SupportLanguageRepository languages;

	private final OrgCodes codes;

	private final AccessSessions sessions;

	private final CurrentTenantUser currentUser;

	private final TenantClock clock;

	public UserAccountService(UserAccountRepository users, UserRoleRepository links, UserPasswordHistoryRepository histories,
			RoleRepository roles, PersonRepository people, SupportLanguageRepository languages, OrgCodes codes,
			AccessSessions sessions, CurrentTenantUser currentUser, TenantClock clock) {
		this.users = users;
		this.links = links;
		this.histories = histories;
		this.roles = roles;
		this.people = people;
		this.languages = languages;
		this.codes = codes;
		this.sessions = sessions;
		this.currentUser = currentUser;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public PageResponse<UserResponse> list(String keyword, String userStatusCode, String accountTypeCode, String roleCode,
			String useYn, Pageable pageable) {
		Page<UserAccount> page = users.findAll(filter(keyword, userStatusCode, accountTypeCode, roleCode, useYn), pageable);
		Map<Long, List<String>> roleCodes = roleCodes(page.getContent());
		List<UserResponse> items = page.getContent().stream()
				.map(user -> response(user, false, roleCodes.getOrDefault(user.getUserPk(), List.of()), null))
				.toList();
		return new PageResponse<>(items, page.getTotalElements(), pageable.getPageNumber() + 1, pageable.getPageSize());
	}

	@Transactional(readOnly = true)
	public UserResponse get(long userPk) {
		return response(require(userPk), true, null, roleItems(userPk));
	}

	@Transactional
	public IssuedCredential create(UserCreateRequest request) {
		String loginId = loginId(request.loginId());
		if (users.existsByLoginId(loginId)) {
			throw OrgExceptions.of(409, "USER.DUPLICATE_LOGIN_ID", "이미 있는 로그인 아이디입니다.");
		}
		String accountType = request.accountTypeCode() == null || request.accountTypeCode().isBlank() ? "NORMAL"
				: request.accountTypeCode();
		codes.require("ACCOUNT_TYPE", accountType, "accountTypeCode");
		Person person = person(request.personPk(), null);
		language(request.supportLanguageCode());
		timezone(request.timezoneId());
		TemporaryPassword password = sessions.issue();
		LocalDateTime expire = sessions.expireAt();
		Long actor = currentUser.pkOrNull();
		UserAccount user = UserAccount.create(loginId, accountType, sessions.hash(password.reveal()), expire, actor);
		user.changeProfile(person, OrgExceptions.trimToNull(request.supportLanguageCode()),
				OrgExceptions.trimToNull(request.timezoneId()), OrgExceptions.trimToNull(request.description()), actor);
		users.save(user);
		histories.save(UserPasswordHistory.record(user, user.getPasswordHash(), "INITIAL", actor));
		replaceRoles(user, grants(request.roleCodes()), false);
		return new IssuedCredential(user.getUserPk(), user.getLoginId(), password.reveal(), expire, "Y");
	}

	@Transactional
	public UserResponse update(long userPk, UserUpdateRequest request) {
		UserAccount user = requireActive(userPk);
		Person person = person(request.personPk(), userPk);
		language(request.supportLanguageCode());
		timezone(request.timezoneId());
		user.changeProfile(person, OrgExceptions.trimToNull(request.supportLanguageCode()),
				OrgExceptions.trimToNull(request.timezoneId()), OrgExceptions.trimToNull(request.description()),
				currentUser.pkOrNull());
		return get(userPk);
	}

	@Transactional
	public UserResponse changeStatus(long userPk, UserStatusRequest request) {
		UserAccount user = require(userPk);
		if (!user.inUse() || "WITHDRAWN".equals(user.getUserStatusCode())) {
			throw OrgExceptions.of(409, "USER.INACTIVE", "사용 중이 아닌 계정입니다.");
		}
		String next = request.userStatusCode();
		if (!STATUSES.contains(next)) {
			throw OrgExceptions.of(409, "USER.INVALID_STATUS_TRANSITION", "바꿀 수 없는 상태입니다.");
		}
		if (next.equals(user.getUserStatusCode())) {
			throw OrgExceptions.of(409, "USER.INVALID_STATUS_TRANSITION", "같은 상태로는 바꿀 수 없습니다.");
		}
		if (("LOCKED".equals(next) || "DORMANT".equals(next)) && self(userPk)) {
			throw OrgExceptions.of(409, "USER.SELF_OPERATION_FORBIDDEN", "본인 계정은 이렇게 바꿀 수 없습니다.");
		}
		if (("LOCKED".equals(next) || "DORMANT".equals(next)) && lastAdmin(user)) {
			throw OrgExceptions.of(409, "USER.LAST_ADMIN", "마지막 관리자 계정입니다.");
		}
		int fails = "ACTIVE".equals(next) ? 0 : user.getLoginFailCount();
		LocalDateTime locked = "ACTIVE".equals(next) || "LOCKED".equals(next) ? null : user.getLockedDatetime();
		user.changeStatus(next, fails, locked, currentUser.pkOrNull());
		if ("LOCKED".equals(next) || "DORMANT".equals(next)) {
			sessions.revokeRefresh(userPk);
		}
		sessions.evictUser(userPk);
		return get(userPk);
	}

	@Transactional
	public IssuedCredential resetPassword(long userPk) {
		if (self(userPk)) {
			throw OrgExceptions.of(409, "USER.SELF_OPERATION_FORBIDDEN", "본인 비밀번호는 이 API로 초기화할 수 없습니다.");
		}
		UserAccount user = requireActive(userPk);
		if (user.getPasswordHash() == null) {
			throw OrgExceptions.of(409, "USER.NO_PASSWORD_ACCOUNT", "비밀번호가 없는 계정입니다.");
		}
		TemporaryPassword password = sessions.issue();
		LocalDateTime expire = sessions.expireAt();
		Long actor = currentUser.pkOrNull();
		user.resetPassword(sessions.hash(password.reveal()), expire, true, actor);
		histories.save(UserPasswordHistory.record(user, user.getPasswordHash(), "RESET", actor));
		sessions.revokeRefresh(userPk);
		sessions.evictUser(userPk);
		return new IssuedCredential(user.getUserPk(), user.getLoginId(), password.reveal(), expire, "Y");
	}

	@Transactional
	public void delete(long userPk) {
		UserAccount user = require(userPk);
		if (self(userPk)) {
			throw OrgExceptions.of(409, "USER.SELF_OPERATION_FORBIDDEN", "본인 계정은 삭제할 수 없습니다.");
		}
		if (lastAdmin(user)) {
			throw OrgExceptions.of(409, "USER.LAST_ADMIN", "마지막 관리자 계정입니다.");
		}
		user.withdraw(currentUser.pkOrNull());
		sessions.revokeRefresh(userPk);
		sessions.evictUser(userPk);
	}

	@Transactional
	public UserResponse restore(long userPk) {
		UserAccount user = require(userPk);
		if (user.getPerson() != null && users.existsByPerson_PersonPkAndUseYnAndUserPkNot(user.getPerson().getPersonPk(), "Y", userPk)) {
			throw OrgExceptions.of(409, "USER.PERSON_ALREADY_LINKED", "이미 계정이 연결된 사람입니다.");
		}
		user.restore(currentUser.pkOrNull());
		sessions.evictUser(userPk);
		return get(userPk);
	}

	@Transactional(readOnly = true)
	public List<UserRoleItem> roles(long userPk, String useYn) {
		require(userPk);
		return roleItems(userPk).stream().filter(item -> useYn == null || useYn.isBlank() || useYn.equals(item.useYn())).toList();
	}

	@Transactional
	public List<UserRoleItem> replaceRoles(long userPk, List<RoleGrantRequest> grants) {
		UserAccount user = requireActive(userPk);
		if (self(userPk)) {
			throw OrgExceptions.of(409, "USER.SELF_OPERATION_FORBIDDEN", "본인 역할은 바꿀 수 없습니다.");
		}
		replaceRoles(user, grants == null ? List.of() : grants, true);
		sessions.evictUser(userPk);
		return roleItems(userPk);
	}

	private void replaceRoles(UserAccount user, List<RoleGrantRequest> grants, boolean protectLastAdmin) {
		Set<String> seen = new HashSet<>();
		Map<String, RoleGrantRequest> requested = new HashMap<>();
		for (RoleGrantRequest grant : grants) {
			if (!seen.add(grant.roleCode())) {
				throw OrgExceptions.field("COMMON.VALIDATION_FAILED", "같은 역할이 두 번 있습니다.", "roleCodes");
			}
			OrgExceptions.requireDateOrder(grant.validFromDate(), grant.validToDate(), "ROLE.INVALID_DATE", "시작일은 종료일보다 늦을 수 없습니다.");
			requested.put(grant.roleCode(), grant);
		}
		if (protectLastAdmin && lastAdmin(user) && !requested.containsKey("TENANT_ADMIN")) {
			throw OrgExceptions.of(409, "USER.LAST_ADMIN", "마지막 관리자 계정입니다.");
		}
		Long actor = currentUser.pkOrNull();
		List<UserRole> existing = links.findByUserAccount_UserPk(user.getUserPk());
		Map<String, UserRole> byCode = new HashMap<>();
		for (UserRole link : existing) {
			byCode.put(link.getRole().getRoleCode(), link);
		}
		for (RoleGrantRequest grant : requested.values()) {
			Role role = roles.findByRoleCode(grant.roleCode())
					.orElseThrow(() -> OrgExceptions.of(404, "ROLE.NOT_FOUND", "역할을 찾을 수 없습니다."));
			if (!role.inUse()) {
				throw OrgExceptions.field("ROLE.INACTIVE", "사용 중이 아닌 역할입니다.", "roleCode");
			}
			UserRole link = byCode.get(role.getRoleCode());
			String kind = link == null ? "grant" : ("Y".equals(link.getUseYn()) ? "period" : "grant");
			if (link == null) {
				links.save(UserRole.grant(user, role, grant.validFromDate(), grant.validToDate(), actor));
			}
			else {
				link.apply(grant.validFromDate(), grant.validToDate(), actor);
			}
			log.info("user role changed userPk={} roleCode={} change={}", user.getUserPk(), role.getRoleCode(), kind);
		}
		for (UserRole link : existing) {
			if ("Y".equals(link.getUseYn()) && !requested.containsKey(link.getRole().getRoleCode())) {
				link.revoke(actor);
				log.info("user role changed userPk={} roleCode={} change=revoke", user.getUserPk(), link.getRole().getRoleCode());
			}
		}
	}

	private List<RoleGrantRequest> grants(List<String> roleCodes) {
		if (roleCodes == null) {
			return List.of();
		}
		return roleCodes.stream().map(code -> new RoleGrantRequest(code, null, null)).toList();
	}

	private Person person(Long personPk, Long exceptUserPk) {
		if (personPk == null) {
			return null;
		}
		Person person = people.findById(personPk)
				.orElseThrow(() -> OrgExceptions.of(404, "PERSON.NOT_FOUND", "사람을 찾을 수 없습니다."));
		if (!person.inUse()) {
			throw OrgExceptions.field("COMMON.VALIDATION_FAILED", "사용 중인 사람이 아닙니다.", "personPk");
		}
		boolean linked = exceptUserPk == null ? users.existsByPerson_PersonPkAndUseYn(personPk, "Y")
				: users.existsByPerson_PersonPkAndUseYnAndUserPkNot(personPk, "Y", exceptUserPk);
		if (linked) {
			throw OrgExceptions.of(409, "USER.PERSON_ALREADY_LINKED", "이미 계정이 연결된 사람입니다.");
		}
		return person;
	}

	private void language(String code) {
		String value = OrgExceptions.trimToNull(code);
		if (value == null) {
			return;
		}
		languages.findById(value).filter(SupportLanguage::inUse)
				.orElseThrow(() -> OrgExceptions.field("I18N.LANGUAGE_NOT_FOUND", "언어를 찾을 수 없습니다.", "supportLanguageCode"));
	}

	private static void timezone(String timezoneId) {
		String value = OrgExceptions.trimToNull(timezoneId);
		if (value == null) {
			return;
		}
		try {
			ZoneId.of(value);
		}
		catch (DateTimeException ex) {
			throw OrgExceptions.field("COMMON.VALIDATION_FAILED", "시간대가 올바르지 않습니다.", "timezoneId");
		}
	}

	private static String loginId(String raw) {
		String value = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
		if (!LOGIN.matcher(value).matches()) {
			throw OrgExceptions.field("COMMON.VALIDATION_FAILED", "로그인 아이디 형식이 올바르지 않습니다.", "loginId");
		}
		return value;
	}

	private boolean self(long userPk) {
		Long actor = currentUser.pkOrNull();
		return actor != null && actor == userPk;
	}

	private boolean lastAdmin(UserAccount user) {
		if (!user.inUse() || !"ACTIVE".equals(user.getUserStatusCode())) {
			return false;
		}
		boolean admin = links.findByUserAccount_UserPk(user.getUserPk()).stream()
				.anyMatch(link -> "TENANT_ADMIN".equals(link.getRole().getRoleCode()) && link.current(clock.today()));
		return admin && users.countOtherActiveAdmins(user.getUserPk(), clock.today()) == 0;
	}

	private UserAccount require(long userPk) {
		return users.findById(userPk).orElseThrow(() -> OrgExceptions.of(404, "USER.NOT_FOUND", "사용자를 찾을 수 없습니다."));
	}

	private UserAccount requireActive(long userPk) {
		UserAccount user = require(userPk);
		if (!user.inUse() || "WITHDRAWN".equals(user.getUserStatusCode())) {
			throw OrgExceptions.of(409, "USER.INACTIVE", "사용 중이 아닌 계정입니다.");
		}
		return user;
	}

	private Map<Long, List<String>> roleCodes(List<UserAccount> page) {
		if (page.isEmpty()) {
			return Map.of();
		}
		Map<Long, List<String>> grouped = new HashMap<>();
		for (Object[] row : users.findCurrentRoleCodes(page.stream().map(UserAccount::getUserPk).toList(), clock.today())) {
			grouped.computeIfAbsent((Long) row[0], key -> new ArrayList<>()).add((String) row[1]);
		}
		return grouped;
	}

	private List<UserRoleItem> roleItems(long userPk) {
		LocalDate today = clock.today();
		return links.findByUserAccount_UserPk(userPk).stream()
				.map(link -> new UserRoleItem(link.getUserRolePk(), link.getRole().getRoleCode(), link.getRole().getRoleName(),
						link.getValidFromDate(), link.getValidToDate(), link.current(today), link.getUseYn()))
				.toList();
	}

	static String lockType(UserAccount user) {
		if (!"LOCKED".equals(user.getUserStatusCode())) {
			return null;
		}
		return user.getLockedDatetime() == null ? "ADMIN" : "AUTO";
	}

	private UserResponse response(UserAccount user, boolean detail, List<String> roleCodes, List<UserRoleItem> roles) {
		Person person = user.getPerson();
		List<String> codes = roleCodes;
		if (codes == null && roles != null) {
			codes = roles.stream().filter(UserRoleItem::currentlyValid).map(UserRoleItem::roleCode).toList();
		}
		return new UserResponse(user.getUserPk(), user.getLoginId(), user.getAccountTypeCode(), user.getUserStatusCode(),
				person == null ? null : person.getPersonPk(), person == null ? null : person.getPersonName(),
				user.getLastLoginDatetime(), user.getMustChangePasswordYn(), codes == null ? List.of() : codes, user.getUseYn(),
				detail ? user.getSupportLanguageCode() : null, detail ? user.getTimezoneId() : null,
				detail ? user.getDescription() : null, detail ? user.getLoginFailCount() : null,
				detail ? user.getLockedDatetime() : null, detail ? lockType(user) : null,
				detail ? user.getPasswordChangedDatetime() : null,
				detail ? user.getPasswordExpireDatetime() : null, user.getCreateDatetime(), user.getCreateUserPk(),
				user.getUpdateDatetime(), user.getUpdateUserPk(), detail ? roles : null);
	}

	private Specification<UserAccount> filter(String keyword, String userStatusCode, String accountTypeCode, String roleCode,
			String useYn) {
		return (root, query, builder) -> {
			List<Predicate> predicates = new ArrayList<>();
			String text = OrgExceptions.trimToNull(keyword);
			if (text != null) {
				var person = root.join("person", JoinType.LEFT);
				predicates.add(builder.or(LikeEscape.contains(builder, root.get("loginId"), text),
						LikeEscape.contains(builder, person.get("personName"), text)));
			}
			if (present(userStatusCode)) {
				predicates.add(builder.equal(root.get("userStatusCode"), userStatusCode));
			}
			if (present(accountTypeCode)) {
				predicates.add(builder.equal(root.get("accountTypeCode"), accountTypeCode));
			}
			if (present(useYn)) {
				predicates.add(builder.equal(root.get("useYn"), useYn));
			}
			if (present(roleCode)) {
				Subquery<Integer> role = query.subquery(Integer.class);
				Root<UserRole> link = role.from(UserRole.class);
				LocalDate today = clock.today();
				role.select(builder.literal(1)).where(
						builder.equal(link.get("userAccount"), root),
						builder.equal(link.get("useYn"), "Y"),
						builder.equal(link.get("role").get("roleCode"), roleCode),
						builder.equal(link.get("role").get("useYn"), "Y"),
						builder.or(builder.isNull(link.get("validFromDate")), builder.lessThanOrEqualTo(link.get("validFromDate"), today)),
						builder.or(builder.isNull(link.get("validToDate")), builder.greaterThanOrEqualTo(link.get("validToDate"), today)));
				predicates.add(builder.exists(role));
			}
			return predicates.isEmpty() ? builder.conjunction() : builder.and(predicates.toArray(Predicate[]::new));
		};
	}

	private static boolean present(String value) {
		return value != null && !value.isBlank();
	}

}
