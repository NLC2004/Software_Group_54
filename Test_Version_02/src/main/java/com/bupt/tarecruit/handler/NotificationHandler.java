package com.bupt.tarecruit.handler;

import com.bupt.tarecruit.model.Notification;
import com.bupt.tarecruit.model.User;
import com.bupt.tarecruit.service.DataService;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Represents the notification handler component of the TA recruitment system.
 */
public class NotificationHandler extends BaseHandler {

    /**
     * Creates a new notification handler instance.
     */
    public NotificationHandler(DataService ds) { super(ds); }

    /**
     * Handles the handle operation.
     */
    @Override
    public void handle(HttpExchange ex) throws IOException {
        if (handleCors(ex)) return;
        User user = authenticate(ex);
        if (user == null) { sendError(ex, 401, "Unauthorized"); return; }

        String path = ex.getRequestURI().getPath();
        String method = ex.getRequestMethod();
        String[] parts = path.split("/");

        try {
            if (parts.length == 3 && "GET".equals(method)) {
                listNotifications(ex, user);
            } else if (parts.length == 5 && "read".equals(parts[4]) && "PUT".equals(method)) {
                markAsRead(ex, user, parts[3]);
            } else if (parts.length == 4 && "read-all".equals(parts[3]) && "PUT".equals(method)) {
                markAllAsRead(ex, user);
            } else {
                sendError(ex, 404, "Not found");
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendError(ex, 500, "Internal error");
        }
    }

    private void listNotifications(HttpExchange ex, User user) throws IOException {
        List<Notification> notifs = ds.getNotificationsByUser(user.id);
        notifs.sort(Comparator.comparingLong((Notification n) -> n.createdAt).reversed());
        sendJson(ex, 200, notifs);
    }

    private void markAsRead(HttpExchange ex, User user, String notifId) throws IOException {
        Notification n = ds.getNotificationById(notifId);
        if (n == null) { sendError(ex, 404, "Notification not found"); return; }
        if (!n.userId.equals(user.id)) { sendError(ex, 403, "Not your notification"); return; }
        n.read = true;
        ds.updateNotification(n);
        sendJson(ex, 200, Map.of("message", "Marked as read"));
    }

    private void markAllAsRead(HttpExchange ex, User user) throws IOException {
        List<Notification> notifs = ds.getNotificationsByUser(user.id);
        for (Notification n : notifs) {
            if (!n.read) { n.read = true; ds.updateNotification(n); }
        }
        sendJson(ex, 200, Map.of("message", "All marked as read"));
    }
}
