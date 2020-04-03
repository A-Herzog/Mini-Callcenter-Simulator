/**
 * Copyright 2020 Alexander Herzog
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ui.statistics;

import java.awt.Color;

import org.jfree.chart.axis.CategoryLabelPositions;

import language.Language;
import mathtools.distribution.DataDistributionImpl;
import simulator.statistics.Statistics;
import systemtools.statistics.StatisticViewerBarChart;

/**
 * Dieser Viewer gibt Verteilungen in Form von Balkendiagrammen zu den Simulationsergebnissen aus.
 * @see StatisticViewerBarChart
 * @author Alexander Herzog
 */
public class ViewerBarChart extends StatisticViewerBarChart {
	private final Statistics statistics;
	private final Mode mode;


	/**
	 * Wählt die von {@link ViewerBarChart} auszugebende Information aus.
	 * @author Alexander Herzog
	 * @see ViewerBarChart#ViewerBarChart(Statistics, Mode)
	 */
	public enum Mode {
		/** Zeitdauern in denen sich das System in Bezug auf die Warteschlangenlängen in den verschiedenen Zuständen befunden hat */
		MODE_QUEUE,
		/** Zeitdauern in denen sich das System in Bezug auf die Anzahlen an Kunden im System in den verschiedenen Zuständen befunden hat */
		MODE_WIP,
		/** Zeitdauern in denen sich das System in Bezug auf die Auslastung in den verschiedenen Zuständen befunden hat */
		MODE_WORKLOAD
	}

	/**
	 * Konstruktor der Klasse
	 * @param statistics	Statistikobjekt, aus dem die anzuzeigenden Daten entnommen werden sollen
	 * @param mode	Gibt an, welche Daten genau ausgegeben werden sollen
	 * @see Mode
	 */
	public ViewerBarChart(final Statistics statistics, final Mode mode) {
		this.statistics=statistics;
		this.mode=mode;
	}

	private void buildQueue() {
		initBarChart(Language.tr("SimStatistic.Queue.Distribution"));
		setupBarChart(Language.tr("SimStatistic.Queue.Distribution"),Language.tr("Statistic.QueueLength"),Language.tr("Statistic.TimeShareInThisState"),true);

		DataDistributionImpl dist=statistics.queueLength.getDistribution();
		double sum=Math.max(1,dist.sum());
		for (int i=0;i<dist.densityData.length;i++) {
			data.addValue(dist.densityData[i]/sum,Language.tr("Statistic.QueueLength"),""+i);
		}

		plot.getRendererForDataset(data).setSeriesPaint(0,Color.RED);
		plot.getDomainAxis().setCategoryLabelPositions(CategoryLabelPositions.UP_45);
		plot.getRangeAxis().setRange(0,dist.getMax()/sum);
		setOutlineColor(Color.BLACK);
		initTooltips();
	}

	private void buildWIP() {
		initBarChart(Language.tr("Statistics.NumberOfClientsInTheSystem.Distribution"));
		setupBarChart(Language.tr("Statistics.NumberOfClientsInTheSystem.Distribution"),Language.tr("Statistics.NumberOfClientsInTheSystem"),Language.tr("Statistic.TimeShareInThisState"),true);

		DataDistributionImpl dist=statistics.systemLength.getDistribution();
		double sum=Math.max(1,dist.sum());
		for (int i=0;i<dist.densityData.length;i++) {
			data.addValue(dist.densityData[i]/sum,Language.tr("Statistics.NumberOfClientsInTheSystem"),""+i);
		}

		plot.getRendererForDataset(data).setSeriesPaint(0,Color.RED);
		plot.getDomainAxis().setCategoryLabelPositions(CategoryLabelPositions.UP_45);
		plot.getRangeAxis().setRange(0,dist.getMax()/sum);
		setOutlineColor(Color.BLACK);
		initTooltips();
	}

	private void buildWorkLoad() {
		initBarChart(Language.tr("Statistic.NumberOfWorkingAgents"));
		setupBarChart(Language.tr("Statistic.NumberOfWorkingAgents"),Language.tr("Statistic.NumberOfWorkingAgents"),Language.tr("Statistic.TimeShareInThisState"),true);

		DataDistributionImpl dist=statistics.busyAgents.getDistribution();
		double sum=Math.max(1,dist.sum());
		for (int i=0;i<dist.densityData.length;i++) {
			data.addValue(dist.densityData[i]/sum,Language.tr("Statistic.NumberOfWorkingAgents"),""+i);
		}

		plot.getRendererForDataset(data).setSeriesPaint(0,Color.RED);
		plot.getRangeAxis().setRange(0,dist.getMax()/sum);
		setOutlineColor(Color.BLACK);
		initTooltips();
	}

	@Override
	protected void firstChartRequest() {
		switch (mode) {
		case MODE_QUEUE: buildQueue(); break;
		case MODE_WIP: buildWIP(); break;
		case MODE_WORKLOAD: buildWorkLoad(); break;
		}
	}
}
