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
            links.put("我的借阅", "loans/mine");
            links.put("借阅统计", "statistics");
            links.put("教师荐书审核", "recommendations/review");
        } else if (user instanceof Operator) {
            links.put("图书管理", "books");
            links.put("借阅办理", "loans");
            links.put("我的借阅", "loans/mine");
        } else {
            links.put("借阅图书", "books");
            links.put("我的借阅", "loans/mine");
            if (user instanceof Teacher) links.put("推荐图书", "recommendations");
        }
        StringBuilder body = new StringBuilder("<section class='dashboard-welcome'><div><span class='welcome-label'>工作台</span><h2>欢迎回来，")
                .append(Page.escape(user.getName())).append("</h2></div><span class='role-chip'><i class='bi bi-person-badge'></i>")
                .append(Page.escape(user.getRole())).append("</span></section><section class='dashboard-grid'>");
        links.forEach((label, url) -> body.append("<a class='dashboard-card' href='").append(url).append("'><span class='dashboard-icon'><i class='bi ")
                .append(icon(label)).append("'></i></span><span class='dashboard-card-label'>").append(Page.escape(label))
                .append("</span><i class='bi bi-arrow-up-right card-arrow'></i></a>"));
        body.append("</section>");
        response.setContentType("text/html;charset=UTF-8");
        response.getWriter().write(Page.html(request.getContextPath(), "首页", user, body.toString()));
    }

    private String icon(String label) {
        return switch (label) {
            case "用户信息" -> "bi-people";
            case "图书管理", "借阅图书" -> "bi-book";
            case "借阅办理", "借阅管理" -> "bi-arrow-left-right";
            case "借阅统计" -> "bi-bar-chart-line";
            case "教师荐书审核" -> "bi-check2-square";
            case "推荐图书" -> "bi-lightbulb";
            default -> "bi-clock-history";
        };
    }
}
