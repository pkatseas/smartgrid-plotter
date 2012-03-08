package com.smartgrid.app.plotter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * 
 * Provides capabilities for retrieving simulation data from the DB.
 * 
 * @author Panos Katseas
 * @version 1.1
 * @since 2012-03-07
 */
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
	 * Closes the connection and the statement used to connect to the DB.
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
	 * @return the {@link ResultSet} if data is retrieved successfully, null
	 *         otherwise.
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

	/**
	 * Returns data from the aggregator_log of the DB.
	 * 
	 * Returns tick, supply and overallDemand values from the aggregator_log
	 * table in the DB in a {@link ResultSet} object.
	 * 
	 * @param runID
	 *            the ID of the run for which aggregator data is retrieved.
	 * @return the {@link ResultSet} if data is retrieved successfully, null
	 *         otherwise.
	 */
	public ResultSet getAggregatorData(int runID) {

		String query = new String();

		query = "SELECT `tick`,`supply`,`overallDemand`,`price` "
				+ "FROM `aggregator_log` " + "WHERE run_id=" + runID;

		return executeQuery(query);
	}

	/**
	 * Returns data from a random household that is assigned the policy
	 * specified, during the run specified.
	 * 
	 * Returns tick, demand and appliancesOn values from the table in the DB in
	 * a {@link ResultSet} object.
	 * 
	 * @param runID
	 *            the ID of the run for which random household data is
	 *            retrieved.
	 * @param policyID
	 *            the ID of the policy which the random household we want to
	 *            find is assigned to
	 * @return the {@link ResultSet} if data is retrieved successfully, null
	 *         otherwise.
	 */
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

	/**
	 * Returns average data from all the households that are assigned the policy
	 * specified, during the run specified.
	 * 
	 * Returns tick, demand and appliancesOn values from the household_log of
	 * the DB in a {@link ResultSet} object.
	 * 
	 * @param runID
	 *            the ID of the run for which average household data is
	 *            retrieved.
	 * @param policyID
	 *            the ID of the policy for which we want the average household
	 *            values
	 * @return the {@link ResultSet} if data is retrieved successfully, null
	 *         otherwise.
	 */
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

	/**
	 * Returns the IDs of all the household policies that were used during the
	 * run specified in an {@link ArrayList} object.
	 * 
	 * 
	 * @param runID
	 *            the ID of the run for which the household policy data is
	 *            retrieved.
	 * @return an {@link ArrayList} containing the IDs if data is retrieved
	 *         successfully, an empty {@link ArrayList} object otherwise.
	 */
	public ArrayList<Integer> getRunPolicies(int runID) {

		String query = new String();

		query = "SELECT DISTINCT `household_policy_id` "
				+ "FROM `run_household_log_household_policy` "
				+ "WHERE `run_id` = " + runID;

		ResultSet rs = executeQuery(query);
		ArrayList<Integer> policyIDs = new ArrayList<Integer>();

		try {
			while (rs.next()) {
				policyIDs.add(rs.getInt("household_policy_id"));
			}
		} catch (SQLException e) {
			System.out
					.println("There was something wrong, execution terminated.\n"
							+ e.toString());
			System.exit(1);
		}

		return policyIDs;
	}

	/**
	 * Returns the IDs and dates of all the runs that have taken place in the
	 * past in a {@link HashMap}.
	 * 
	 * @return a {@link HashMap} containing the IDs and dates or the runs if
	 *         data is retrieved successfully, an empty {@link HashMap} object
	 *         otherwise.
	 */
	public HashMap<Integer, String> getRuns() {

		String query = new String();

		query = "SELECT `run_id`,`date` FROM `run`";

		ResultSet rs = executeQuery(query);
		HashMap<Integer, String> runs = new HashMap<Integer, String>();

		try {
			while (rs.next()) {
				runs.put(rs.getInt("run_id"), rs.getString("date"));
			}
		} catch (SQLException e) {
			System.out
					.println("There was something wrong, execution terminated.\n"
							+ e.toString());
			System.exit(1);
		}

		return runs;
	}

	/**
	 * Returns the price values for the run specified in an {@link ArrayList}.
	 * 
	 * @return an {@link ArrayList} containing the prices if data is retrieved
	 *         successfully, an empty {@link ArrayList} object otherwise.
	 */
	public ArrayList<Double> getPrices(int runID) {

		String query = new String();

		query = "SELECT `price` " + "FROM `aggregator_log` "
				+ "WHERE `run_id` = " + runID + " ORDER BY `tick` ASC";

		ResultSet rs = executeQuery(query);
		ArrayList<Double> prices = new ArrayList<Double>();

		try {
			while (rs.next()) {
				prices.add(rs.getDouble("price"));
			}
		} catch (SQLException e) {
			System.out
					.println("There was something wrong, execution terminated.\n"
							+ e.toString());
			System.exit(1);
		}

		return prices;
	}

	/**
	 * Returns name and version information of the policy that matches the ID
	 * given as a parameter.
	 * 
	 * @param policyID
	 *            the ID of the policy in question.
	 * @return a {@link String} containing the policy information if data is
	 *         retrieved successfully, null otherwise.
	 */
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

	/**
	 * Returns the date information of the run that matches the ID given as a
	 * parameter.
	 * 
	 * @param runID
	 *            the ID of the run in question.
	 * @return a {@link String} containing the run date information if data is
	 *         retrieved successfully, null otherwise.
	 */
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