-- 使用 psql 连接 postgres 后执行，仅在数据库不存在时创建。
SELECT 'CREATE DATABASE "DateTest"'
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'DateTest')
\gexec
