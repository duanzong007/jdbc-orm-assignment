package cn.edu.jdbcorm;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(urlPatterns = {"/loans", "/loans/borrow", "/loans/return", "/loans/mine", "/loans/admin"})
public class LoansServlet extends HttpServlet {
    private final LoanRepository loans = new LoanRepository();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        User user = (User) request.getSession().getAttribute("user");
        response.setContentType("text/html;charset=UTF-8");
        if (request.getServletPath().endsWith("admin")) {
            if (!(user instanceof Admin)) { response.sendError(HttpServletResponse.SC_FORBIDDEN); return; }
            response.getWriter().write(Page.html(request.getContextPath(), "借阅记录查询", user, adminPage(request)));
            return;
        }
        StringBuilder body = new StringBuilder();
        if (request.getServletPath().endsWith("mine")) {
            response.getWriter().write(Page.html(request.getContextPath(), "个人借阅", user, personalPage(request)));
            return;
        }
        String message = request.getParameter("message");
        if (message != null) body.append("<p class='notice'>").append(Page.escape(message)).append("</p>");
        try {
            body.append("<section class='content-section'><div class='section-heading'><h2>可借图书</h2></div><form method='post' action='loans/borrow' class='filter-form borrow-form'><label class='form-label'>选择图书<select class='form-select' name='bookId' required>");
            java.util.List<Book> availableBooks = loans.availableBooks();
            for (Book book : availableBooks) {
                body.append("<option value='").append(Page.escape(book.getId())).append("'")
                        .append(book.getId().equals(request.getParameter("bookId")) ? " selected" : "")
                        .append("> ").append(Page.escape(book.getTitle())).append(" · ").append(Page.escape(book.getAuthor()))
                        .append(" · ").append(Page.escape(book.getId())).append("</option>");
            }
            if (availableBooks.isEmpty()) body.append("<option value='' selected disabled>暂无可借图书</option>");
            body.append("</select></label><button class='btn btn-success'").append(availableBooks.isEmpty() ? " disabled" : "")
                    .append("><i class='bi bi-journal-plus'></i>借书</button></form></section><section class='content-section'><div class='section-heading'><h2>当前未还</h2></div>");
            boolean staff = user instanceof Admin || user instanceof Operator;
            body.append(table(staff ? loans.active() : loans.mine(user.getId(), true), staff || user instanceof Student || user instanceof Teacher));
            body.append("</section>");
        } catch (Exception e) {
            body.append("<p class='error'>读取借阅信息失败</p>");
        }
        response.getWriter().write(Page.html(request.getContextPath(), "借阅图书", user, body.toString()));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");
        User user = (User) request.getSession().getAttribute("user");
        if (!(user instanceof Admin || user instanceof Operator || user instanceof Student || user instanceof Teacher)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        try {
            if (request.getServletPath().endsWith("return")) {
                String copyId = request.getParameter("copyId");
                if (copyId == null || copyId.isBlank()) throw new IllegalArgumentException("请选择图书副本");
                loans.returnBook(user.getId(), copyId, user instanceof Admin || user instanceof Operator);
                response.sendRedirect(request.getContextPath() + "/loans?message="
                        + java.net.URLEncoder.encode("还书成功", java.nio.charset.StandardCharsets.UTF_8));
                return;
            }
            String bookId = request.getParameter("bookId");
            if (bookId == null || bookId.isBlank()) throw new IllegalArgumentException("请选择图书");
            loans.borrow(user.getId(), bookId);
            response.sendRedirect(request.getContextPath() + "/loans?message="
                    + java.net.URLEncoder.encode("借阅成功", java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().write(Page.html(request.getContextPath(), "借阅图书", user,
                    "<p class='error'>" + Page.escape(e.getMessage()) + "</p><a href='loans'>返回</a>"));
        }
    }

    private String table(java.util.List<LoanRecord> rows, boolean allowReturn) {
        StringBuilder body = new StringBuilder("<div class='table-responsive'><table class='table table-hover align-middle data-table'><thead><tr><th>图书</th><th>副本编号</th><th>借阅人</th><th>借阅时间</th><th>应还时间</th><th>操作</th></tr></thead><tbody>");
        for (LoanRecord row : rows) {
            body.append("<tr><td>").append(Page.escape(row.bookTitle())).append("</td><td>")
                    .append(Page.escape(row.bookCopyId())).append("</td><td>").append(Page.escape(row.userName()))
                    .append("</td><td>").append(Page.escape(row.borrowDate())).append("</td><td>").append(Page.escape(row.dueDate())).append("</td><td>");
            if (allowReturn) body.append("<form method='post' action='loans/return' class='inline-form'><input type='hidden' name='copyId' value='")
                    .append(Page.escape(row.bookCopyId())).append("'><button class='btn btn-sm btn-outline-success'>还书</button></form>");
            body.append("</td></tr>");
        }
        if (rows.isEmpty()) body.append("<tr><td colspan='6' class='empty-cell'>暂无借阅记录</td></tr>");
        return body.append("</tbody></table></div>").toString();
    }

    private String personalPage(HttpServletRequest request) {
        String status = request.getParameter("status");
        boolean active = "active".equals(status);
        String fromValue = request.getParameter("from");
        String toValue = request.getParameter("to");
        StringBuilder body = new StringBuilder("<form method='get' class='filter-form'><label class='form-label'>记录<select class='form-select' name='status'><option value='all'")
                .append(active ? "" : " selected").append(">全部</option><option value='active'")
                .append(active ? " selected" : "").append(">未还</option></select></label>")
                .append("<label class='form-label'>开始日期<input class='form-control' type='date' name='from' value='").append(Page.escape(fromValue)).append("'></label>")
                .append("<label class='form-label'>结束日期<input class='form-control' type='date' name='to' value='").append(Page.escape(toValue)).append("'></label><button class='btn btn-success'><i class='bi bi-search'></i>查询</button></form>");
        try {
            java.time.LocalDate from = fromValue == null || fromValue.isBlank() ? null : java.time.LocalDate.parse(fromValue);
            java.time.LocalDate to = toValue == null || toValue.isBlank() ? null : java.time.LocalDate.parse(toValue);
            if (from != null && to != null && from.isAfter(to)) return body.append("<p class='error'>开始日期不能晚于结束日期</p>").toString();
            body.append(table(loans.mine(((User) request.getSession().getAttribute("user")).getId(), active, from, to), true));
        } catch (Exception e) {
            body.append("<p class='error'>日期格式不正确或查询失败</p>");
        }
        return body.toString();
    }

    private String adminPage(HttpServletRequest request) {
        String mode = request.getParameter("mode");
        if (mode == null || mode.isBlank()) mode = "keyword";
        String keyword = request.getParameter("keyword");
        String fromValue = request.getParameter("from");
        String toValue = request.getParameter("to");
        StringBuilder body = new StringBuilder("<form method='get' class='filter-form'><label class='form-label'>查询方式<select class='form-select' name='mode'>")
                .append(option("keyword", "关键字查询", mode)).append(option("overdue", "超期查询", mode))
                .append(option("date", "日期范围", mode)).append("</select></label>")
                .append("<label class='form-label'>关键字<input class='form-control' name='keyword' value='").append(Page.escape(keyword)).append("' placeholder='用户名或书名'></label>")
                .append("<label class='form-label'>开始日期<input class='form-control' type='date' name='from' value='").append(Page.escape(fromValue)).append("'></label>")
                .append("<label class='form-label'>结束日期<input class='form-control' type='date' name='to' value='").append(Page.escape(toValue)).append("'></label><button class='btn btn-success'><i class='bi bi-search'></i>查询</button></form>");
        try {
            java.time.LocalDate from = fromValue == null || fromValue.isBlank() ? null : java.time.LocalDate.parse(fromValue);
            java.time.LocalDate to = toValue == null || toValue.isBlank() ? null : java.time.LocalDate.parse(toValue);
            if ("date".equals(mode) && from != null && to != null && from.isAfter(to)) {
                return body.append("<p class='error'>开始日期不能晚于结束日期</p>").toString();
            }
            body.append(table(loans.adminSearch(mode, keyword, from, to), true));
        } catch (Exception e) {
            body.append("<p class='error'>查询条件不正确或读取失败</p>");
        }
        return body.toString();
    }

    private String option(String value, String label, String selected) {
        return "<option value='" + value + "'" + (value.equals(selected) ? " selected" : "") + ">" + label + "</option>";
    }
}
