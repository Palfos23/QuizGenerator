package com.quizapp.controller;

import com.quizapp.service.ImageStorageService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/** Admin-only (see SecurityConfig's /api/admin/** rule): upload a picture, get back its public URL. */
@RestController
@RequestMapping("/api/admin/images")
public class AdminImageController {

    private final ImageStorageService imageStorageService;

    public AdminImageController(ImageStorageService imageStorageService) {
        this.imageStorageService = imageStorageService;
    }

    @PostMapping
    public Map<String, String> upload(@RequestParam("file") MultipartFile file) throws IOException {
        return Map.of("url", imageStorageService.upload(file.getBytes()));
    }
}
