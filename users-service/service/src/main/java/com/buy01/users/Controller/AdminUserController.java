package com.buy01.users.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.buy01.users.DTOs.AdminRoleUpdateReqDTOs;
import com.buy01.users.DTOs.PageResponseDTOs;
import com.buy01.users.DTOs.ProfileResDTOs;
import com.buy01.users.DTOs.RegisterResDTOs;
import com.buy01.users.Service.AdminUserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {
    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public ResponseEntity<PageResponseDTOs<ProfileResDTOs>> getUsers(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "12") int size) {
        return ResponseEntity.ok(adminUserService.getUsers(page, size));
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<ProfileResDTOs> updateRole(
            @PathVariable("id") String id,
            @RequestBody @Valid AdminRoleUpdateReqDTOs req) {
        return ResponseEntity.ok(adminUserService.updateRole(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<RegisterResDTOs> deleteUser(@PathVariable("id") String id) {
        return ResponseEntity.ok(adminUserService.deleteUser(id));
    }
}
