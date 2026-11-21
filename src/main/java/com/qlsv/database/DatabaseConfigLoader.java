package com.qlsv.database;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Properties;

import com.qlsv.model.SqlConfig;

/**
 * Loads and manages external database configurations from db.properties.
 */
public class DatabaseConfigLoader {

    private static final String CONFIG_FILE_PATH = "db.properties";

    public static SqlConfig loadConfig() {
        Properties props = new Properties();
        File file = new File(CONFIG_FILE_PATH);

        if (!file.exists()) {
            createDefaultConfigFile(file);
        }

        try (InputStream in = new FileInputStream(file)) {
            props.load(in);

            String activeProfile = props.getProperty("db.active", "h2").trim().toLowerCase();

            SqlConfig.DbType type;
            String prefix;
            switch (activeProfile) {
                case "supabase":
                case "postgres":
                case "postgresql":
                    type = SqlConfig.DbType.POSTGRESQL;
                    prefix = "db.postgresql.";
                    break;
                case "mysql":
                    type = SqlConfig.DbType.MYSQL;
                    prefix = "db.mysql.";
                    break;
                case "sqlserver":
                case "mssql":
                    type = SqlConfig.DbType.SQL_SERVER;
                    prefix = "db.sqlserver.";
                    break;
                case "sqlite":
                    type = SqlConfig.DbType.SQLITE_EMBEDDED;
                    prefix = "db.sqlite.";
                    break;
                case "h2":
                default:
                    type = SqlConfig.DbType.H2_EMBEDDED;
                    prefix = "db.h2.";
                    break;
            }

            String host = props.getProperty(prefix + "host", "localhost");
            int port = parseInt(props.getProperty(prefix + "port"), getDefaultPort(type));
            String databaseName = props.getProperty(prefix + "database", "qlsv_db");
            String username = props.getProperty(prefix + "username", "sa");
            String password = props.getProperty(prefix + "password", "");

            return new SqlConfig(type, host, port, databaseName, username, password);
        } catch (Exception e) {
            System.err.println("Không thể đọc db.properties, sử dụng H2 Embedded mặc định: " + e.getMessage());
            return new SqlConfig(SqlConfig.DbType.H2_EMBEDDED, "localhost", 0, "qlsv_h2", "sa", "");
        }
    }

    private static int getDefaultPort(SqlConfig.DbType type) {
        switch (type) {
            case SQL_SERVER: return 1433;
            case MYSQL: return 3306;
            case POSTGRESQL: return 5432;
            default: return 0;
        }
    }

    private static int parseInt(String str, int def) {
        if (str == null || str.trim().isEmpty()) return def;
        try {
            return Integer.parseInt(str.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static void createDefaultConfigFile(File file) {
        try (FileOutputStream out = new FileOutputStream(file)) {
            String content = "# ====================================================================\n" +
                    "# CAU HINH CO SO DU LIEU CHO HE THONG QUAN LY SINH VIEN PTIT (RUDP)\n" +
                    "# ====================================================================\n" +
                    "# Chon loai CSDL kich hoat: h2 | sqlite | mysql | postgresql | sqlserver\n" +
                    "db.active=h2\n\n" +
                    "# 1. H2 Embedded (Khong can cai dat, du lieu luu tru vao file qlsv_h2.mv.db)\n" +
                    "db.h2.database=qlsv_h2\n" +
                    "db.h2.username=sa\n" +
                    "db.h2.password=\n\n" +
                    "# 2. SQLite Embedded (Luu tru vao file qlsv_db.db)\n" +
                    "db.sqlite.database=qlsv_db\n\n" +
                    "# 3. PostgreSQL / Supabase Cloud (Khuyen nghi de demo qua Internet)\n" +
                    "db.postgresql.host=aws-0-ap-southeast-1.pooler.supabase.com\n" +
                    "db.postgresql.port=5432\n" +
                    "db.postgresql.database=postgres\n" +
                    "db.postgresql.username=postgres.your-project-id\n" +
                    "db.postgresql.password=your-password\n\n" +
                    "# 4. MySQL Server (Localhost / Docker / XAMPP)\n" +
                    "db.mysql.host=localhost\n" +
                    "db.mysql.port=3306\n" +
                    "db.mysql.database=qlsv_ptit\n" +
                    "db.mysql.username=root\n" +
                    "db.mysql.password=root123\n\n" +
                    "# 5. Microsoft SQL Server (Localhost / Network)\n" +
                    "db.sqlserver.host=localhost\n" +
                    "db.sqlserver.port=1433\n" +
                    "db.sqlserver.database=QLSV\n" +
                    "db.sqlserver.username=sa\n" +
                    "db.sqlserver.password=YourStrongPassword123\n";
            out.write(content.getBytes());
        } catch (Exception ignored) {}
    }
}

