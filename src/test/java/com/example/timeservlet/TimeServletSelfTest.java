package com.example.timeservlet;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeServletSelfTest {
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Pattern TIME = Pattern.compile("<time>([^<]+)</time>");

    public static void main(String[] args) throws Exception {
        verifyTime(null, "UTC", ZoneId.of("UTC"));
        System.out.println("Default UTC: OK");

        verifyTime("UTC+2", "UTC+2", ZoneId.of("+02:00"));
        verifyTime("UTC 2", "UTC+2", ZoneId.of("+02:00"));
        verifyTime("UTC-3", "UTC-3", ZoneId.of("-03:00"));
        verifyTime("UTC+05:30", "UTC+05:30", ZoneId.of("+05:30"));
        verifyTime("Europe/Kyiv", "Europe/Kyiv", ZoneId.of("Europe/Kyiv"));
        System.out.println("Custom timezones and plus decoding: OK");

        verifyInvalid("");
        verifyInvalid("Mars/Olympus");
        verifyInvalid("UTC+99");
        verifyInvalid("UTC+18:01");
        verifyInvalid("UTC+2:99");
        System.out.println("Invalid timezone HTTP 400: OK");
        System.out.println("All servlet and filter tests passed!");
    }

    private static void verifyTime(String input, String label, ZoneId zone) throws Exception {
        MockRequest request = new MockRequest(input);
        MockResponse response = new MockResponse();
        AtomicBoolean servletCalled = new AtomicBoolean();
        FilterChain chain = (req, resp) -> {
            servletCalled.set(true);
            new TimeServlet().doGet((HttpServletRequest) req, (HttpServletResponse) resp);
        };
        new TimezoneValidateFilter().doFilter(request.proxy(), response.proxy(), chain);
        require(servletCalled.get(), "Servlet was not called for " + input);
        require(response.status == 200, "HTTP status should be 200");
        require("text/html; charset=UTF-8".equals(response.contentType), "Wrong HTML content type");
        Matcher matcher = TIME.matcher(response.body.toString());
        require(matcher.find(), "HTML should include a time element");
        String text = matcher.group(1);
        require(text.endsWith(" " + label), "Wrong timezone label: " + text);
        String datePart = text.substring(0, 19);
        LocalDateTime actual = LocalDateTime.parse(datePart, FORMAT);
        long seconds = Math.abs(Duration.between(actual, LocalDateTime.now(zone)).getSeconds());
        require(seconds <= 5, "Wrong time for " + label + ": " + text);
    }

    private static void verifyInvalid(String input) throws Exception {
        MockRequest request = new MockRequest(input);
        MockResponse response = new MockResponse();
        AtomicBoolean servletCalled = new AtomicBoolean();
        FilterChain chain = (req, resp) -> servletCalled.set(true);
        new TimezoneValidateFilter().doFilter(request.proxy(), response.proxy(), chain);
        require(!servletCalled.get(), "Invalid timezone reached servlet");
        require(response.status == 400, "Invalid timezone should return HTTP 400");
        require(response.body.toString().contains("Invalid timezone"), "Missing error message");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class MockRequest implements InvocationHandler {
        private final String timezone;
        private final Map<String, Object> attributes = new HashMap<>();

        private MockRequest(String timezone) {
            this.timezone = timezone;
        }

        private HttpServletRequest proxy() {
            return (HttpServletRequest) Proxy.newProxyInstance(
                    HttpServletRequest.class.getClassLoader(), new Class<?>[]{HttpServletRequest.class}, this);
        }

        @Override
        public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
            return switch (method.getName()) {
                case "getParameter" -> "timezone".equals(args[0]) ? timezone : null;
                case "getAttribute" -> attributes.get(args[0]);
                case "setAttribute" -> { attributes.put((String) args[0], args[1]); yield null; }
                default -> throw new UnsupportedOperationException("MockRequest: " + method.getName());
            };
        }
    }

    private static final class MockResponse implements InvocationHandler {
        private final StringWriter body = new StringWriter();
        private final PrintWriter writer = new PrintWriter(body, true);
        private int status = 200;
        private String contentType;

        private HttpServletResponse proxy() {
            return (HttpServletResponse) Proxy.newProxyInstance(
                    HttpServletResponse.class.getClassLoader(), new Class<?>[]{HttpServletResponse.class}, this);
        }

        @Override
        public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
            return switch (method.getName()) {
                case "getWriter" -> writer;
                case "setStatus" -> { status = (Integer) args[0]; yield null; }
                case "setContentType" -> { contentType = (String) args[0]; yield null; }
                default -> throw new UnsupportedOperationException("MockResponse: " + method.getName());
            };
        }
    }
}
