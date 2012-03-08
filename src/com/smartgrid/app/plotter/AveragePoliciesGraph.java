package com.smartgrid.app.plotter;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Random;

import javax.swing.JFrame;

import de.erichseifert.gral.data.DataSource;
import de.erichseifert.gral.data.DataSeries;
import de.erichseifert.gral.data.DataTable;
import de.erichseifert.gral.plots.Legend;
import de.erichseifert.gral.plots.Plot;
import de.erichseifert.gral.plots.XYPlot;
import de.erichseifert.gral.plots.XYPlot.XYPlotNavigator;
import de.erichseifert.gral.plots.axes.AxisRenderer;
import de.erichseifert.gral.plots.lines.DefaultLineRenderer2D;
import de.erichseifert.gral.plots.lines.LineRenderer;
import de.erichseifert.gral.plots.points.PointRenderer;
import de.erichseifert.gral.ui.InteractivePanel;
import de.erichseifert.gral.util.Insets2D;
import de.erichseifert.gral.util.Location;
import de.erichseifert.gral.util.Orientation;

/**
 * 
 * Provides a pair of plots representing the average demand and average active
 * appliances across all policies of the run specified.
 * 
 * @author Panos Katseas
 * @version 1.1
 * @since 2012-03-07
 */
public class AveragePoliciesGraph {

	private static final long serialVersionUID = -748511568599450975L;

	/**
	 * DataSource for demand
	 */
	private ArrayList<DataSource> demandSeries;

	/**
	 * DataSource for active appliances
	 */
	private ArrayList<DataSource> appliancesSeries;

	/**
	 * DataSource for price
	 */
	private DataSource priceSeries;

	/**
	 * The specific run's date information
	 */
	private String runInfo;

	/**
	 * A list of colors to be used across both plots for the plotted lines
	 */
	private ArrayList<Color> colors;

	/**
	 * The plot window's width
	 */
	private int width;

	/**
	 * The plot window's height
	 */
	private int height;

	/**
	 * The demand plot's X axis center, used to map the axis on the visible area
	 */
	private double demandAxisX;

	/**
	 * The appliances plot's X axis center, used to map the axis on the visible
	 * area
	 */
	private int appliancesAxisX;

	/**
	 * The price plot's X axis center, used to map the axis on the visible area
	 */
	private double priceAxisX;

	/**
	 * The plot's Y axis center, used to map the axis on the visible area
	 */
	private long axisY;

