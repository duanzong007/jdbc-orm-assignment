package cn.edu.jdbcorm;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet(urlPatterns = {"/users", "/users/detail"})
public class AdminUsersServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        User current = (User) request.getSession().getAttribute("user");
        if (!(current instanceof Admin)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        response.setContentType("text/html;charset=UTF-8");
        String body = request.getServletPath().endsWith("detail")
                ? detail(request) : search(request);
        response.getWriter().write(Page.html(request.getContextPath(), "用户信息", current, body));
    }

    private String search(HttpServletRequest request) {
        String keyword = request.getParameter("keyword");
        StringBuilder body = new StringBuilder("<form method='get'><label>关键字<input name='keyword' value='")
                .append(Page.escape(keyword)).append("' placeholder='姓名、角色、手机号或专业'></label><button>查询</button></form>");
        if (keyword == null) return body.toString();
        body.append("<p>查询结果</p><table><thead><tr><th>姓名</th><th>角色</th><th>手机号</th><th>专业</th><th>创建时间</th><th></th></tr></thead><tbody>");
        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id,name,role,phone,major,createTime FROM `user` WHERE name LIKE ? OR role LIKE ? OR phone LIKE ? OR major LIKE ? ORDER BY createTime DESC")) {
            String value = "%" + keyword.trim() + "%";
            for (int i = 1; i <= 4; i++) statement.setString(i, value);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    body.append("<tr><td>").append(Page.escape(result.getString("name")))
                            .append("</td><td>").append(Page.escape(result.getString("role")))
                            .append("</td><td>").append(Page.escape(result.getString("phone")))
                            .append("</td><td>").append(Page.escape(result.getString("major")))
                            .append("</td><td>").append(Page.escape(result.getTimestamp("createTime")))
                            .append("</td><td><a href='users/detail?id=").append(Page.escape(result.getString("id")))
                            .append("'>查看</a></td></tr>");
                }
            }
        } catch (Exception e) {
            body.append("<tr><td colspan='6' class='error'>查询失败</td></tr>");
        }
        return body.append("</tbody></table>").toString();
    }

    private String detail(HttpServletRequest request) {
        String id = request.getParameter("id");
        if (id == null || id.isBlank()) return "<p class='error'>未找到用户</p><a href='users'>返回</a>";
        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM `user` WHERE id=?")) {
            statement.setString(1, id);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return "<p class='error'>未找到用户</p><a href='users'>返回</a>";
                StringBuilder body = new StringBuilder("<dl>");
                for (String column : new String[]{"name", "role", "phone", "major", "createTime", "loginTime", "permissionLevel", "entryTime", "area", "duty", "grade", "college", "title"}) {
                    Object value = result.getObject(column);
                    if (value != null) body.append("<dt>").append(Page.escape(column)).append("</dt><dd>")
                            .append(Page.escape(value)).append("</dd>");
                }
                return body.append("</dl><a href='users'>返回</a>").toString();
            }
        } catch (Exception e) {
            return "<p class='error'>查询失败</p><a href='users'>返回</a>";
        }
    }
}
