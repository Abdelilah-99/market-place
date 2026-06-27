package com.buy01.users.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

import com.buy01.users.DTOs.LoginReqDTOs;
import com.buy01.users.DTOs.LoginResDTOs;
import com.buy01.users.DTOs.RegisterReqDTOs;
import com.buy01.users.DTOs.RegisterResDTOs;
import com.buy01.users.Service.AuthService;

@RestController
@RequestMapping({ "/api/users", "/auth" })
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterReqDTOs req) {
        return ResponseEntity.ok(authService.register(req));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResDTOs> login(@Valid @RequestBody LoginReqDTOs req) {
        return ResponseEntity.ok(authService.login(req));
    }
}
