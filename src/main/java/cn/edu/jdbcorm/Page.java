package cn.edu.jdbcorm;

public final class Page {
    private Page() {}

    public static String escape(Object value) {
        if (value == null) return "";
        return value.toString().replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    public static String html(String contextPath, String title, User user, String body) {
        StringBuilder nav = new StringBuilder();
        if (user != null) {
            nav.append("<header><a href='home'>图书馆</a><span>")
                    .append(escape(user.getName())).append(" · ").append(escape(user.getRole()))
                    .append("</span><a href='logout'>退出</a></header>");
        }
        return "<!doctype html><html lang='zh-CN'><head><meta charset='UTF-8'>"
                + "<meta name='viewport' content='width=device-width, initial-scale=1'>"
                + "<title>" + escape(title) + "</title><base href='" + escape(contextPath) + "/'>"
                + "<link rel='stylesheet' href='assets/style.css'>"
                + "</head><body>" + nav + "<main class='page'><h1>" + escape(title) + "</h1>" + body
                + "</main></body></html>";
    }
}
