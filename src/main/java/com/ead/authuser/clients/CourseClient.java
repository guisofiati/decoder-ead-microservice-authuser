package com.ead.authuser.clients;

import com.ead.authuser.dtos.CourseDto;
import com.ead.authuser.dtos.ResponsePageDto;
import com.ead.authuser.services.UtilsService;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@Log4j2
public class CourseClient {

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    UtilsService utilsService;

    @Value("${ead.api.url.course}")
    String REQUEST_URL_COURSE;

    @Retry(name = "retryInstance", fallbackMethod = "retryFallback")
    public Page<CourseDto> getAllCoursesByUser(UUID userId, Pageable pageable) {
        ResponseEntity<ResponsePageDto<CourseDto>> result = null;

        String url = REQUEST_URL_COURSE + utilsService.createUrl(userId, pageable);

        log.debug("[Request] userId {}", userId);
        log.debug("[Request] URL: {}", url);
        log.info("[Request] URL: {}", url);

        try {
            ParameterizedTypeReference<ResponsePageDto<CourseDto>> responseType = new ParameterizedTypeReference<ResponsePageDto<CourseDto>> () {};
            result = restTemplate.exchange(url, HttpMethod.GET, null, responseType);
            List<CourseDto> searchResult = result.getBody().getContent();
            log.debug("[Response] number of elements {}", searchResult.size());
        } catch(HttpStatusCodeException e) {
            log.error("Error request /courses. {} ", e);
        }

        log.info("Ending request /courses. userId: {}", userId);
        return result.getBody();
    }

    //e.g of default method retry
    public Page<CourseDto> retryFallback(UUID userId, Pageable pageable, Throwable t) {
        log.error("Inside retry retryFallback, cause - {}", t.toString());
        List<CourseDto> searchResult = new ArrayList<>();
        return new PageImpl<>(searchResult);
    }
}
