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
        return "<details class='add-book'><summary><i class='bi bi-plus-circle'></i>添加图书</summary><form method='post' action='books/add' class='form-grid'>"
                + "<label class='form-label'>书名<input class='form-control' name='title' required maxlength='30'></label><label class='form-label'>作者<input class='form-control' name='author' required maxlength='30'></label>"
                + "<label class='form-label'>类型<select class='form-select' name='type' required><option>哲学</option><option>宗教</option><option>计算机</option><option>文学</option><option>历史</option><option>艺术</option><option>其他</option></select></label>"
                + "<label class='form-label'>出版社<input class='form-control' name='publisher' required maxlength='20'></label><label class='form-label'>借阅天数<input class='form-control' name='days' type='number' min='1' value='30' required></label>"
                + "<label class='form-label'>价格<input class='form-control' name='price' type='number' min='0' step='0.01' required></label><label class='form-label'>库存<input class='form-control' name='stock' type='number' min='0' max='500' value='1' required></label>"
                + "<label class='form-label full-width'>简介<textarea class='form-control' name='introduction' maxlength='512'></textarea></label><div class='form-actions'><button class='btn btn-success'>保存图书</button></div></form></details>";
    }

    private String searchForm(String keyword) {
        return "<form method='get' class='filter-form'><label class='form-label'>关键字<input class='form-control' name='keyword' value='" + Page.escape(keyword)
                + "' placeholder='书名、作者、类型或出版社'></label><button class='btn btn-success'><i class='bi bi-search'></i>查询</button></form>";
    }

    private String table(List<Book> list, boolean canManage) {
        StringBuilder body = new StringBuilder("<div class='table-responsive'><table class='table table-hover align-middle data-table'><thead><tr><th>书名</th><th>作者</th><th>类型</th><th>出版社</th><th>借阅天数</th><th>库存</th><th>状态</th><th>操作</th></tr></thead><tbody>");
        for (Book book : list) {
            body.append("<tr><td>").append(Page.escape(book.getTitle())).append("</td><td>")
                    .append(Page.escape(book.getAuthor())).append("</td><td>").append(Page.escape(book.getType()))
                    .append("</td><td>").append(Page.escape(book.getPublisher())).append("</td><td>")
                    .append(Page.escape(book.getDays())).append("</td><td>").append(Page.escape(book.getStock()))
                    .append("</td><td><span class='status-badge ").append("正常".equals(book.getStatus()) ? "status-available" : "status-muted")
                    .append("'>").append(Page.escape(book.getStatus())).append("</span></td><td>");
            if (canManage) body.append("<a class='btn btn-sm btn-outline-secondary' href='books/edit?id=").append(Page.escape(book.getId())).append("'>编辑</a> ")
                    .append("<a class='btn btn-sm btn-outline-secondary' href='books/copies?id=").append(Page.escape(book.getId())).append("'>副本</a>");
            else if ("正常".equals(book.getStatus())) body.append("<a class='btn btn-sm btn-success' href='loans?bookId=").append(Page.escape(book.getId())).append("'>借阅</a>");
            if (canManage && "正常".equals(book.getStatus())) body.append(" <form method='post' action='books/discard' class='inline-form'><input type='hidden' name='id' value='")
                    .append(Page.escape(book.getId())).append("'><button class='btn btn-sm btn-link text-danger'>废弃</button></form>");
            body.append("</td></tr>");
        }
        if (list.isEmpty()) body.append("<tr><td colspan='8' class='empty-cell'>没有符合条件的图书</td></tr>");
        return body.append("</tbody></table></div>").toString();
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
            return "<form method='post' action='books/edit' class='form-grid form-narrow'><input type='hidden' name='id' value='" + Page.escape(id) + "'>"
                    + "<label class='form-label'>书名<input class='form-control' name='title' required value='" + Page.escape(book.getTitle()) + "'></label>"
                    + "<label class='form-label'>作者<input class='form-control' name='author' required value='" + Page.escape(book.getAuthor()) + "'></label>"
                    + "<label class='form-label'>类型<input class='form-control' name='type' required value='" + Page.escape(book.getType()) + "'></label>"
                    + "<label class='form-label'>出版社<input class='form-control' name='publisher' required value='" + Page.escape(book.getPublisher()) + "'></label>"
                    + "<label class='form-label'>借阅天数<input class='form-control' name='days' type='number' min='1' required value='" + Page.escape(book.getDays()) + "'></label>"
                    + "<label class='form-label'>价格<input class='form-control' name='price' type='number' min='0' step='0.01' required value='" + Page.escape(book.getPrice()) + "'></label>"
                    + "<div class='form-actions'><button class='btn btn-success'>保存修改</button><a class='btn btn-light' href='books'>取消</a></div></form>";
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
            if ("正常".equals(book.getStatus())) body.append("<form method='post' action='books/copies/add' class='inline-form'><input type='hidden' name='bookId' value='")
                    .append(Page.escape(id)).append("'><button class='btn btn-success'><i class='bi bi-plus-lg'></i>新增副本</button></form>");
            body.append("<div class='table-responsive'><table class='table table-hover align-middle data-table'><thead><tr><th>副本编号</th><th>状态</th><th>操作</th></tr></thead><tbody>");
            List<BookCopy> copies = books.copies(id);
            for (BookCopy copy : copies) {
                body.append("<tr><td>").append(Page.escape(copy.getId())).append("</td><td>")
                        .append(Boolean.TRUE.equals(copy.getBorrowed()) ? "借出" : "可借")
                        .append("</td><td><form method='post' action='books/copies/delete' class='inline-form'><input type='hidden' name='bookId' value='")
                        .append(Page.escape(id)).append("'><input type='hidden' name='copyId' value='").append(Page.escape(copy.getId()))
                        .append("'><button class='btn btn-sm btn-outline-danger'>删除</button></form></td></tr>");
            }
            if (copies.isEmpty()) body.append("<tr><td colspan='3' class='empty-cell'>暂无图书副本</td></tr>");
            return body.append("</tbody></table></div><a class='btn btn-light' href='books'>返回图书列表</a>").toString();
        } catch (Exception e) {
            return "<p class='error'>读取副本失败</p>";
        }
    }
}
