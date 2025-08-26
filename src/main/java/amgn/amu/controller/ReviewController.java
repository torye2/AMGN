package amgn.amu.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import amgn.amu.dto.ReviewCreateRequest;
import amgn.amu.dto.ReviewDto;
import amgn.amu.service.ReviewService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    // POST /api/reviews?uid=123
    @PostMapping
    public ReviewDto create(
            @RequestParam(name = "uid") Long uid,
            @Valid @RequestBody ReviewCreateRequest req) {
        return reviewService.create(uid, req);
    }

    // GET /api/reviews/users/{userId}?limit=20
    @GetMapping("/users/{userId}")
    public List<ReviewDto> userReviews(
            @PathVariable(name = "userId") Long userId,
            @RequestParam(name = "limit", defaultValue = "20") int limit) {
        return reviewService.listForUser(userId, limit);
    }
}
