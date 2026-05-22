package com.bupt.tarecruit.unit_testing;

import com.bupt.tarecruit.handler.AuthHandler;
import com.bupt.tarecruit.handler.DraftHandler;
import com.bupt.tarecruit.handler.UploadHandler;
import com.bupt.tarecruit.model.User;
import com.bupt.tarecruit.service.DataService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class AuthUploadDraftCoverageTest {

    @TempDir
    Path tempDir;

    @Test
    void authShouldRejectSelfRegisteredAdminAccounts() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        AuthHandler handler = new AuthHandler(ds);
        TestHttpExchange ex = new TestHttpExchange(
                "POST",
                "/api/auth/register",
                null,
                "{\"username\":\"bad_admin\",\"password\":\"pass123\",\"role\":\"ADMIN\",\"fullName\":\"Bad Admin\",\"email\":\"bad@example.com\",\"studentId\":\"BAD001\"}"
        );

        handler.handle(ex);

        assertEquals(403, ex.getResponseCode());
        assertNull(ds.getUserByUsername("bad_admin"));
        assertTrue(ex.getResponseBodyAsString().contains("Admin accounts cannot be registered"));
    }

    @Test
    void authLogoutShouldInvalidateSessionToken() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User ta = addUser(ds, "logout_ta", "TA");
        String token = ds.createSession(ta.id);
        AuthHandler handler = new AuthHandler(ds);

        TestHttpExchange logout = new TestHttpExchange("POST", "/api/auth/logout", null, null);
        logout.setBearerToken(token);
        handler.handle(logout);

        assertEquals(200, logout.getResponseCode());

        TestHttpExchange me = new TestHttpExchange("GET", "/api/auth/me", null, null);
        me.setBearerToken(token);
        handler.handle(me);

        assertEquals(401, me.getResponseCode());
    }

    @Test
    void uploadDownloadShouldReturnStoredPdfBytesForAuthenticatedUser() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User ta = addUser(ds, "download_ta", "TA");
        String token = ds.createSession(ta.id);
        UploadHandler handler = new UploadHandler(ds);
        String encoded = Base64.getEncoder().encodeToString("%PDF test".getBytes());

        TestHttpExchange upload = new TestHttpExchange("POST", "/api/upload", null,
                "{\"fileName\":\"resume.pdf\",\"data\":\"" + encoded + "\"}");
        upload.setBearerToken(token);
        handler.handle(upload);

        assertEquals(200, upload.getResponseCode());
        String savedName = upload.getResponseBodyAsString().replaceAll(".*\"fileName\"\\s*:\\s*\"([^\"]+)\".*", "$1");

        TestHttpExchange download = new TestHttpExchange("GET", "/api/upload/" + savedName, null, null);
        download.setBearerToken(token);
        handler.handle(download);

        assertEquals(200, download.getResponseCode());
        assertEquals("application/pdf", download.getResponseHeaders().getFirst("Content-Type"));
        assertArrayEquals("%PDF test".getBytes(), download.getResponseBodyAsString().getBytes());
    }

    @Test
    void draftHandlerShouldReturnEmptyDraftAndAllowUpsertThenGet() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User ta = addUser(ds, "draft_api_ta", "TA");
        String token = ds.createSession(ta.id);
        DraftHandler handler = new DraftHandler(ds);

        TestHttpExchange empty = new TestHttpExchange("GET", "/api/drafts/application", "jobId=job-x", null);
        empty.setBearerToken(token);
        handler.handle(empty);

        assertEquals(200, empty.getResponseCode());
        assertTrue(empty.getResponseBodyAsString().contains("\"draft\":null"));

        TestHttpExchange put = new TestHttpExchange("PUT", "/api/drafts/application", "jobId=job-x",
                "{\"coverLetter\":\"draft body\",\"priority\":3,\"confirmFullName\":\"Draft User\"}");
        put.setBearerToken(token);
        handler.handle(put);

        assertEquals(200, put.getResponseCode());

        TestHttpExchange get = new TestHttpExchange("GET", "/api/drafts/application", "jobId=job-x", null);
        get.setBearerToken(token);
        handler.handle(get);

        assertEquals(200, get.getResponseCode());
        assertTrue(get.getResponseBodyAsString().contains("draft body"));
        assertTrue(get.getResponseBodyAsString().contains("\"priority\":3"));
    }

    private User addUser(DataService ds, String username, String role) {
        User user = new User();
        user.username = username;
        user.password = "pass123";
        user.role = role;
        user.fullName = username + " Full";
        user.email = username + "@example.com";
        user.studentId = "SID_" + username;
        return ds.addUser(user);
    }
}
