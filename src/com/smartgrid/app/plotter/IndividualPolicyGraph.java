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
import de.erichseifert.gral.plots.XYPlot;
import de.erichseifert.gral.plots.axes.AxisRenderer;
import de.erichseifert.gral.plots.lines.DefaultLineRenderer2D;
import de.erichseifert.gral.plots.lines.LineRenderer;
import de.erichseifert.gral.plots.points.PointRenderer;
import de.erichseifert.gral.ui.InteractivePanel;
import de.erichseifert.gral.util.Insets2D;

public class IndividualPolicyGraph extends JFrame {

	private static final long serialVersionUID = -748511568599450975L;

	private int runID;
	private int policyID;

	private boolean average;
	private String mode;

	public IndividualPolicyGraph(int runID, int policyID, boolean average) {
		this.average = average;
		if (this.average) {
			mode = "Average";
		} else {
			mode = "Random";
		}

		this.runID = runID;
		this.policyID = policyID;
	}

	@SuppressWarnings("unchecked")
	public void getDemandGraph() {

		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		double width = screenSize.getWidth();
		double height = screenSize.getHeight();

		int w = (int) (width/2);
		int h = (int) (height/1.5);

		// frame parameters
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setBounds(0, 0, w, h);

		PlotterDB p = new PlotterDB();
		ResultSet rs = null;

		if (p.open()) {
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

		// dataset
		DataTable dt = null;

		dt = new DataTable(Long.class, Double.class);// demand

		boolean b = true;
		long axisYcenter = 0;
		double axisXcenter = Double.MAX_VALUE;
		try {
			while (rs.next()) {

				long date = rs.getTimestamp("tick").getTime();
				double demand = rs.getDouble("demand");

				dt.add(date, demand);

				if (b) {
					axisYcenter = date;
					b = false;
				}
				if (axisXcenter > demand) {
					axisXcenter = demand;
				}

			}
			rs.close();
		} catch (SQLException e) {
			System.out
					.println("There was something wrong, execution terminated.\n"
							+ e.toString());
			System.exit(1);
		}

		// dataseries

		DataSource ds = new DataSeries(dt, 0, 1);

		// our plot
		XYPlot plot = new XYPlot(ds);

		// Format the plot
		plot.setInsets(new Insets2D.Double(10, 10, 10, 10));
		plot.setSetting(XYPlot.BACKGROUND, Color.WHITE);
		plot.setSetting(XYPlot.TITLE, mode + " Household Demand for Policy: "
				+ p.getPolicyInfo(policyID)
				+"\n at run: " + p.getRunInfo(runID));

		// axis format
		AxisRenderer axisRendererY = plot.getAxisRenderer(XYPlot.AXIS_Y);
		AxisRenderer axisRendererX = plot.getAxisRenderer(XYPlot.AXIS_X);
		axisRendererX.setSetting(AxisRenderer.LABEL, "Time");
		axisRendererY.setSetting(AxisRenderer.LABEL, "Household Demand");
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
		LineRenderer lines = new DefaultLineRenderer2D();
		plot.setLineRenderer(ds, lines);
		Color color = new Color(0.0f, 0.5f, 1.0f);
		plot.getPointRenderer(ds).setSetting(PointRenderer.COLOR,
				new Color(0, true));
		plot.getLineRenderer(ds).setSetting(LineRenderer.COLOR, color);

	}

	@SuppressWarnings("unchecked")
	public void getAppliancesGraph() {

		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		double width = screenSize.getWidth();
		double height = screenSize.getHeight();

		int w = (int) (width/2);
		int h = (int) (height/1.5);

		// frame parameters
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setBounds(w, 0, w, h);

		PlotterDB p = new PlotterDB();
		ResultSet rs = null;

		if (p.open()) {
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

		// dataset
		DataTable dt = new DataTable(Long.class, Integer.class);// appliances

		boolean b = true;
		long axisYcenter = 0;
		int axisXcenter = Integer.MAX_VALUE;
		try {
			while (rs.next()) {

				long date = rs.getTimestamp("tick").getTime();
				int appliancesOn = rs.getInt("appliancesOn");

				dt.add(date, appliancesOn);

				if (b) {
					axisYcenter = date;
					b = false;
				}
				if (axisXcenter > appliancesOn) {
					axisXcenter = appliancesOn;
				}

			}
			rs.close();
		} catch (SQLException e) {
			System.out
					.println("There was something wrong, execution terminated.\n"
							+ e.toString());
			System.exit(1);
		}

		// dataseries

		DataSource ds = new DataSeries(dt, 0, 1);

		// our plot
		XYPlot plot = new XYPlot(ds);

		// Format the plot
		plot.setInsets(new Insets2D.Double(10, 10, 10, 10));
		plot.setSetting(XYPlot.BACKGROUND, Color.WHITE);
		plot.setSetting(
				XYPlot.TITLE,
				mode + " Household Active Appliances for Policy: "
						+ p.getPolicyInfo(policyID)
						+"\n at run: " + p.getRunInfo(runID));

		// axis format
		AxisRenderer axisRendererY = plot.getAxisRenderer(XYPlot.AXIS_Y);
		AxisRenderer axisRendererX = plot.getAxisRenderer(XYPlot.AXIS_X);
		axisRendererX.setSetting(AxisRenderer.LABEL, "Time");
		axisRendererY.setSetting(AxisRenderer.LABEL, "Household Demand");
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
		LineRenderer lines = new DefaultLineRenderer2D();
		plot.setLineRenderer(ds, lines);
		Color color = new Color(0.0f, 1.0f, 0.3f);
		plot.getPointRenderer(ds).setSetting(PointRenderer.COLOR,
				new Color(0, true));
		plot.getLineRenderer(ds).setSetting(LineRenderer.COLOR, color);

	}

	public static void main(String[] args) {

		boolean average = true;

		IndividualPolicyGraph frame = new IndividualPolicyGraph(1, 1, average);
		IndividualPolicyGraph frame2 = new IndividualPolicyGraph(1, 1, average);

		frame.getDemandGraph();
		frame.setVisible(true);

		frame2.getAppliancesGraph();
		frame2.setVisible(true);

	}
}