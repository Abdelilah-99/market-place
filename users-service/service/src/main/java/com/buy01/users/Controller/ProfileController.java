package com.buy01.users.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;

import com.buy01.users.DTOs.PageResponseDTOs;
import com.buy01.users.DTOs.ProfileResDTOs;
import com.buy01.users.DTOs.ProfileUpdateReqDTOs;
import com.buy01.users.DTOs.PublicProfileResDTO;
import com.buy01.users.DTOs.RegisterResDTOs;
import com.buy01.users.Service.ProfileService;

@RestController
@RequestMapping("/api/users")
public class ProfileController {
    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/me")
    public ResponseEntity<ProfileResDTOs> getMe() {
        return ResponseEntity.ok(profileService.getCurrentProfile());
    }

    @GetMapping("/public/{userId}")
    public ResponseEntity<PublicProfileResDTO> getPublicProfile(@PathVariable String userId) {
        return ResponseEntity.ok(profileService.getPublicProfile(userId));
    }

    @GetMapping("/search")
    public ResponseEntity<PageResponseDTOs<ProfileResDTOs>> searchUsers(
            @RequestParam(name = "q", defaultValue = "") String query,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "8") int size) {
        return ResponseEntity.ok(profileService.searchUsers(query, page, size));
    }

    @PutMapping("/me")
    public ResponseEntity<ProfileResDTOs> updateMe(@RequestBody ProfileUpdateReqDTOs req) {
        return ResponseEntity.ok(profileService.updateCurrentProfile(req));
    }

    @DeleteMapping("/me")
    public ResponseEntity<RegisterResDTOs> deleteMe() {
        return ResponseEntity.ok(profileService.deleteCurrentUser());
    }
}
