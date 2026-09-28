package com.smartmall.tlai.web.access;

import com.smartmall.tlai.business.access.application.AccessControlService;
import com.smartmall.tlai.business.access.application.ChangeRoleEnabledCommand;
import com.smartmall.tlai.business.access.application.ChangeRolePermissionsCommand;
import com.smartmall.tlai.business.access.application.RoleCommand;
import com.smartmall.tlai.business.access.application.UpdateRoleCommand;
import com.smartmall.tlai.business.user.application.ChangeUserEnabledCommand;
import com.smartmall.tlai.business.user.application.ChangeUserRolesCommand;
import com.smartmall.tlai.business.user.application.CreateManagedUserCommand;
import com.smartmall.tlai.business.user.application.UserAccountService;
import com.smartmall.tlai.common.api.ApiResponse;
import com.smartmall.tlai.common.api.PageResponse;
import com.smartmall.tlai.security.AppUserPrincipal;
import com.smartmall.tlai.web.access.dto.AdminUserResponse;
import com.smartmall.tlai.web.access.dto.ChangeEnabledRequest;
import com.smartmall.tlai.web.access.dto.ChangeRolePermissionsRequest;
import com.smartmall.tlai.web.access.dto.ChangeUserRolesRequest;
import com.smartmall.tlai.web.access.dto.CreateAdminUserRequest;
import com.smartmall.tlai.web.access.dto.PermissionAuditResponse;
import com.smartmall.tlai.web.access.dto.PermissionResponse;
import com.smartmall.tlai.web.access.dto.RoleRequest;
import com.smartmall.tlai.web.access.dto.RoleResponse;
import com.smartmall.tlai.web.access.dto.UpdateRoleRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/admin")
public class AccessControlController {
    private final UserAccountService userService;
    private final AccessControlService accessService;

    public AccessControlController(UserAccountService userService, AccessControlService accessService) {
        this.userService = userService;
        this.accessService = accessService;
    }

    @GetMapping("/users")
    @PreAuthorize("hasAuthority('system:user_view')")
    public ApiResponse<PageResponse<AdminUserResponse>> listUsers(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String roleCode,
            @RequestParam(required = false) Boolean enabled) {
        var result = userService.list(page, size, keyword, roleCode, enabled);
        return ApiResponse.success(new PageResponse<>(result.items().stream()
                .map(AdminUserResponse::from).toList(), result.total(), result.page(), result.size()));
    }

    @PostMapping("/users")
    @PreAuthorize("hasAuthority('system:user_manage')")
    public ResponseEntity<ApiResponse<AdminUserResponse>> createUser(
            @Valid @RequestBody CreateAdminUserRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        var command = new CreateManagedUserCommand(request.username(), request.email(), request.password(),
                request.roleCodes(), principal.id(), principal.username());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(AdminUserResponse.from(userService.createManagedUser(command))));
    }

    @PutMapping("/users/{id}/roles")
    @PreAuthorize("hasAuthority('system:user_manage')")
    public ApiResponse<AdminUserResponse> changeUserRoles(
            @PathVariable @Min(1) long id, @Valid @RequestBody ChangeUserRolesRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        var command = new ChangeUserRolesCommand(request.roleCodes(), principal.id(), principal.username());
        return ApiResponse.success(AdminUserResponse.from(userService.changeRoles(id, command)));
    }

    @PatchMapping("/users/{id}/enabled")
    @PreAuthorize("hasAuthority('system:user_manage')")
    public ApiResponse<AdminUserResponse> changeUserEnabled(
            @PathVariable @Min(1) long id, @Valid @RequestBody ChangeEnabledRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        var command = new ChangeUserEnabledCommand(request.enabled(), principal.id(), principal.username());
        return ApiResponse.success(AdminUserResponse.from(userService.changeEnabled(id, command)));
    }

    @GetMapping("/roles")
    @PreAuthorize("hasAnyAuthority('system:user_view', 'system:rbac_manage')")
    public ApiResponse<List<RoleResponse>> listRoles() {
        return ApiResponse.success(accessService.listRoles().stream().map(RoleResponse::from).toList());
    }

    @PostMapping("/roles")
    @PreAuthorize("hasAuthority('system:rbac_manage')")
    public ResponseEntity<ApiResponse<RoleResponse>> createRole(
            @Valid @RequestBody RoleRequest request, @AuthenticationPrincipal AppUserPrincipal principal) {
        var command = new RoleCommand(request.code(), request.name(), request.description(),
                request.permissionCodes(), principal.id(), principal.username());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(RoleResponse.from(accessService.createRole(command))));
    }

    @PutMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('system:rbac_manage')")
    public ApiResponse<RoleResponse> updateRole(
            @PathVariable @Min(1) long id, @Valid @RequestBody UpdateRoleRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        var command = new UpdateRoleCommand(request.name(), request.description(),
                principal.id(), principal.username());
        return ApiResponse.success(RoleResponse.from(accessService.updateRole(id, command)));
    }

    @PutMapping("/roles/{id}/permissions")
    @PreAuthorize("hasAuthority('system:rbac_manage')")
    public ApiResponse<RoleResponse> changeRolePermissions(
            @PathVariable @Min(1) long id, @Valid @RequestBody ChangeRolePermissionsRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        var command = new ChangeRolePermissionsCommand(request.permissionCodes(),
                principal.id(), principal.username());
        return ApiResponse.success(RoleResponse.from(accessService.changePermissions(id, command)));
    }

    @PatchMapping("/roles/{id}/enabled")
    @PreAuthorize("hasAuthority('system:rbac_manage')")
    public ApiResponse<RoleResponse> changeRoleEnabled(
            @PathVariable @Min(1) long id, @Valid @RequestBody ChangeEnabledRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        var command = new ChangeRoleEnabledCommand(request.enabled(), principal.id(), principal.username());
        return ApiResponse.success(RoleResponse.from(accessService.changeEnabled(id, command)));
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('system:rbac_manage')")
    public ApiResponse<List<PermissionResponse>> listPermissions() {
        return ApiResponse.success(accessService.listPermissions().stream()
                .map(PermissionResponse::from).toList());
    }

    @GetMapping("/audits")
    @PreAuthorize("hasAuthority('system:rbac_manage')")
    public ApiResponse<PageResponse<PermissionAuditResponse>> listAudits(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) Long targetId) {
        var result = accessService.listAudits(page, size, targetType, targetId);
        return ApiResponse.success(new PageResponse<>(result.items().stream()
                .map(PermissionAuditResponse::from).toList(), result.total(), result.page(), result.size()));
    }
}
