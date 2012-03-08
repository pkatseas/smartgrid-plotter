package com.smartgrid.app.plotter;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import javax.swing.JFrame;
import de.erichseifert.gral.data.DataSource;
import de.erichseifert.gral.data.DataSeries;
import de.erichseifert.gral.data.DataTable;
import de.erichseifert.gral.plots.Plot;
import de.erichseifert.gral.plots.XYPlot;
import de.erichseifert.gral.plots.XYPlot.XYPlotNavigator;
import de.erichseifert.gral.plots.axes.AxisRenderer;
import de.erichseifert.gral.plots.lines.DefaultLineRenderer2D;
import de.erichseifert.gral.plots.lines.LineRenderer;
import de.erichseifert.gral.plots.points.PointRenderer;
import de.erichseifert.gral.ui.InteractivePanel;
import de.erichseifert.gral.util.Insets2D;

/**
 * 
 * Provides a pair of plots representing the demand and active appliances for a
 * specific policy of the run specified, either with average data across all
 * houses that are assigned this policy, or with data from random a random house
 * that was assigned this policy.
 * 
 * @author Panos Katseas
 * @version 1.1
 * @since 2012-03-07
 */
public class IndividualPolicyGraph {

	private static final long serialVersionUID = -7485115078599450975L;

	/**
	 * DataSource for demand
	 */
	private DataSource demandSeries;

	/**
	 * DataSource for appliances
	 */
	private DataSource appliancesSeries;

	/**
	 * DataSource for price
	 */
	private DataSource priceSeries;

	/**
	 * The specific run's date information
	 */
	private String runInfo;

	/**
	 * The specific policy's date information
	 */
	private String policyInfo;

	/**
	 * Specifies if this object refers to an individual random house or the
	 * average of all houses with this policy
	 */
	private String averageMode;

	/**
	 * The plot window's width
	 */
	private int width;

	/**
	 * The plot window's height
	 */
	private int height;

	/**
	 * The demand-supply plot's X axis center, used to map the axis on the
	 * visible area
	 */
	private double demandAxisX;

	/**
	 * The price plot's X axis center, used to map the axis on the visible area
	 */
	private double priceAxisX;

	/**
	 * The appliance plot's X axis center, used to map the axis on the visible
	 * area
	 */
	private int appliancesAxisX;
	/**
	 * The plot's Y axis center, used to map the axis on the visible area
	 */
	private long axisY;

