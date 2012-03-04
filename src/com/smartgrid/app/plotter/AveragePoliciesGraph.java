package com.smartgrid.app.plotter;

import java.awt.Color;
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
import de.erichseifert.gral.plots.XYPlot;
import de.erichseifert.gral.plots.axes.AxisRenderer;
import de.erichseifert.gral.plots.lines.DefaultLineRenderer2D;
import de.erichseifert.gral.plots.lines.LineRenderer;
import de.erichseifert.gral.plots.points.PointRenderer;
import de.erichseifert.gral.ui.InteractivePanel;
import de.erichseifert.gral.util.Insets2D;
import de.erichseifert.gral.util.Orientation;

public class AveragePoliciesGraph extends JFrame {

	private static final long serialVersionUID = -748511568599450975L;

	private int runID;

	public AveragePoliciesGraph(int runID) {
		this.runID = runID;
	}

	@SuppressWarnings("unchecked")
	public void getDemandGraph() {

		// frame parameters
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setBounds(0, 0, 640, 720);

		PlotterDB p = new PlotterDB();
		ResultSet rs = null;
		ResultSet rs2 = null;

		if (p.open()) {
			rs = p.getRunPolicies(runID);
		} else {
			System.out
					.println("There was something wrong with getting data from the DB,"
							+ " execution terminated.");
			System.exit(1);
		}

		ArrayList<Integer> IDs = new ArrayList<Integer>();
		try {
			while (rs.next()) {
				IDs.add(rs.getInt("household_policy_id"));
			}
		} catch (SQLException e) {
			System.out
					.println("There was something wrong, execution terminated.\n"
							+ e.toString());
			System.exit(1);
		}

		ArrayList<DataSource> ds = new ArrayList<DataSource>();

		boolean check = true;
		long axisYcenter = 0;
		double axisXcenter = Double.MAX_VALUE;
		String runInfo = null;

		try {
			for (int policyID : IDs) {
				// dataset creation
				DataTable dt = new DataTable(Long.class, Double.class);// demand

				rs2 = p.getPolicyAverageData(runID, policyID);

				try {
					while (rs2.next()) {

						long date = rs2.getTimestamp("tick").getTime();
						double demand = rs2.getDouble("demand");

						// System.out.println(date + " " + demand);

						dt.add(date, demand);

						if (check) {
							axisYcenter = date;
							check = false;
						}
						if (axisXcenter > demand) {
							axisXcenter = demand;
						}

					}
				} catch (SQLException e) {
					System.out
							.println("There was something wrong, execution terminated.\n"
									+ e.toString());
					e.printStackTrace();
					System.exit(1);
				}

				// dataseries

				DataSource dsource = new DataSeries(p.getPolicyInfo(policyID),
						dt, 0, 1);
				ds.add(dsource);
			}
			runInfo = p.getRunInfo(runID);
			p.close();
			rs.close();
			rs2.close();
		} catch (SQLException e) {
			System.out
					.println("There was something wrong, execution terminated.\n"
							+ e.toString());
			e.printStackTrace();
			System.exit(1);
		}

		DataSource[] dsAll = new DataSource[ds.size()];

		for (int i = 0; i < ds.size(); i++) {
			dsAll[i] = ds.get(i);
		}

		// our plot
		XYPlot plot = new XYPlot(dsAll);

		// Format the plot
		plot.setInsets(new Insets2D.Double(10, 10, 10, 10));
		plot.setSetting(XYPlot.BACKGROUND, Color.WHITE);
		plot.setSetting(XYPlot.TITLE,
				"Average Demand across Policies for run: "
						+ runInfo);
		plot.setSetting(XYPlot.LEGEND, true);
		plot.getLegend().setSetting(Legend.ORIENTATION, Orientation.HORIZONTAL);

		// axis format
		AxisRenderer axisRendererY = plot.getAxisRenderer(XYPlot.AXIS_Y);
		AxisRenderer axisRendererX = plot.getAxisRenderer(XYPlot.AXIS_X);
		axisRendererX.setSetting(AxisRenderer.LABEL, "Time");
		axisRendererY.setSetting(AxisRenderer.LABEL, "Average Demand");
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		axisRendererX.setSetting(AxisRenderer.TICK_LABELS_FORMAT, dateFormat);
		AxisRenderer rendererY = plot.getAxisRenderer(XYPlot.AXIS_Y);
		AxisRenderer rendererX = plot.getAxisRenderer(XYPlot.AXIS_X);
		rendererY.setSetting(AxisRenderer.INTERSECTION, axisYcenter);
		rendererX.setSetting(AxisRenderer.INTERSECTION, axisXcenter);

		// apply axis
		plot.setAxisRenderer(XYPlot.AXIS_X, axisRendererX);
		plot.setAxisRenderer(XYPlot.AXIS_Y, axisRendererY);

		// get the plot on the frame
		getContentPane().add(new InteractivePanel(plot));

		// draw the lines

		for (DataSource d : dsAll) {
			Random random = new Random();

			float hue = random.nextFloat();
			// Saturation between 0.1 and 0.3
			float saturation = (random.nextInt(2000) + 6000) / 10000f;
			float luminance = 1.0f;

			LineRenderer lines = new DefaultLineRenderer2D();
			plot.setLineRenderer(d, lines);
			Color color = Color.getHSBColor(hue, saturation, luminance);
			plot.getPointRenderer(d).setSetting(PointRenderer.COLOR,
					new Color(0, true));
			plot.getLineRenderer(d).setSetting(LineRenderer.COLOR, color);
		}

	}

