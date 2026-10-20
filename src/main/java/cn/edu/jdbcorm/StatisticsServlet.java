package cn.edu.jdbcorm;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/statistics")
public class StatisticsServlet extends HttpServlet {
    private final StatisticsRepository statistics = new StatisticsRepository();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        User user = (User) request.getSession().getAttribute("user");
        if (!(user instanceof Admin)) { response.sendError(HttpServletResponse.SC_FORBIDDEN); return; }
        response.setContentType("text/html;charset=UTF-8");
        StringBuilder body;
        try {
            body = new StringBuilder("<div class='statistics-layout'><section class='stats-section'><div class='section-heading'><h2>按图书类型</h2></div>")
                    .append(table(statistics.bookTypes())).append("</section><section class='stats-section'><div class='section-heading'><h2>按专业</h2></div>")
                    .append(table(statistics.majors())).append("</section></div>");
        } catch (Exception e) {
            body = new StringBuilder("<p class='error'>统计查询失败</p>");
        }
        response.getWriter().write(Page.html(request.getContextPath(), "借阅统计", user, body.toString()));
    }

    private String table(java.util.List<StatisticsRepository.CountRow> rows) {
        StringBuilder table = new StringBuilder("<div class='table-responsive'><table class='table table-hover align-middle data-table stats-table'><thead><tr><th>分类</th><th>借阅次数</th></tr></thead><tbody>");
        for (var row : rows) table.append("<tr><td>").append(Page.escape(row.name())).append("</td><td>")
                .append(row.count()).append("</td></tr>");
        if (rows.isEmpty()) table.append("<tr><td colspan='2' class='empty-cell'>暂无统计数据</td></tr>");
        return table.append("</tbody></table></div>").toString();
    }
}