	/**
	 * The constructor for the Supply Demand Graph.
	 * 
	 * @param runID
	 *            the ID of the specified run to show the graphs for
	 * @param policyID
	 *            the ID of the specified policy to show the graphs for
	 * @param average
	 *            states whether the graphs will be about one random house or
	 *            the average of all houses with the specified policy
	 */
	@SuppressWarnings("unchecked")
	public IndividualPolicyGraph(int runID, int policyID, boolean average) {

		if (average) {
			averageMode = "Average ";
		} else {
			averageMode = "Random ";
		}

		// get user's screen size for calculating the plot windows sizes
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		width = (int) (screenSize.getWidth() / 2);
		height = (int) (screenSize.getHeight() / 2);

		// create an object that provides connection to the DB
		PlotterDB p = new PlotterDB();
		ResultSet rs = null;
		ArrayList<Double> prices = new ArrayList<Double>();

		// connect to the DB and get the run's information,
		// as well as the household data for this run and policy
		if (p.open()) {
			runInfo = p.getRunInfo(runID);
			policyInfo = p.getPolicyInfo(policyID);

			// retrieving price data from the DB
			prices = p.getPrices(runID);
			if (average) {
				rs = p.getPolicyAverageData(runID, policyID);
			} else {
				rs = p.getPolicyRandomData(runID, policyID);
			}
		} else {
			System.out
					.println("There was something wrong with getting data from the DB,"
							+ " execution terminated.");
			System.exit(1);
		}

		// initialize our DataTables
		DataTable demandTable = new DataTable(Long.class, Double.class);
		DataTable appliancesTable = new DataTable(Long.class, Integer.class);

		// temp variables used for calculating the axes positions
		boolean b = true;
		axisY = 0;
		demandAxisX = Double.MAX_VALUE;
		appliancesAxisX = Integer.MAX_VALUE;
		priceAxisX = Double.MAX_VALUE;

		// temp variables for the priceList iteration
		int i = 0;
		priceAxisX = Double.MAX_VALUE;

		// initializing the price DataTable
		DataTable priceTable = new DataTable(Long.class, Double.class);

		// retrieving data for each policy and populating the DataTables
		try {

			// while the ResultSet returned contains more rows
			while (rs.next()) {

				// get the individual data of this row:
				// tick, demand, appliances
				long date = rs.getTimestamp("tick").getTime();
				double demand = rs.getDouble("demand");
				int appliances = rs.getInt("appliancesOn");

				// add this data to the DataTables
				demandTable.add(date, demand);
				appliancesTable.add(date, appliances);

				// price data added to its DataTable
				double pr = prices.get(i);
				priceTable.add(date, pr);

				// perform checks for the price axis position
				if (priceAxisX > pr) {
					priceAxisX = pr;
				}

				// perform checks/calculations for the axes positions
				if (b) {
					axisY = date;
					b = false;
				}

				if (demandAxisX > demand) {
					demandAxisX = demand;
				}

				if (appliancesAxisX > appliances) {
					appliancesAxisX = appliances;
				}

				i++;
			}

			// close the ResultSet since all its rows have been parsed
			rs.close();

			// create new DataSources with the data parsed from the
			// ResultSet
			demandSeries = new DataSeries("Demand", demandTable, 0, 1);
			appliancesSeries = new DataSeries("Appliances", appliancesTable, 0,
					1);
			priceSeries = new DataSeries(priceTable, 0, 1);

			// close the connection to the DB
			p.close();

		} catch (SQLException e) {
			System.out
					.println("There was something wrong, execution terminated.\n"
							+ e.toString());
			System.exit(1);
		}

	}

	/**
	 * Calls the getGraph method with a "demand" as parameter, which results in
	 * a Demand graph
	 */
	public void getDemandGraph() {
		getGraph("demand");

	}

	/**
	 * Calls the getGraph method with a "appliances" as parameter, which results
	 * in an Appliance graph
	 */
	public void getAppliancesGraph() {
		getGraph("appliances");
	}

	/**
	 * Calls the getGraph method with a "priceRight" as parameter, which results
	 * in a Price graph on the right part of the screen
	 */
	public void getRightPriceGraph() {
		getGraph("priceRight");
	}

	/**
	 * Calls the getGraph method with a "priceLeft" as parameter, which results
	 * in a Price graph on the left part of the screen
	 */
	public void getLeftPriceGraph() {
		getGraph("priceLeft");
	}

