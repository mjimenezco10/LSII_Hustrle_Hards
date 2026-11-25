package com.tecnocampus.LS2.protube_back.services;

import com.tecnocampus.LS2.protube_back.controller.dto.PageResponse;
import com.tecnocampus.LS2.protube_back.controller.dto.ProfileResponse;
import com.tecnocampus.LS2.protube_back.controller.dto.UpdateProfileRequest;
import com.tecnocampus.LS2.protube_back.controller.dto.VideoSummaryResponse;
import com.tecnocampus.LS2.protube_back.domain.User;
import com.tecnocampus.LS2.protube_back.repository.UserRepository;
import com.tecnocampus.LS2.protube_back.repository.VideoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * US-03: public profiles and the profile of the logged-in user. A profile exists for every registered user and for
 * every channel of the video store (author of imported videos), which has only its name and videos.
 */
@Service
public class ProfileService {

    private static final Logger LOG = LoggerFactory.getLogger(ProfileService.class);

    // Folder of the video store where avatars are kept; the importer only reads the .json files of the store root
    static final String AVATARS_DIR = "avatars";
    static final Map<String, String> IMAGE_TYPES = Map.of("image/jpeg", "jpg", "image/png", "png", "image/webp", "webp");
    static final long MAX_AVATAR_BYTES = 2L * 1024 * 1024;

    private final UserRepository userRepository;
    private final VideoRepository videoRepository;
    private final Path storeDir;

    public ProfileService(UserRepository userRepository, VideoRepository videoRepository,
                          @Value("${pro_tube.store.dir}") String storeDir) {
        this.userRepository = userRepository;
        this.videoRepository = videoRepository;
        this.storeDir = Path.of(storeDir);
    }

    /**
     * @throws ProfileNotFoundException if there is no user nor channel with that name
     */
    @Transactional(readOnly = true)
    public ProfileResponse getProfile(String name) {
        return userRepository.findByUsername(name)
                .map(user -> ProfileResponse.of(user, videoRepository.countByAuthor(name)))
                .orElseGet(() -> {
                    long videoCount = videoRepository.countByAuthor(name);
                    if (videoCount == 0) {
                        throw new ProfileNotFoundException();
                    }
                    return ProfileResponse.ofChannel(name, videoCount);
                });
    }

    /**
     * Videos of the profile, newest first.
     *
     * @throws ProfileNotFoundException if there is no user nor channel with that name
     */
    @Transactional(readOnly = true)
    public PageResponse<VideoSummaryResponse> getProfileVideos(String name, int page, int size) {
        if (!userRepository.existsByUsername(name) && !videoRepository.existsByAuthor(name)) {
            throw new ProfileNotFoundException();
        }
        Sort newestFirst = Sort.by(Sort.Order.desc("publishedAt"), Sort.Order.desc("id"));
        return PageResponse.from(videoRepository.findByAuthor(name, PageRequest.of(page, size, newestFirst))
                .map(VideoSummaryResponse::from));
    }

    /**
     * Saves the editable fields of the profile of {@code username}: blank fields are cleared.
     *
     * @throws InvalidCredentialsException if the user of the token no longer exists
     */
    @Transactional
    public ProfileResponse updateProfile(String username, UpdateProfileRequest request) {
        User user = find(username);
        user.updateProfile(blankToNull(request.description()), blankToNull(request.location()),
                blankToNull(request.website()));
        return ProfileResponse.of(user, videoRepository.countByAuthor(username));
    }

    /**
     * Replaces the avatar of {@code username}. The old image is deleted once the change is committed, and the new
     * one if the change is rolled back, so the store never keeps images nobody uses.
     *
     * @throws InvalidUploadException     if the image is missing, empty, too big or of an unsupported format
     * @throws VideoStorageException      if the image cannot be written to the store
     * @throws InvalidCredentialsException if the user of the token no longer exists
     */
    @Transactional
    public ProfileResponse updateAvatar(String username, MultipartFile avatar) {
        String extension = checkAvatar(avatar);
        User user = find(username);

        String avatarFile = AVATARS_DIR + "/" + UUID.randomUUID() + "." + extension;
        Path avatarPath = storeDir.resolve(avatarFile);
        try {
            Files.createDirectories(avatarPath.getParent());
            avatar.transferTo(avatarPath);
        } catch (IOException e) {
            throw new VideoStorageException(e);
        }

        String oldAvatarFile = user.getAvatarFile();
        user.changeAvatar(avatarFile);
        afterTransaction(
                () -> deleteQuietly(oldAvatarFile),
                () -> deleteQuietly(avatarFile));
        return ProfileResponse.of(user, videoRepository.countByAuthor(username));
    }

    /**
     * Removes the avatar of {@code username} (nothing happens without one).
     *
     * @throws InvalidCredentialsException if the user of the token no longer exists
     */
    @Transactional
    public ProfileResponse deleteAvatar(String username) {
        User user = find(username);
        String oldAvatarFile = user.getAvatarFile();
        user.changeAvatar(null);
        afterTransaction(() -> deleteQuietly(oldAvatarFile), () -> { });
        return ProfileResponse.of(user, videoRepository.countByAuthor(username));
    }

    private static String checkAvatar(MultipartFile avatar) {
        if (avatar == null || avatar.isEmpty()) {
            throw new InvalidUploadException("avatar", "error.avatar.required", "The avatar file is required");
        }
        String contentType = avatar.getContentType() == null ? "" : avatar.getContentType().toLowerCase(Locale.ROOT);
        String extension = IMAGE_TYPES.get(contentType);
        if (extension == null) {
            throw new InvalidUploadException("avatar", "error.avatar.type",
                    "The avatar must be a JPG, PNG or WebP image");
        }
        if (avatar.getSize() > MAX_AVATAR_BYTES) {
            throw new InvalidUploadException("avatar", "error.avatar.size", "The avatar must be at most 2 MB");
        }
        return extension;
    }

    private User find(String username) {
        return userRepository.findByUsername(username).orElseThrow(InvalidCredentialsException::new);
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.strip();
    }

    // Outside a transaction (unit tests) the change counts as committed at once
    private static void afterTransaction(Runnable onCommit, Runnable onRollback) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            onCommit.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_COMMITTED) {
                    onCommit.run();
                } else {
                    onRollback.run();
                }
            }
        });
    }

    // An image that cannot be deleted only wastes space
    private void deleteQuietly(String avatarFile) {
        if (avatarFile == null) {
            return;
        }
        Path path = storeDir.resolve(avatarFile);
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            LOG.warn("Could not delete the avatar {}", path, e);
        }
    }
}
