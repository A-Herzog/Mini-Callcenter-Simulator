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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import language.Language;
import mathtools.NumberTools;
import mathtools.Table;
import mathtools.distribution.DataDistributionImpl;
import simulator.statistics.Statistics;
import statistics.StatisticsDataPerformanceIndicator;
import statistics.StatisticsTimePerformanceIndicator;
import systemtools.statistics.StatisticViewerTable;

/**
 * Dieser Viewer gibt Daten in Tabellenform zu den Simulationsergebnissen aus.
 * @see StatisticViewerTable
 * @author Alexander Herzog
 */
public class ViewerTable extends StatisticViewerTable {
	private final Statistics statistics;
	private final Mode mode;

	/**
	 * Wählt die von {@link ViewerTable} auszugebende Information aus.
	 * @author Alexander Herzog
	 * @see ViewerTable#ViewerTable(Statistics, Mode)
	 */
	public enum Mode {
		/** Tabelle "Anruferanzahl" */
		MODE_CALLER,
		/** Tabelle "Warteschlange" */
		MODE_QUEUE,
		/** Tabelle "Kunden im System" */
		MODE_WIP,
		/** Tabelle "Verteilung der Zwischenankunftszeiten" */
		MODE_INTERARRIVALTIMES,
		/** Tabelle "Verteilung der Zwischenabgangszeiten" */
		MODE_INTERLEAVETIMES,
		/** Tabelle "Verteilung der Wartezeiten der erfolgreichen Kunden" */
		MODE_WAITINGTIMES_SUCCESS,
		/** Tabelle "Verteilung der Abbruchzeiten" */
		MODE_WAITINGTIMES_CANCEL,
		/** Tabelle "Verteilung der Wartezeiten über alle Kunden" */
		MODE_WAITINGTIMES_ALL,
		/** Tabelle "Verteilung der Bedienzeiten" */
		MODE_WORKINGTIMES,
		/** Tabelle "Verteilung der Nachbearbeitungszeiten" */
		MODE_POSTPROCESSINGTIMES,
		/** Tabelle "Verteilung der Verweilzeiten der erfolgreichen Kunden" */
		MODE_SYSTEMTIMES_SUCCESS,
		/** Tabelle "Verteilung der Verweilzeiten aller Kunden" */
		MODE_SYSTEMTIMES_ALL,
		/** Tabelle "Auslastung" */
		MODE_WORKLOAD,
		/** Autokorrelation der Wartezeiten */
		MODE_AUTOCORRELATION
	}

	/**
	 * Konstruktor der Klasse
	 * @param statistics	Statistikobjekt, aus dem die anzuzeigenden Daten entnommen werden sollen
	 * @param mode	Gibt an, welche Daten genau ausgegeben werden sollen
	 * @see Mode
	 */
	public ViewerTable(final Statistics statistics, final Mode mode) {
		this.statistics=statistics;
		this.mode=mode;
	}

	private void buildCallerCount() {
		Table table=new Table();
		table.addLine(new String[]{Language.tr("SimStatistic.FreshCalls"),""+statistics.freshCalls.get()});
		table.addLine(new String[]{Language.tr("SimStatistic.ForwardedCalls"),""+statistics.callContinued.getSuccess()});
		table.addLine(new String[]{Language.tr("SimStatistic.Retryer"),""+statistics.callRetry.getSuccess()});
		table.addLine(new String[]{Language.tr("Statistic.Sum"),""+(statistics.freshCalls.get()+statistics.callContinued.getSuccess()+statistics.callRetry.getSuccess())});
		setData(table,new String[]{Language.tr("Statistic.Viewer.Chart.Mode"),Language.tr("Statistic.Viewer.Chart.Number")});
	}

	private void buildLengthTable(final Mode mode) {
		Table table=new Table();

		StatisticsTimePerformanceIndicator performanceIndicator=null;
		String[] headings=null;

		switch (mode) {
		case MODE_QUEUE:
			performanceIndicator=statistics.queueLength;
			headings=new String[]{Language.tr("Statistic.NumberOfWaitingClients"),Language.tr("Statistic.TimeShare")};
			break;
		case MODE_WIP:
			performanceIndicator=statistics.systemLength;
			headings=new String[]{Language.tr("Statistic.NumberOfClientsInSystem"),Language.tr("Statistic.TimeShare")};
			break;
		case MODE_WORKLOAD:
			performanceIndicator=statistics.busyAgents;
			headings=new String[]{Language.tr("Statistic.NumberOfWorkingAgents"),Language.tr("Statistic.TimeShare")};
		default:
			/* Andere Fälle behandeln wir hier nicht. */
			break;
		}

		if (performanceIndicator==null || headings==null) return;

		DataDistributionImpl dist=performanceIndicator.getDistribution();
		double sum=Math.max(1,dist.sum());
		for (int i=0;i<dist.densityData.length;i++) table.addLine(new String[]{
				""+i,
				NumberTools.formatPercent(dist.densityData[i]/sum,3)
		});
		table.addLine(new String[]{Language.tr("Statistics.Average"),NumberTools.formatNumber(dist.getMean(),3)});
		table.addLine(new String[]{Language.tr("Distribution.StdDev"),NumberTools.formatNumber(dist.getStandardDeviation(),3)});
		table.addLine(new String[]{Language.tr("Distribution.CV"),NumberTools.formatNumber(performanceIndicator.getTimeCV())});
		table.addLine(new String[]{Language.tr("Statistics.Minimum"),NumberTools.formatNumber(performanceIndicator.getTimeMin())});
		table.addLine(new String[]{Language.tr("Statistics.Maximum"),NumberTools.formatNumber(performanceIndicator.getTimeMax())});

		setData(table,headings);
	}

