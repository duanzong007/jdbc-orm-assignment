# 图书馆管理系统

项目使用 Servlet、JDBC、MySQL 和 Maven，页面由 Servlet 输出 HTML，不使用 JSP。完成的功能包括用户注册与登录、图书和副本管理、借还书、图书推荐审核，以及管理员和个人借阅查询、借阅统计。

## 环境

- JDK 17 或以上
- Maven 3.9 或以上
- MySQL
- Tomcat 11

## 数据库

数据库备份位于 `docs/作业2要求/LibraryDB.nb3`。MySQL 本机使用 Homebrew 安装时，可先启动服务，再将备份恢复到空数据库：

```sh
brew services start mysql
mysql -u root -e 'CREATE DATABASE libraryadmin CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci'
python3 scripts/restore-library-db.py docs/作业2要求/LibraryDB.nb3 --database libraryadmin
```

应用默认连接 `jdbc:mysql://localhost:3306/libraryadmin`，本机默认用户为 `root`、密码为空。也可以通过 `DB_URL`、`DB_USER`、`DB_PASSWORD` 环境变量修改连接配置。

备份里的演示账号：`admin / admin123`、`operator / operator123`、`student1 / student123`、`teacher / teacher123`。

## 构建与部署

```sh
mvn clean verify
brew services start tomcat
cp target/LibraryWeb2026.war "$(brew --prefix tomcat)/libexec/webapps/"
```

Tomcat 默认使用 8080 端口。浏览器访问 `http://localhost:8080/LibraryWeb2026/`。同一局域网的设备可使用本机局域网 IP 替换 `localhost`。

测试会连接 `libraryadmin` 数据库，临时新增的用户、图书、副本和借阅记录会在测试后清理。
