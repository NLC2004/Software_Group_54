package com.bupt.tarecruit.unit_testing;

import com.bupt.tarecruit.handler.AuthHandler;
import com.bupt.tarecruit.handler.UploadHandler;
import com.bupt.tarecruit.model.User;
import com.bupt.tarecruit.service.DataService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class Tv1TaProfileAndUploadTest {

    @TempDir
    Path tempDir;

    @Test
    void ta01_profileUpdateShouldPersistPersonalInformation() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User ta = createTa(ds, "tv1_profile_ta");
        String token = ds.createSession(ta.id);

        AuthHandler handler = new AuthHandler(ds);
        TestHttpExchange ex = new TestHttpExchange(
                "PUT",
                "/api/auth/profile",
                null,
                "{\"studentId\":\"20240001\",\"fullName\":\"Alice Updated\",\"email\":\"alice.updated@example.com\",\"phone\":\"13911112222\",\"gender\":\"F\"}"
        );
        ex.setBearerToken(token);

        handler.handle(ex);

        assertEquals(200, ex.getResponseCode());
        User updated = ds.getUserById(ta.id);
        assertEquals("20240001", updated.studentId);
        assertEquals("Alice Updated", updated.fullName);
        assertEquals("alice.updated@example.com", updated.email);
        assertEquals("13911112222", updated.phone);
        assertEquals("F", updated.gender);
    }

    @Test
    void ta03_uploadShouldSanitizeFileNameAndKeepBytes() throws Exception {
        DataService ds = new DataService(tempDir.toString());
        User ta = createTa(ds, "tv1_upload_ta");
        String token = ds.createSession(ta.id);
        byte[] original = "resume-binary-content".getBytes(StandardCharsets.UTF_8);
        String base64 = Base64.getEncoder().encodeToString(original);

        UploadHandler handler = new UploadHandler(ds);
        TestHttpExchange ex = new TestHttpExchange(
                "POST",
                "/api/upload",
                null,
                "{\"fileName\":\"resume 2024?.pdf\",\"data\":\"" + base64 + "\"}"
        );
        ex.setBearerToken(token);

        handler.handle(ex);

        assertEquals(200, ex.getResponseCode());
        String body = ex.getResponseBodyAsString();
        assertTrue(body.contains("fileName"));
        String savedName = extractFileName(body);
        assertTrue(savedName.endsWith("resume_2024_.pdf"));
        assertArrayEquals(original, ds.getUpload(savedName));
    }

    private User createTa(DataService ds, String username) {
        User ta = new User();
        ta.username = username;
        ta.password = "pass123";
        ta.role = "TA";
        ta.fullName = username + " Full";
        ta.email = username + "@example.com";
        ta.studentId = "SID_" + username;
        return ds.addUser(ta);
    }

    private String extractFileName(String body) {
        int keyIndex = body.indexOf("\"fileName\":\"");
        int start = keyIndex + 12;
        int end = body.indexOf('"', start);
        return body.substring(start, end);
    }
}
