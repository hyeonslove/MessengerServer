package DataBase;

import UserException.ResultHandler;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class DAO {
    public static ArrayList<Map<String, String>> executeQuery(String query) throws SQLException {
        ArrayList<Map<String, String>> result = null;
        Connection conn = null;
        try {
            conn = DatabaseConnection.pool.getConnection();
            try (Statement statement = conn.createStatement()) {
                ResultSet res = statement.executeQuery(query);
                res.last(); // 총 데이터 개수를 세기 위해서
                int row_count = res.getRow();  // 데이터의 개수
                if (row_count > 0) {  // 데이터가 존재하면
                    res.first();
                    res.previous(); // 다시 offset을 처음으로
                    result = new ArrayList<>();
                    int col_count = res.getMetaData().getColumnCount(); //  속성 개수를 알아옴
                    String[] col_name = new String[col_count]; // 속성 이름을 담을 공간
                    for (int i = 0; i < col_count; i++)
                        col_name[i] = res.getMetaData().getColumnName(i + 1); // 속성이름을 담아둠
                    while (res.next()) {
                        Map<String, String> dict = new HashMap<>();
                        for (int i = 0; i < col_count; i++)
                            dict.put(col_name[i], res.getString(i + 1)); // 속성이름과 벨류를 삽입

                        result.add(dict);
                    }
                }
            }
            return result;
        } finally {
            if (conn != null) {
                DatabaseConnection.pool.returnConnection(conn);
            }
        }
    }

    public static Boolean executeUpdate(String query) throws SQLException {
        int res = 0;
        Connection conn = null;
        try {
            conn = DatabaseConnection.pool.getConnection();
            try (Statement statement = conn.createStatement()) {
                res = statement.executeUpdate(query);
            }
            return res == 1;
        } finally {
            if (conn != null) {
                DatabaseConnection.pool.returnConnection(conn);
            }
        }
    }

    private static ResultHandler processErr(SQLException err) {
        switch (err.getErrorCode()) {
            case 1064:
                return ResultHandler.SQLSyntax_Err;
            case 1146:
                return ResultHandler.TableDoesntExist_Err;
            case 1054:
                return ResultHandler.UnkownColumn_Err;
            case 1062:
                return ResultHandler.DuplicateKey_Err;
            default:
                err.printStackTrace();
                return ResultHandler.Unknown_Err;
        }
    }

    public static ResultHandler testFunc() {
        try {
            ArrayList<Map<String, String>> result = DAO.executeQuery("SELECT 1");
            return ResultHandler.Fail;
        } catch (SQLException ex) {
            // 실패한 이유를 적어야함.
            return processErr(ex);
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResultHandler.Unknown_Err;
        }
    }

    //region SELECT
    public static ResultHandler canLogin(String email, String password) {
        try {
            ArrayList<Map<String, String>> result = DAO.executeQuery("SELECT password FROM account WHERE email=\"" + email + "\";");
            if (result != null) {
                return result.get(0).get("password").equals(password) ? ResultHandler.Success : ResultHandler.Fail;
            }
            return ResultHandler.Fail;
        } catch (SQLException ex) {
            // 실패한 이유를 적어야함.
            return processErr(ex);
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResultHandler.Unknown_Exception;
        }
    }

    public static int getUid(String email) {
        try {
            ArrayList<Map<String, String>> result = DAO.executeQuery("SELECT uid FROM account WHERE email=\"" + email + "\";");
            if (result != null) {
                return Integer.parseInt(result.get(0).get("uid"));
            }
            return -1;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return -1;
        }
    }
    //endregion

    //region INSERT
    public static ResultHandler insertAccount(String email, String password, String name, int bd_year, int bd_month, int bd_day, String phone_number, int cash_point) {
        try {
            if (DAO.executeUpdate(
                    "INSERT INTO account(`email`, `password`, `name`, `brithday`,`phone_number`,`cash_point`, `nickname`, `introduce`) " +
                            "VALUE(\"" + email + "\",\"" + password + "\",\"" + name + "\",\"" + bd_year + "-" + bd_month + "-" + bd_day + "\",\"" + phone_number + "\"," + cash_point + ", \"" + name + "\", \"\");"
            )) {
                return ResultHandler.Success;
            }
            return ResultHandler.Fail;
        } catch (SQLException ex) {
            return processErr(ex);
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResultHandler.Unknown_Exception;
        }
    }

    public static ResultHandler insertFriend(int my_uid, int friend_uid) {
        try {
            if (DAO.executeUpdate(
                    "INSERT INTO `account_friend`(`my_uid`,`friend_uid`,`nickname`)" +
                            "value(" + my_uid + ", " + friend_uid + ", (SELECT nickname FROM account WHERE uid = " + friend_uid + "))"
            )) {
                return ResultHandler.Success;
            }
            return ResultHandler.Fail;
        } catch (SQLException ex) {
            // 실패한 이유를 적어야함.
            return processErr(ex);
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResultHandler.Unknown_Exception;
        }
    }
    //endregion

    //region UPDATE
    //endregion

    //region DELETE
    public static ResultHandler deleteAccount(String email) {
        try {
            if (DAO.executeUpdate(
                    "DELETE FROM account WHERE email=\"" + email + "\";"
            )) {
                return ResultHandler.Success;
            }
            return ResultHandler.Fail;
        } catch (SQLException ex) {
            return processErr(ex);
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResultHandler.Unknown_Exception;
        }
    }
    public static ResultHandler deleteAccount(int uid) {
        try {
            if (DAO.executeUpdate(
                    "DELETE FROM account WHERE uid=" + uid + ";"
            )) {
                return ResultHandler.Success;
            }
            return ResultHandler.Fail;
        } catch (SQLException ex) {
            return processErr(ex);
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResultHandler.Unknown_Exception;
        }
    }
    //endregion

}
