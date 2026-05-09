package com.example.demo.controller;

import com.example.demo.MockJwtFactory;
import com.example.demo.security.JwtAuthFilter;
import com.example.demo.security.JwtService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock JwtService jwtService;
    @Mock UserDetailsService userDetailsService;
    @Mock FilterChain filterChain;

    @InjectMocks JwtAuthFilter filter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request  = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validToken_setsAuthenticationInSecurityContext() throws Exception {
        String token = "valid.jwt.token";
        request.addHeader("Authorization", "Bearer " + token);
        when(jwtService.extractUsername(token)).thenReturn("alice");
        when(jwtService.isValid(token)).thenReturn(true);
        when(userDetailsService.loadUserByUsername("alice"))
                .thenReturn(User.withUsername("alice").password("").roles("USER").build());

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("alice");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void noAuthorizationHeader_chainContinuesWithoutAuthentication() throws Exception {
        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }

    @Test
    void bearerPrefixMissing_treatedAsNoToken() throws Exception {
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtService);
    }

    @Test
    void expiredToken_chainContinuesWithoutAuthentication() throws Exception {
        String token = "expired.token";
        request.addHeader("Authorization", "Bearer " + token);
        when(jwtService.extractUsername(token)).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void invalidSignature_chainContinuesWithoutAuthentication() throws Exception {
        String token = MockJwtFactory.tokenSignedWithWrongSecret();
        request.addHeader("Authorization", "Bearer " + token);
        when(jwtService.extractUsername(token)).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void userDetailsServiceThrows_chainContinuesWithoutAuthentication() throws Exception {
        String token = "some.token";
        request.addHeader("Authorization", "Bearer " + token);
        when(jwtService.extractUsername(token)).thenReturn("alice");
        when(userDetailsService.loadUserByUsername("alice"))
                .thenThrow(new org.springframework.security.core.userdetails.UsernameNotFoundException("not found"));

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void tokenNotValid_doesNotSetAuthentication() throws Exception {
        String token = "parseable.but.invalid";
        request.addHeader("Authorization", "Bearer " + token);
        when(jwtService.extractUsername(token)).thenReturn("alice");
        when(jwtService.isValid(token)).thenReturn(false);
        when(userDetailsService.loadUserByUsername("alice"))
                .thenReturn(User.withUsername("alice").password("").roles("USER").build());

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
