package cn.edu.jdbcorm;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    private final UserRepository users = new UserRepository();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        String message = "1".equals(request.getParameter("registered")) ? "注册成功，请登录" : "";
        response.getWriter().write(Page.html(request.getContextPath(), "登录", null, form(message)));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");
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

    private String form(String message) {
        return (message.isBlank() ? "" : "<p class='error'>" + Page.escape(message) + "</p>")
                + "<form method='post' class='auth-form'><label class='form-label'>用户名<input class='form-control' name='name' required autocomplete='username'></label>"
                + "<label class='form-label'>密码<input class='form-control' name='password' type='password' required autocomplete='current-password'></label>"
                + "<div class='form-actions'><button class='btn btn-success' type='submit'><i class='bi bi-box-arrow-in-right'></i>登录</button>"
                + "<button type='reset' class='btn btn-light'>重置</button></div>"
                + "<a class='auth-link' href='register'>注册账号</a></form>";
    }
}
