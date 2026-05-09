package com.example.demo.controller;

import com.example.demo.controller.dto.ChangeRoleRequest;
import com.example.demo.controller.dto.CreateAdminRequest;
import com.example.demo.controller.dto.UserResponse;
import com.example.demo.exception.DuplicateResourceException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock    UserService    userService;
    @InjectMocks UserController userController;

    private UserResponse aliceResponse;

    @BeforeEach
    void setUp() {
        aliceResponse = new UserResponse(1L, "alice", "alice@example.com", "USER", LocalDateTime.now(), null);
    }

    // ── findAll ───────────────────────────────────────────────────────────────

    @Test
    void findAll_returns200WithList() {
        when(userService.findAll()).thenReturn(List.of(aliceResponse));

        ResponseEntity<List<UserResponse>> resp = userController.findAll();

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).hasSize(1);
        assertThat(resp.getBody().get(0).username()).isEqualTo("alice");
        verify(userService).findAll();
    }

    @Test
    void findAll_emptyList_returns200WithEmptyBody() {
        when(userService.findAll()).thenReturn(List.of());

        ResponseEntity<List<UserResponse>> resp = userController.findAll();

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isEmpty();
    }

    // ── findById ──────────────────────────────────────────────────────────────

    @Test
    void findById_returns200WithBody() {
        when(userService.findById(1L)).thenReturn(aliceResponse);

        ResponseEntity<UserResponse> resp = userController.findById(1L);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().id()).isEqualTo(1L);
        verify(userService).findById(1L);
    }

    @Test
    void findById_delegatesNotFoundFromService() {
        when(userService.findById(99L)).thenThrow(new ResourceNotFoundException("User", "id", 99L));

        assertThatThrownBy(() -> userController.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ── createAdmin ───────────────────────────────────────────────────────────

    @Test
    void createAdmin_returns201WithBody() {
        UserResponse adminResponse = new UserResponse(2L, "admin", "admin@example.com", "ADMIN", LocalDateTime.now(), null);
        CreateAdminRequest req = new CreateAdminRequest("admin", "admin@example.com", "Adm1n!", "secret");
        when(userService.createAdmin(req)).thenReturn(adminResponse);

        ResponseEntity<UserResponse> resp = userController.createAdmin(req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().role()).isEqualTo("ADMIN");
        verify(userService).createAdmin(req);
    }

    @Test
    void createAdmin_delegatesDuplicateFromService() {
        CreateAdminRequest req = new CreateAdminRequest("admin", "admin@example.com", "Adm1n!", "secret");
        when(userService.createAdmin(req)).thenThrow(new DuplicateResourceException("username", "admin"));

        assertThatThrownBy(() -> userController.createAdmin(req))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("admin");
    }

    // ── deleteUser ────────────────────────────────────────────────────────────

    @Test
    void deleteUser_returns204NoBody() {
        doNothing().when(userService).deleteUser(1L);

        ResponseEntity<Void> resp = userController.deleteUser(1L);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(resp.getBody()).isNull();
        verify(userService).deleteUser(1L);
    }

    @Test
    void deleteUser_delegatesNotFoundFromService() {
        doThrow(new ResourceNotFoundException("User", "id", 1L)).when(userService).deleteUser(1L);

        assertThatThrownBy(() -> userController.deleteUser(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── changeRole ────────────────────────────────────────────────────────────

    @Test
    void changeRole_returns200WithUpdatedRole() {
        UserResponse adminResponse = new UserResponse(1L, "alice", "alice@example.com", "ADMIN", LocalDateTime.now(), null);
        ChangeRoleRequest req = new ChangeRoleRequest("ADMIN");
        when(userService.changeRole(1L, req)).thenReturn(adminResponse);

        ResponseEntity<UserResponse> resp = userController.changeRole(1L, req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().role()).isEqualTo("ADMIN");
        verify(userService).changeRole(1L, req);
    }

    @Test
    void changeRole_delegatesIllegalArgumentFromService() {
        ChangeRoleRequest req = new ChangeRoleRequest("SUPERUSER");
        when(userService.changeRole(1L, req)).thenThrow(new IllegalArgumentException("Invalid role: 'SUPERUSER'"));

        assertThatThrownBy(() -> userController.changeRole(1L, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SUPERUSER");
    }
}
