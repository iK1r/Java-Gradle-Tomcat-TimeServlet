package com.example.timeservlet;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@WebServlet("/time")
public class TimeServlet extends HttpServlet {
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // The filter validates the query and stores the timezone in the request.
        TimezoneResolver.ResolvedTimezone timezone =
                (TimezoneResolver.ResolvedTimezone) req.getAttribute(
                        TimezoneValidateFilter.TIMEZONE_ATTRIBUTE);

        if (timezone == null) {
            timezone = TimezoneResolver.resolve(null);
        }

        String currentTime = ZonedDateTime.now(timezone.zoneId()).format(TIME_FORMAT);
        String display = currentTime + " " + timezone.label();

        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("text/html; charset=UTF-8");
        resp.getWriter().write("<!DOCTYPE html><html lang=\"en\"><head>" +
                "<meta charset=\"UTF-8\"><title>Current time</title>" +
                "</head><body><h1>Current time</h1><p><time>" +
                escapeHtml(display) + "</time></p></body></html>");
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
