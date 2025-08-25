package amgn.amu.service;

import java.util.List;

import amgn.amu.dto.ReviewCreateRequest;
import amgn.amu.dto.ReviewDto;

public interface ReviewService {
	  ReviewDto create(Long raterId, ReviewCreateRequest req); // uq_review_once 보장
	  List<ReviewDto> listForUser(Long rateeId, int limit);
	  double ratingAverage(Long rateeId);
	}