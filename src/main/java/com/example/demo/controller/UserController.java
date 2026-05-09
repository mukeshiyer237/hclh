package com.example.demo.controller;

import com.example.demo.controller.dto.ChangeRoleRequest;
import com.example.demo.controller.dto.CreateAdminRequest;
import com.example.demo.controller.dto.UserResponse;
import com.example.demo.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Users", description = "User management — list, create admin, soft-delete, and role assignment")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(
        summary     = "List all active users",
        description = "Returns every user whose `deleted_at` is null. Requires ADMIN role.",
        security    = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Active users returned",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                array = @ArraySchema(schema = @Schema(implementation = UserResponse.class)))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",          content = @Content),
        @ApiResponse(responseCode = "403", description = "Caller does not have ADMIN role", content = @Content)
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> findAll() {
        return ResponseEntity.ok(userService.findAll());
    }

    @Operation(
        summary     = "Get user by ID",
        description = "ADMIN may fetch any user. A regular user may only fetch their own record.",
        security    = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",                    content = @Content),
        @ApiResponse(responseCode = "403", description = "Caller is not ADMIN and ID is not their own", content = @Content),
        @ApiResponse(responseCode = "404", description = "User not found or has been deleted",        content = @Content)
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public ResponseEntity<UserResponse> findById(
            @Parameter(description = "ID of the user to retrieve", required = true)
            @PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @Operation(
        summary     = "Bootstrap the first ADMIN user",
        description = "Public endpoint — no JWT required. Request must include the `bootstrapSecret` " +
                      "that matches the `ADMIN_BOOTSTRAP_SECRET` environment variable. " +
                      "Disable after first use by leaving the env var unset (defaults to `disabled`)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Admin user created",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(responseCode = "400", description = "Validation failure on request body", content = @Content),
        @ApiResponse(responseCode = "403", description = "Bootstrap secret is wrong or feature is disabled", content = @Content),
        @ApiResponse(responseCode = "409", description = "Username or e-mail already in use",  content = @Content)
    })
    @PostMapping("/admin")
    public ResponseEntity<UserResponse> createAdmin(@Valid @RequestBody CreateAdminRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createAdmin(request));
    }

    @Operation(
        summary     = "Soft-delete a user",
        description = "Sets `deleted_at` to now. The user record is retained for audit purposes. Requires ADMIN role.",
        security    = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "User deleted — no body returned",         content = @Content),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",                  content = @Content),
        @ApiResponse(responseCode = "403", description = "Caller does not have ADMIN role",         content = @Content),
        @ApiResponse(responseCode = "404", description = "User not found or already deleted",       content = @Content)
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "ID of the user to delete", required = true)
            @PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary     = "Change a user's role",
        description = "Updates the `role` field for the given user. Requires ADMIN role.",
        security    = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Role updated — updated user returned",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(responseCode = "400", description = "Validation failure on request body", content = @Content),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",             content = @Content),
        @ApiResponse(responseCode = "403", description = "Caller does not have ADMIN role",    content = @Content),
        @ApiResponse(responseCode = "404", description = "User not found or has been deleted", content = @Content)
    })
    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> changeRole(
            @Parameter(description = "ID of the user whose role should be changed", required = true)
            @PathVariable Long id,
            @Valid @RequestBody ChangeRoleRequest request) {
        return ResponseEntity.ok(userService.changeRole(id, request));
    }
}
