package com.tommy.common.global.multitenancy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.tommy.common.global.exception.ApiException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

class MultitenancyInfrastructureTest {

	@AfterEach
	void clearContext() {
		TenantContext.clear();
	}

	@Test
	void contextIsEmptyAfterClear() {
		TenantContext.beginRequest();
		TenantContext.setSchema("t_acme");
		assertTrue(TenantContext.inRequest());
		TenantContext.clear();
		assertFalse(TenantContext.inRequest());
		assertNull(TenantContext.schema());
	}

	@Test
	void resolverUsesPublicSchemaOutsideRequestAndRejectsEmptyRequest() {
		RequestTenantIdentifierResolver resolver = new RequestTenantIdentifierResolver();
		assertEquals(SchemaNames.PUBLIC_SCHEMA, resolver.resolveCurrentTenantIdentifier());
		TenantContext.beginRequest();
		ApiException exception = assertThrows(ApiException.class, resolver::resolveCurrentTenantIdentifier);
		assertEquals("TENANT.UNRESOLVED", exception.errorCode());
	}

	@Test
	void identifierUsesHeaderOnlyWithoutJwtAndRequiresMatch() {
		assertEquals("jwt", TenantRequestIdentifier.identify(
				Optional.of("jwt"), Optional.of("jwt"), Optional.of("jwt")));
		assertEquals("jwt", TenantRequestIdentifier.identify(Optional.empty(), Optional.of("jwt"), Optional.empty()));
		assertEquals("header", TenantRequestIdentifier.identify(Optional.empty(), Optional.empty(), Optional.of("header")));
		assertEquals("host", TenantRequestIdentifier.identify(Optional.of("host"), Optional.empty(), Optional.empty()));
		ApiException headerMismatch = assertThrows(ApiException.class, () -> TenantRequestIdentifier.identify(
				Optional.of("other"), Optional.of("jwt"), Optional.of("acme")));
		assertEquals("TENANT.HOST_MISMATCH", headerMismatch.errorCode());
		ApiException mismatch = assertThrows(ApiException.class, () -> TenantRequestIdentifier.identify(
				Optional.of("host"), Optional.of("jwt"), Optional.empty()));
		assertEquals("TENANT.HOST_MISMATCH", mismatch.errorCode());
		ApiException unresolved = assertThrows(ApiException.class, () -> TenantRequestIdentifier.identify(
				Optional.empty(), Optional.empty(), Optional.empty()));
		assertEquals("TENANT.UNRESOLVED", unresolved.errorCode());
	}

	@Test
	void headerAndJwtResolvers() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getHeader(HeaderTenantResolver.HEADER)).thenReturn(" acme ");
		assertEquals(Optional.of("acme"), new HeaderTenantResolver().resolveTenantCode(request));
		when(request.getHeader(HeaderTenantResolver.HEADER)).thenReturn(" ");
		assertEquals(Optional.empty(), new HeaderTenantResolver().resolveTenantCode(request));
		assertEquals(Optional.empty(), new SecurityContextTenantClaimReader().readTenantCode(request));
		assertEquals(Optional.empty(), new JwtTenantResolver(new SecurityContextTenantClaimReader()).resolveTenantCode(request));
	}

	@Test
	void hostResolverReadsDirectory() {
		TenantDirectory directory = mock(TenantDirectory.class);
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getServerName()).thenReturn("acme.test");
		when(directory.findByDomain("acme.test"))
				.thenReturn(Optional.of(new TenantSnapshot("acme", "t_acme", "ACTIVE", true)));
		assertEquals(Optional.of("acme"), new HostTenantResolver(directory).resolveTenantCode(request));
	}

	@Test
	void accessRejectsMissingAndInactive() {
		ApiException missing = assertThrows(ApiException.class, () -> TenantAccess.requireActiveSchema(null));
		assertEquals("TENANT.NOT_FOUND", missing.errorCode());
		ApiException inactive = assertThrows(ApiException.class,
				() -> TenantAccess.requireActiveSchema(new TenantSnapshot("acme", "t_acme", "SUSPENDED", true)));
		assertEquals("TENANT.INACTIVE", inactive.errorCode());
		assertEquals("t_acme", TenantAccess.requireActiveSchema(new TenantSnapshot("acme", "t_acme", "ACTIVE", true)));
	}

	@Test
	void databaseNameMustBePmis() {
		DatabaseNameGuard.require("pmis");
		assertThrows(IllegalStateException.class, () -> DatabaseNameGuard.require("other"));
	}

	@Test
	void providerResetsSearchPathOnRelease() throws Exception {
		Connection connection = mock(Connection.class);
		Statement statement = mock(Statement.class);
		DataSource dataSource = mock(DataSource.class);
		when(dataSource.getConnection()).thenReturn(connection);
		when(connection.createStatement()).thenReturn(statement);
		SchemaMultiTenantConnectionProvider provider = new SchemaMultiTenantConnectionProvider(dataSource);
		provider.getConnection("t_it_abc");
		provider.releaseConnection("t_it_abc", connection);
		verify(statement).execute("SET search_path TO \"t_it_abc\"");
		verify(statement).execute("RESET search_path");
		verify(connection).close();
	}

	@Test
	void providerDiscardsConnectionWhenResetFails() throws Exception {
		Connection connection = mock(Connection.class);
		Statement statement = mock(Statement.class);
		DataSource dataSource = mock(DataSource.class);
		when(dataSource.getConnection()).thenReturn(connection);
		when(connection.createStatement()).thenReturn(statement);
		when(statement.execute(org.mockito.ArgumentMatchers.anyString())).thenAnswer(invocation -> {
			Object sql = invocation.getArgument(0);
			if (String.valueOf(sql).startsWith("RESET")) {
				throw new SQLException("reset");
			}
			return true;
		});
		SchemaMultiTenantConnectionProvider provider = new SchemaMultiTenantConnectionProvider(dataSource);
		provider.getConnection("t_it_abc");
		assertThrows(SQLException.class, () -> provider.releaseConnection("t_it_abc", connection));
		verify(connection, never()).close();
		verify(connection).abort(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void filterRejectsSuspendedTenantAndClearsContext() throws Exception {
		TenantDirectory directory = mock(TenantDirectory.class);
		when(directory.findByDomain("acme.test"))
				.thenReturn(Optional.of(new TenantSnapshot("acme", "t_acme", "ACTIVE", true)));
		when(directory.findByTenantCode("acme"))
				.thenReturn(Optional.of(new TenantSnapshot("acme", "t_acme", "SUSPENDED", true)));
		TenantResolutionFilter filter = new TenantResolutionFilter(
				new HostTenantResolver(directory),
				new JwtTenantResolver(new SecurityContextTenantClaimReader()),
				Optional.empty(),
				directory,
				new tools.jackson.databind.ObjectMapper());
		HttpServletRequest request = mock(HttpServletRequest.class);
		HttpServletResponse response = mock(HttpServletResponse.class);
		java.io.StringWriter body = new java.io.StringWriter();
		when(request.getServerName()).thenReturn("acme.test");
		when(request.getRequestURI()).thenReturn("/api/sample");
		when(response.getWriter()).thenReturn(new java.io.PrintWriter(body));
		FilterChain chain = mock(FilterChain.class);
		filter.doFilter(request, response, chain);
		verify(chain, never()).doFilter(request, response);
		verify(response).setStatus(403);
		assertTrue(body.toString().contains("TENANT.INACTIVE"));
		assertFalse(TenantContext.inRequest());
		assertNull(TenantContext.schema());
	}

}
