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

import language.Language;
import mathtools.NumberTools;
import mathtools.distribution.DataDistributionImpl;
import simulator.statistics.Statistics;
import systemtools.statistics.StatisticViewerPieChart;

/**
 * Dieser Viewer gibt Verteilungen in Form von Tortendiagrammen zu den Simulationsergebnissen aus.
 * @see StatisticViewerPieChart
 * @author Alexander Herzog
 */
public class ViewerPieChart extends StatisticViewerPieChart {
	private final Statistics statistics;
	private final Mode mode;

	/**
	 * Wählt die von {@link ViewerPieChart} auszugebende Information aus.
	 * @author Alexander Herzog
	 * @see ViewerPieChart#ViewerPieChart(Statistics, Mode)
	 */
	public enum Mode {
		/** Anzeige der Anruf-Anteile (Erstanrufer, Wiederholer, Weiterleitungen) */
		MODE_CALLER,
		/** Anzeige der Aufteilung der Zeit für die Agenten (Leerlauf, Teillast, Volllast) */
		MODE_WORKLOAD
	}

	/**
	 * Konstruktor der Klasse
	 * @param statistics	Statistikobjekt, aus dem die anzuzeigenden Daten entnommen werden sollen
	 * @param mode	Gibt an, welche Daten genau ausgegeben werden sollen
	 * @see Mode
	 */
	public ViewerPieChart(final Statistics statistics, final Mode mode) {
		this.statistics=statistics;
		this.mode=mode;
	}

	private void buildCallerCount() {
		initPieChart("Anruferanteile");
		addPieSegment(String.format("%s "+Language.tr("SimStatistic.FreshCalls"),NumberTools.formatLong(statistics.freshCalls.get())),statistics.freshCalls.get());
		addPieSegment(String.format("%s "+Language.tr("SimStatistic.ForwardedCalls"),NumberTools.formatLong(statistics.callContinued.getSuccess())),statistics.callContinued.getSuccess());
		addPieSegment(String.format("%s "+Language.tr("SimStatistic.Retryer"),NumberTools.formatLong(statistics.callRetry.getSuccess())),statistics.callRetry.getSuccess());
	}

	private void buildWorkLoad() {
		initPieChart("Lastanteile");
		DataDistributionImpl dist=statistics.busyAgents.getDistribution();
		double sum=Math.max(1,dist.sum());
		double idle=dist.densityData[0]/sum;
		double full=dist.densityData[dist.densityData.length-1]/sum;

		addPieSegment(String.format("%s "+Language.tr("Statistics.UtilizationAndFailures.Idle")+" (0 "+Language.tr("SimStatistic.AgentsBusy")+")",NumberTools.formatPercent(idle)),idle);
		addPieSegment(String.format("%s "+Language.tr("Statistics.UtilizationAndFailures.PartialUtilization")+" (1-%d "+Language.tr("SimStatistic.AgentsBusy")+")",NumberTools.formatPercent(1-full-idle),dist.densityData.length-2),1-full-idle);
		addPieSegment(String.format("%s "+Language.tr("Statistics.UtilizationAndFailures.FullUtilization")+" (%d "+Language.tr("SimStatistic.AgentsBusy")+")",NumberTools.formatPercent(full),dist.densityData.length-1),full);

	}

	@Override
	protected void firstChartRequest() {
		switch (mode) {
		case MODE_CALLER: buildCallerCount(); break;
		case MODE_WORKLOAD: buildWorkLoad(); break;
		}
	}
}