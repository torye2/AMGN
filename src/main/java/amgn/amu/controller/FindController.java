package amgn.amu.controller;

import amgn.amu.common.ApiResult;
import amgn.amu.dto.FindIdRequest;
import amgn.amu.dto.FindIdResponse;
import amgn.amu.service.FindService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FindController {
    private final FindService findService;

    @PostMapping("/find-id")
    public ApiResult<FindIdResponse> findId(@RequestBody FindIdRequest req) {


        return ApiResult.ok(new FindIdResponse());
    }
}
