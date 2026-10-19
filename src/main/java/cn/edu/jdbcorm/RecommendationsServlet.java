package cn.edu.jdbcorm;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(urlPatterns = {"/recommendations", "/recommendations/review"})
public class RecommendationsServlet extends HttpServlet {
    private final RecommendationRepository recommendations = new RecommendationRepository();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        User user = (User) request.getSession().getAttribute("user");
        response.setContentType("text/html;charset=UTF-8");
        if (request.getServletPath().endsWith("review")) {
            if (!(user instanceof Admin)) { response.sendError(HttpServletResponse.SC_FORBIDDEN); return; }
            response.getWriter().write(Page.html(request.getContextPath(), "图书推荐审核", user, reviewPage()));
        } else {
            if (!(user instanceof Teacher)) { response.sendError(HttpServletResponse.SC_FORBIDDEN); return; }
            response.getWriter().write(Page.html(request.getContextPath(), "推荐图书", user, recommendPage(request)));
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");
        User user = (User) request.getSession().getAttribute("user");
        try {
            if (request.getServletPath().endsWith("review")) {
                if (!(user instanceof Admin)) { response.sendError(HttpServletResponse.SC_FORBIDDEN); return; }
                boolean approved = "approve".equals(request.getParameter("action"));
                String reason = request.getParameter("reason");
                if (!approved && (reason == null || reason.isBlank())) throw new IllegalArgumentException("驳回时请填写理由");
                recommendations.review(request.getParameter("id"), approved, reason == null ? "" : reason.trim());
                response.sendRedirect(request.getContextPath() + "/recommendations/review");
                return;
            }
            if (!(user instanceof Teacher)) { response.sendError(HttpServletResponse.SC_FORBIDDEN); return; }
            String name = value(request, "bookName");
            String author = value(request, "author");
            String publisher = value(request, "publisher");
            if (name.isBlank() || author.isBlank()) throw new IllegalArgumentException("请填写书名和作者");
            var matches = recommendations.findBooks(name, author);
            if (!matches.isEmpty()) {
                response.setContentType("text/html;charset=UTF-8");
                StringBuilder body = new StringBuilder("<p>馆藏中找到相关图书：</p><ul>");
                for (Book book : matches) body.append("<li>").append(Page.escape(book.getTitle())).append(" · ").append(Page.escape(book.getAuthor())).append("</li>");
                response.getWriter().write(Page.html(request.getContextPath(), "推荐图书", user, body.append("</ul><a href='recommendations'>返回</a>").toString()));
                return;
            }
            BookRecommendation item = new BookRecommendation();
            item.setUserId(user.getId());
            item.setBookName(name);
            item.setAuthor(author);
            item.setPublisher(publisher);
            String price = request.getParameter("price");
            item.setPrice(price == null || price.isBlank() ? null : Double.valueOf(price));
            recommendations.submit(item);
            response.sendRedirect(request.getContextPath() + "/recommendations?message="
                    + java.net.URLEncoder.encode("已提交审核", java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().write(Page.html(request.getContextPath(), "图书推荐", user,
                    "<p class='error'>" + Page.escape(e.getMessage()) + "</p><a href='recommendations'>返回</a>"));
        }
    }

    private String recommendPage(HttpServletRequest request) {
        String message = request.getParameter("message");
        return (message == null ? "" : "<p class='notice'>" + Page.escape(message) + "</p>")
                + "<form method='post'><label>书名<input name='bookName' required maxlength='20'></label>"
                + "<label>作者<input name='author' required maxlength='20'></label><label>出版社<input name='publisher' maxlength='25'></label>"
                + "<label>价格<input name='price' type='number' min='0' step='0.01'></label><button>提交推荐</button></form>";
    }

    private String reviewPage() {
        StringBuilder body = new StringBuilder("<table><thead><tr><th>书名</th><th>作者</th><th>出版社</th><th>价格</th><th>推荐人</th><th>操作</th></tr></thead><tbody>");
        try {
            for (var item : recommendations.rows()) {
                if (!"待审".equals(item.status())) continue;
                body.append("<tr><td>").append(Page.escape(item.bookName())).append("</td><td>").append(Page.escape(item.author()))
                        .append("</td><td>").append(Page.escape(item.publisher())).append("</td><td>").append(Page.escape(item.price()))
                        .append("</td><td>").append(Page.escape(item.userName())).append("</td><td><form method='post'>")
                        .append("<input type='hidden' name='id' value='").append(Page.escape(item.id())).append("'>")
                        .append("<button name='action' value='approve'>加入馆藏</button><input name='reason' placeholder='驳回理由'>")
                        .append("<button name='action' value='reject' class='secondary'>驳回</button></form></td></tr>");
            }
        } catch (Exception e) {
            return "<p class='error'>读取推荐记录失败</p>";
        }
        return body.append("</tbody></table>").toString();
    }

    private String value(HttpServletRequest request, String key) {
        String value = request.getParameter(key);
        return value == null ? "" : value.trim();
    }
}
