package com.tecnocampus.LS2.protube_back.controller.dto;

import com.tecnocampus.LS2.protube_back.domain.Comment;
import com.tecnocampus.LS2.protube_back.domain.User;
import com.tecnocampus.LS2.protube_back.domain.Video;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Static factory methods of the response DTOs (docs/adr/0002-static-factory-dtos.md): the only place that turns an
 * entity into what the API returns, e.g. file names into {@code /media} URLs.
 */
class DtoFactoriesTest {

    private static Video video() {
        return new Video("abc", "Title", "Description", "Some Channel", 65, 1280, 720,
                Instant.parse("2026-01-01T00:00:00Z"), "abc.mp4", "abc.webp");
    }

    @Test
    void summaryTurnsTheFileNamesIntoMediaUrls() {
        VideoSummaryResponse summary = VideoSummaryResponse.from(video());

        assertEquals("/media/abc.webp", summary.thumbnailUrl());
        assertEquals("/media/abc.mp4", summary.videoUrl());
        assertEquals("Title", summary.title());
    }

    @Test
    void detailOfAnImportedVideoHasNoUploaderAndOnlyTopLevelComments() {
        Video video = video();
        Comment comment = new Comment("bob", "Nice", 2, null);
        video.addComment(comment);

        VideoDetailResponse detail = VideoDetailResponse.from(video);

        assertNull(detail.uploader());
        assertEquals("/media/abc.mp4", detail.videoUrl());
        assertEquals(1, detail.comments().size());
        // Not saved yet, so it has no id and nobody can like it
        assertFalse(detail.comments().get(0).likedByMe());
    }

    @Test
    void profileOfAUserWithoutAvatarHasNoAvatarUrl() {
        ProfileResponse profile = ProfileResponse.of(new User("alice", "alice@mail.com", "hash"), 3);

        assertNull(profile.avatarUrl());
        assertEquals(3, profile.videoCount());
    }
}
