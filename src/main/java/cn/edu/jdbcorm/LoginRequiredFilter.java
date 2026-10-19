package cn.edu.jdbcorm;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebFilter(urlPatterns = {"/home", "/users/*", "/books/*", "/loans/*", "/recommendations/*", "/statistics"})
public class LoginRequiredFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        if (httpRequest.getCharacterEncoding() == null) httpRequest.setCharacterEncoding("UTF-8");
        if (httpRequest.getSession(false) == null || httpRequest.getSession(false).getAttribute("user") == null) {
            ((HttpServletResponse) response).sendRedirect(httpRequest.getContextPath() + "/login");
            return;
        }
        chain.doFilter(request, response);
    }
}
