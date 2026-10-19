package cn.edu.jdbcorm;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@WebServlet("/home")
public class HomeServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        User user = (User) request.getSession().getAttribute("user");
        Map<String, String> links = new LinkedHashMap<>();
        if (user instanceof Admin) {
            links.put("用户信息", "users");
            links.put("图书管理", "books");
            links.put("借阅管理", "loans/admin");
            links.put("借阅统计", "statistics");
            links.put("图书推荐审核", "recommendations/review");
        } else if (user instanceof Operator) {
            links.put("图书管理", "books");
            links.put("借阅办理", "loans");
        } else {
            links.put("借阅图书", "books");
            links.put("我的借阅", "loans/mine");
            if (user instanceof Teacher) links.put("推荐图书", "recommendations");
        }
        StringBuilder body = new StringBuilder("<p>欢迎，").append(Page.escape(user.getName()))
                .append("（").append(Page.escape(user.getRole())).append("）</p><section class='menu'>");
        links.forEach((label, url) -> body.append("<a class='menu-item' href='").append(url).append("'>")
                .append(Page.escape(label)).append("</a>"));
        body.append("</section>");
        response.setContentType("text/html;charset=UTF-8");
        response.getWriter().write(Page.html("首页", user, body.toString()));
    }
}
