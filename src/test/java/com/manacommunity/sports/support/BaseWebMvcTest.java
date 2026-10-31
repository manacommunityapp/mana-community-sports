package com.manacommunity.sports.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Base class for all @WebMvcTest controller tests.
 *
 * Provides:
 *  - MockMvc for HTTP request simulation
 *  - ObjectMapper for JSON serialization helpers
 *  - "test" profile → H2 datasource, mock-auth-enabled=false
 *  - @WithMockUserPrincipal annotation to inject a real UserPrincipal
 *
 * Security setup:
 *  The main SecurityConfig and MockJwtFilter are still loaded.
 *  With mock-auth-enabled=false, MockJwtFilter does NOT auto-authenticate.
 *  Annotate test methods with @WithMockUserPrincipal to set auth explicitly.
 */
@ActiveProfiles("test")
@org.springframework.context.annotation.Import({
        com.manacommunity.sports.config.SecurityConfig.class,
        com.manacommunity.common.exception.GlobalExceptionHandler.class
})
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
@org.springframework.test.context.NestedTestConfiguration(org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration.INHERIT)
public abstract class BaseWebMvcTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired(required = false)
    protected ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    protected com.manacommunity.common.user.repository.AppUserRepository userRepository;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    protected com.manacommunity.common.security.JwtAuthenticationFilter jwtAuthenticationFilter;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    protected com.manacommunity.common.security.RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    protected com.manacommunity.common.security.RestAccessDeniedHandler restAccessDeniedHandler;

    @org.junit.jupiter.api.BeforeEach
    void setupFilterPassThrough() throws Exception {
        org.mockito.Mockito.doAnswer(invocation -> {
            jakarta.servlet.ServletRequest req = invocation.getArgument(0);
            jakarta.servlet.ServletResponse res = invocation.getArgument(1);
            jakarta.servlet.FilterChain chain = invocation.getArgument(2);
            chain.doFilter(req, res);
            return null;
        }).when(jwtAuthenticationFilter).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());

        org.mockito.Mockito.doAnswer(invocation -> {
            jakarta.servlet.http.HttpServletResponse res = invocation.getArgument(1);
            res.sendError(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
            return null;
        }).when(restAuthenticationEntryPoint).commence(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());

        org.mockito.Mockito.doAnswer(invocation -> {
            jakarta.servlet.http.HttpServletResponse res = invocation.getArgument(1);
            res.sendError(jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN, "Forbidden");
            return null;
        }).when(restAccessDeniedHandler).handle(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    protected String toJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }
}
