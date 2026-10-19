package cn.edu.jdbcorm;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(urlPatterns = {"/loans", "/loans/borrow", "/loans/return"})
public class LoansServlet extends HttpServlet {
    private final LoanRepository loans = new LoanRepository();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        User user = (User) request.getSession().getAttribute("user");
        response.setContentType("text/html;charset=UTF-8");
        StringBuilder body = new StringBuilder();
        String message = request.getParameter("message");
        if (message != null) body.append("<p class='notice'>").append(Page.escape(message)).append("</p>");
        try {
            body.append("<h2>可借图书</h2><form method='post' action='loans/borrow'><label>选择图书<select name='bookId' required>");
            for (Book book : loans.availableBooks()) {
                body.append("<option value='").append(Page.escape(book.getId())).append("'")
                        .append(book.getId().equals(request.getParameter("bookId")) ? " selected" : "")
                        .append("> ").append(Page.escape(book.getTitle())).append(" · ").append(Page.escape(book.getAuthor()))
                        .append(" · ").append(Page.escape(book.getId())).append("</option>");
            }
            body.append("</select></label><button>借书</button></form><h2>当前未还</h2>");
            boolean staff = user instanceof Admin || user instanceof Operator;
            body.append(table(staff ? loans.active() : loans.mine(user.getId(), true), staff || user instanceof Student || user instanceof Teacher));
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
        StringBuilder body = new StringBuilder("<table><thead><tr><th>图书</th><th>副本编号</th><th>借阅人</th><th>借阅时间</th><th>应还时间</th><th></th></tr></thead><tbody>");
        for (LoanRecord row : rows) {
            body.append("<tr><td>").append(Page.escape(row.bookTitle())).append("</td><td>")
                    .append(Page.escape(row.bookCopyId())).append("</td><td>").append(Page.escape(row.userName()))
                    .append("</td><td>").append(Page.escape(row.borrowDate())).append("</td><td>").append(Page.escape(row.dueDate())).append("</td><td>");
            if (allowReturn) body.append("<form method='post' action='loans/return' class='inline'><input type='hidden' name='copyId' value='")
                    .append(Page.escape(row.bookCopyId())).append("'><button class='text-button'>还书</button></form>");
            body.append("</td></tr>");
        }
        return body.append("</tbody></table>").toString();
    }
}
