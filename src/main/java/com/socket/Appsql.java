package com.socket;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.json.*;

public class Appsql {
    private static Statement statement = null;
    private static PreparedStatement preparedStatement = null;
    private static ResultSet resultSet = null;

    public static String stackTraceToString(Throwable e) {
        StringBuilder sb = new StringBuilder();
        sb.append(e.toString());
        sb.append("\n");
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append(element.toString());
            sb.append("\n");
        }
        return sb.toString();
    }

    public int logData(Connection conn, String sData, String sJenis, StringBuilder sbLOG){
        sbLOG.setLength(0);
        String sSQL = String.format("INSERT INTO tbl_log SET data = ?, jenis = ?");
        try{
            PreparedStatement pStatement = conn.prepareStatement(sSQL);
            pStatement.setString(1, sData);
            pStatement.setString(2, sJenis);
            pStatement.executeUpdate();
            return 1;
        }catch(Exception e){
            e.printStackTrace();
            sbLOG.append(stackTraceToString(e));
            return -1;
        }
    }

    public int getBalanceInquiry(Connection conn, String sAccNo, StringBuilder sbName, StringBuilder sbSaldoMin, StringBuilder sbSaldoAwal, StringBuilder sbAvailSaldo, StringBuilder sbKodeCab, StringBuilder sbJenisRek, StringBuilder sbStatus, StringBuilder sbNoCif,  StringBuilder sbLOG){
        sbName.setLength(0);
        sbSaldoMin.setLength(0);
        sbSaldoAwal.setLength(0);
        sbAvailSaldo.setLength(0);
        sbKodeCab.setLength(0);
        sbJenisRek.setLength(0);
        sbStatus.setLength(0);
        sbNoCif.setLength(0);
        sbLOG.setLength(0);
        String sSQL = "";
        String sRowID = "";
        sSQL = String.format("SELECT a.rowid AS rowid, a.no_cif AS cif, a.saldo_min AS saldoMin, a.saldo_start AS saldoStart, a.saldo_avail AS availSaldo, a.kode_cabang AS kodeCabang, a.jenis_rekening AS jenisRek, a.status AS status, b.nama AS nama FROM tbl_saldo a, tbl_cif b WHERE a.norek = b.norek AND a.norek = ?");
        try {
            PreparedStatement pStatement = conn.prepareStatement(sSQL);
            pStatement.setString(1, sAccNo);
            resultSet = pStatement.executeQuery();
            if (resultSet.next()) {
                sbName.append(resultSet.getString("nama"));
                sbSaldoMin.append(resultSet.getString("saldoMin"));
                sbSaldoAwal.append(resultSet.getString("saldoStart"));
                sbAvailSaldo.append(resultSet.getString("availSaldo"));
                sbKodeCab.append(resultSet.getString("kodeCabang"));
                sbJenisRek.append(resultSet.getString("jenisRek"));
                sbStatus.append(resultSet.getString("status"));
                sbNoCif.append(resultSet.getString("cif"));
                sRowID = resultSet.getString("rowid");
                sbLOG.append(String.format("Getting Data Rekening RowID=[%s] AccNo=[%s] CIF=[%s] Name=[%s] Saldo Min=[%s] Saldo Awal=[%s] Saldo Tersedia=[%s] KodeCab=[%s] Jenis Rekening=[%s] Status=[%s]", sRowID, sAccNo, sbNoCif, sbName, sbSaldoMin, sbSaldoAwal, sbAvailSaldo, sbKodeCab, sbJenisRek, sbStatus));
                return 1;
            } else {
                sbLOG.append(String.format("Getting Data Rekening AccNo=[%s] Not Found", sAccNo));
                return 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
            sbLOG.append(stackTraceToString(e));
            return -1;
        }
    }

    public int getHistoryTransaction(Connection conn, String sAccNo, StringBuilder sbData,
            StringBuilder sbLOG) {
        sbData.setLength(0);
        sbLOG.setLength(0);
        String sSQL = "";
        String sRowID = "";
        String aData = "[";

        try {
            Statement stmt = conn.createStatement();
            sSQL = String.format(
                    "SELECT rowid, norek, kode_trx, amount, stan, time, date, rrn, status_trx, norek_tujuan FROM tbl_history WHERE norek = '%s'", sAccNo);

            System.out.println(sSQL);
            ResultSet rs = stmt.executeQuery(sSQL);
            JSONArray ajData = new JSONArray();
            int nData = 0;
            while (rs.next()) {
                nData++;
                String rowid = rs.getString("rowid");
                String kodeTrx = rs.getString("kode_trx");
                String amount = rs.getString("amount");
                double nAmount = Double.parseDouble(amount);
                amount = String.format("%.0f", nAmount);
                String stan = rs.getString("stan");
                String time = rs.getString("time");
                String date = rs.getString("date");
                String rrn = rs.getString("rrn");
                String rcode = rs.getString("status_trx");
                String toAcc = rs.getString("norek_tujuan");
                JSONObject jData = new JSONObject();
                jData.put("no", nData);
                jData.put("kodeTrx", kodeTrx);
                jData.put("amount", amount);
                jData.put("stan", stan);
                jData.put("time", time);
                jData.put("date", date);
                jData.put("rrn", rrn);
                jData.put("rCode", rcode);
                ajData.put(jData);
            }

            sbData.append(ajData.toString().replaceAll("\\\\", ""));
            sbLOG.append(
                    String.format("Get Data Transaksi AccNo=[%s] TXData=[%s]", sAccNo, sbData));
            return nData;
        } catch (Exception e) {
            e.printStackTrace();
            // sLOG.append(e.toString());
            sbLOG.append(stackTraceToString(e));
            return -1;
        }
    }

    public int getLoginUser(Connection conn, String sUserId, StringBuilder sbPassword, StringBuilder sbNorek, StringBuilder sbIsLogin, StringBuilder sbTermId, StringBuilder sbInvalidLogin, StringBuilder sbStatus, StringBuilder sbLOG) {
        sbNorek.setLength(0);
        sbIsLogin.setLength(0);
        sbTermId.setLength(0);
        sbPassword.setLength(0);
        sbInvalidLogin.setLength(0);
        sbStatus.setLength(0);
        sbLOG.setLength(0);
        String sSQL = "";
        String sRowID = "";
        sSQL = String.format(
                "SELECT rowid, userid, password, norek, term_id, is_login, invalid_password, status FROM tbl_user WHERE userid = ?");
        try {
            PreparedStatement pStatement = conn.prepareStatement(sSQL);
            pStatement.setString(1, sUserId);
            resultSet = pStatement.executeQuery();
            if (resultSet.next()) {
                sbNorek.append(resultSet.getString("norek"));
                sbIsLogin.append(resultSet.getString("is_login"));
                sbTermId.append(resultSet.getString("term_id"));
                sbPassword.append(resultSet.getString("password"));
                sbInvalidLogin.append(resultSet.getString("invalid_password"));
                sbStatus.append(resultSet.getString("status"));
                sRowID = resultSet.getString("rowid");
                sbLOG.append(String.format("Getting Login User RowID=[%s] UserID=[%s] NoRek=[%s] isLogin=[%s] AndroidID=[%s] Invalid Login=[%s] Password=[%s] Status=[%s]", sRowID, sUserId, sbNorek, sbIsLogin, sbTermId, sbInvalidLogin, sbPassword, sbStatus));
                return 1;
            } else {
                sbLOG.append(String.format("Getting Login UserID=[%s] Not Found", sUserId));
                return 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
            sbLOG.append(stackTraceToString(e));
            return -1;
        }
    }

    public int updateLoginUser(Connection conn, String sUserId, String sLoginStatus, StringBuilder sbLOG){
        sbLOG.setLength(0);
        String sSQL = "";
        sSQL = String.format("UPDATE tbl_user SET is_login = ?, time_last_login = NOW() WHERE userid = ?");
        try{
            PreparedStatement pStatement = conn.prepareStatement(sSQL);
            pStatement.setString(1, sLoginStatus);
            pStatement.setString(2, sUserId);
            pStatement.executeUpdate();
            return 1;
        }catch(Exception e){
            e.printStackTrace();
            sbLOG.append(stackTraceToString(e));
            return -1;
        }
    }

    public int updateDeviceUser(Connection conn, String sUserId, String sDeviceId, StringBuilder sbLOG) {
        sbLOG.setLength(0);
        String sSQL = "";
        sSQL = String.format("UPDATE tbl_user SET term_id = ? WHERE userid = ?");
        try {
            PreparedStatement pStatement = conn.prepareStatement(sSQL);
            pStatement.setString(1, sDeviceId);
            pStatement.setString(2, sUserId);
            pStatement.executeUpdate();
            return 1;
        } catch (Exception e) {
            e.printStackTrace();
            sbLOG.append(stackTraceToString(e));
            return -1;
        }
    }

    public int updateInvalidLogin(Connection conn, String sUserId, StringBuilder sbLOG) {
        sbLOG.setLength(0);
        String sSQL = "";
        sSQL = String.format("UPDATE tbl_user SET invalid_password = invalid_password+1 WHERE userid = ?");
        try {
            PreparedStatement pStatement = conn.prepareStatement(sSQL);
            pStatement.setString(1, sUserId);
            pStatement.executeUpdate();
            return 1;
        } catch (Exception e) {
            e.printStackTrace();
            sbLOG.append(stackTraceToString(e));
            return -1;
        }
    }

    public int updateStatusUser(Connection conn, String sUserId, StringBuilder sbLOG) {
        sbLOG.setLength(0);
        String sSQL = "";
        sSQL = String.format("UPDATE tbl_user SET status='1' WHERE userid = ?");
        try {
            PreparedStatement pStatement = conn.prepareStatement(sSQL);
            pStatement.setString(1, sUserId);
            pStatement.executeUpdate();
            return 1;
        } catch (Exception e) {
            e.printStackTrace();
            sbLOG.append(stackTraceToString(e));
            return -1;
        }
    }

    public int getInqRek(Connection conn, String sAccNo, StringBuilder sbName, StringBuilder sbStatus, StringBuilder sbAvailableSaldo, StringBuilder sbLOG) {
        sbName.setLength(0);
        sbStatus.setLength(0);
        sbAvailableSaldo.setLength(0);
        sbLOG.setLength(0);
        String sSQL = "";
        String sRowID = "";
        sSQL = String.format(
                "SELECT a.rowid AS rowid, a.no_cif AS cif, a.saldo_min AS saldoMin, a.saldo_start AS saldoStart, a.saldo_avail AS availSaldo, a.kode_cabang AS kodeCabang, a.jenis_rekening AS jenisRek, a.status AS status, b.nama AS nama FROM tbl_saldo a, tbl_cif b WHERE a.norek = b.norek AND a.norek = ?");
        // System.out.println(sSQL);
        try {
            PreparedStatement pStatement = conn.prepareStatement(sSQL);
            pStatement.setString(1, sAccNo);
            resultSet = pStatement.executeQuery();
            if (resultSet.next()) {
                sbName.append(resultSet.getString("nama"));
                sbStatus.append(resultSet.getString("status"));
                sbAvailableSaldo.append(resultSet.getString("availSaldo"));
                sRowID = resultSet.getString("rowid");
                sbLOG.append(String.format(
                        "Getting Data Rekening RowID=[%s] AccNo=[%s] Name=[%s] Status=[%s] AvailSaldo=[%s]",
                        sRowID, sAccNo, sbName, sbStatus, sbAvailableSaldo));
                return 1;
            } else {
                sbLOG.append(String.format("Getting Inq Rekening AccNo=[%s] Not Found", sAccNo));
                return 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
            sbLOG.append(stackTraceToString(e));
            return -1;
        }
    }

    public int changePin(Connection conn, String sUserId, String sNewPassword, StringBuilder sbLOG) {
        sbLOG.setLength(0);
        String sSQL = "";
        sSQL = String.format("UPDATE tbl_user SET password= ? WHERE userid = ?");
        try {
            PreparedStatement pStatement = conn.prepareStatement(sSQL);
            pStatement.setString(1, sNewPassword);
            pStatement.setString(2, sUserId);
            pStatement.executeUpdate();
            return 1;
        } catch (Exception e) {
            e.printStackTrace();
            sbLOG.append(stackTraceToString(e));
            return -1;
        }
    }
}