package com.quizapp.controller;

import com.quizapp.dto.FiveOhOneCategoryDto;
import com.quizapp.dto.FiveOhOneCategorySummaryDto;
import com.quizapp.service.FiveOhOneCategoryService;
import com.quizapp.service.PlayAccessService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/501/categories")
public class FiveOhOneCategoryController {

    private final FiveOhOneCategoryService categoryService;
    private final PlayAccessService playAccessService;

    public FiveOhOneCategoryController(FiveOhOneCategoryService categoryService, PlayAccessService playAccessService) {
        this.categoryService = categoryService;
        this.playAccessService = playAccessService;
    }

    @GetMapping
    public List<FiveOhOneCategorySummaryDto> findAll() {
        return categoryService.findAllSummaries();
    }

    @GetMapping("/{id}")
    public FiveOhOneCategoryDto getOne(@PathVariable Long id, Authentication authentication) {
        // A guest has no AppUser row to check canPlayFiveOhOne against - same
        // exemption RoomController applies for joining a room in the first
        // place, since the host already passed this same check when creating
        // it (see RoomController#isGuest). This lookup is that guest reading
        // the category's own checkout rules mid-game in a room they're
        // already validly sitting in, not a fresh attempt to open 501.
        if (!isGuest(authentication)) {
            playAccessService.requireFiveOhOneAccess(authentication);
        }
        return categoryService.getOne(id);
    }

    private boolean isGuest(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_GUEST"));
    }
}
