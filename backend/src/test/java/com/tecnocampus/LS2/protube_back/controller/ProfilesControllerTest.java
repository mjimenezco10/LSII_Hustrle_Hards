package com.tecnocampus.LS2.protube_back.controller;

import com.tecnocampus.LS2.protube_back.domain.User;
import com.tecnocampus.LS2.protube_back.domain.Video;
import com.tecnocampus.LS2.protube_back.repository.UserRepository;
import com.tecnocampus.LS2.protube_back.repository.VideoRepository;
import com.tecnocampus.LS2.protube_back.services.JwtService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * US-03: public profiles and the profile of the logged-in user, through the real security chain, an in-memory
 * database (rolled back after each test) and a temporary video store for the avatars.
 */
@SpringBootTest(properties = "pro_tube.load_initial_data=false")
@AutoConfigureMockMvc
@Transactional
class ProfilesControllerTest {

    private static final Path STORE_DIR = createStoreDir();

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    VideoRepository videoRepository;

    @Autowired
    JwtService jwtService;

    String token;
    User alice;

    @DynamicPropertySource
    static void storeDir(DynamicPropertyRegistry registry) {
        // Ends with a separator, as MvcConfig expects
        registry.add("pro_tube.store.dir", () -> STORE_DIR + File.separator);
    }

