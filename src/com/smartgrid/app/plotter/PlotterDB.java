package com.smartgrid.app.plotter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class PlotterDB {
	/**
	 * Object providing connection to the DB.
	 */
	private Connection con;

	/**
	 * The connection URL to the DB.
	 */
	private String connectionURL;

	/**
	 * Statement object, for executing queries on the DB.
	 */
	private Statement stmt;

	/**
	 * Default Constructor
	 * 
	 * Assumes default connection URL since no parameters are given.
	 */
	public PlotterDB() {
		con = null;
		stmt = null;
		connectionURL = "jdbc:mysql://localhost:3306/smartgrid?"
				+ "user=smartgrid&password=smartgrid";
	}

	/**
	 * Opens DB connection and creates a statement to execute queries on the DB.
	 * 
	 * @return true if the connection is established, false otherwise.
	 */
	public boolean open() {
		try {
			con = DriverManager.getConnection(connectionURL);
		} catch (SQLException e) {
			System.out.println("SQL Exception: " + e.toString());
			return false;
		}

		try {
			stmt = con.createStatement();
		} catch (SQLException e) {
			System.out.println("SQL Exception: " + e.toString());
			return false;
		} catch (Exception e) {
			System.out.println("General Exception: " + e.toString());
			return false;
		}

		return true;
	}

	/**
	 * Closes the connection and the statement.
	 */
	public void close() {
		try {
			con.close();
			stmt.close();
		} catch (SQLException e) {
			System.out.println("SQL Exception: " + e.toString());
		}
	}

	/**
	 * Executes a query on the DB.
	 * 
	 * Receives a set of results from the DB.
	 * 
	 * @param query
	 *            the update query to be executed.
	 * @return true if the connection is established, false otherwise.
	 */
	private ResultSet executeQuery(String query) {
		try {
			ResultSet rs = stmt.executeQuery(query);
			return rs;
		} catch (SQLException e) {
			System.out.println("SQL Exception: " + e.toString());
			return null;
		}
	}

	public ResultSet getAggregatorData(int runID) {

		String query = new String();

		query = "SELECT `tick`,`supply`,`overallDemand` "
				+ "FROM `aggregator_log` " + "WHERE run_id=" + runID;

		return executeQuery(query);

	}

	public ResultSet getPolicyRandomData(int runID, int policyID) {

		String query = new String();

		query = "SELECT `household_id` "
				+ "FROM `run_household_log_household_policy` "
				+ "WHERE `run_id` = " + runID + " AND `household_policy_id` = "
				+ policyID + " ORDER BY rand( )" + " LIMIT 1";

		ResultSet rs = executeQuery(query);
		int houseID = 0;

		if (rs != null) {
			try {
				rs.next();
				houseID = rs.getInt("household_id");
			} catch (SQLException e) {
				System.out.println(e.toString());
				return null;
			}
		} else {
			return null;
		}

		query = "SELECT `tick`,`demand`,`appliancesOn` "
				+ "FROM household_log " + "WHERE `run_id` = " + runID
				+ " AND `household_id` = " + houseID + " ORDER BY `tick` ASC";

		return executeQuery(query);

	}

	public ResultSet getPolicyAverageData(int runID, int policyID) {

		String query = new String();

		query = "SELECT `tick`, AVG(`demand`) AS `demand`, AVG(`appliancesOn`) AS `appliancesOn` "
				+ "FROM `household_log` "
				+ "WHERE `run_id` = "
				+ runID
				+ " AND `household_id` IN "
				+ "(SELECT `household_id` "
				+ "FROM `run_household_log_household_policy` "
				+ "WHERE `household_policy_id` = "
				+ policyID
				+ ")"
				+ "GROUP BY `tick` " + "ORDER BY `tick` ASC";

		return executeQuery(query);

	}

	public ResultSet getRunPolicies(int runID) {

		String query = new String();

		query = "SELECT DISTINCT `household_policy_id` "
				+ "FROM `run_household_log_household_policy` "
				+ "WHERE `run_id` = " + runID;

		return executeQuery(query);

	}

	public String getPolicyInfo(int policyID) {

		String query = new String();

		query = "SELECT `name`,`version` " + "FROM `household_policy` "
				+ "WHERE `household_policy_id` = " + policyID;

		ResultSet rs = executeQuery(query);

		String info = null;
		try {
			rs.next();
			info = rs.getString("name") + " version " + rs.getString("version");
		} catch (SQLException e) {
			System.out.println("SQL Exception: " + e.toString());
		}

		return info;

	}

	@SuppressWarnings("deprecation")
	public String getRunInfo(int runID) {

		String query = new String();

		query = "SELECT `date` FROM `run` " + "WHERE " + "`run_id` = " + runID;

		ResultSet rs = executeQuery(query);

		String info = null;
		try {
			rs.next();
			info = rs.getTimestamp("date").toGMTString();
		} catch (SQLException e) {
			System.out.println("SQL Exception: " + e.toString());
		}

		return info;

	}
}