	@SuppressWarnings("unchecked")
	public void getAppliancesGraph() {

		// frame parameters
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setBounds(641, 0, 640, 720);

		PlotterDB p = new PlotterDB();
		ResultSet rs = null;
		ResultSet rs2 = null;

		if (p.open()) {
			rs = p.getRunPolicies(runID);
		} else {
			System.out
					.println("There was something wrong with getting data from the DB,"
							+ " execution terminated.");
			System.exit(1);
		}

		ArrayList<Integer> IDs = new ArrayList<Integer>();
		try {
			while (rs.next()) {
				IDs.add(rs.getInt("household_policy_id"));
			}
		} catch (SQLException e) {
			System.out
					.println("There was something wrong, execution terminated.\n"
							+ e.toString());
			System.exit(1);
		}

		ArrayList<DataSource> ds = new ArrayList<DataSource>();

		boolean check = true;
		long axisYcenter = 0;
		double axisXcenter = Integer.MAX_VALUE;
		String runInfo = null;

		try {
			for (int policyID : IDs) {
				// dataset creation
				DataTable dt = new DataTable(Long.class, Integer.class);// demand

				rs2 = p.getPolicyAverageData(runID, policyID);

				try {
					while (rs2.next()) {

						long date = rs2.getTimestamp("tick").getTime();
						int appliancesOn = rs2.getInt("appliancesOn");

						dt.add(date, appliancesOn);

						if (check) {
							axisYcenter = date;
							check = false;
						}
						if (axisXcenter > appliancesOn) {
							axisXcenter = appliancesOn;
						}

					}
				} catch (SQLException e) {
					System.out
							.println("There was something wrong, execution terminated.\n"
									+ e.toString());
					e.printStackTrace();
					System.exit(1);
				}

				// dataseries

				DataSource dsource = new DataSeries(p.getPolicyInfo(policyID),
						dt, 0, 1);
				ds.add(dsource);
			}
			runInfo = p.getRunInfo(runID);
			p.close();
			rs.close();
			rs2.close();
		} catch (SQLException e) {
			System.out
					.println("There was something wrong, execution terminated.\n"
							+ e.toString());
			e.printStackTrace();
			System.exit(1);
		}

		DataSource[] dsAll = new DataSource[ds.size()];

		for (int i = 0; i < ds.size(); i++) {
			dsAll[i] = ds.get(i);
		}

		// our plot
		XYPlot plot = new XYPlot(dsAll);

		// Format the plot
		plot.setInsets(new Insets2D.Double(10, 10, 10, 10));
		plot.setSetting(XYPlot.BACKGROUND, Color.WHITE);
		plot.setSetting(XYPlot.TITLE,
				"Average Active Appliances across Policies for run: "
						+ runInfo);
		plot.setSetting(XYPlot.LEGEND, true);
		plot.getLegend().setSetting(Legend.ORIENTATION, Orientation.HORIZONTAL);

		// axis format
		AxisRenderer axisRendererY = plot.getAxisRenderer(XYPlot.AXIS_Y);
		AxisRenderer axisRendererX = plot.getAxisRenderer(XYPlot.AXIS_X);
		axisRendererX.setSetting(AxisRenderer.LABEL, "Time");
		axisRendererY.setSetting(AxisRenderer.LABEL,
				"Average Active Appliances");
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		axisRendererX.setSetting(AxisRenderer.TICK_LABELS_FORMAT, dateFormat);
		AxisRenderer rendererY = plot.getAxisRenderer(XYPlot.AXIS_Y);
		AxisRenderer rendererX = plot.getAxisRenderer(XYPlot.AXIS_X);
		rendererY.setSetting(AxisRenderer.INTERSECTION, axisYcenter);
		rendererX.setSetting(AxisRenderer.INTERSECTION, axisXcenter);

		// apply axis
		plot.setAxisRenderer(XYPlot.AXIS_X, axisRendererX);
		plot.setAxisRenderer(XYPlot.AXIS_Y, axisRendererY);

		// get the plot on the frame
		getContentPane().add(new InteractivePanel(plot));

		// draw the lines

		for (DataSource d : dsAll) {
			Random random = new Random();

			float hue = random.nextFloat();
			// Saturation between 0.1 and 0.3
			float saturation = (random.nextInt(2000) + 6000) / 10000f;
			float luminance = 1.0f;

			LineRenderer lines = new DefaultLineRenderer2D();
			plot.setLineRenderer(d, lines);
			Color color = Color.getHSBColor(hue, saturation, luminance);
			plot.getPointRenderer(d).setSetting(PointRenderer.COLOR,
					new Color(0, true));
			plot.getLineRenderer(d).setSetting(LineRenderer.COLOR, color);
		}

	}

	public static void main(String[] args) {

		AveragePoliciesGraph frame = new AveragePoliciesGraph(1);
		AveragePoliciesGraph frame2 = new AveragePoliciesGraph(1);

		frame.getDemandGraph();
		frame.setVisible(true);

		frame2.getAppliancesGraph();
		frame2.setVisible(true);

	}
}