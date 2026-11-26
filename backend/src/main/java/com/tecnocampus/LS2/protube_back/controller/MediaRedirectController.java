package com.tecnocampus.LS2.protube_back.controller;

import com.tecnocampus.LS2.protube_back.storage.S3MediaStorage;
import com.tecnocampus.LS2.protube_back.storage.StorageConfig;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * With the S3 storage, {@code /media/<key>} (the URLs the API returns) redirects to the file in the bucket, so the
 * browser downloads videos and images from the static storage instead of through the backend. With the local
 * storage {@code MvcConfig} serves {@code /media/**} from the folder instead.
 */
@Hidden
@RestController
@ConditionalOnProperty(name = StorageConfig.TYPE_PROPERTY, havingValue = "s3")
public class MediaRedirectController {

    private static final String PREFIX = "/media/";

    private final S3MediaStorage storage;

    public MediaRedirectController(S3MediaStorage storage) {
        this.storage = storage;
    }

    @GetMapping(PREFIX + "**")
    public ResponseEntity<Void> redirect(HttpServletRequest request) {
        String key = request.getRequestURI().substring(request.getContextPath().length() + PREFIX.length());
        if (key.isEmpty() || key.contains("..")) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.status(HttpStatus.FOUND).location(storage.publicUrl(key)).build();
    }
}
