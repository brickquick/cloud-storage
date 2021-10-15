package qbrick;

import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
public class BaseAuthService implements AuthService {
    private class Entry {
        private final String login;
        private final String pass;
        private int id;

        public Entry(int id, String login, String pass) {
            this.id = id;
            this.login = login;
            this.pass = pass;
        }
    }

    private static final String CON_STR = "jdbc:sqlite:server/src/main/resources/entry.db";

    private static Connection connection;
    private static Statement stmt;
    private static ResultSet resultSet;

    private List<Entry> entries = new ArrayList<>();

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

        entries = new ArrayList<>(getAllEntries());

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

//    @Override
//    public void changeNick(String oldNick, String newNick) {
//        try(PreparedStatement preparedStatement = connection.prepareStatement(
//                "UPDATE entries set nick = '" + newNick + "' where nick = '" + oldNick + "' ;")) {
//            connection.setAutoCommit(false);
//            preparedStatement.execute();
//            connection.commit();
//
//            entries.removeAll(entries);
//            entries.addAll(getAllEntries());
//            log.debug("Изменения в БД произведены успешно");
//        } catch (SQLException e) {
//            e.printStackTrace();
//            log.error("Ошибка с базой данных");
//        }
//    }

    @Override
    public boolean isLoginBusy(String newLogin) {
        try {
            resultSet = stmt.executeQuery("SELECT * FROM entries;");
            while (resultSet.next()) {
                String login = resultSet.getString("login");
                if (login.equals(newLogin)) {
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            log.error("Ошибка связанная с базой данных");
        }
        return false;
    }

    @Override
    public Integer getAccByLoginPass(String login, String pass) {
        for (Entry o : entries) {
            if (o.login.equals(login) && o.pass.equals(pass)) {
                return o.id;
            }
        }
        return null;
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

    private List<Entry> getAllEntries() {
        try {
            resultSet = stmt.executeQuery("SELECT ID, login, pass FROM entries");
            while (resultSet.next()) {
                entries.add(new Entry(
                        resultSet.getInt("ID"),
                        resultSet.getString("login"),
                        resultSet.getString("pass")));
            }
            return entries;
        } catch (SQLException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    private static void createTable() throws SQLException {
        String sql = "CREATE TABLE entries " +
                "(ID       INT PRIMARY KEY NOT NULL," +
                " login    CHAR(50)        NOT NULL, " +
                " pass     CHAR(50)        NOT NULL)";
        stmt.executeUpdate(sql);
        log.debug("Таблица создана успешно");
    }

    private void addEntry() throws SQLException {
        connection.setAutoCommit(false);
        String sql = "INSERT INTO entries (ID,login,pass) VALUES (1, 'login1', 'pass1');";
        stmt.executeUpdate(sql);

        sql = "INSERT INTO entries (ID,login,pass) VALUES (2, 'login2', 'pass2');";
        stmt.executeUpdate(sql);

        sql = "INSERT INTO entries (ID,login,pass) VALUES (3, 'login3', 'pass3');";
        stmt.executeUpdate(sql);
        connection.commit();
        log.debug("INSERT INTO entries successfully");
    }

}