    private static Path createStoreDir() {
        try {
            return Files.createTempDirectory("protube-profile-test");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @AfterAll
    static void deleteStoreDir() throws IOException {
        FileSystemUtils.deleteRecursively(STORE_DIR);
    }

    @BeforeEach
    void setUp() {
        alice = userRepository.save(new User("alice", "alice@example.com", "hash"));
        token = jwtService.generateToken("alice");
    }

    private Video video(String storeId, String author, Instant publishedAt) {
        return videoRepository.save(new Video(storeId, "Video " + storeId, null, author, 10, null, null,
                publishedAt, storeId + ".mp4", storeId + ".webp"));
    }

    private ResultActions updateProfile(String body) throws Exception {
        return mockMvc.perform(put("/api/account/profile")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions uploadAvatar(MockMultipartFile file) throws Exception {
        return mockMvc.perform(multipart("/api/account/avatar")
                .file(file)
                .with(request -> {
                    request.setMethod("PUT");
                    return request;
                })
                .header("Authorization", "Bearer " + token));
    }

    private static MockMultipartFile avatar(String type, int bytes) {
        return new MockMultipartFile("avatar", "me.img", type, new byte[bytes]);
    }

    @Test
    void anyoneSeesThePublicProfileOfAUserWithoutItsEmail() throws Exception {
        alice.updateProfile("I film mountains", "Mataró", "https://alice.dev");
        video("upload-1", "alice", Instant.parse("2026-01-01T00:00:00Z"));

        mockMvc.perform(get("/api/profiles/alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.registered").value(true))
                .andExpect(jsonPath("$.description").value("I film mountains"))
                .andExpect(jsonPath("$.location").value("Mataró"))
                .andExpect(jsonPath("$.website").value("https://alice.dev"))
                .andExpect(jsonPath("$.joinedAt").isNotEmpty())
                .andExpect(jsonPath("$.videoCount").value(1))
                .andExpect(jsonPath("$.avatarUrl").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist());
    }

    @Test
    void aChannelOfTheStoreHasAProfileWithItsVideosOnly() throws Exception {
        video("store-1", "Nature Channel", Instant.parse("2026-01-01T00:00:00Z"));

        mockMvc.perform(get("/api/profiles/Nature Channel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("Nature Channel"))
                .andExpect(jsonPath("$.registered").value(false))
                .andExpect(jsonPath("$.videoCount").value(1))
                .andExpect(jsonPath("$.joinedAt").doesNotExist());
    }

    @Test
    void anUnknownProfileIsNotFound() throws Exception {
        mockMvc.perform(get("/api/profiles/nobody"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Profile not found"));
        mockMvc.perform(get("/api/profiles/nobody/videos"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listsTheVideosOfAProfileNewestFirstAndPaginated() throws Exception {
        video("old", "alice", Instant.parse("2026-01-01T00:00:00Z"));
        video("new", "alice", Instant.parse("2026-03-01T00:00:00Z"));
        video("middle", "alice", Instant.parse("2026-02-01T00:00:00Z"));
        video("other", "bob", Instant.parse("2026-04-01T00:00:00Z"));

        mockMvc.perform(get("/api/profiles/alice/videos").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].title").value("Video new"))
                .andExpect(jsonPath("$.content[1].title").value("Video middle"))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
        mockMvc.perform(get("/api/profiles/alice/videos").param("page", "1").param("size", "2"))
                .andExpect(jsonPath("$.content[0].title").value("Video old"));
    }

    @Test
    void anInvalidPageOrSizeIsABadRequest() throws Exception {
        mockMvc.perform(get("/api/profiles/alice/videos").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.page").value("Page must be 0 or greater"));
        mockMvc.perform(get("/api/profiles/alice/videos").param("size", "51"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.size").value("Size must be between 1 and 50"));
    }

    @Test
    void theOwnerSavesTheProfileAndItIsShownAtOnce() throws Exception {
        updateProfile("""
                {"description": "  New bio  ", "location": "", "website": "http://alice.dev"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("New bio"))
                .andExpect(jsonPath("$.location").doesNotExist())
                .andExpect(jsonPath("$.website").value("http://alice.dev"));

        mockMvc.perform(get("/api/profiles/alice"))
                .andExpect(jsonPath("$.description").value("New bio"))
                .andExpect(jsonPath("$.website").value("http://alice.dev"));
    }

    @Test
    void invalidProfileFieldsAreRejectedOneByOne() throws Exception {
        updateProfile("{\"description\": \"%s\"}".formatted("a".repeat(501)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.description").value("Description must be at most 500 characters"));
        updateProfile("{\"location\": \"%s\"}".formatted("a".repeat(101)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.location").value("Location must be at most 100 characters"));
        updateProfile("{\"website\": \"https://%s\"}".formatted("a".repeat(200)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.website").value("Website must be at most 200 characters"));
        updateProfile("{\"website\": \"alice.dev\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.website").value("Website must start with http:// or https://"));
    }

    @Test
    void profileErrorsAreInTheLanguageOfTheRequest() throws Exception {
        mockMvc.perform(put("/api/account/profile")
                        .header("Authorization", "Bearer " + token)
                        .header("Accept-Language", "ca")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"website\": \"alice.dev\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.website").value("El web ha de començar per http:// o https://"));
        mockMvc.perform(get("/api/profiles/nobody").header("Accept-Language", "ca"))
                .andExpect(jsonPath("$.message").value("No s'ha trobat el perfil"));
    }

    @Test
    void editingTheProfileNeedsALoggedInUser() throws Exception {
        mockMvc.perform(put("/api/account/profile").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(multipart("/api/account/avatar").file(avatar("image/png", 10)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/account/avatar"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void uploadsTheAvatarToTheStoreAndShowsItOnTheProfile() throws Exception {
        String avatarUrl = com.jayway.jsonpath.JsonPath.read(uploadAvatar(avatar("image/webp", 10))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl", startsWith("/media/avatars/")))
                .andExpect(jsonPath("$.avatarUrl", endsWith(".webp")))
                .andReturn().getResponse().getContentAsString(), "$.avatarUrl");

        assertTrue(Files.exists(STORE_DIR.resolve(avatarUrl.substring("/media/".length()))));
        mockMvc.perform(get("/api/profiles/alice"))
                .andExpect(jsonPath("$.avatarUrl").value(avatarUrl));
    }

    @Test
    void rejectsAnAvatarThatIsMissingOfTheWrongTypeOrTooBig() throws Exception {
        mockMvc.perform(multipart("/api/account/avatar")
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.avatar").value("The avatar file is required"));
        uploadAvatar(avatar("image/gif", 10))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.avatar").value("The avatar must be a JPG, PNG or WebP image"));
        uploadAvatar(avatar("image/png", 2 * 1024 * 1024 + 1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.avatar").value("The avatar must be at most 2 MB"));
    }

    @Test
    void deletingTheAvatarClearsIt() throws Exception {
        uploadAvatar(avatar("image/png", 10)).andExpect(status().isOk());

        mockMvc.perform(delete("/api/account/avatar").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").doesNotExist());
        mockMvc.perform(get("/api/profiles/alice"))
                .andExpect(jsonPath("$.avatarUrl").doesNotExist());
    }
}
