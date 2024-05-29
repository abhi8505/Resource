package resources;

/**
 * ------------------@author AYUSH BHARDWAJ (1788364)-------------------
 * -----------This Script is Used to Create a Regression Report---------
 * -----------After Creating it will send the report by mail------------
 * -----------Report includes 3 sheets in excel .xlsx format------------
 * -----------1st sheet is Pie Chart showing overall result-------------
 * -----------2nd sheet is Test Case wise Details of Regression---------
 * -----------3rd sheet is Script wise Details of the Regression--------
 * ---------------------------------------------------------------------
 * ---------------------------------------------------------------------
 *
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang.SystemUtils;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xddf.usermodel.chart.ChartTypes;
import org.apache.poi.xddf.usermodel.chart.LegendPosition;
import org.apache.poi.xddf.usermodel.chart.XDDFChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFChartLegend;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSourcesFactory;
import org.apache.poi.xddf.usermodel.chart.XDDFNumericalDataSource;
import org.apache.poi.xssf.usermodel.XSSFChart;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class Regression_BUILD_Report {

	public static ArrayList<String> startTime = new ArrayList<String>();
	public static ArrayList<String> endTime = new ArrayList<String>();
	public static String successCount = "";
	public static String failedCount = "";
	public static String totalCount = "";
	public static int[] passfailhold = { 0, 0, 0 };
	public static int[] reSubmitStat = { 0, 0, 0 ,0,0}; 
	public static String executionStartTime = "";

	public static XSSFWorkbook workbook = new XSSFWorkbook();

	public static String FILE_SAVE_LOCATION() {
		String l2 = null;
		if (SystemUtils.IS_OS_LINUX) {
			String checkPath = System.getProperty("user.dir") + "/NRTDetails/Regression_BUILD_Report";
			File f = new File(checkPath);
			Boolean check = f.exists();
			if (check) {
				l2 = System.getProperty("user.dir") + "/NRTDetails/Regression_BUILD_Report/";
			} else {
				l2 = System.getProperty("user.dir") + "/Regression_BUILD_Report/";
			}
			System.out.println("LINUX2");
		} else
			l2 = System.getProperty("user.dir") + "\\Regression_BUILD_Report\\";
		return l2;
	}

	public static String FILE_NAME() {
		String file_name = AC_MAIN_V.REG_PKG.trim() + ".xlsx";
		try {
			file_name = AC_MAIN_V.REG_PKG.trim().replace(" ", "_") + ".xlsx";
		} catch (Exception e) {
			System.out.println("Unable to replace the blank spaces.");
		}
		return file_name;
	}

	public static void Build_Regression_Report(Connection conn)
			throws Test_Case_Exit_Exception, IOException, SQLException, InterruptedException {

		Connection dbConnection = Utility.createDataBaseConnection();
		AC_MAIN_V.db2 = true;
		conn = Utility.createDataBaseConnection();
		AC_MAIN_V.db2 = false;

		String query1 = "select rgrsn_sut_id from rgrsn_dtls where rgrsn_pkg_name='" + AC_MAIN_V.REG_PKG
				+ "' and env_part_id='" + ENV_Variables.ENV_PART_ID + "' ";
		String tag1 = "RGRSN_SUT_ID";

		String suit_id = Run_Query(dbConnection, query1, tag1);
		System.out.println("Regression Suit-Id is::" + suit_id);

		String query11 = "select run from rgr_rpt_data where rgrsn_sut_id='" + suit_id + "' and env_part_id='" + ENV_Variables.ENV_PART_ID + "' ";
		PreparedStatement ps2 = dbConnection.prepareStatement(query11);
		ResultSet rs2 = ps2.executeQuery();
		int success = 0;
		int failure = 0;
		int others = 0;
		int total = 0;
		String value = "";
		while (rs2.next()) {
			total += 1;
			value = rs2.getString("RUN");
			if (value.equals("D")) {
				success += 1;
			} else if (value.equals("T")) {
				failure += 1;
			} else {
				others += 1;
			}
		}
		String dd = Integer.toString(success);
		String tt = Integer.toString(failure);
		String oo = Integer.toString(others);
		String tot = Integer.toString(total);

		successCount = dd;
		failedCount = tt;
		totalCount = tot;

		System.out.println("Success / Failure / Others / Total :: " + dd + " / " + tt + " / " + oo + " / " + tot);

		System.out.println("Creating Directory.");

		create_dir(); // Creating Directory (C:\\NRTDetails\\Regression_BUILD_Report)

		try {
			PieChart_Firstsheet(dd, tt, oo, AC_MAIN_V.REG_PKG); // success cases, failed cases, other cases, regression
																// pack name
		} catch (Exception e) {
			System.out.println("Error while creating the Report. - 1 Sheet");
			e.printStackTrace();
		}

		try {
			TestData_SecondSheet(suit_id, dbConnection);
		} catch (Exception e) {
			System.out.println("Error while creating the Report. - 2 Sheet");
			e.printStackTrace();
		}

		try {
			Report_ThirdSheet(suit_id, dbConnection);
		} catch (Exception e) {
			System.out.println("Error while creating the Report. - 3 Sheet");
			e.printStackTrace();
		}

		boolean holdflag = false;
		try {
			holdflag = checkHoldStatus(dbConnection);
			System.out.println("Hold Flag is : " + holdflag);
		} catch (Exception e) {
			System.out.println("Error while checking the hold status.");
		}

		try {
			Send_Mail_BUILD_Regression.send_mail_BUILD(holdflag);
		} catch (Exception e) {
			System.out.println("Error while sending the mail.");
		}

	}

	/**
	 * @throws SQLException 
	 * ---------Added By Ayush (5-Feb-24) for resubmit status----------------------------
	 * 
	 * @param 
	 * @return 
	 * @throws
	 */
	public static boolean checkResubmitStatus(Connection conn) throws SQLException {
		
		boolean stat = false;
		String query = "select RUN from test_case_dtls where ENV_PART_ID = '" + ENV_Variables.ENV_PART_ID
				+ "' and TEST_CASE_NUM in (select test_case_num from rgrsn_test_dtls where RGRSN_SUT_ID = (select rgrsn_sut_id from rgrsn_dtls where rgrsn_pkg_name='"
				+ AC_MAIN_V.REG_PKG + "' and env_part_id='" + ENV_Variables.ENV_PART_ID + "')) ";

		String query2 = "select RUN,EXECUTION_TIME from rgrsn_dtls where rgrsn_pkg_name='" + AC_MAIN_V.REG_PKG + "' and env_part_id='"
				+ ENV_Variables.ENV_PART_ID + "' ";


		String query3="select run from dp_main where test_case_num in (select rtd.test_case_num from rgrsn_test_dtls rtd join rgrsn_dtls rd on rd.rgrsn_sut_id=rtd.rgrsn_sut_id and rd.ENV_PART_ID=rtd.ENV_PART_ID"
				+ " where rd.rgrsn_pkg_name = '"+AC_MAIN_V.REG_PKG+"') and env_part_id='"+ENV_Variables.ENV_PART_ID+"' ";
		
		System.out.println("Query1: " + query);
		System.out.println("Query2: " + query2);
		System.out.println("Query3: " + query3);
		
		PreparedStatement pstmt3 = conn.prepareStatement(query3);
		ResultSet rs3 = pstmt3.executeQuery();

		boolean y_stat = false;
		boolean d_stat = false;
		boolean t_stat = false;
		while (rs3.next()) {
			if (rs3.getString(1).contains("Y")) {
				y_stat = true;
			}
			if (rs3.getString(1).contains("D")) {
				d_stat = true;
			}
			if (rs3.getString(1).contains("T")) {
				t_stat = true;
			}
			if (y_stat && (d_stat || t_stat )) {
				break;
			}
			
		}
		rs3.close();
		pstmt3.close();
		
		if(y_stat && (d_stat || t_stat )) {
			stat = true;
		}
		
		PreparedStatement pstmt2 = conn.prepareStatement(query);
		ResultSet rs1 = pstmt2.executeQuery();

		String run = null;
if(stat) {
		while (rs1.next()) {
			reSubmitStat[0] += 1;
			run = rs1.getString(1);
			if (run.contains("D")) {
				reSubmitStat[1] += 1;
			} else if (run.contains("T")) {
				reSubmitStat[2] += 1;
			} else if (run.contains("H")) {
				reSubmitStat[3] += 1;
			}
			else if (run.contains("Y")) {
				reSubmitStat[4] += 1;
			}
		} 
}
		
		rs1.close();
		pstmt2.close();

		PreparedStatement pstmt1 = conn.prepareStatement(query2);
		ResultSet rs2 = pstmt1.executeQuery();

		
		while (rs2.next()) {
			executionStartTime = rs2.getString(2);
		}
		rs2.close();
		pstmt1.close();

		return stat;
	}
	
	public static boolean checkHoldStatus(Connection conn) throws SQLException {
		boolean stat = false;

		String query = "select RUN from test_case_dtls where ENV_PART_ID = '" + ENV_Variables.ENV_PART_ID
				+ "' and TEST_CASE_NUM in (select test_case_num from rgrsn_test_dtls where RGRSN_SUT_ID = (select rgrsn_sut_id from rgrsn_dtls where rgrsn_pkg_name='"
				+ AC_MAIN_V.REG_PKG + "' and env_part_id='" + ENV_Variables.ENV_PART_ID + "')) ";

		String query2 = "select RUN,EXECUTION_TIME from rgrsn_dtls where rgrsn_pkg_name='" + AC_MAIN_V.REG_PKG + "' and env_part_id='"
				+ ENV_Variables.ENV_PART_ID + "' ";

		PreparedStatement pstmt2 = conn.prepareStatement(query);
		ResultSet rs1 = pstmt2.executeQuery();

		String run = null;

		while (rs1.next()) {
			run = rs1.getString(1);
			if (run.contains("D")) {
				passfailhold[0] += 1;
			} else if (run.contains("T")) {
				passfailhold[1] += 1;
			} else if (run.contains("H")) {
				passfailhold[2] += 1;
			}
		} 
		rs1.close();
		pstmt2.close();

		PreparedStatement pstmt1 = conn.prepareStatement(query2);
		ResultSet rs2 = pstmt1.executeQuery();

		String regressionStat = null;
		while (rs2.next()) {
			regressionStat = rs2.getString(1);
			executionStartTime = rs2.getString(2);
		}
		rs2.close();
		pstmt1.close();

		if (regressionStat.contains("H")) {
			stat = true;
		}

		return stat;
	}

	@SuppressWarnings("unused")
	public static void Report_ThirdSheet(String suit_id, Connection dbConnection) throws IOException, SQLException {

		String[] header = { "Test Case Name", "Seq. No", "Step Name", "Step Description", "Status", "Execution Time" };

		XSSFSheet spreadsheet = workbook.createSheet("Report");

		spreadsheet.createFreezePane(0, 1);

		CellStyle borderStyle = workbook.createCellStyle();
		CellStyle borderline = workbook.createCellStyle();
		borderStyle.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
		borderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		borderStyle.setBorderBottom(BorderStyle.THIN);
		borderStyle.setBottomBorderColor(IndexedColors.WHITE.getIndex());
		borderStyle.setBorderLeft(BorderStyle.THIN);
		borderStyle.setLeftBorderColor(IndexedColors.WHITE.getIndex());
		borderStyle.setBorderRight(BorderStyle.THIN);
		borderStyle.setRightBorderColor(IndexedColors.WHITE.getIndex());
		borderStyle.setBorderTop(BorderStyle.THIN);
		borderStyle.setTopBorderColor(IndexedColors.WHITE.getIndex());
		borderline.setBorderBottom(BorderStyle.THIN);
		borderline.setBorderLeft(BorderStyle.THIN);
		borderline.setBorderRight(BorderStyle.THIN);
		borderline.setBorderTop(BorderStyle.THIN);

		Font font = workbook.createFont();
		font.setFontHeightInPoints((short) 12);
		font.setFontName("Calibri");
		font.setBold(true);
		font.setColor(IndexedColors.WHITE.getIndex());

		Font font2 = workbook.createFont();
		font2.setFontHeightInPoints((short) 12);
		font2.setFontName("Calibri");

		borderStyle.setFont(font);
		borderline.setFont(font2);
		borderStyle.setAlignment(HorizontalAlignment.CENTER);
		borderStyle.setVerticalAlignment(VerticalAlignment.CENTER);

		Row row1 = spreadsheet.createRow(0);
		for (int i = 0; i <= 5; ++i) {
			Cell cell = row1.createCell(i);
			cell.setCellStyle(borderStyle);
			cell.setCellValue(header[i]);
			spreadsheet.autoSizeColumn(i);
		}

		ArrayList<String> testcases_name = new ArrayList<String>();
		ArrayList<String> testcases_name_multiple = new ArrayList<String>();
		ArrayList<String> seq_no = new ArrayList<String>();
		ArrayList<String> step_name = new ArrayList<String>();
		ArrayList<String> status = new ArrayList<String>();
		ArrayList<String> execution_time = new ArrayList<String>();
		ArrayList<String> description = new ArrayList<String>();

		String query2 = "select * from rgrsn_test_dtls where RGRSN_SUT_ID='" + suit_id + "' order by seq_no ";
		PreparedStatement ps2 = dbConnection.prepareStatement(query2);
		ResultSet rs2 = ps2.executeQuery();

		int act_size = 0;
		String value = "";
		while (rs2.next()) { // Regression_Test_Details..
			value = rs2.getString("TEST_CASE_NUM");
			testcases_name.add(value);
		}
		rs2.close();
		ps2.close();
		int testcase_size = testcases_name.size();
		for (int p = 0; p < testcase_size; p++) { // DP Main
			String query3 = "select * from DP_MAIN WHERE TEST_CASE_NUM='" + testcases_name.get(p)
					+ "' and ENV_PART_ID = '" + ENV_Variables.ENV_PART_ID + "' order by seq_no ";
			int seq = 0;
			String text = "";
			String testcase = testcases_name.get(p);
			PreparedStatement ps3 = dbConnection.prepareStatement(query3);
			ResultSet rs3 = ps3.executeQuery();
			while (rs3.next()) {
				act_size += 1;
				text = rs3.getString("SCRIPT_NAME");
				step_name.add(text);
				String query9 = "select DESCRIPTION from input_params where SCRIPT_NAME='" + text + "' ";
				PreparedStatement ps9 = dbConnection.prepareStatement(query9);
				ResultSet rs9 = ps9.executeQuery();
				while (rs9.next()) {
					text = rs9.getString("DESCRIPTION");
					break;
				}
				rs9.close();
				description.add(text);
				seq += 1;
				text = Integer.toString(seq);
				seq_no.add(text);
				testcases_name_multiple.add(testcase);
				text = rs3.getString("RUN");
				if (text.equals("D")) {
					text = "Successful";
				}
				if (text.equals("T")) {
					text = "Terminated";
				}
				if (text.equals("Y")) {
					text = "Ready";
				}
				if (text.equals("N")) {
					text = "New";
				}
				status.add(text);
				text = rs3.getString("EXECUTION_TIME");
				if (text == null || text.isEmpty() || text.equals("null")) {
					text = "";
				}
				execution_time.add(text);
			}
			rs3.close();
			ps3.close();
		}

		XSSFRow row;

		Map<String, Object[]> studentData = new TreeMap<String, Object[]>();

		for (int k = 1; k <= act_size; k++) {
			String dup = Integer.toString(k);
			studentData.put(dup, new Object[] { testcases_name_multiple.get(k - 1), seq_no.get(k - 1),
					step_name.get(k - 1), description.get(k - 1), status.get(k - 1), execution_time.get(k - 1) });
		}

		Set<String> keyid = studentData.keySet();

		int rowid = 1;
		int n = 1;

		// writing the data into the sheets...

		for (String key : keyid) {
			String num = Integer.toString(n);
			n += 1;
			row = spreadsheet.createRow(rowid++);
			Object[] objectArr = studentData.get(num);
			int cellid = 0;

			for (Object obj : objectArr) {
				Cell cell = row.createCell(cellid++);
				cell.setCellValue((String) obj);
				cell.setCellStyle(borderline);
			}
		}

		String filename = FILE_NAME();
		String filelocation = FILE_SAVE_LOCATION();
		System.out.println("Address : " + filelocation + filename);

		FileOutputStream out = new FileOutputStream(new File(filelocation + filename));

		workbook.write(out);
		out.close();
		workbook.close();

		System.out.println("Processing of 3rd Sheet done.");

	}

	public static void TestData_SecondSheet(String suit_id, Connection dbConnection) throws IOException, SQLException {

		String[] header = { "Test Case Name", "Description", "Test Case Id", "Status", "Entity", "Business Area",
				"Order/Event-Id", "Start Time", "End Time", "Total Execution Time", "User Instance", "Termination Step",
				"Failure Reason" };

		XSSFSheet spreadsheet = workbook.createSheet("Test Cases");

		spreadsheet.createFreezePane(0, 1);

		CellStyle borderStyle = workbook.createCellStyle();
		CellStyle borderline = workbook.createCellStyle();
		borderStyle.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
		borderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		borderStyle.setBorderBottom(BorderStyle.THIN);
		borderStyle.setBottomBorderColor(IndexedColors.WHITE.getIndex());
		borderStyle.setBorderLeft(BorderStyle.THIN);
		borderStyle.setLeftBorderColor(IndexedColors.WHITE.getIndex());
		borderStyle.setBorderRight(BorderStyle.THIN);
		borderStyle.setRightBorderColor(IndexedColors.WHITE.getIndex());
		borderStyle.setBorderTop(BorderStyle.THIN);
		borderStyle.setTopBorderColor(IndexedColors.WHITE.getIndex());
		borderline.setBorderBottom(BorderStyle.THIN);
		borderline.setBorderLeft(BorderStyle.THIN);
		borderline.setBorderRight(BorderStyle.THIN);
		borderline.setBorderTop(BorderStyle.THIN);

		Font font = workbook.createFont();
		font.setFontHeightInPoints((short) 12);
		font.setFontName("Calibri");
		font.setBold(true);
		font.setColor(IndexedColors.WHITE.getIndex());

		Font font2 = workbook.createFont();
		font2.setFontHeightInPoints((short) 12);
		font2.setFontName("Calibri");

		borderStyle.setFont(font);
		borderline.setFont(font2);
		borderStyle.setAlignment(HorizontalAlignment.CENTER);
		borderStyle.setVerticalAlignment(VerticalAlignment.CENTER);

		Row row1 = spreadsheet.createRow(0);
		for (int i = 0; i <= 12; ++i) {
			Cell cell = row1.createCell(i);
			cell.setCellStyle(borderStyle);
			cell.setCellValue(header[i]);
			spreadsheet.autoSizeColumn(i);
		}

		ArrayList<String> testcases_name = new ArrayList<String>();
		ArrayList<String> testcase_id = new ArrayList<String>();
		ArrayList<String> descrptn = new ArrayList<String>();
		ArrayList<String> status = new ArrayList<String>();
		ArrayList<String> entity = new ArrayList<String>();
		ArrayList<String> businesssArea = new ArrayList<String>();
		ArrayList<String> Order_ID = new ArrayList<String>();
		ArrayList<String> StartTime = new ArrayList<String>();
		ArrayList<String> EndTime = new ArrayList<String>();
		ArrayList<String> TotalTime = new ArrayList<String>();
		ArrayList<String> UserInstance = new ArrayList<String>();
		ArrayList<String> TerminateStep = new ArrayList<String>();
		ArrayList<String> FailureReason = new ArrayList<String>();

		String query2 = "select * from rgrsn_test_dtls where RGRSN_SUT_ID='" + suit_id + "' order by seq_no ";
		PreparedStatement ps2 = dbConnection.prepareStatement(query2);
		ResultSet rs2 = ps2.executeQuery();

		String value = "";
		while (rs2.next()) { // Regression_Test_Details..
			value = rs2.getString("TEST_CASE_NUM");
			testcases_name.add(value);
			value = rs2.getString("TEST_CASE_ID");
			testcase_id.add(value);

		}
		rs2.close();
		ps2.close();
		int testcase_size = testcases_name.size();
		for (int p = 0; p <= testcase_size; p++) {
			String query3 = "select * from rgr_rpt_data where test_case_num='" + testcases_name.get(p)
					+ "' and rgrsn_sut_id='" + suit_id + "' ";
			String query4 = "select * from test_case_dtls where test_case_num='" + testcases_name.get(p)
					+ "' and env_part_id='" + ENV_Variables.ENV_PART_ID + "' ";
			PreparedStatement ps3 = dbConnection.prepareStatement(query3);
			PreparedStatement ps4 = dbConnection.prepareStatement(query4);
			ResultSet rs3 = ps3.executeQuery();
			ResultSet rs4 = ps4.executeQuery();
			String text = "";
			boolean terminated = false;
			while (rs3.next()) { // RGR_RPT_DATA
				terminated = false;
				text = rs3.getString("TEST_CASE_NUM");
				testcases_name.add(text);
				text = rs3.getString("RUN");
				if (text.equals("D")) {
					text = "Successful";
				}
				if (text.equals("T")) {
					text = "Terminated";
					terminated = true;
				}
				if (text.equals("Y")) {
					text = "Ready";
				}
				if (text.equals("N")) {
					text = "New";
				}
				if (text.equals("H")) {
					text = "Hold";
				}
				status.add(text);
				text = rs3.getString("ORDER_ID");
				if (text == null || text.isEmpty() || text.equals("null") || text.toLowerCase().contains("null")) {
					text = "";
				}
				Order_ID.add(text);
				text = rs3.getString("STRT_TIME");
				StartTime.add(text);
				text = rs3.getString("END_TIME");
				EndTime.add(text);
				text = rs3.getString("TOTAL_EXEC_TIME");
				if (text == null || text.isEmpty() || text.equals("null") || text.toLowerCase().contains("null")) {
					text = "";
				}
				TotalTime.add(text);
				text = rs3.getString("USERNAME");
				UserInstance.add(text);
				if (terminated) {
					text = rs3.getString("TRMINATION_STEP");
					TerminateStep.add(text);
					text = rs3.getString("FAILURE_REASON");
					FailureReason.add(text);
				} else {
					TerminateStep.add("");
					FailureReason.add("");
				}
			}
			rs3.close();
			ps3.close();
			while (rs4.next()) { // Test_case_dtls
				text = rs4.getString("TEST_DESCRIPTION");
				descrptn.add(text);
				text = rs4.getString("ENTITY");
				entity.add(text);
				text = rs4.getString("BUSINESS_AREA");
				businesssArea.add(text);
			}
			rs4.close();
			ps4.close();
		}

		XSSFRow row;

		startTime = StartTime;
		endTime = EndTime;

		Map<String, Object[]> studentData = new TreeMap<String, Object[]>();

		try {
			for (int k = 1; k <= testcase_size; k++) {
				String dup = Integer.toString(k);
				studentData.put(dup,
						new Object[] { testcases_name.get(k - 1), descrptn.get(k - 1), testcase_id.get(k - 1),
								status.get(k - 1), entity.get(k - 1), businesssArea.get(k - 1), Order_ID.get(k - 1),
								StartTime.get(k - 1), EndTime.get(k - 1), TotalTime.get(k - 1), UserInstance.get(k - 1),
								TerminateStep.get(k - 1), FailureReason.get(k - 1) });
				// System.out.println(k);
				// System.out.println(studentData);

			}
		} catch (Exception ee) {
			System.out.println("inside catch");
		}

		Set<String> keyid = studentData.keySet();

		int rowid = 1;

		for (String key : keyid) {

			row = spreadsheet.createRow(rowid++);
			Object[] objectArr = studentData.get(key);
			int cellid = 0;

			for (Object obj : objectArr) {
				Cell cell = row.createCell(cellid++);
				cell.setCellValue((String) obj);
				cell.setCellStyle(borderline);
			}
		}

		String filename = FILE_NAME();
		String filelocation = FILE_SAVE_LOCATION();
		System.out.println("Address : " + filelocation + filename);

		FileOutputStream out = new FileOutputStream(new File(filelocation + filename));

		workbook.write(out);
		out.close();
		// workbook.close();

		System.out.println("Processing of second sheet done.");
	}

	@SuppressWarnings("static-access")
	public static void PieChart_Firstsheet(String success, String failed, String oth, String package_name)
			throws FileNotFoundException, IOException {

		// creating workbook
		String totalcases = "";
		int pass = 0;
		int fail = 0;
		int others = 0;
		int total = 0;
		int percentage = 0;
		try {
			pass = Integer.parseInt(success);
			fail = Integer.parseInt(failed);
			others = Integer.parseInt(oth);
			total = pass + fail + others;
			totalcases = Integer.toString(total);
			percentage = (pass * 100) / total;

		} catch (Exception e) {
			System.out.println("Unable to pase the int.");
		}
		// XSSFWorkbook workbook = new XSSFWorkbook();
		// creating sheet with name "Report" in workbook
		XSSFSheet sheet = workbook.createSheet("OverView");

		CellStyle borderStyle = workbook.createCellStyle();
		borderStyle.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
		borderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

		Font font = workbook.createFont();
		font.setFontHeightInPoints((short) 14);
		font.setFontName("Calibri");
		font.setBold(true);
		font.setUnderline(Font.U_SINGLE);
		font.setColor(IndexedColors.BLACK.GOLD.getIndex());

		borderStyle.setFont(font);
		borderStyle.setAlignment(HorizontalAlignment.CENTER);
		borderStyle.setVerticalAlignment(VerticalAlignment.CENTER);

		for (int j = 0; j <= 3; ++j) {
			Row row = sheet.createRow(j);
			for (int i = 2; i <= 14; ++i) {
				Cell cell = row.createCell(i);
				cell.setCellStyle(borderStyle);
				if (j == 0 && i == 2) {
					cell.setCellValue("RESULT OF REGRESSION SUITE :- " + package_name);
				}
			}
		}
		sheet.addMergedRegion(new CellRangeAddress(0, 3, 2, 14));

		CellStyle borderStyle21 = workbook.createCellStyle();
		borderStyle21.setFillForegroundColor(IndexedColors.BLACK.getIndex());
		borderStyle21.setFillPattern(FillPatternType.SOLID_FOREGROUND);

		Font font21 = workbook.createFont();
		font21.setFontHeightInPoints((short) 12);
		font21.setFontName("Calibri");
		font21.setBold(true);
		font21.setColor(IndexedColors.LIGHT_TURQUOISE.getIndex());

		borderStyle21.setFont(font21);
		borderStyle21.setAlignment(HorizontalAlignment.CENTER);
		borderStyle21.setVerticalAlignment(VerticalAlignment.CENTER);

		for (int j = 4; j <= 5; ++j) {
			Row row = sheet.createRow(j);
			for (int i = 2; i <= 14; ++i) {
				Cell cell = row.createCell(i);
				cell.setCellStyle(borderStyle21);
				if (j == 4 && i == 2) {
					cell.setCellValue("Success Ratio - " + percentage + " %");
				}
			}
		}
		sheet.addMergedRegion(new CellRangeAddress(4, 5, 2, 14));

		XSSFDrawing drawing = sheet.createDrawingPatriarch();

		XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 2, 20, 15, 6);

		XSSFChart chart = drawing.createChart(anchor);
		chart.setTitleText("Test Results of " + totalcases + " Cases.");

		XDDFChartLegend legend = chart.getOrAddLegend();
		legend.setPosition(LegendPosition.TOP_RIGHT);
		String[] legendData = { "Successful (" + success + ")", "Failed (" + failed + ")", "Others (" + oth + ")" };
		XDDFDataSource<String> testOutcomes = XDDFDataSourcesFactory.fromArray(legendData);
		Integer[] numericData = { pass, fail, others };
		XDDFNumericalDataSource<Integer> values = XDDFDataSourcesFactory.fromArray(numericData);

		XDDFChartData data = chart.createData(ChartTypes.PIE3D, null, null);

		chart.displayBlanksAs(null);
		data.setVaryColors(true);
		data.addSeries(testOutcomes, values);

		try {
			chart.plot(data);
		} catch (Exception e) {
			e.printStackTrace();
		}
		String filename = FILE_NAME();
		String filelocation = FILE_SAVE_LOCATION();
		System.out.println("Address : " + filelocation + filename);

		try (FileOutputStream outputStream = new FileOutputStream(filelocation + filename)) {
			workbook.write(outputStream);
		} finally {
			// don't forget to close workbook to prevent memory leaks
			// workbook.close();
		}
		System.out.println("Processing of chart is done.");

	}

	public static String Run_Query(Connection conn, String a, String b) throws InterruptedException, SQLException {
		String qury = a;
		System.out.println("Query == " + qury);
		ResultSet rs1 = null;
		PreparedStatement pstmt2 = conn.prepareStatement(qury);
		rs1 = pstmt2.executeQuery();
		String result = "";
		while (rs1.next()) {
			result = rs1.getString(b);
		}
		rs1.close();
		pstmt2.close();
		return result;
	}

	public static void create_dir() throws Test_Case_Exit_Exception {
		String l2 = null;
		if (SystemUtils.IS_OS_LINUX) {
			l2 = System.getProperty("user.dir") + "/Regression_BUILD_Report";
			System.out.println("LINUX");
		} else
			l2 = System.getProperty("user.dir") + "\\Regression_BUILD_Report";
		File f1 = new File(l2);
		boolean bool = f1.mkdir();
		if (bool) {
			System.out.println("Folder is created successfully");
		} else {
			if (f1.exists()) {
				System.out.println("Folder Regression_BUILD_Report already exists.");
			} else {
				System.out.println("Error in creating the folder.");
			}
		}
	}

	public static String getStartTime() {
		String starttime = "";
		List<LocalTime> localtime = new ArrayList<LocalTime>();
		try {
			for (int i = 0; i < startTime.size(); i++) {
				String[] split = startTime.get(i).split(" ");
				String[] split2 = split[1].split(":");
				LocalTime init = LocalTime.of(Integer.parseInt(split2[0]), Integer.parseInt(split2[1]),
						Integer.parseInt(split2[2]));
				localtime.add(init);
			}
			Collections.sort(localtime);
			starttime = localtime.get(0).toString();
			String[] split3 = starttime.split(":");
			starttime = split3[0] + " Hrs " + split3[1] + " min";
		} catch (Exception e) {
			System.out.println("Unable to get the Start Time.");
			e.printStackTrace();
		}

		return starttime;

	}

	public static String getEndTime() {
		String endtime = "";

		List<LocalTime> localtime = new ArrayList<LocalTime>();
		try {
			for (int i = 0; i < endTime.size(); i++) {
				String[] split = endTime.get(i).split(" ");
				String[] split2 = split[1].split(":");
				LocalTime init = LocalTime.of(Integer.parseInt(split2[0]), Integer.parseInt(split2[1]),
						Integer.parseInt(split2[2]));
				localtime.add(init);
			}
			Collections.sort(localtime);
			endtime = localtime.get(localtime.size() - 1).toString();
			String[] split3 = endtime.split(":");
			endtime = split3[0] + " Hrs " + split3[1] + " min";
		} catch (Exception e) {
			System.out.println("Unable to get the End Time.");
			e.printStackTrace();
		}

		return endtime;

	}

}