	private void buildTimesTable(final Mode mode) {
		Table table=new Table();

		StatisticsDataPerformanceIndicator performanceIndicator=null;

		switch (mode) {
		case MODE_INTERARRIVALTIMES: performanceIndicator=statistics.interarrivalTime; break;
		case MODE_INTERLEAVETIMES: performanceIndicator=statistics.interleaveTime; break;
		case MODE_WAITINGTIMES_SUCCESS: performanceIndicator=statistics.waitingTimeSuccess; break;
		case MODE_WAITINGTIMES_CANCEL: performanceIndicator=statistics.waitingTimeCancel; break;
		case MODE_WAITINGTIMES_ALL: performanceIndicator=statistics.waitingTimeAll; break;
		case MODE_WORKINGTIMES: performanceIndicator=statistics.workingTime; break;
		case MODE_POSTPROCESSINGTIMES: performanceIndicator=statistics.postProcessingTime; break;
		case MODE_SYSTEMTIMES_SUCCESS: performanceIndicator=statistics.systemTimeSuccess; break;
		case MODE_SYSTEMTIMES_ALL: performanceIndicator=statistics.systemTimeSuccess; break;
		default: /* Andere Fälle behandeln wir hier nicht. */ break;
		}

		if (performanceIndicator==null) return;

		DataDistributionImpl dist=performanceIndicator.getDistribution();
		double sum=Math.max(1,dist.sum());
		for (int i=0;i<dist.densityData.length;i++) table.addLine(new String[]{
				""+i,
				""+Math.round(dist.densityData[i]),
				NumberTools.formatPercent(dist.densityData[i]/sum,3)
		});
		table.addLine(new String[]{Language.tr("Statistics.Average"),NumberTools.formatNumber(dist.getMean(),3),""});
		table.addLine(new String[]{Language.tr("Distribution.StdDev"),NumberTools.formatNumber(dist.getStandardDeviation(),3),""});
		table.addLine(new String[]{Language.tr("Distribution.CV"),NumberTools.formatNumber(performanceIndicator.getCV()),""});

		setData(table,new String[]{Language.tr("Statistic.NumberOfSeconds"),Language.tr("Statistic.Viewer.Chart.Number"),Language.tr("Statistic.Viewer.Chart.Part")});
	}

	private void buildAutoCorrelationTable() {
		final Table table=new Table();
		final List<String> cols=new ArrayList<>();

		cols.add(Language.tr("Statistics.AutoCorrelation.Distance"));
		final List<String> line=new ArrayList<>();
		if (statistics.waitingTimeAll.isCorrelationAvailable()) {
			final int length=statistics.waitingTimeAll.getCorrelationData().length;
			for (int i=0;i<length;i++) line.add(NumberTools.formatLong(i*StatisticsDataPerformanceIndicator.CORRELATION_RANGE_STEPPING));
		}
		table.addLine(line);

		cols.add(Language.tr("Statistics.AutoCorrelation.WaitingTimes"));
		table.addLine(Arrays.stream(statistics.waitingTimeAll.getCorrelationData()).mapToObj(d->NumberTools.formatPercent(d,3)).collect(Collectors.toList()));

		setData(table.transpose(),cols);
	}

	@Override
	protected void buildTable() {
		switch (mode) {
		case MODE_CALLER:
			buildCallerCount();
			break;
		case MODE_QUEUE:
		case MODE_WIP:
		case MODE_WORKLOAD:
			buildLengthTable(mode);
			break;
		case MODE_INTERARRIVALTIMES:
		case MODE_INTERLEAVETIMES:
		case MODE_WAITINGTIMES_SUCCESS:
		case MODE_WAITINGTIMES_CANCEL:
		case MODE_WAITINGTIMES_ALL:
		case MODE_WORKINGTIMES:
		case MODE_POSTPROCESSINGTIMES:
		case MODE_SYSTEMTIMES_SUCCESS:
		case MODE_SYSTEMTIMES_ALL:
			buildTimesTable(mode);
			break;
		case MODE_AUTOCORRELATION:
			buildAutoCorrelationTable();
			break;
		}
	}
}
