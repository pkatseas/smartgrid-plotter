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
import de.erichseifert.gral.plots.XYPlot;
import de.erichseifert.gral.plots.axes.AxisRenderer;
import de.erichseifert.gral.plots.lines.DefaultLineRenderer2D;
import de.erichseifert.gral.plots.lines.LineRenderer;
import de.erichseifert.gral.plots.points.PointRenderer;
import de.erichseifert.gral.ui.InteractivePanel;
import de.erichseifert.gral.util.Insets2D;
import de.erichseifert.gral.util.Orientation;

public class SupplyDemandGraph extends JFrame {

	private static final long serialVersionUID = -7485115078599450975L;

	@SuppressWarnings("unchecked")
	public SupplyDemandGraph(int runID) {

		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int w = (int) screenSize.getWidth();
		int h = (int) (screenSize.getHeight()/1.5);

		// frame parameters
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setBounds(0, 0, w, h);

		PlotterDB p = new PlotterDB();
		ResultSet rs = null;

		if (p.open()) {
			rs = p.getAggregatorData(runID);
		} else {
			System.out
					.println("There was something wrong with getting data from the DB,"
							+ " execution terminated.");
			System.exit(1);
		}

		// datasets
		DataTable elDemand = new DataTable(Long.class, Double.class);
		DataTable elSupply = new DataTable(Long.class, Double.class);
		boolean b = true;
		long axiscenter = 0;
		String runInfo = null;
		try {
			while (rs.next()) {
				double supply = rs.getDouble("supply");
				double demand = rs.getDouble("overallDemand");
				long date = rs.getTimestamp("tick").getTime();

				if (b) {
					axiscenter = date;
					b = false;
				}

				elSupply.add(date, supply);
				elDemand.add(date, demand);
			}
			runInfo = p.getRunInfo(runID);
			rs.close();
			p.close();
		} catch (SQLException e) {
			System.out
					.println("There was something wrong, execution terminated.\n"
							+ e.toString());
			System.exit(1);
		}

		// dataseries

		DataSource dem = new DataSeries("Overall Demand", elDemand, 0, 1);
		DataSource sup = new DataSeries("Supply", elSupply, 0, 1);

		// our plot
		XYPlot plot = new XYPlot(dem, sup);

		// Format the plot
		plot.setInsets(new Insets2D.Double(10, 10, 10, 10));
		plot.setSetting(XYPlot.BACKGROUND, Color.WHITE);
		plot.setSetting(XYPlot.TITLE, "Supply - Overall Demand \n at run: "
				+ runInfo);
		plot.setSetting(XYPlot.LEGEND, true);
		plot.getLegend().setSetting(Legend.ORIENTATION, Orientation.HORIZONTAL);

		// axis format
		AxisRenderer axisRendererY = plot.getAxisRenderer(XYPlot.AXIS_Y);
		AxisRenderer axisRendererX = plot.getAxisRenderer(XYPlot.AXIS_X);
		axisRendererX.setSetting(AxisRenderer.LABEL, "Time");
		axisRendererY.setSetting(AxisRenderer.LABEL,
				"Supply and Overall Demand");
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		axisRendererX.setSetting(AxisRenderer.TICK_LABELS_FORMAT, dateFormat);
		AxisRenderer rendererY = plot.getAxisRenderer(XYPlot.AXIS_Y);
		rendererY.setSetting(AxisRenderer.INTERSECTION, axiscenter);

		// apply axis
		plot.setAxisRenderer(XYPlot.AXIS_X, axisRendererX);
		plot.setAxisRenderer(XYPlot.AXIS_Y, axisRendererY);

		// get the plot on the frame
		getContentPane().add(new InteractivePanel(plot));

		// draw the lines
		LineRenderer lines = new DefaultLineRenderer2D();
		plot.setLineRenderer(dem, lines);
		Color color = new Color(0.0f, 0.5f, 1.0f);
		plot.getPointRenderer(dem).setSetting(PointRenderer.COLOR,
				new Color(0, true));
		plot.getLineRenderer(dem).setSetting(LineRenderer.COLOR, color);

		LineRenderer lines2 = new DefaultLineRenderer2D();
		plot.setLineRenderer(sup, lines2);
		Color color2 = new Color(0.3f, 1.0f, 0.0f);
		plot.getPointRenderer(sup).setSetting(PointRenderer.COLOR,
				new Color(0, true));
		plot.getLineRenderer(sup).setSetting(LineRenderer.COLOR, color2);

	}

	public static void main(String[] args) {
		SupplyDemandGraph frame = new SupplyDemandGraph(1);
		frame.setVisible(true);

	}
}