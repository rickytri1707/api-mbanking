package com.socket;
import java.io.DataInputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.DriverManager;
import java.sql.Connection;
import org.json.*;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.util.TimeZone;
import java.util.Date;
import java.io.FileWriter;
import java.io.IOException;
import java.io.BufferedWriter;
import java.io.File;

/**
 * Programmer: Ricky Tri Utomo
 * Project: API Transaction Mbanking
 */

class toolsDate{
    public static String getDateTimeNow(){
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        String strDate = formatter.format(date);
        return strDate;
    }
    public static String getDateNow(){
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("ddMMyyyy");
        String strDate = formatter.format(date);
        return strDate;
    }

    public static String GetDateTimeGMT(){
        DateFormat dateGMT = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss");
        Date date = new Date();
        dateGMT.setTimeZone(TimeZone.getTimeZone("GMT"));
        return dateGMT.format(date);
    }
}
public class App extends Appsql
{   
    public static int debugMessage(String sProgram, String sProfile, String sContent){
        toolsDate tool = new toolsDate();
        String sProgramName = "REST-SERVER";
        String sFile = "D:\\maven-socket\\socket-program\\debug\\" + sProgramName + "."+tool.getDateNow() + ".debug";
        try{
            String sDataDebug = "["+sProgram+"-"+sProfile+"-"+tool.getDateTimeNow()+"] " +sContent+ "\r\n";
            File file = new File(sFile);
            if (!file.exists()) {
                file.createNewFile();
            }

            FileWriter fw = new FileWriter(file.getAbsoluteFile(), true);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(sDataDebug);
            bw.close();
            System.out.println(sDataDebug);
        }catch (IOException e){
            e.printStackTrace();
            System.exit(1);
        }
        return 1;
    }
    
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
    public static int sendResponse(PrintStream out, String jdata, Connection conn){
        Appsql psql = new Appsql();
	    toolsDate tool = new toolsDate();
        StringBuilder sbLOG = new StringBuilder();
        String sProgram = "REST-SERVER";
        String sProfile = "111111";
        JSONObject jreq = new JSONObject(jdata);
        JSONObject jsend = new JSONObject();
        jsend.put("MBANKRsp", jreq);

	    String sDetail = jsend.toString();
        int nLength = jsend.toString().length();
	    String sLen = String.format("%d", nLength);
        String sHead = "HTTP/1.1 200 OK\r\nContent-Type: application/json; charset=UTF-8;\r\nDate: " + tool.GetDateTimeGMT() + " GMT\r\nTransfer-Encoding: chunked\r\nConnection: Keep-Alive\r\nContent-Length: "+ sLen +"\r\n\r\n";

        String sBufferLen = String.format("%x", nLength);
       	sDetail = sBufferLen+"\r\n"+sDetail+"\r\n0\r\n\r\n";
        debugMessage(sProgram, sProfile, String.format("Sending Response to Client Header Data=[%d][%s]", sHead.length(), sHead));
        debugMessage(sProgram, sProfile, String.format("Sending Response to Client Data=[%d][%s]", nLength, sDetail));
        out.print(sHead+sDetail);
        int nLogData = psql.logData(conn, jsend.toString(), "1", sbLOG);
        if (nLogData < 1) {
            debugMessage(sProgram, sProfile, sbLOG.toString());
            System.exit(1);
        }
        out.close();
        debugMessage(sProgram, sProfile, "Client Disconnect");
        return 1;
    }
    
