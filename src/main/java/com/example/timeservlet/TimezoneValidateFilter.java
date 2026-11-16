package com.example.timeservlet;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebFilter("/time")
public class TimezoneValidateFilter extends HttpFilter {
    public static final String TIMEZONE_ATTRIBUTE = "validatedTimezone";

    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        TimezoneResolver.ResolvedTimezone timezone;
        try {
            timezone = TimezoneResolver.resolve(req.getParameter("timezone"));
        } catch (IllegalArgumentException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.setContentType("text/html; charset=UTF-8");
            resp.getWriter().write("<!DOCTYPE html><html lang=\"en\"><head>" +
                    "<meta charset=\"UTF-8\"><title>Invalid timezone</title>" +
                    "</head><body>Invalid timezone</body></html>");
            return;
        }

        req.setAttribute(TIMEZONE_ATTRIBUTE, timezone);
        chain.doFilter(req, resp);
    }
}
