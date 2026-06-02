package com.buy01.users.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
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
        // if (avatar != null && avatar.getSize() < 1024 * 1024 * 1024 * 2048) {
        // return ResponseEntity.badRequest().body("size too large");
        // } else if (avatar != null && !avatar.getContentType().equals("image/png")) {
        // return ResponseEntity.badRequest().body("type must be image");
        // }
        System.out.println("test if the testing checl for changes");
        return ResponseEntity.ok(authService.register(req));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResDTOs> login(@RequestBody LoginReqDTOs req) {
        return ResponseEntity.ok(authService.login(req));
    }
    // @PostMapping("/login")
}