    public static void main( String[] args )
    {
        String sProgram = "REST-SERVER";
        String sProfile = "111111";
        Connection conn = null;
        String sDB = "api";
        String sUser = "root";
        String sPwd = "";
        StringBuilder sbLOG = new StringBuilder();
        try{
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/"+sDB+"?serverTimezone=UTC", sUser,sPwd);
            debugMessage(sProgram, sProfile, String.format("Database Connected to [%s] User [%s] Password [*****]", sDB, sUser));
        }catch (Exception e){
            e.printStackTrace();
            debugMessage(sProgram, sProfile, stackTraceToString(e));
            System.exit(1);
        }
        debugMessage(sProgram, sProfile, String.format("===== Starting Program %s Profile %s =====", sProgram, sProfile));
        Appsql psql = new Appsql();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                String sProgram = "REST-SERVER";
                String sProfile = "111111";
                debugMessage(sProgram, sProfile, "Shutdown detected");
            }
        });
        try {
            ServerSocket server = new ServerSocket(5555);
            debugMessage(sProgram, sProfile, "Listen on Port 5555");
            while(true){
                Socket client = server.accept();
                DataInputStream in = new DataInputStream(client.getInputStream());
                PrintStream out = new PrintStream(client.getOutputStream());
                int read;
                byte[] buff = new byte[10000];
                try {
                    read = in.read(buff);
                } catch (Exception e) {
                    read = -1;
                }
                if(read<0){
                    debugMessage(sProgram, sProfile, "Client Disconnected");
                    break;
                }
                String sMessage = new String(buff, 0, read);
                debugMessage(sProgram, sProfile, String.format("Getting Incoming Client Message=[%s]", sMessage));
                int nLogData = psql.logData(conn, sMessage, "0", sbLOG);
                if(nLogData<1){
                    debugMessage(sProgram, sProfile, sbLOG.toString());
                    System.exit(1);
                }
                int nPos = sMessage.indexOf("{\"MBANKReq");
                if(nPos<0){
                    debugMessage(sProgram, sProfile, "Invalid JSON Request");
                    out.print("Invalid JSON Request");
                    continue;
                    
                }
                String sContent = sMessage.substring(nPos, sMessage.length());
                debugMessage(sProgram, sProfile, String.format("Parse Request Message=[%d][%s]", sContent.length(), sContent));
                JSONObject jdata = new JSONObject(sContent);
                JSONObject jhead = jdata.getJSONObject("MBANKReq");
                String sMTI = jhead.get("mti").toString();
                String sPcode = "";
                debugMessage(sProgram, sProfile, String.format("Parse Request Message MTI=[%s]", sMTI));
                if(sMTI.equals("0800")){
                    debugMessage(sProgram, sProfile, "Doing Network Management Message");
                }
                JSONObject jresp = new JSONObject();
                if(sMTI.equals("0200")){
                    try {
                        sPcode = jhead.get("pcode").toString();
                        debugMessage(sProgram, sProfile, String.format("Parse Request Message Pcode=[%s]", sPcode));
                        String sSTAN = jhead.get("stan").toString();
                        debugMessage(sProgram, sProfile, String.format("Parse Request Message STAN=[%s]", sSTAN));
                        String sGMT = jhead.get("gmt").toString();
                        debugMessage(sProgram, sProfile, String.format("Parse Request Message GMT=[%s]", sGMT));
                        String sRRN = jhead.get("rrn").toString();
                        debugMessage(sProgram, sProfile, String.format("Parse Request Message RRN=[%s]", sRRN));
                        String sTermID = jhead.get("termId").toString();
                        debugMessage(sProgram, sProfile, String.format("Parse Request Message TermID=[%s]", sTermID));
                        if(sMTI.equals("0200")) sMTI = "0210";
                        if(sMTI.equals("0400")) sMTI = "0410";
                        jresp.put("mti", sMTI);
                        jresp.put("pcode", sPcode);
                        jresp.put("stan", sSTAN);
                        jresp.put("rrn", sRRN);
                        jresp.put("gmt", sGMT);
                        jresp.put("termId", sTermID);
                        jresp.put("currCode", "360");
                        String sRcode = "";
                        String sDescr = "";

                        /* *** Login User *** */
                        if(sPcode.equals("000000")){
                            debugMessage(sProgram, sProfile, "***** Doing Login User *****");
                            try{
                                String sUserId = jhead.get("userId").toString();
                                String sPassword = jhead.get("password").toString();
                                debugMessage(sProgram, sProfile, String.format("Parse Request Message userId=[%s]", sUserId));
                                debugMessage(sProgram, sProfile, String.format("Parse Request Message password=[%s]", sPassword));
                                debugMessage(sProgram, sProfile, String.format("Parse Request Message AndroidID=[%s]", sTermID.substring(10, sTermID.length()).trim()));
                                StringBuilder sbNorek = new StringBuilder();
                                StringBuilder sbIsLogin = new StringBuilder();
                                StringBuilder sbTermId = new StringBuilder();
                                StringBuilder sbPassword = new StringBuilder();
                                StringBuilder sbInvalidLogin = new StringBuilder();
                                StringBuilder sbStatus = new StringBuilder();
                                int nGetLoginUser = psql.getLoginUser(conn, sUserId, sbPassword, sbNorek, sbIsLogin, sbTermId, sbInvalidLogin, sbStatus, sbLOG);
                                debugMessage(sProgram, sProfile, sbLOG.toString());
                                if(nGetLoginUser==0){
                                    jresp.put("rCode", "76");
                                    jresp.put("descr", "UserID Salah");
                                    sendResponse(out, jresp.toString(), conn);
                                    continue;
                                }
                                if(nGetLoginUser<0){
                                    jresp.put("rCode", "96");
                                    jresp.put("descr", "System Error");
                                    sendResponse(out, jresp.toString(), conn);
                                    continue;
                                }
                                if(!sbStatus.toString().equals("0")){
                                    jresp.put("rCode", "76");
                                    jresp.put("descr", "UserID Tidak Aktif");
                                    sendResponse(out, jresp.toString(), conn);
                                    continue;
                                }
                                if(Integer.parseInt(sbInvalidLogin.toString())==3){
                                    int nUpdateStatusUser = psql.updateStatusUser(conn, sUserId, sbLOG);
                                    if(nUpdateStatusUser<0){
                                        debugMessage(sProgram, sProfile, sbLOG.toString());
                                        jresp.put("rCode", "96");
                                        jresp.put("descr", "Error Update Status User");
                                        sendResponse(out, jresp.toString(), conn);
                                        continue;
                                    }
                                    jresp.put("rCode", "75");
                                    jresp.put("descr", "Limit Salah Password");
                                    sendResponse(out, jresp.toString(), conn);
                                    continue;
                                }
                                if(!sPassword.equals(sbPassword.toString())){
                                    int nUpdateInvalidLogin = psql.updateInvalidLogin(conn, sUserId, sbLOG);
                                    if(nUpdateInvalidLogin<0){
                                        debugMessage(sProgram, sProfile, sbLOG.toString());
                                        jresp.put("rCode", "96");
                                        jresp.put("descr", "Error Update Invalid Password");
                                        sendResponse(out, jresp.toString(), conn);
                                        continue;
                                    }
                                    jresp.put("rCode", "55");
                                    jresp.put("descr", "Password Salah");
                                    sendResponse(out, jresp.toString(), conn);
                                    continue;
                                }
                                if(sbIsLogin.toString().equals("1")){
                                    jresp.put("rCode", "05");
                                    jresp.put("descr", "Anda sudah login di device lain");
                                    sendResponse(out, jresp.toString(), conn);
                                    continue;
                                }
                                if(!sTermID.substring(10, sTermID.length()).trim().equals(sbTermId.toString()) && !sbTermId.toString().equals("")){
                                    jresp.put("rCode", "55");
                                    jresp.put("descr", "Device Id Tidak sama");
                                    sendResponse(out, jresp.toString(), conn);
                                    continue;
                                }
                                if(sbTermId.toString().equals("")){
                                    int nUpdateDeviceUser = psql.updateDeviceUser(conn, sUserId, sTermID.substring(10, sTermID.length()).trim(), sbLOG);
                                    if(nUpdateDeviceUser<0){
                                        debugMessage(sProgram, sProfile, sbLOG.toString());
                                        jresp.put("rCode", "96");
                                        jresp.put("descr", "System Error");
                                        sendResponse(out, jresp.toString(), conn);
                                        continue;
                                    }
                                }
                                int nUpdateLoginUser = psql.updateLoginUser(conn, sUserId, "1", sbLOG);
                                if(nUpdateLoginUser<1){
                                    debugMessage(sProgram, sProfile, sbLOG.toString());
                                    System.exit(1);
                                }
                                // Thread.sleep(5000);
                                sRcode = "00";
                                sDescr = "Login Berhasil";
                                jresp.put("rCode", sRcode);
                                jresp.put("descr", sDescr);
                                jresp.put("accNo", sbNorek.toString());
                                sendResponse(out, jresp.toString(), conn);
                                continue;
                            }catch(Exception e){
                                e.printStackTrace();
                                debugMessage(sProgram, sProfile, stackTraceToString(e));
                                jresp.put("rCode", "96");
                                jresp.put("descr", "Exception Error");
                                sendResponse(out, jresp.toString(), conn);
                                continue;
                            }
                        }

                        /* *** Logout User *** */
                        if (sPcode.equals("000001")) {
                            debugMessage(sProgram, sProfile, "***** Doing Logout User *****");
                            try {
                                String sUserId = jhead.get("userId").toString();
                                debugMessage(sProgram, sProfile,
                                String.format("Parse Request Message userId=[%s]", sUserId));
                                int nUpdateLoginUser = psql.updateLoginUser(conn, sUserId, "0", sbLOG);
                                if (nUpdateLoginUser < 1) {
                                    debugMessage(sProgram, sProfile, sbLOG.toString());
                                    System.exit(1);
                                }
                                sRcode = "00";
                                sDescr = "Logout Berhasil";
                                jresp.put("rCode", sRcode);
                                jresp.put("descr", sDescr);
                                sendResponse(out, jresp.toString(), conn);
                                continue;
                            } catch (Exception e) {
                                e.printStackTrace();
                                debugMessage(sProgram, sProfile, stackTraceToString(e));
                                jresp.put("rCode", "96");
                                jresp.put("descr", "Exception Error");
                                sendResponse(out, jresp.toString(), conn);
                                continue;
                            }
                        }

                        /* *** Change PIN *** */
                        if (sPcode.equals("900000")) {
                            debugMessage(sProgram, sProfile, "***** Doing Change PIN *****");
                            try {
                                String sUserId = jhead.get("userId").toString();
                                debugMessage(sProgram, sProfile, String.format("Parse Request Message userId=[%s]", sUserId));
                                String sNewPassword = jhead.get("newPassword").toString();
                                debugMessage(sProgram, sProfile, String.format("Parse Request Message newPassword=[%s]", sNewPassword));
                                int nChangePin = psql.changePin(conn, sUserId, sNewPassword, sbLOG);
                                if (nChangePin < 1) {
                                    debugMessage(sProgram, sProfile, sbLOG.toString());
                                    System.exit(1);
                                }
                                sRcode = "00";
                                sDescr = "Ganti Password Berhasil";
                                jresp.put("rCode", sRcode);
                                jresp.put("descr", sDescr);
                                sendResponse(out, jresp.toString(), conn);
                                continue;
                            } catch (Exception e) {
                                e.printStackTrace();
                                debugMessage(sProgram, sProfile, stackTraceToString(e));
                                jresp.put("rCode", "96");
                                jresp.put("descr", "Exception Error");
                                sendResponse(out, jresp.toString(), conn);
                                continue;
                            }
                        }

                        /* *** Balance Inquiry *** */
                        if(sPcode.equals("300000")){
                            debugMessage(sProgram, sProfile, "***** Doing Balance Inquiry *****");
                            try {
                                String sAccNo = jhead.get("accNo").toString();
                                debugMessage(sProgram, sProfile, String.format("Parse Request Message AccNo=[%s]", sAccNo));
                                StringBuilder sbName = new StringBuilder();
                                StringBuilder sbSaldoMin = new StringBuilder();
                                StringBuilder sbSaldoAwal = new StringBuilder();
                                StringBuilder sbAvailSaldo = new StringBuilder();
                                StringBuilder sbKodeCab = new StringBuilder();
                                StringBuilder sbJenisRek = new StringBuilder();
                                StringBuilder sbStatus = new StringBuilder();
                                StringBuilder sbNoCif = new StringBuilder();
                                int nGetBalanceInquiry = psql.getBalanceInquiry(conn, sAccNo, sbName, sbSaldoMin, sbSaldoAwal, sbAvailSaldo, sbKodeCab, sbJenisRek, sbStatus, sbNoCif, sbLOG);
                                debugMessage(sProgram, sProfile, sbLOG.toString());
                                if(nGetBalanceInquiry<1){
                                    jresp.put("rCode", "76");
                                    jresp.put("descr", "Rekening Tidak Ditemukan");
                                    sendResponse(out, jresp.toString(), conn);
                                    continue;
                                }
                                JSONObject jdetail = new JSONObject();
                                jdetail.put("name", sbName.toString());
                                jdetail.put("saldoMin", sbSaldoMin.toString());
                                jdetail.put("openSaldo", sbSaldoAwal.toString());
                                jdetail.put("availableSaldo", sbAvailSaldo.toString());
                                jdetail.put("kodeCabang", sbKodeCab.toString());
                                jdetail.put("jenisRekening", sbJenisRek.toString());
                                jdetail.put("status", sbStatus.toString());
                                jdetail.put("noCif", sbNoCif.toString());
                                jresp.put("detailData", jdetail);
                                jresp.put("accNo", sAccNo);
                                sRcode = "00";
                                sDescr = "Transaksi Berhasil";
                                jresp.put("rCode", sRcode);
                                jresp.put("descr", sDescr);
                                sendResponse(out, jresp.toString(), conn);
                                continue;
                            } catch (Exception e) {
                                e.printStackTrace();
                                debugMessage(sProgram, sProfile, stackTraceToString(e));
                                jresp.put("rCode", "96");
                                jresp.put("descr", "Exception Error");
                                sendResponse(out, jresp.toString(), conn);
                                continue;
                            }
                        }
                        
                        /* *** Inquiry Transaction History *** */
                        if(sPcode.equals("330000")){
                            debugMessage(sProgram, sProfile, "***** Doing Inquiry Transaction History *****");
                            try{
                                String sAccNo = jhead.get("accNo").toString();
                                debugMessage(sProgram, sProfile, String.format("Parse Request Message AccNo=[%s]", sAccNo));
                                StringBuilder sbKodeTRX = new StringBuilder();
                                StringBuilder sbAmount = new StringBuilder();
                                StringBuilder sbSTAN = new StringBuilder();
                                StringBuilder sbTime = new StringBuilder();
                                StringBuilder sbDate = new StringBuilder();
                                StringBuilder sbRRN = new StringBuilder();
                                StringBuilder sbRcode = new StringBuilder();
                                StringBuilder sbToAcc = new StringBuilder();
                                StringBuilder sbData = new StringBuilder();
                                int nGetHistoryTransaction = psql.getHistoryTransaction(conn, sAccNo, sbData, sbLOG);
                                debugMessage(sProgram, sProfile, sbLOG.toString());
                                if(nGetHistoryTransaction<1){
                                    jresp.put("rCode", "01");
                                    jresp.put("descr", "Data Tidak Ditemukan");
                                    sendResponse(out, jresp.toString(), conn);
                                    continue;
                                }
                                JSONArray ajdata = new JSONArray(sbData.toString());
                                sRcode = "00";
                                sDescr = "Transaksi Berhasil";
                                jresp.put("detailData", ajdata);
                                jresp.put("rCode", sRcode);
                                jresp.put("descr", sDescr);
                                sendResponse(out, jresp.toString(), conn);
                                continue;
                            }catch (Exception e){
                                e.printStackTrace();
                                debugMessage(sProgram, sProfile, stackTraceToString(e));
                                jresp.put("rCode", "96");
                                jresp.put("descr", "Exception Error");
                                sendResponse(out, jresp.toString(), conn);
                                continue;
                            }
                        }

                        /* *** Inquiry Transfer *** */
                        if(sPcode.equals("390000")){
                            debugMessage(sProgram, sProfile, String.format("***** Doing Inquiry Transfer *****"));
                            try{
                                String sAccNo = jhead.get("accNo").toString();
                                debugMessage(sProgram, sProfile, String.format("Parse Request Message accNo=[%s]", sAccNo));
                                String sToAccNo = jhead.get("toAccNo").toString();
                                debugMessage(sProgram, sProfile, String.format("Parse Request Message toAccNo=[%s]", sToAccNo));
                                StringBuilder sbFromName = new StringBuilder();
                                StringBuilder sbFromStatus = new StringBuilder();
                                StringBuilder sbFromAvailSaldo = new StringBuilder();
                                int nGetFromAccNo = psql.getInqRek(conn, sAccNo, sbFromName, sbFromStatus, sbFromAvailSaldo, sbLOG);
                                debugMessage(sProgram, sProfile, sbLOG.toString());
                                StringBuilder sbToName = new StringBuilder();
                                StringBuilder sbToStatus = new StringBuilder();
                                StringBuilder sbToAvailSaldo = new StringBuilder();
                                int nGetToAccNo = psql.getInqRek(conn, sToAccNo, sbToName, sbToStatus, sbToAvailSaldo, sbLOG);
                                debugMessage(sProgram, sProfile, sbLOG.toString());
                                if(nGetFromAccNo<0 | nGetToAccNo<0){
                                    jresp.put("rCode", "96");
                                    jresp.put("descr", "System Error");
                                    sendResponse(out, jresp.toString(), conn);
                                    continue;
                                }
                                if(nGetFromAccNo==0){
                                    jresp.put("rCode", "76");
                                    jresp.put("descr", "Rekening Sumber Tidak Terdaftar");
                                    sendResponse(out, jresp.toString(), conn);
                                    continue;
                                }
                                if(nGetToAccNo==0){
                                    jresp.put("rCode", "76");
                                    jresp.put("descr", "Rekening Tujuan Tidak Terdaftar");
                                    sendResponse(out, jresp.toString(), conn);
                                    continue;
                                }
                                if(!sbFromStatus.toString().equals("0")){
                                    jresp.put("rCode", "77");
                                    jresp.put("descr", "Rekening Sumber Tidak Aktif");
                                    sendResponse(out, jresp.toString(), conn);
                                    continue;
                                }
                                if(!sbToStatus.toString().equals("0")){
                                    jresp.put("rCode", "76");
                                    jresp.put("descr", "Rekening Tujuan Tidak Aktif");
                                    sendResponse(out, jresp.toString(), conn);
                                    continue;
                                }
                                JSONObject jdetail = new JSONObject();
                                jdetail.put("toAccName", sbToName.toString());
                                jdetail.put("toAccStatus", sbToStatus.toString());
                                jresp.put("detailData", jdetail);
                                jresp.put("accNo", sAccNo);
                                jresp.put("toAccNo", sToAccNo);
                                jresp.put("rCode", "00");
                                jresp.put("descr", "Transaksi Berhasil");
                                sendResponse(out, jresp.toString(), conn);
                                continue;
                            }catch(Exception e){
                                e.printStackTrace();
                                debugMessage(sProgram, sProfile, stackTraceToString(e));
                                jresp.put("rCode", "96");
                                jresp.put("descr", "Exception Error");
                                sendResponse(out, jresp.toString(), conn);
                                continue;
                            }
                        }

                        /* *** Posting Transfer *** */
                        if (sPcode.equals("400000")) {
                            debugMessage(sProgram, sProfile, String.format("***** Doing Posting Transfer *****"));
                            double nAmount = 0;
                            try {
                                String sAmount = jhead.get("amount").toString();
                                debugMessage(sProgram, sProfile, String.format("Parse Request Message amount=[%s]", sAmount));
                                try {
                                    nAmount = Double.parseDouble(sAmount);
                                    if(nAmount==0){
                                        jresp.put("rCode", "13");
                                        jresp.put("descr", "Amount Tidak Valid");
                                        sendResponse(out, jresp.toString(), conn);
                                        continue;
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    debugMessage(sProgram, sProfile, stackTraceToString(e));
                                    jresp.put("rCode", "96");
                                    jresp.put("descr", "Exception Error Convert Amount");
                                    sendResponse(out, jresp.toString(), conn);
                                    continue;
                                }
                                String sAccNo = jhead.get("accNo").toString();
                                debugMessage(sProgram, sProfile, String.format("Parse Request Message accNo=[%s]", sAccNo));
                                String sToAccNo = jhead.get("toAccNo").toString();
                                debugMessage(sProgram, sProfile, String.format("Parse Request Message toAccNo=[%s]", sToAccNo));
                                StringBuilder sbFromName = new StringBuilder();
                                StringBuilder sbFromStatus = new StringBuilder();
                                StringBuilder sbFromAvailSaldo = new StringBuilder();
                                int nGetFromAccNo = psql.getInqRek(conn, sAccNo, sbFromName, sbFromStatus, sbFromAvailSaldo, sbLOG);
                                debugMessage(sProgram, sProfile, sbLOG.toString());
                                StringBuilder sbToName = new StringBuilder();
                                StringBuilder sbToStatus = new StringBuilder();
                                StringBuilder sbToAvailSaldo = new StringBuilder();
                                int nGetToAccNo = psql.getInqRek(conn, sToAccNo, sbToName, sbToStatus, sbToAvailSaldo, sbLOG);
                                debugMessage(sProgram, sProfile, sbLOG.toString());
                                if (nGetFromAccNo < 0 | nGetToAccNo < 0) {
                                    jresp.put("rCode", "96");
                                    jresp.put("descr", "System Error");
                                    sendResponse(out, jresp.toString(), conn);
                                    continue;
                                }
                                if (nGetFromAccNo == 0) {
                                    jresp.put("rCode", "76");
                                    jresp.put("descr", "Rekening Sumber Tidak Terdaftar");
                                    sendResponse(out, jresp.toString(), conn);
                                    continue;
                                }
                                if (nGetToAccNo == 0) {
                                    jresp.put("rCode", "76");
                                    jresp.put("descr", "Rekening Tujuan Tidak Terdaftar");
                                    sendResponse(out, jresp.toString(), conn);
                                    continue;
                                }
                                if (!sbFromStatus.toString().equals("0")) {
                                    jresp.put("rCode", "77");
                                    jresp.put("descr", "Rekening Sumber Tidak Aktif");
                                    sendResponse(out, jresp.toString(), conn);
                                    continue;
                                }
                                if (!sbToStatus.toString().equals("0")) {
                                    jresp.put("rCode", "76");
                                    jresp.put("descr", "Rekening Tujuan Tidak Aktif");
                                    sendResponse(out, jresp.toString(), conn);
                                    continue;
                                }

                                double nFromAvailSaldo = Double.parseDouble(sbFromAvailSaldo.toString());
                                if(nFromAvailSaldo < nAmount){
                                    jresp.put("rCode", "51");
                                    jresp.put("descr", "Saldo Tidak Cukup");
                                    sendResponse(out, jresp.toString(), conn);
                                    continue;
                                }
                                JSONObject jdetail = new JSONObject();
                                jdetail.put("toAccName", sbToName.toString());
                                jdetail.put("toAccStatus", sbToStatus.toString());
                                jresp.put("detailData", jdetail);
                                jresp.put("accNo", sAccNo);
                                jresp.put("toAccNo", sToAccNo);
                                jresp.put("rCode", "00");
                                jresp.put("descr", "Transaksi Berhasil");
                                sendResponse(out, jresp.toString(), conn);
                                continue;
                            } catch (Exception e) {
                                e.printStackTrace();
                                debugMessage(sProgram, sProfile, stackTraceToString(e));
                                jresp.put("rCode", "96");
                                jresp.put("descr", "Exception Error");
                                sendResponse(out, jresp.toString(), conn);
                                continue;
                            }
                        }
                    } catch (Exception e) {
                       e.printStackTrace();
                       debugMessage(sProgram, sProfile, stackTraceToString(e));
                       System.exit(1);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            debugMessage(sProgram, sProfile, stackTraceToString(e));
            System.exit(1);
        }
    }
}