	/**
	 * The constructor for the Average Policies Graph.
	 * 
	 * @param runID
	 *            the ID of the specified run to show the graphs for
	 */
	@SuppressWarnings("unchecked")
	public AveragePoliciesGraph(int runID) {

		// get user's screen size for calculating the plot windows sizes
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		width = (int) (screenSize.getWidth() / 2);
		height = (int) (screenSize.getHeight() / 2);

		// initialize our DataSources
		demandSeries = new ArrayList<DataSource>();
		appliancesSeries = new ArrayList<DataSource>();

		// create an object that provides connection to the DB
		PlotterDB p = new PlotterDB();

		// the list of IDs for the policies that will be shown on the plots
		ArrayList<Integer> policyIDs = new ArrayList<Integer>();

		// connect to the DB and get the run's information,
		// as well as the list of policy IDs
		if (p.open()) {
			runInfo = p.getRunInfo(runID);
			policyIDs = p.getRunPolicies(runID);
		} else {
			System.out
					.println("There was something wrong with getting data from the DB,"
							+ " execution terminated.");
			System.exit(1);
		}

		// create the list of colors with one color per policy
		randomColors(policyIDs.size());

		// temp variables used for calculating the axes positions
		boolean b = true;
		axisY = 0;
		demandAxisX = Double.MAX_VALUE;
		appliancesAxisX = Integer.MAX_VALUE;

		// temp variables for the priceList iteration
		int i = 0;
		boolean c = true;
		priceAxisX = Double.MAX_VALUE;

		// retrieving price data from the DB
		ArrayList<Double> prices = p.getPrices(runID);

		// initializing the price DataTable
		DataTable priceTable = new DataTable(Long.class, Double.class);

		// retrieving data for each policy and populating the DataTables
		try {
			for (int policyID : policyIDs) {

				// initializing the DataTables
				DataTable demandTable = new DataTable(Long.class, Double.class);
				DataTable appliancesTable = new DataTable(Long.class,
						Integer.class);

				// retrieving the policy's data from the DB
				ResultSet rs = p.getPolicyAverageData(runID, policyID);

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
					if (c) {
						double pr = prices.get(i);
						priceTable.add(date, pr);

						// perform checks for the price axis position
						if (priceAxisX > pr) {
							priceAxisX = pr;
						}

						i++;
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

				}

				// priceList iteration done
				c = false;

				// get the policy's information (name and version)
				String policyInfo = p.getPolicyInfo(policyID);

				// create new DataSources with the data parsed from the
				// ResultSet
				DataSource dem = new DataSeries(policyInfo, demandTable, 0, 1);
				DataSource app = new DataSeries(policyInfo, appliancesTable, 0,
						1);

				// populate the object's attribute DataSources with the
				// new DataSources just created
				demandSeries.add(dem);
				appliancesSeries.add(app);

				// close the ResultSet since all data has been parsed
				rs.close();

			}

			// close the connection to the DB
			p.close();

			// populate the object's price DataSource attribute
			priceSeries = new DataSeries(priceTable, 0, 1);

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
	 * Creates a new window that displays a plot with active appliances or
	 * demand on axis Y, depending on the boolean that is passed, and time on
	 * axis X
	 * 
	 * @param the
	 *            graph mode, true for Demand graph, false for Appliance graph
	 */
	private void getGraph(String mode) {

		// the frame window on which the plot is to be presented
		JFrame graph = new JFrame();

		// window close operation (exit application)
		graph.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// create an Array of all the DataSources that will be used to create
		// the plot
		DataSource[] dsAll = new DataSource[appliancesSeries.size()];

		// titles for the plot and the axis Y
		String plotTitle = null;
		String axisYTitle = null;

		// depending on the String value passed as a parameter:
		// 1. set window size and position
		// 2. get the appropriate DataSource in an Array format
		// 3. set the titles for the plot and Y axis
		if (mode == "demand") {
			graph.setBounds(0, 0, width, height);
			demandSeries.toArray(dsAll);
			plotTitle = "Average Demand across Policies \n for run: " + runInfo;
			axisYTitle = "Average Demand";
		} else if (mode == "appliances") {
			graph.setBounds(width, 0, width, height);
			appliancesSeries.toArray(dsAll);
			plotTitle = "Average Active Appliances across Policies \n for run: "
					+ runInfo;
			axisYTitle = "Average Active Appliances";
		} else if (mode == "priceLeft") {
			graph.setBounds(0, height, width, height);
			plotTitle = "Price for run: " + runInfo;
			axisYTitle = "Price";
		} else if (mode == "priceRight") {
			graph.setBounds(width, height, width, height);
			plotTitle = "Price for run: " + runInfo;
			axisYTitle = "Price";
		}

		// the actual plot that contains all the data
		XYPlot plot = null;

		if (mode.contains("price")) {
			plot = new XYPlot(priceSeries);
		} else {
			plot = new XYPlot(dsAll);
		}
		
		// --- formating the plot ---

		// set background
		plot.setSetting(Plot.BACKGROUND, Color.WHITE);

		// set title
		plot.setSetting(Plot.TITLE, plotTitle);

		if (!mode.contains("price")) {
			// set the padding
			plot.setInsets(new Insets2D.Double(10, 10, 70, 10));

			// --- formating the legend ---

			// set legend
			plot.setSetting(Plot.LEGEND, true);

			// set legend location
			plot.setSetting(Plot.LEGEND_LOCATION, Location.SOUTH);

			// set legend horizontally
			plot.getLegend().setSetting(Legend.ORIENTATION,
					Orientation.HORIZONTAL);

			// set legend placing along the X axis
			plot.getLegend().setSetting(Legend.ALIGNMENT_X, 0.5);
		} else {
			// set the padding
			plot.setInsets(new Insets2D.Double(10, 10, 10, 10));
		}

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
					demandAxisX * 0.95);
		} else if (mode == "appliances") {
			axisRendererX.setSetting(AxisRenderer.INTERSECTION,
					appliancesAxisX * 0.95);
		} else if (mode.contains("price")) {
			axisRendererX.setSetting(AxisRenderer.INTERSECTION,
					priceAxisX * 0.95);
		}

		// apply axes changes
		plot.setAxisRenderer(XYPlot.AXIS_X, axisRendererX);
		plot.setAxisRenderer(XYPlot.AXIS_Y, axisRendererY);

		// get the plot on the frame
		graph.getContentPane().add(new InteractivePanel(plot));

		if (mode.contains("price")) {
			// create new line object
			LineRenderer lines = new DefaultLineRenderer2D();

			// assign it the current DataSource
			plot.setLineRenderer(priceSeries, lines);

			// get the color for the current DataSource
			Color color = colors.get(0);

			// set the line and point renderers for the current
			// DataSource on the plot
			plot.getPointRenderer(priceSeries).setSetting(PointRenderer.COLOR,
					new Color(0, true));
			plot.getLineRenderer(priceSeries).setSetting(LineRenderer.COLOR,
					color);
		} else {
			// draw a line on the plot for each of the policies
			for (int i = 0; i < dsAll.length; i++) {

				// create new line object
				LineRenderer lines = new DefaultLineRenderer2D();

				// assign it the current DataSource
				plot.setLineRenderer(dsAll[i], lines);

				// get the color for the current DataSource
				Color color = colors.get(i);

				// set the line and point renderers for the current
				// DataSource on the plot
				plot.getPointRenderer(dsAll[i]).setSetting(PointRenderer.COLOR,
						new Color(0, true));
				plot.getLineRenderer(dsAll[i]).setSetting(LineRenderer.COLOR,
						color);
			}
		}

		// set the zoom for the frame
		XYPlotNavigator xy = new XYPlotNavigator(plot);
		xy.setZoom(2);

		// presenting the plot window
		graph.setVisible(true);
	}

	/**
	 * Creates a random color for each of the policies that will be shown on the
	 * plot and stores the list of colors in the object's colors attribute.
	 * 
	 * @param count
	 *            the number of policies for which colors need to be created
	 */
	private void randomColors(int count) {

		ArrayList<Color> colors = new ArrayList<Color>();

		for (int i = 0; i < count; i++) {
			Random random = new Random();

			// random color magic
			float hue = random.nextFloat();
			hue = random.nextFloat();
			hue = random.nextFloat();
			float saturation = (random.nextInt(2000) + 6000) / 10000f;
			float luminance = 1f;

			colors.add(Color.getHSBColor(hue, saturation, luminance));
		}
		// set this list for use by the plot frames
		this.colors = colors;
	}

	public static void main(String[] args) {

		int runID = 1;
		AveragePoliciesGraph frame = new AveragePoliciesGraph(runID);

		frame.getDemandGraph();
		frame.getAppliancesGraph();
		frame.getLeftPriceGraph();
		frame.getRightPriceGraph();

	}

}