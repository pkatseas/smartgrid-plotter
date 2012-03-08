package com.smartgrid.app.plotter;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

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
 * Provides a plot representing the supply and demand over the time of a
 * specific run.
 * 
 * @author Panos Katseas
 * @version 1.1
 * @since 2012-03-07
 */
public class SupplyDemandGraph {

	private static final long serialVersionUID = -7485115078599450975L;

	/**
	 * DataSource for demand
	 */
	private DataSource demandSeries;

	/**
	 * DataSource for supply
	 */
	private DataSource supplySeries;

	/**
	 * DataSource for price
	 */
	private DataSource priceSeries;

	/**
	 * The specific run's date information
	 */
	private String runInfo;

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
	 * The plot's Y axis center, used to map the axis on the visible area
	 */
	private long axisY;

	/**
	 * The constructor for the Supply Demand Graph.
	 * 
	 * @param runID
	 *            the ID of the specified run to show the graphs for
	 */
	@SuppressWarnings("unchecked")
	public SupplyDemandGraph(int runID) {

		// get user's screen size for calculating the plot windows sizes
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		width = (int) screenSize.getWidth();
		height = (int) (screenSize.getHeight() / 2);

		// create an object that provides connection to the DB
		PlotterDB p = new PlotterDB();
		ResultSet rs = null;

		// connect to the DB and get the run's information,
		// as well as the aggregator's data for this run
		if (p.open()) {
			runInfo = p.getRunInfo(runID);
			rs = p.getAggregatorData(runID);
		} else {
			System.out
					.println("There was something wrong with getting data from the DB,"
							+ " execution terminated.");
			System.exit(1);
		}

		// initialize our DataTables
		DataTable demandTable = new DataTable(Long.class, Double.class);
		DataTable supplyTable = new DataTable(Long.class, Double.class);
		DataTable priceTable = new DataTable(Long.class, Double.class);

		// temp variables used for calculating the axes positions
		boolean b = true;
		axisY = 0;
		demandAxisX = Double.MAX_VALUE;
		priceAxisX = Double.MAX_VALUE;

		// retrieving data for each policy and populating the DataTables
		try {

			// while the ResultSet returned contains more rows
			while (rs.next()) {

				// get the individual data of this row:
				// tick, demand, appliances
				long date = rs.getTimestamp("tick").getTime();
				double demand = rs.getDouble("overallDemand");
				double supply = rs.getDouble("supply");
				double price = rs.getDouble("price");

				// add this data to the DataTables
				demandTable.add(date, demand);
				supplyTable.add(date, supply);
				priceTable.add(date, price);

				// perform checks/calculations for the axes positions
				if (b) {
					axisY = date;
					b = false;
				}

				if (demandAxisX > demand) {
					demandAxisX = demand;
					if (demandAxisX > supply) {
						demandAxisX = supply;
					}
				}

				if (priceAxisX > price) {
					priceAxisX = price;
				}

			}
			// close the ResultSet since all its rows have been parsed
			rs.close();

			// create new DataSources with the data parsed from the
			// ResultSet
			demandSeries = new DataSeries("Overall Demand", demandTable, 0, 1);
			supplySeries = new DataSeries("Supply", supplyTable, 0, 1);
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
	 * Calls the getGraph method with true as a parameter, which results in a
	 * Supply-Demand graph on the top part of the screen
	 */
	public void getSupplyDemandGraph() {
		getGraph(true);
	}

	/**
	 * Calls the getGraph method with false as a parameter, which results in a
	 * Price graph on the bottom part of the screen
	 */
	public void getPriceGraph() {
		getGraph(false);
	}

	/**
	 * Depending on the parameter given, shows either a Price graph on the
	 * bottom part of the screen (for false) or a Supply-Demand graph on the top
	 * part of the screen (for true)
	 * 
	 * @param mode
	 *            controls the type of graph that will be shown
	 */
	private void getGraph(boolean mode) {

		// the frame window on which the plot is to be presented
		JFrame graph = new JFrame();

		// frame parameters
		graph.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// titles for the plot and the axis Y
		String plotTitle = null;
		String axisYTitle = null;

		// depending on the boolean value passed as a parameter:
		// 1. set window size and position
		// 2. set the titles for the plot and Y axis
		if (mode) {
			graph.setBounds(0, 0, width, height);
			plotTitle = "Supply - Overall Demand \n for run: " + runInfo;
			axisYTitle = "Supply and Overall Demand";
		} else {
			graph.setBounds(0, height, width, height);
			plotTitle = "Price for run: " + runInfo;
			axisYTitle = "Price";
		}

		// the actual plot that contains all the data
		XYPlot plot = null;
		if (mode) {
			plot = new XYPlot(supplySeries, demandSeries);
		} else {
			plot = new XYPlot(priceSeries);
		}

		// --- formating the plot ---

		// set background
		plot.setSetting(Plot.BACKGROUND, Color.WHITE);

		// set title
		plot.setSetting(Plot.TITLE, plotTitle);

		// set the padding
		plot.setInsets(new Insets2D.Double(10, 10, 10, 10));

		// --- formating the legend ---
		if (mode) {
			// set legend
			plot.setSetting(Plot.LEGEND, true);

			// set legend location
			plot.setSetting(Plot.LEGEND_LOCATION, Location.NORTH);

			// set legend horizontally
			plot.getLegend().setSetting(Legend.ORIENTATION,
					Orientation.HORIZONTAL);

			// set legend placing along the X axis
			plot.getLegend().setSetting(Legend.ALIGNMENT_X, 1);
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
		if (mode) {
			axisRendererX.setSetting(AxisRenderer.INTERSECTION, demandAxisX);
		} else {
			axisRendererX.setSetting(AxisRenderer.INTERSECTION, priceAxisX);
		}

		// apply axes changes
		plot.setAxisRenderer(XYPlot.AXIS_X, axisRendererX);
		plot.setAxisRenderer(XYPlot.AXIS_Y, axisRendererY);

		// get the plot on the frame
		graph.getContentPane().add(new InteractivePanel(plot));

		// draw the lines
		if (mode) {
			// create new line object
			LineRenderer lines = new DefaultLineRenderer2D();

			// assign it the current DataSource
			plot.setLineRenderer(demandSeries, lines);

			// create new color
			Color color = new Color(0.0f, 0.5f, 1.0f);

			// set the line and point renderers for the current
			// DataSource on the plot
			plot.getPointRenderer(demandSeries).setSetting(PointRenderer.COLOR,
					new Color(0, true));
			plot.getLineRenderer(demandSeries).setSetting(LineRenderer.COLOR,
					color);

			// create new line object
			LineRenderer lines2 = new DefaultLineRenderer2D();

			// assign it the current DataSource
			plot.setLineRenderer(supplySeries, lines2);

			// create new color

			Color color2 = new Color(0.3f, 1.0f, 0.0f);

			// set the line and point renderers for the current
			// DataSource on the plot
			plot.getPointRenderer(supplySeries).setSetting(PointRenderer.COLOR,
					new Color(0, true));
			plot.getLineRenderer(supplySeries).setSetting(LineRenderer.COLOR,
					color2);
		} else {
			// create new line object
			LineRenderer lines = new DefaultLineRenderer2D();

			// assign it the current DataSource
			plot.setLineRenderer(priceSeries, lines);

			// create new color
			Color color = new Color(0.0f, 0.5f, 1.0f);

			// set the line and point renderers for the current
			// DataSource on the plot
			plot.getPointRenderer(priceSeries).setSetting(PointRenderer.COLOR,
					new Color(0, true));
			plot.getLineRenderer(priceSeries).setSetting(LineRenderer.COLOR,
					color);
		}

		// set the zoom for the frame
		XYPlotNavigator xy = new XYPlotNavigator(plot);
		xy.setZoom(1.65);

		// presenting the plot window
		graph.setVisible(true);
	}

	public static void main(String[] args) {

		SupplyDemandGraph frame = new SupplyDemandGraph(1);
		frame.getSupplyDemandGraph();
		frame.getPriceGraph();
	}
}