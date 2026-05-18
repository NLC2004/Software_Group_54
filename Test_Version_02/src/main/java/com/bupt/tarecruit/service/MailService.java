package com.bupt.tarecruit.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Properties;
import javax.net.ssl.SSLSocketFactory;

public class MailService {
    private String host;
    private int port;
    private String username;
    private String authCode;
    private String fromAddress;
    private String fromName;
    private boolean enabled;

    public MailService(Map<String, String> settings) {
        refresh(settings);
    }

    public final void refresh(Map<String, String> settings) {
        Properties p = new Properties();
        if (settings != null) p.putAll(settings);
        this.host = value(p, "mail.smtp.host", System.getenv().getOrDefault("MAIL_SMTP_HOST", "smtp.163.com"));
        this.port = parseInt(value(p, "mail.smtp.port", System.getenv().getOrDefault("MAIL_SMTP_PORT", "465")), 465);
        this.username = value(p, "mail.smtp.username", System.getenv().getOrDefault("MAIL_SMTP_USERNAME", ""));
        this.authCode = value(p, "mail.smtp.password", System.getenv().getOrDefault("MAIL_SMTP_PASSWORD", ""));
        this.fromAddress = value(p, "mail.smtp.from", System.getenv().getOrDefault("MAIL_SMTP_FROM", this.username));
        this.fromName = value(p, "mail.smtp.fromName", System.getenv().getOrDefault("MAIL_SMTP_FROM_NAME", "TA Recruit Support"));
        this.enabled = !host.isBlank() && port > 0 && !username.isBlank() && !authCode.isBlank() && !fromAddress.isBlank();
    }

    private String value(Properties p, String key, String fallback) {
        String v = p.getProperty(key);
        return v == null || v.isBlank() ? fallback : v.trim();
    }

    private int parseInt(String v, int fallback) {
        try { return Integer.parseInt(v.trim()); } catch (Exception ignored) { return fallback; }
    }

    public boolean isEnabled() { return enabled; }

    public void sendPasswordResetApproved(String to, String fullName, String newPassword) throws IOException {
        String subject = "密码重置结果通知";
        String body = "你好" + (fullName == null || fullName.isBlank() ? "" : "，" + fullName) + "\n\n"
                + "你的密码重置申请已通过，管理员已为你重置密码。\n"
                + "新密码：" + newPassword + "\n\n"
                + "请尽快登录系统并修改密码。\n\n"
                + "此邮件为系统自动发送，请勿直接回复。";
        sendMail(to, subject, body);
    }

    public void sendPasswordResetRejected(String to, String fullName, String reason) throws IOException {
        String subject = "密码重置结果通知";
        String body = "你好" + (fullName == null || fullName.isBlank() ? "" : "，" + fullName) + "\n\n"
                + "你的密码重置申请未通过。\n"
                + (reason == null || reason.isBlank() ? "" : "原因：" + reason + "\n\n")
                + "如有疑问，请联系管理员。\n\n"
                + "此邮件为系统自动发送，请勿直接回复。";
        sendMail(to, subject, body);
    }

    private void sendMail(String to, String subject, String body) throws IOException {
        if (!enabled) {
            throw new IOException("Mail service is not configured");
        }

        try (Socket socket = SSLSocketFactory.getDefault().createSocket(host, port);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             OutputStream out = socket.getOutputStream()) {

            expect(in, 220);
            sendLine(out, "HELO localhost");
            expect(in, 250);
            sendLine(out, "AUTH LOGIN");
            expect(in, 334);
            sendLine(out, Base64.getEncoder().encodeToString(username.getBytes(StandardCharsets.UTF_8)));
            expect(in, 334);
            sendLine(out, Base64.getEncoder().encodeToString(authCode.getBytes(StandardCharsets.UTF_8)));
            expect(in, 235);
            sendLine(out, "MAIL FROM:<" + fromAddress + ">");
            expect(in, 250);
            sendLine(out, "RCPT TO:<" + to + ">");
            expect(in, 250, 251);
            sendLine(out, "DATA");
            expect(in, 354);

            String mimeSubject = Base64.getEncoder().encodeToString(subject.getBytes(StandardCharsets.UTF_8));
            String fromHeader = fromName + " <" + fromAddress + ">";
            String message = "From: " + fromHeader + "\r\n"
                    + "To: <" + to + ">\r\n"
                    + "Subject: =?UTF-8?B?" + mimeSubject + "?=\r\n"
                    + "MIME-Version: 1.0\r\n"
                    + "Content-Type: text/plain; charset=UTF-8\r\n"
                    + "Content-Transfer-Encoding: 8bit\r\n\r\n"
                    + body.replace("\n", "\r\n") + "\r\n.\r\n";
            out.write(message.getBytes(StandardCharsets.UTF_8));
            out.flush();
            expect(in, 250);
            sendLine(out, "QUIT");
        }
    }

    private void sendLine(OutputStream out, String line) throws IOException {
        out.write((line + "\r\n").getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    private void expect(BufferedReader in, int... allowed) throws IOException {
        String line = in.readLine();
        if (line == null || line.length() < 3) throw new IOException("Unexpected SMTP response");
        int code;
        try { code = Integer.parseInt(line.substring(0, 3)); }
        catch (NumberFormatException e) { throw new IOException("Unexpected SMTP response: " + line); }
        for (int a : allowed) if (code == a) return;
        throw new IOException("SMTP error: " + line);
    }
}
