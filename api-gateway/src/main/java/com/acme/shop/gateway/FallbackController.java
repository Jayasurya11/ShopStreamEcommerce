package com.acme.shop.gateway;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Answered by the gateway itself when a circuit is open or a downstream call fails or times out. */
@RestController
public class FallbackController {
    @RequestMapping("/fallback/{service}")
    ResponseEntity<ProblemDetail> fallback(@PathVariable String service) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
            "The " + service + " service is temporarily unavailable. Please try again shortly.");
        problem.setTitle("Service unavailable");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(problem);
    }
}