	/**
	 * Depending on the parameter given, shows either a Price graph on the
	 * bottom part of the screen (for false) or a Household Demand graph on the
	 * top part of the screen (for true)
	 * 
	 * @param mode
	 *            controls the type of graph that will be shown
	 */
	private void getGraph(String mode) {

		// the frame window on which the plot is to be presented
		JFrame graph = new JFrame();

		// frame parameters
		graph.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// titles for the plot and the axis Y
		String plotTitle = null;
		String axisYTitle = null;

		// temp variable used to store the appropriate DataSource
		DataSource tempSeries = null;

		// depending on the String value passed as a parameter:
		// 1. set window size and position
		// 2. get the appropriate DataSource in an Array format
		// 3. set the titles for the plot and Y axis
		if (mode == "demand") {
			graph.setBounds(0, 0, width, height);
			plotTitle = averageMode + "Household Demand for Policy: "
					+ policyInfo + "\nfor run: " + runInfo;
			axisYTitle = "Household Demand";
			tempSeries = demandSeries;
		} else if (mode == "appliances") {
			graph.setBounds(width, 0, width, height);
			plotTitle = averageMode
					+ "Household Active Appliances for Policy: " + policyInfo
					+ "\nfor run: " + runInfo;
			axisYTitle = "Active Appliances";
			tempSeries = appliancesSeries;
		} else if (mode == "priceLeft") {
			graph.setBounds(0, height, width, height);
			plotTitle = "Price for run: " + runInfo;
			axisYTitle = "Price";
			tempSeries = priceSeries;
		} else if (mode == "priceRight") {
			graph.setBounds(width, height, width, height);
			plotTitle = "Price for run: " + runInfo;
			axisYTitle = "Price";
			tempSeries = priceSeries;
		}

		// the actual plot that contains all the data
		XYPlot plot = new XYPlot(tempSeries);

		// --- formating the plot ---

		// set background
		plot.setSetting(Plot.BACKGROUND, Color.WHITE);

		// set title
		plot.setSetting(Plot.TITLE, plotTitle);

		// set the padding
		plot.setInsets(new Insets2D.Double(10, 10, 10, 10));

		// --- formating the axes ---

		// getting the axes renderers to apply settings
		AxisRenderer axisRendererY = plot.getAxisRenderer(XYPlot.AXIS_Y);
		AxisRenderer axisRendererX = plot.getAxisRenderer(XYPlot.AXIS_X);

		// set axes labels
		axisRendererX.setSetting(AxisRenderer.LABEL, "Time");
		axisRendererY.setSetting(AxisRenderer.LABEL, axisYTitle);

		// set distance for the Y axis label (otherwise overlaps with the axis's
		// values)
		axisRendererY.setSetting(AxisRenderer.LABEL_DISTANCE, 2);

		// set the X axis values to Date format
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd\nHH:mm");
		axisRendererX.setSetting(AxisRenderer.TICK_LABELS_FORMAT, dateFormat);

		// set the axes positions from the object's attributes which were
		// calculated beforehand (in the ResultSet parsing)
		axisRendererY.setSetting(AxisRenderer.INTERSECTION, axisY);

		// assign appropriate axis position depending on the graph mode
		if (mode == "demand") {
			axisRendererX.setSetting(AxisRenderer.INTERSECTION,
					demandAxisX * 0.97);
		} else if (mode == "appliances") {
			axisRendererX.setSetting(AxisRenderer.INTERSECTION,
					appliancesAxisX * 0.97);
		} else {
			axisRendererX.setSetting(AxisRenderer.INTERSECTION,
					priceAxisX * 0.97);
		}
		// apply axes changes
		plot.setAxisRenderer(XYPlot.AXIS_X, axisRendererX);
		plot.setAxisRenderer(XYPlot.AXIS_Y, axisRendererY);

		// get the plot on the frame
		graph.getContentPane().add(new InteractivePanel(plot));

		// create new line object
		LineRenderer lines = new DefaultLineRenderer2D();

		// assign it the current DataSource
		plot.setLineRenderer(tempSeries, lines);

		// get the color for the current DataSource
		Color color = new Color(0.0f, 0.5f, 1.0f);

		// set the line and point renderers for the current
		// DataSource on the plot
		plot.getPointRenderer(tempSeries).setSetting(PointRenderer.COLOR,
				new Color(0, true));
		plot.getLineRenderer(tempSeries).setSetting(LineRenderer.COLOR, color);

		// set the zoom for the frame
		XYPlotNavigator xy = new XYPlotNavigator(plot);
		xy.setZoom(1.65);

		// presenting the plot window
		graph.setVisible(true);
	}

	public static void main(String[] args) {

		IndividualPolicyGraph frame = new IndividualPolicyGraph(1, 1, false);
		frame.getDemandGraph();
		frame.getAppliancesGraph();
		frame.getLeftPriceGraph();
		frame.getRightPriceGraph();
	}
}