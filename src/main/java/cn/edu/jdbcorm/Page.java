package cn.edu.jdbcorm;

public final class Page {
    private Page() {}

    public static String escape(Object value) {
        if (value == null) return "";
        return value.toString().replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    public static String html(String contextPath, String title, User user, String body) {
        String head = "<!doctype html><html lang='zh-CN'><head><meta charset='UTF-8'>"
                + "<meta name='viewport' content='width=device-width, initial-scale=1'>"
                + "<title>" + escape(title) + " · 图书馆管理系统</title><base href='" + escape(contextPath) + "/'>"
                + "<link rel='stylesheet' href='assets/vendor/bootstrap.min.css'>"
                + "<link rel='stylesheet' href='assets/vendor/icons/bootstrap-icons.min.css'>"
                + "<link rel='stylesheet' href='assets/style.css?v=6'></head><body>";
        if (user == null) {
            String panel = title.contains("注册") ? " auth-panel-wide" : "";
            return head + "<main class='auth-screen'><section class='auth-aside'><div class='auth-brand'>"
                    + "<span class='auth-brand-mark'><i class='bi bi-journal-bookmark-fill'></i></span>"
                    + "<span>图书馆管理系统</span></div><div class='auth-aside-title'><span>馆藏与借阅</span>"
                    + "<h2>馆藏管理<br>借阅服务</h2></div></section>"
                    + "<section class='auth-main'><div class='auth-panel" + panel + "'><div class='auth-heading'>"
                    + "<span class='auth-kicker'>账号访问</span><h1>" + escape(title) + "</h1></div>" + body
                    + "</div></section></main><script src='assets/vendor/bootstrap.bundle.min.js'></script></body></html>";
        }
        return head + "<div class='app-shell'><aside class='sidebar'><a class='side-brand' href='home'>"
                + "<span class='side-brand-mark'><i class='bi bi-journal-bookmark-fill'></i></span>"
                + "<span>图书馆管理系统<small>管理平台</small></span></a><div class='sidebar-label'>工作台</div>"
                + "<nav class='side-nav'>" + navigation(user) + "</nav></aside><div class='main-column'>"
                + "<header class='topbar'><div class='topbar-context'><span>图书馆</span><i class='bi bi-chevron-right'></i><strong>"
                + escape(title) + "</strong></div><div class='topbar-actions'><span class='user-avatar'>"
                + escape(user.getName().isEmpty() ? "用" : user.getName().substring(0, 1)) + "</span><span class='user-info'><strong>"
                + escape(user.getName()) + "</strong><small>" + escape(user.getRole()) + "</small></span>"
                + "<a class='logout' href='logout' title='退出登录' aria-label='退出登录'><i class='bi bi-box-arrow-right'></i></a></div></header>"
                + "<main class='page-container'><div class='page-heading'><h1>" + escape(title)
                + "</h1></div><div class='page-content'>" + body + "</div></main></div></div><script src='assets/vendor/bootstrap.bundle.min.js'></script><script>"
                + "const basePath=new URL(document.querySelector('base').href).pathname;"
                + "const current=location.pathname.substring(basePath.length).replace(/^\\/+|\\/+$/g,'');"
                + "const links=[...document.querySelectorAll('.side-link')].filter(a=>current===a.dataset.route||current.startsWith(a.dataset.route+'/'));"
                + "links.sort((a,b)=>b.dataset.route.length-a.dataset.route.length);if(links[0])links[0].classList.add('active');"
                + "</script></body></html>";
    }

    private static String navigation(User user) {
        StringBuilder links = new StringBuilder();
        addLink(links, "首页", "home", "bi-speedometer2");
        if (user instanceof Admin) {
            addLink(links, "用户信息", "users", "bi-people");
            addLink(links, "图书管理", "books", "bi-book");
            addLink(links, "借阅记录", "loans/admin", "bi-journal-text");
            addLink(links, "我的借阅", "loans/mine", "bi-clock-history");
            addLink(links, "借阅统计", "statistics", "bi-bar-chart-line");
            addLink(links, "教师荐书审核", "recommendations/review", "bi-check2-square");
        } else if (user instanceof Operator) {
            addLink(links, "图书管理", "books", "bi-book");
            addLink(links, "借阅办理", "loans", "bi-arrow-left-right");
            addLink(links, "我的借阅", "loans/mine", "bi-clock-history");
        } else {
            addLink(links, "图书借阅", "books", "bi-book");
            addLink(links, "我的借阅", "loans/mine", "bi-clock-history");
            if (user instanceof Teacher) addLink(links, "推荐图书", "recommendations", "bi-lightbulb");
        }
        return links.toString();
    }

    private static void addLink(StringBuilder links, String label, String route, String icon) {
        links.append("<a class='side-link' href='").append(route).append("' data-route='").append(route)
                .append("'><i class='bi ").append(icon).append("'></i><span>").append(escape(label)).append("</span></a>");
    }
}
