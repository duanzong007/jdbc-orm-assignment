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
            response.getWriter().write(Page.html(request.getContextPath(), "教师荐书审核", user, reviewPage()));
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
                StringBuilder body = new StringBuilder("<p class='notice'>馆藏中找到相关图书：</p><ul class='match-list'>");
                for (Book book : matches) body.append("<li>").append(Page.escape(book.getTitle())).append(" · ").append(Page.escape(book.getAuthor())).append("</li>");
                response.getWriter().write(Page.html(request.getContextPath(), "推荐图书", user, body.append("</ul><a class='btn btn-light' href='recommendations'>返回</a>").toString()));
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
                    "<p class='error'>" + Page.escape(e.getMessage()) + "</p><a class='btn btn-light' href='recommendations'>返回</a>"));
        }
    }

    private String recommendPage(HttpServletRequest request) {
        String message = request.getParameter("message");
        return (message == null ? "" : "<p class='notice'>" + Page.escape(message) + "</p>")
                + "<form method='post' class='form-grid form-narrow'><label class='form-label'>书名<input class='form-control' name='bookName' required maxlength='20'></label>"
                + "<label class='form-label'>作者<input class='form-control' name='author' required maxlength='20'></label><label class='form-label'>出版社<input class='form-control' name='publisher' maxlength='25'></label>"
                + "<label class='form-label'>价格<input class='form-control' name='price' type='number' min='0' step='0.01'></label><div class='form-actions'><button class='btn btn-success'>提交推荐</button></div></form>";
    }

    private String reviewPage() {
        StringBuilder body = new StringBuilder("<div class='table-responsive'><table class='table table-hover align-middle data-table'><thead><tr><th>书名</th><th>作者</th><th>出版社</th><th>价格</th><th>推荐人</th><th>操作</th></tr></thead><tbody>");
        try {
            int count = 0;
            for (var item : recommendations.rows()) {
                if (!"待审".equals(item.status())) continue;
                count++;
                body.append("<tr><td>").append(Page.escape(item.bookName())).append("</td><td>").append(Page.escape(item.author()))
                        .append("</td><td>").append(Page.escape(item.publisher())).append("</td><td>").append(Page.escape(item.price()))
                        .append("</td><td>").append(Page.escape(item.userName())).append("</td><td><form method='post' class='review-actions'>")
                        .append("<input type='hidden' name='id' value='").append(Page.escape(item.id())).append("'>")
                        .append("<button class='btn btn-sm btn-success' name='action' value='approve'>通过</button><input class='form-control form-control-sm' name='reason' placeholder='驳回理由'>")
                        .append("<button name='action' value='reject' class='btn btn-sm btn-outline-danger'>驳回</button></form></td></tr>");
            }
            if (count == 0) body.append("<tr><td colspan='6' class='empty-cell'>暂无待审核的推荐</td></tr>");
        } catch (Exception e) {
            return "<p class='error'>读取推荐记录失败</p>";
        }
        return body.append("</tbody></table></div>").toString();
    }

    private String value(HttpServletRequest request, String key) {
        String value = request.getParameter(key);
        return value == null ? "" : value.trim();
    }
}
