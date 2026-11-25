package com.tecnocampus.LS2.protube_back.services;

import com.tecnocampus.LS2.protube_back.controller.dto.ProfileResponse;
import com.tecnocampus.LS2.protube_back.controller.dto.UpdateProfileRequest;
import com.tecnocampus.LS2.protube_back.domain.User;
import com.tecnocampus.LS2.protube_back.repository.UserRepository;
import com.tecnocampus.LS2.protube_back.repository.VideoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    UserRepository userRepository;
    @Mock
    VideoRepository videoRepository;

    @TempDir
    Path store;

    ProfileService service;
    User alice;

    @BeforeEach
    void setUp() {
        service = new ProfileService(userRepository, videoRepository, store.toString());
        alice = new User("alice", "alice@mail.com", "hash");
        lenient().when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
    }

    @AfterEach
    void clearTransaction() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private static MockMultipartFile image(String type, int bytes) {
        return new MockMultipartFile("avatar", "me", type, new byte[bytes]);
    }

    private Path avatarPath(ProfileResponse profile) {
        return store.resolve(profile.avatarUrl().substring("/media/".length()));
    }

    @Test
    void theProfileOfAUserHasItsPublicFields() {
        alice.updateProfile("Hi", "Mataró", "https://alice.dev");
        when(videoRepository.countByAuthor("alice")).thenReturn(2L);

        ProfileResponse profile = service.getProfile("alice");

        assertTrue(profile.registered());
        assertEquals("Hi", profile.description());
        assertEquals("Mataró", profile.location());
        assertEquals("https://alice.dev", profile.website());
        assertEquals(alice.getCreatedAt(), profile.joinedAt());
        assertEquals(2, profile.videoCount());
        assertNull(profile.avatarUrl());
    }

    @Test
    void aChannelOfTheStoreHasOnlyItsNameAndVideos() {
        when(videoRepository.countByAuthor("Nature")).thenReturn(3L);

        ProfileResponse profile = service.getProfile("Nature");

        assertEquals(ProfileResponse.ofChannel("Nature", 3), profile);
    }

    @Test
    void anUnknownNameHasNoProfile() {
        assertThrows(ProfileNotFoundException.class, () -> service.getProfile("nobody"));
        assertThrows(ProfileNotFoundException.class, () -> service.getProfileVideos("nobody", 0, 12));
    }

    @Test
    void blankFieldsAreClearedAndTheOthersStripped() {
        ProfileResponse profile = service.updateProfile("alice",
                new UpdateProfileRequest("  About me  ", "   ", null));

        assertEquals("About me", profile.description());
        assertNull(profile.location());
        assertNull(profile.website());
        assertEquals("About me", alice.getDescription());
    }

    @Test
    void aDeletedUserCannotChangeTheProfile() {
        UpdateProfileRequest request = new UpdateProfileRequest(null, null, null);

        assertThrows(InvalidCredentialsException.class, () -> service.updateProfile("ghost", request));
    }

    @Test
    void savesTheAvatarInTheStoreAndDeletesThePreviousOne() throws IOException {
        ProfileResponse first = service.updateAvatar("alice", image("image/png", 10));
        Path firstPath = avatarPath(first);
        assertTrue(first.avatarUrl().startsWith("/media/avatars/"));
        assertTrue(first.avatarUrl().endsWith(".png"));
        assertTrue(Files.exists(firstPath));

        ProfileResponse second = service.updateAvatar("alice", image("image/webp", 10));

        assertFalse(Files.exists(firstPath));
        assertTrue(Files.exists(avatarPath(second)));
        assertEquals(second.avatarUrl(), "/media/" + alice.getAvatarFile());
    }

    @Test
    void insideATransactionTheOldAvatarIsDeletedOnlyAfterTheCommit() {
        Path oldPath = avatarPath(service.updateAvatar("alice", image("image/jpeg", 10)));
        TransactionSynchronizationManager.initSynchronization();

        ProfileResponse profile = service.updateAvatar("alice", image("image/jpeg", 10));
        assertTrue(Files.exists(oldPath));
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));

        assertFalse(Files.exists(oldPath));
        assertTrue(Files.exists(avatarPath(profile)));
    }

    @Test
    void aRollbackDeletesTheNewAvatarAndKeepsTheOldOne() {
        Path oldPath = avatarPath(service.updateAvatar("alice", image("image/jpeg", 10)));
        TransactionSynchronizationManager.initSynchronization();

        Path newPath = avatarPath(service.updateAvatar("alice", image("image/jpeg", 10)));
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        assertTrue(Files.exists(oldPath));
        assertFalse(Files.exists(newPath));
    }

    @Test
    void rejectsAMissingWrongOrTooBigAvatar() {
        assertEquals("The avatar file is required",
                assertThrows(InvalidUploadException.class, () -> service.updateAvatar("alice", null)).getMessage());
        MultipartFile empty = image("image/png", 0);
        assertEquals("The avatar file is required",
                assertThrows(InvalidUploadException.class, () -> service.updateAvatar("alice", empty)).getMessage());
        MultipartFile gif = image("image/gif", 10);
        assertEquals("The avatar must be a JPG, PNG or WebP image",
                assertThrows(InvalidUploadException.class, () -> service.updateAvatar("alice", gif)).getMessage());
        MultipartFile noType = new MockMultipartFile("avatar", "me", null, new byte[10]);
        assertThrows(InvalidUploadException.class, () -> service.updateAvatar("alice", noType));
        MultipartFile big = image("image/png", (int) ProfileService.MAX_AVATAR_BYTES + 1);
        InvalidUploadException tooBig = assertThrows(InvalidUploadException.class,
                () -> service.updateAvatar("alice", big));
        assertEquals("The avatar must be at most 2 MB", tooBig.getMessage());
        assertEquals("avatar", tooBig.getField());
    }

    @Test
    void anImageThatCannotBeWrittenIsAStorageError() throws IOException {
        MultipartFile broken = mock(MultipartFile.class);
        when(broken.isEmpty()).thenReturn(false);
        when(broken.getContentType()).thenReturn("image/png");
        when(broken.getSize()).thenReturn(10L);
        doThrow(new IOException("disk full")).when(broken).transferTo(any(Path.class));

        assertThrows(VideoStorageException.class, () -> service.updateAvatar("alice", broken));
        assertNull(alice.getAvatarFile());
    }

    @Test
    void deletingTheAvatarRemovesItsFileAndIsIdempotent() {
        Path path = avatarPath(service.updateAvatar("alice", image("image/png", 10)));

        assertNull(service.deleteAvatar("alice").avatarUrl());
        assertFalse(Files.exists(path));
        assertNull(service.deleteAvatar("alice").avatarUrl());
    }
}
