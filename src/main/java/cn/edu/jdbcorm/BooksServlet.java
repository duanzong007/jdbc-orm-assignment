package cn.edu.jdbcorm;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@WebServlet(urlPatterns = {"/books", "/books/add", "/books/edit", "/books/discard", "/books/copies", "/books/copies/add", "/books/copies/delete"})
public class BooksServlet extends HttpServlet {
    private final BookRepository books = new BookRepository();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        User user = (User) request.getSession().getAttribute("user");
        response.setContentType("text/html;charset=UTF-8");
        if (request.getServletPath().endsWith("copies")) {
            response.getWriter().write(Page.html(request.getContextPath(), "图书副本", user, copiesPage(request)));
            return;
        }
        if (request.getServletPath().endsWith("edit")) {
            response.getWriter().write(Page.html(request.getContextPath(), "编辑图书", user, editForm(request)));
            return;
        }
        String message = request.getParameter("message");
        String body = (message == null ? "" : "<p class='notice'>" + Page.escape(message) + "</p>")
                + (canManage(user) ? addForm() : "") + searchForm(request.getParameter("keyword"));
        try {
            body += table(books.search(request.getParameter("keyword")), canManage(user));
        } catch (Exception e) {
            body += "<p class='error'>图书查询失败</p>";
        }
        response.getWriter().write(Page.html(request.getContextPath(), "图书", user, body));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");
        User user = (User) request.getSession().getAttribute("user");
        if (!canManage(user)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        try {
            String path = request.getServletPath();
            if (path.endsWith("copies/add")) {
                books.addCopy(required(request, "bookId"));
                response.sendRedirect(request.getContextPath() + "/books/copies?id=" + request.getParameter("bookId"));
                return;
            }
            if (path.endsWith("copies/delete")) {
                books.deleteCopy(required(request, "copyId"));
                response.sendRedirect(request.getContextPath() + "/books/copies?id=" + request.getParameter("bookId"));
                return;
            }
            if (path.endsWith("discard")) {
                books.discard(required(request, "id"));
                response.sendRedirect(request.getContextPath() + "/books?message=" + java.net.URLEncoder.encode("图书已废弃", java.nio.charset.StandardCharsets.UTF_8));
                return;
            }
            if (path.endsWith("edit")) {
                Book update = new Book();
                update.setId(required(request, "id"));
                update.setTitle(required(request, "title"));
                update.setAuthor(required(request, "author"));
                update.setType(required(request, "type"));
                update.setPublisher(required(request, "publisher"));
                update.setDays(Integer.valueOf(required(request, "days")));
                update.setPrice(Double.valueOf(required(request, "price")));
                if (update.getDays() <= 0 || update.getPrice() < 0) throw new IllegalArgumentException("借阅天数或价格不合法");
                books.updateInfo(update);
                response.sendRedirect(request.getContextPath() + "/books?message=" + java.net.URLEncoder.encode("图书资料已更新", java.nio.charset.StandardCharsets.UTF_8));
                return;
            }
            Book book = new Book();
            book.setTitle(required(request, "title"));
            book.setAuthor(required(request, "author"));
            book.setType(required(request, "type"));
            book.setPublisher(required(request, "publisher"));
            book.setDays(Integer.valueOf(required(request, "days")));
            book.setPrice(Double.valueOf(required(request, "price")));
            book.setStock(Integer.valueOf(required(request, "stock")));
            book.setIntroduction(request.getParameter("introduction"));
            if (book.getDays() <= 0 || book.getPrice() < 0 || book.getStock() < 0 || book.getStock() > 500) {
                throw new IllegalArgumentException("天数、价格或库存不合法，库存不能超过 500");
            }
            books.add(book);
            response.sendRedirect(request.getContextPath() + "/books?message=" + java.net.URLEncoder.encode("图书已添加", java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().write(Page.html(request.getContextPath(), "添加图书", user,
                    "<p class='error'>" + Page.escape(e.getMessage()) + "</p><a href='books'>返回</a>"));
        }
    }

    static boolean canManage(User user) { return user instanceof Admin || user instanceof Operator; }

    private String addForm() {
        return "<details><summary>添加图书</summary><form method='post' action='books/add'>"
                + "<label>书名<input name='title' required maxlength='30'></label><label>作者<input name='author' required maxlength='30'></label>"
                + "<label>类型<select name='type' required><option>哲学</option><option>宗教</option><option>计算机</option><option>文学</option><option>历史</option><option>艺术</option><option>其他</option></select></label>"
                + "<label>出版社<input name='publisher' required maxlength='20'></label><label>借阅天数<input name='days' type='number' min='1' value='30' required></label>"
                + "<label>价格<input name='price' type='number' min='0' step='0.01' required></label><label>库存<input name='stock' type='number' min='0' max='500' value='1' required></label>"
                + "<label>简介<textarea name='introduction' maxlength='512'></textarea></label><button>保存</button></form></details>";
    }

    private String searchForm(String keyword) {
        return "<form method='get'><label>关键字<input name='keyword' value='" + Page.escape(keyword)
                + "' placeholder='书名、作者、类型或出版社'></label><button>查询</button></form>";
    }

    private String table(List<Book> list, boolean canManage) {
        StringBuilder body = new StringBuilder("<table><thead><tr><th>书名</th><th>作者</th><th>类型</th><th>出版社</th><th>借阅天数</th><th>库存</th><th>状态</th><th></th></tr></thead><tbody>");
        for (Book book : list) {
            body.append("<tr><td>").append(Page.escape(book.getTitle())).append("</td><td>")
                    .append(Page.escape(book.getAuthor())).append("</td><td>").append(Page.escape(book.getType()))
                    .append("</td><td>").append(Page.escape(book.getPublisher())).append("</td><td>")
                    .append(Page.escape(book.getDays())).append("</td><td>").append(Page.escape(book.getStock()))
                    .append("</td><td>").append(Page.escape(book.getStatus())).append("</td><td>");
            if (canManage) body.append("<a href='books/edit?id=").append(Page.escape(book.getId())).append("'>编辑</a> ")
                    .append("<a href='books/copies?id=").append(Page.escape(book.getId())).append("'>副本</a>");
            else if ("正常".equals(book.getStatus())) body.append("<a href='loans?bookId=").append(Page.escape(book.getId())).append("'>借阅</a>");
            if (canManage && "正常".equals(book.getStatus())) body.append(" <form method='post' action='books/discard' class='inline'><input type='hidden' name='id' value='")
                    .append(Page.escape(book.getId())).append("'><button class='text-button'>废弃</button></form>");
            body.append("</td></tr>");
        }
        return body.append("</tbody></table>").toString();
    }

    private String required(HttpServletRequest request, String key) {
        String value = request.getParameter(key);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("请填写完整图书信息");
        return value.trim();
    }

    private String editForm(HttpServletRequest request) {
        String id = request.getParameter("id");
        try {
            Book book = id == null ? null : books.find(id);
            if (book == null) return "<p class='error'>未找到图书</p>";
            return "<form method='post' action='books/edit'><input type='hidden' name='id' value='" + Page.escape(id) + "'>"
                    + "<label>书名<input name='title' required value='" + Page.escape(book.getTitle()) + "'></label>"
                    + "<label>作者<input name='author' required value='" + Page.escape(book.getAuthor()) + "'></label>"
                    + "<label>类型<input name='type' required value='" + Page.escape(book.getType()) + "'></label>"
                    + "<label>出版社<input name='publisher' required value='" + Page.escape(book.getPublisher()) + "'></label>"
                    + "<label>借阅天数<input name='days' type='number' min='1' required value='" + Page.escape(book.getDays()) + "'></label>"
                    + "<label>价格<input name='price' type='number' min='0' step='0.01' required value='" + Page.escape(book.getPrice()) + "'></label>"
                    + "<button>保存</button></form>";
        } catch (Exception e) {
            return "<p class='error'>读取图书失败</p>";
        }
    }

    private String copiesPage(HttpServletRequest request) {
        String id = request.getParameter("id");
        try {
            Book book = id == null ? null : books.find(id);
            if (book == null) return "<p class='error'>未找到图书</p><a href='books'>返回</a>";
            StringBuilder body = new StringBuilder("<p>").append(Page.escape(book.getTitle())).append("，库存 ")
                    .append(Page.escape(book.getStock())).append(" 本</p>");
            if ("正常".equals(book.getStatus())) body.append("<form method='post' action='books/copies/add'><input type='hidden' name='bookId' value='")
                    .append(Page.escape(id)).append("'><button>新增副本</button></form>");
            body.append("<table><thead><tr><th>副本编号</th><th>状态</th><th></th></tr></thead><tbody>");
            for (BookCopy copy : books.copies(id)) {
                body.append("<tr><td>").append(Page.escape(copy.getId())).append("</td><td>")
                        .append(Boolean.TRUE.equals(copy.getBorrowed()) ? "借出" : "可借")
                        .append("</td><td><form method='post' action='books/copies/delete' class='inline'><input type='hidden' name='bookId' value='")
                        .append(Page.escape(id)).append("'><input type='hidden' name='copyId' value='").append(Page.escape(copy.getId()))
                        .append("'><button class='text-button'>删除</button></form></td></tr>");
            }
            return body.append("</tbody></table><a href='books'>返回图书列表</a>").toString();
        } catch (Exception e) {
            return "<p class='error'>读取副本失败</p>";
        }
    }
}
