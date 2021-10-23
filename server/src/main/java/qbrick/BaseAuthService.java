package qbrick;

import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.*;

@Slf4j
public class BaseAuthService implements AuthService {

    private static final String CON_STR = "jdbc:sqlite:server/src/main/resources/entry.db";

    private static Connection connection;
    private static Statement stmt;
    private static ResultSet resultSet;


    private static Set<String> activeEntries = new TreeSet<>();

    public BaseAuthService() {
        start();

        try {
            createTable();
        } catch (SQLException e) {
            log.debug("Таблица уже существует");
        }
        try {
            addEntry();
        } catch (SQLException e) {
            log.debug("Таблица уже заполнена");
        }

        showTable();
    }

    @Override
    public void showTable() {
        try {
            connection.setAutoCommit(false);
            ResultSet rs = stmt.executeQuery("SELECT * FROM entries;");

            while (rs.next()) {
                int id = rs.getInt("id");
                String login = rs.getString("login");
                String pass = rs.getString("pass");

                System.out.println("id = " + id);
                System.out.println("login = " + login);
                System.out.println("pass = " + pass);
                System.out.println();
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
            log.error("Ошибка с базой данных");
        }
    }

    @Override
    public boolean addAcc(String login, String pass) {
        try {
            if (!isAccExist(login, pass)) {
                connection.setAutoCommit(false);
                String sql = "INSERT INTO entries (login,pass) VALUES ('" + login + "', '" + pass + "');";
                stmt.executeUpdate(sql);
                connection.commit();
                log.debug("INSERT INTO entries successfully");
            }
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            log.error("Ошибка записи в базу данных");
            return false;
        }
    }

    @Override
    public boolean isAccExist(String login, String pass) {
        try {
            resultSet = stmt.executeQuery("SELECT login, pass FROM entries");
            while (resultSet.next()) {
                if (resultSet.getString("login").equals(login) && resultSet.getString("pass").equals(pass)) {
                    activeEntries.add(login);
                    return true;
                }
            }
            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            log.error("Ошибка с базой данных");
            return false;
        }
    }

    @Override
    public boolean isAccBusy(String login) {
        for (String l:  activeEntries) {
            if (l.equals(login)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void releaseAcc(String login) {
        activeEntries.remove(login);
    }

    @Override
    public void start() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(CON_STR);
            stmt = connection.createStatement();
            log.debug("Успешное подключение к базе данных");
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
            log.error("Ошибка подключения к базе дынных");
        }
    }

    @Override
    public void stop() {
        try {
            stmt.close();
            connection.close();
            log.debug("Сервис аутентификации остановлен");
        } catch (SQLException e) {
            e.printStackTrace();
            log.error("Ошибка с БД");
        }
    }

    private static void createTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS `entries` (" +
                "`ID` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL ," +
                "`login`    varchar(255)        NOT NULL, " +
                "`pass`     varchar(255)       NOT NULL)";
        stmt.executeUpdate(sql);
        log.debug("Таблица создана успешно");
    }

    private void addEntry() throws SQLException {
        addAcc("login1", "pass1");
        addAcc("login2", "pass2");
        addAcc("login3", "pass3");
    }
}
