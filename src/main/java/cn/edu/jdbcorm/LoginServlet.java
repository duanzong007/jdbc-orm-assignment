package cn.edu.jdbcorm;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.security.SecureRandom;

@WebServlet(urlPatterns = {"/login", "/captcha"})
public class LoginServlet extends HttpServlet {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final UserRepository users = new UserRepository();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (request.getServletPath().equals("/captcha")) {
            captcha(request, response);
            return;
        }
        response.setContentType("text/html;charset=UTF-8");
        String message = "1".equals(request.getParameter("registered")) ? "注册成功，请登录" : "";
        response.getWriter().write(Page.html(request.getContextPath(), "登录", null, form(message)));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession(false);
        String expected = session == null ? null : (String) session.getAttribute("captcha");
        if (session != null) session.removeAttribute("captcha");
        String supplied = request.getParameter("captcha");
        if (expected == null || supplied == null || !expected.equals(supplied.trim())) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().write(Page.html(request.getContextPath(), "登录", null, form("验证码错误")));
            return;
        }
        try {
            User user = users.login(request.getParameter("name"), request.getParameter("password"));
            if (user == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("text/html;charset=UTF-8");
                response.getWriter().write(Page.html(request.getContextPath(), "登录", null, form("用户名或密码错误")));
                return;
            }
            request.getSession(true);
            request.changeSessionId();
            request.getSession().setAttribute("user", user);
            response.sendRedirect(request.getContextPath() + "/home");
        } catch (Exception e) {
            getServletContext().log("登录失败", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().write(Page.html(request.getContextPath(), "登录", null, form("登录暂时失败，请稍后重试")));
        }
    }

    private void captcha(HttpServletRequest request, HttpServletResponse response) throws IOException {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 4; i++) code.append(RANDOM.nextInt(10));
        request.getSession(true).setAttribute("captcha", code.toString());
        response.setContentType("image/svg+xml;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
        response.getWriter().write("<svg xmlns='http://www.w3.org/2000/svg' width='110' height='40' viewBox='0 0 110 40'>"
                + "<rect width='110' height='40' fill='#e7efeb'/><path d='M4 31L102 8M10 9L88 34' stroke='#9cb8aa'/>"
                + "<text x='18' y='29' font-size='24' letter-spacing='7' fill='#244e42'>" + code + "</text></svg>");
    }

    private String form(String message) {
        return (message.isBlank() ? "" : "<p class='error'>" + Page.escape(message) + "</p>")
                + "<form method='post' class='auth-form'><label class='form-label'>用户名<input class='form-control' name='name' required autocomplete='username'></label>"
                + "<label class='form-label'>密码<input class='form-control' name='password' type='password' required autocomplete='current-password'></label>"
                + "<div class='captcha-field'><label class='form-label'>验证码<input class='form-control' name='captcha' required maxlength='4' inputmode='numeric' autocomplete='off'></label>"
                + "<div class='captcha-control'><img class='captcha-image' id='captchaImage' src='captcha' alt='验证码'>"
                + "<button type='button' class='captcha-refresh' id='refreshCaptcha' aria-label='刷新验证码' title='刷新验证码'><i class='bi bi-arrow-clockwise'></i></button></div></div>"
                + "<div class='form-actions'><button class='btn btn-success' type='submit'><i class='bi bi-box-arrow-in-right'></i>登录</button>"
                + "<button type='reset' class='btn btn-light'>重置</button></div>"
                + "<a class='auth-link' href='register'>注册账号</a></form>"
                + "<script>document.getElementById('refreshCaptcha').addEventListener('click',()=>{document.getElementById('captchaImage').src='captcha?'+Date.now()})</script>";
    }
}
