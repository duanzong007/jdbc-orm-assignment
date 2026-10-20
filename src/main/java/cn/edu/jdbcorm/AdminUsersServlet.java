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
        if (request.getServletPath().endsWith("detail") && "1".equals(request.getParameter("fragment"))) {
            response.getWriter().write(detail(request));
            return;
        }
        String body = request.getServletPath().endsWith("detail")
                ? detail(request) : search(request);
        response.getWriter().write(Page.html(request.getContextPath(), "用户信息", current, body));
    }

    private String search(HttpServletRequest request) {
        String keyword = request.getParameter("keyword");
        boolean filtering = keyword != null && !keyword.isBlank();
        StringBuilder body = new StringBuilder("<form method='get' class='filter-form'><label class='form-label'>关键字<input class='form-control' name='keyword' value='")
                .append(Page.escape(keyword)).append("' placeholder='姓名、角色、手机号或专业'></label><button class='btn btn-success'><i class='bi bi-search'></i>查询</button></form>");
        body.append("<div class='section-heading'><h2>").append(filtering ? "查询结果" : "用户列表")
                .append("</h2></div><div class='table-responsive'><table class='table table-hover align-middle data-table'><thead><tr><th>姓名</th><th>角色</th><th>手机号</th><th>专业</th><th>创建时间</th><th>操作</th></tr></thead><tbody>");
        int count = 0;
        boolean queryFailed = false;
        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id,name,role,phone,major,createTime FROM `user` WHERE name LIKE ? OR role LIKE ? OR phone LIKE ? OR major LIKE ? ORDER BY createTime DESC")) {
            String value = "%" + (keyword == null ? "" : keyword.trim()) + "%";
            for (int i = 1; i <= 4; i++) statement.setString(i, value);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    count++;
                    body.append("<tr><td>").append(Page.escape(result.getString("name")))
                            .append("</td><td>").append(Page.escape(result.getString("role")))
                            .append("</td><td>").append(Page.escape(result.getString("phone")))
                            .append("</td><td>").append(Page.escape(result.getString("major")))
                            .append("</td><td>").append(Page.escape(result.getTimestamp("createTime")))
                            .append("</td><td><button class='btn btn-sm btn-outline-secondary' data-bs-toggle='modal' data-bs-target='#userDetail' data-user-id='")
                            .append(Page.escape(result.getString("id"))).append("'>查看</button></td></tr>");
                }
            }
        } catch (Exception e) {
            queryFailed = true;
            body.append("<tr><td colspan='6' class='error'>查询失败</td></tr>");
        }
        if (count == 0 && !queryFailed) body.append("<tr><td colspan='6' class='empty-cell'>没有符合条件的用户</td></tr>");
        body.append("</tbody></table></div><div class='modal fade' id='userDetail' tabindex='-1' aria-hidden='true' aria-labelledby='userDetailTitle'>")
                .append("<div class='modal-dialog modal-dialog-centered'><div class='modal-content'><div class='modal-header'><h2 class='modal-title fs-5' id='userDetailTitle'>用户信息</h2>")
                .append("<button type='button' class='btn-close' data-bs-dismiss='modal' aria-label='关闭'></button></div><div class='modal-body' id='userDetailBody'></div></div></div></div>")
                .append("<script>document.getElementById('userDetail').addEventListener('show.bs.modal',e=>{const id=e.relatedTarget.dataset.userId;const box=document.getElementById('userDetailBody');box.innerHTML='<div class=\"text-center text-secondary py-4\">正在加载</div>';fetch('users/detail?id='+encodeURIComponent(id)+'&fragment=1').then(r=>{if(!r.ok)throw new Error();return r.text()}).then(html=>box.innerHTML=html).catch(()=>box.innerHTML='<p class=\"text-danger mb-0\">读取用户信息失败</p>')})</script>");
        return body.toString();
    }

    private String detail(HttpServletRequest request) {
        String id = request.getParameter("id");
        if (id == null || id.isBlank()) return "<p class='error'>未找到用户</p><a href='users'>返回</a>";
        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM `user` WHERE id=?")) {
            statement.setString(1, id);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return "<p class='error'>未找到用户</p>";
                StringBuilder body = new StringBuilder("<dl class='detail-list'>");
                for (String column : new String[]{"name", "role", "phone", "major", "createTime", "loginTime", "permissionLevel", "entryTime", "area", "duty", "grade", "college", "title"}) {
                    Object value = result.getObject(column);
                    if (value != null) body.append("<dt>").append(columnLabel(column)).append("</dt><dd>")
                            .append(Page.escape(value)).append("</dd>");
                }
                return body.append("</dl>").toString();
            }
        } catch (Exception e) {
            return "<p class='text-danger mb-0'>查询失败</p>";
        }
    }

    private String columnLabel(String column) {
        return switch (column) {
            case "name" -> "姓名";
            case "role" -> "角色";
            case "phone" -> "手机号";
            case "major" -> "专业";
            case "createTime" -> "创建时间";
            case "loginTime" -> "最近登录";
            case "permissionLevel" -> "权限等级";
            case "entryTime" -> "入职时间";
            case "area" -> "负责区域";
            case "duty" -> "职务";
            case "grade" -> "年级";
            case "college" -> "学院";
            case "title" -> "职称";
            default -> column;
        };
    }
}
