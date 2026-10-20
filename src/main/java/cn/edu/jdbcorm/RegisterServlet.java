package cn.edu.jdbcorm;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {
    private final UserRepository users = new UserRepository();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        response.getWriter().write(Page.html(request.getContextPath(), "注册账号", null, form("")));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");
        String name = value(request, "name");
        String password = value(request, "password");
        String phone = value(request, "phone");
        String role = value(request, "role");
        String major = value(request, "major");
        try {
            if (name.isBlank() || password.length() < 6) throw new IllegalArgumentException("姓名不能为空，密码至少需要 6 位");
            if (!phone.matches("1[3-9][0-9]{9}")) throw new IllegalArgumentException("请输入正确的手机号");
            if (!role.equals("学生") && !role.equals("教师")) throw new IllegalArgumentException("请选择注册角色");
            if (major.isBlank()) throw new IllegalArgumentException("请选择专业");
            User user;
            if (role.equals("学生")) {
                Student student = new Student();
                student.setGrade(value(request, "grade"));
                student.setCollege(value(request, "college"));
                user = student;
            } else {
                Teacher teacher = new Teacher();
                teacher.setCollege(value(request, "college"));
                teacher.setTitle(value(request, "title"));
                user = teacher;
            }
            user.setName(name);
            user.setPassword(password);
            user.setPhone(phone);
            user.setRole(role);
            user.setMajor(major);
            users.register(user);
            response.sendRedirect(request.getContextPath() + "/login?registered=1");
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().write(Page.html(request.getContextPath(), "注册账号", null, form(e.getMessage())));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().write(Page.html(request.getContextPath(), "注册账号", null, form("注册失败，请检查数据库连接后重试")));
        }
    }

    private String form(String error) {
        return (error.isBlank() ? "" : "<p class='error'>" + Page.escape(error) + "</p>")
                + "<form method='post' class='auth-form auth-form-wide'><label class='form-label'>姓名<input class='form-control' name='name' required maxlength='40'></label>"
                + "<label class='form-label'>密码<input class='form-control' name='password' type='password' minlength='6' required></label>"
                + "<label class='form-label'>手机号<input class='form-control' name='phone' inputmode='numeric' pattern='1[3-9][0-9]{9}' required></label>"
                + "<label class='form-label'>角色<select class='form-select' name='role' id='role'><option value='学生'>学生</option><option value='教师'>教师</option></select></label>"
                + "<label class='form-label'>专业<select class='form-select' name='major' required><option value=''>请选择</option><option>计算机科学与技术</option><option>软件工程</option><option>物联网工程</option><option>电子信息工程</option><option>其他</option></select></label>"
                + "<div id='studentFields' class='conditional-field'><label class='form-label'>年级<input class='form-control' name='grade' placeholder='如：2026级'></label></div>"
                + "<label class='form-label'>学院<input class='form-control' name='college' required></label>"
                + "<div id='teacherFields' class='conditional-field' hidden><label class='form-label'>职称<input class='form-control' name='title'></label></div>"
                + "<div class='form-actions'><button class='btn btn-success' type='submit'>创建账号</button>"
                + "<a class='auth-link' href='login'>已有账号？登录</a></div></form>"
                + "<script>const r=document.querySelector('#role');const studentFields=document.querySelector('#studentFields');const teacherFields=document.querySelector('#teacherFields');const updateFields=()=>{const student=r.value==='学生';studentFields.hidden=!student;teacherFields.hidden=student};r.addEventListener('change',updateFields);updateFields()</script>";
    }

    private String value(HttpServletRequest request, String key) {
        String value = request.getParameter(key);
        return value == null ? "" : value.trim();
    }
}
