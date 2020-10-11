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

import org.jfree.data.xy.XYSeries;

import language.Language;
import simulator.statistics.Statistics;
import statistics.StatisticsDataPerformanceIndicator;
import systemtools.statistics.StatisticViewerLineChart;

/**
 * Dieser Viewer gibt Verteilungen in Form von Liniendiagrammen zu den Simulationsergebnissen aus.
 * @see StatisticViewerLineChart
 * @author Alexander Herzog
 */
public class ViewerLineChart extends StatisticViewerLineChart {
	/** Statistikobjekt, aus dem die anzuzeigenden Daten entnommen werden sollen */
	private final Statistics statistics;
	/** Gibt an, welche Daten genau ausgegeben werden sollen */
	private final Mode mode;

	/**
	 * Wählt die von {@link ViewerLineChart} auszugebende Information aus.
	 * @author Alexander Herzog
	 * @see ViewerLineChart#ViewerLineChart(Statistics, Mode)
	 */
	public enum Mode {
		/** Verteilung der Zwischenankunftszeiten */
		MODE_INTERARRIVALTIMES,
		/** Verteilung der Zwischenabgangszeiten */
		MODE_INTERLEAVETIMES,
		/** Verteilung der Wartezeiten der erfolgreichen Kunden */
		MODE_WAITINGTIMES_SUCCESS,
		/** Verteilung der Abbruchzeiten */
		MODE_WAITINGTIMES_CANCEL,
		/** Verteilung der Wartezeiten über alle Kunden */
		MODE_WAITINGTIMES_ALL,
		/** Verteilung der Bedienzeiten */
		MODE_WORKINGTIMES,
		/** Verteilung der Nachbearbeitungszeiten */
		MODE_POSTPROCESSINGTIMES,
		/** Verteilung der Verweilzeiten der erfolgreichen Kunden */
		MODE_SYSTEMTIMES_SUCCESS,
		/** Verteilung der Verweilzeiten über alle Kunden */
		MODE_SYSTEMTIMES_ALL,
		/** Autokorrelation der Wartezeiten */
		MODE_AUTOCORRELATION
	}

	/**
	 * Konstruktor der Klasse
	 * @param statistics	Statistikobjekt, aus dem die anzuzeigenden Daten entnommen werden sollen
	 * @param mode	Gibt an, welche Daten genau ausgegeben werden sollen
	 * @see Mode
	 */
	public ViewerLineChart(final Statistics statistics, final Mode mode) {
		this.statistics=statistics;
		this.mode=mode;
	}

	/**
	 * Verteilung der Zwischenankunftszeiten
	 * @see Mode#MODE_INTERARRIVALTIMES
	 * @see #firstChartRequest()
	 */
	private void buildInterArrivalTimes() {
		initLineChart(Language.tr("Statistics.DistributionOfTheInterArrivalTimes"));
		setupChartTimePercent(Language.tr("Statistics.DistributionOfTheInterArrivalTimes"),Language.tr("Statistics.Distance"),Language.tr("Statistics.Part"));
		addSeriesTruncated(Language.tr("Statistics.DistributionOfTheInterArrivalTimes"),Color.RED,statistics.interarrivalTime.getNormalizedDistribution(),1800);
		addFillColor(0);
		initTooltips();
	}

	/**
	 * Verteilung der Zwischenabgangszeiten
	 * @see Mode#MODE_INTERLEAVETIMES
	 * @see #firstChartRequest()
	 */
	private void buildInterLeaveTimes() {
		initLineChart(Language.tr("Statistics.DistributionOfTheInterLeaveTimes"));
		setupChartTimePercent(Language.tr("Statistics.DistributionOfTheInterLeaveTimes"),Language.tr("Statistics.Distance"),Language.tr("Statistics.Part"));
		addSeriesTruncated(Language.tr("Statistics.DistributionOfTheInterLeaveTimes"),Color.RED,statistics.interleaveTime.getNormalizedDistribution(),1800);
		addFillColor(0);
		initTooltips();
	}

	/**
	 * Verteilung der Wartezeiten der erfolgreichen Kunden
	 * @see Mode#MODE_WAITINGTIMES_SUCCESS
	 * @see #firstChartRequest()
	 */
	private void buildWaitingTimesSuccess() {
		initLineChart(Language.tr("SimStatistic.WaitingTimes.Distribution.Successful"));
		setupChartTimePercent(Language.tr("SimStatistic.WaitingTimes.Distribution.Successful"),Language.tr("SimStatistic.WaitingTime"),Language.tr("Statistics.Part"));
		addSeriesTruncated(Language.tr("SimStatistic.WaitingTimes.Distribution.Successful"),Color.RED,statistics.waitingTimeSuccess.getNormalizedDistribution(),1800);
		addFillColor(0);
		initTooltips();
	}

	/**
	 * Verteilung der Abbruchzeiten
	 * @see Mode#MODE_WAITINGTIMES_CANCEL
	 * @see #firstChartRequest()
	 */
	private void buildWaitingTimesCancel() {
		initLineChart(Language.tr("SimStatistic.CancelationTimes.Distribution"));
		setupChartTimePercent(Language.tr("SimStatistic.CancelationTimes.Distribution"),Language.tr("SimStatistic.CancelTime"),Language.tr("Statistics.Part"));
		addSeriesTruncated(Language.tr("SimStatistic.CancelationTimes.Distribution"),Color.RED,statistics.waitingTimeCancel.getNormalizedDistribution(),1800);
		addFillColor(0);
		initTooltips();
	}

	/**
	 * Verteilung der Wartezeiten über alle Kunden
	 * @see Mode#MODE_WAITINGTIMES_ALL
	 * @see #firstChartRequest()
	 */
	private void buildWaitingTimesAll() {
		initLineChart(Language.tr("SimStatistic.WaitingCancelationTimes.Distribution"));
		setupChartTimePercent(Language.tr("SimStatistic.WaitingCancelationTimes.Distribution"),Language.tr("SimStatistic.WaitingCancelationTimes"),Language.tr("Statistics.Part"));
		addSeriesTruncated(Language.tr("SimStatistic.WaitingCancelationTimes.Distribution"),Color.RED,statistics.waitingTimeAll.getNormalizedDistribution(),1800);
		addFillColor(0);
		initTooltips();
	}

	/**
	 * Verteilung der Bedienzeiten
	 * @see Mode#MODE_WORKINGTIMES
	 * @see #firstChartRequest()
	 */
	private void buildWorkingTimes() {
		initLineChart(Language.tr("Statistics.DistributionOfTheProcessTimes"));
		setupChartTimePercent(Language.tr("Statistics.DistributionOfTheProcessTimes"),Language.tr("Statistics.ProcessTime"),Language.tr("Statistics.Part"));
		addSeriesTruncated(Language.tr("Statistics.DistributionOfTheProcessTimes"),Color.RED,statistics.workingTime.getNormalizedDistribution(),1800);
		addFillColor(0);
		initTooltips();
	}

	/**
	 * Verteilung der Nachbearbeitungszeiten
	 * @see Mode#MODE_POSTPROCESSINGTIMES
	 * @see #firstChartRequest()
	 */
	private void buildPostProcessingTimes() {
		initLineChart(Language.tr("Statistics.DistributionOfThePostProcessTimes"));
		setupChartTimePercent(Language.tr("Statistics.DistributionOfThePostProcessTimes"),Language.tr("SimStatistic.PostProcessingTime"),Language.tr("Statistics.Part"));
		addSeriesTruncated(Language.tr("Statistics.DistributionOfThePostProcessTimes"),Color.RED,statistics.postProcessingTime.getNormalizedDistribution(),1800);
		addFillColor(0);
		initTooltips();
	}

	/**
	 * Verteilung der Verweilzeiten der erfolgreichen Kunden
	 * @see Mode#MODE_SYSTEMTIMES_SUCCESS
	 * @see #firstChartRequest()
	 */
	private void buildSystemTimesSuccess() {
		initLineChart(Language.tr("Statistics.ResidenceTimes.Distribution.Successful"));
		setupChartTimePercent(Language.tr("Statistics.ResidenceTimes.Distribution.Successful"),Language.tr("SimStatistic.ResidenceTime"),Language.tr("Statistics.Part"));
		addSeriesTruncated(Language.tr("Statistics.ResidenceTimes.Distribution.Successful"),Color.RED,statistics.systemTimeSuccess.getNormalizedDistribution(),1800);
		addFillColor(0);
		initTooltips();
	}

	/**
	 * Verteilung der Verweilzeiten über alle Kunden
	 * @see Mode#MODE_SYSTEMTIMES_ALL
	 * @see #firstChartRequest()
	 */
	private void buildSystemTimesAll() {
		initLineChart(Language.tr("Statistics.ResidenceTimes.Distribution.All"));
		setupChartTimePercent(Language.tr("Statistics.ResidenceTimes.Distribution.All"),Language.tr("SimStatistic.ResidenceTime"),Language.tr("Statistics.Part"));
		addSeriesTruncated(Language.tr("Statistics.ResidenceTimes.Distribution.All"),Color.RED,statistics.systemTimeAll.getNormalizedDistribution(),1800);
		addFillColor(0);
		initTooltips();
	}

	/**
	 * Fügt eine Autokorrelations-Reihe zu einem Diagramm hinzu
	 * @param indicator	Datenreihe
	 * @param name	Name der Datenreihe
	 * @param color	Farbe für die Datenreihe
	 * @see #buildAutoCorrelation()
	 */
	private void addAutoCorrelationSeries(final StatisticsDataPerformanceIndicator indicator, final String name, final Color color) {
		if (!indicator.isCorrelationAvailable()) return;

		final XYSeries series=addSeries(name,color);

		final double[] data=indicator.getCorrelationData();
		for (int i=0;i<data.length;i++) {
			series.add(i*StatisticsDataPerformanceIndicator.CORRELATION_RANGE_STEPPING,Math.abs(data[i]),false);
		}
		series.fireSeriesChanged();
	}

	/**
	 * Autokorrelation der Wartezeiten
	 * @see Mode#MODE_AUTOCORRELATION
	 * @see #firstChartRequest()
	 */
	private void buildAutoCorrelation() {
		initLineChart(Language.tr("Statistics.AutoCorrelation.WaitingTimes"));
		setupChartValuePercent(Language.tr("Statistics.AutoCorrelation.WaitingTimes"),Language.tr("Statistics.AutoCorrelation.Distance"),Language.tr("Statistics.AutoCorrelation"));
		addAutoCorrelationSeries(statistics.waitingTimeAll,Language.tr("Statistics.AutoCorrelation.WaitingTimes"),Color.RED);
		smartZoom(1);
		initTooltips();
	}

	@Override
	protected void firstChartRequest() {
		switch (mode) {
		case MODE_INTERARRIVALTIMES: buildInterArrivalTimes(); break;
		case MODE_INTERLEAVETIMES: buildInterLeaveTimes(); break;
		case MODE_WAITINGTIMES_SUCCESS: buildWaitingTimesSuccess(); break;
		case MODE_WAITINGTIMES_CANCEL: buildWaitingTimesCancel(); break;
		case MODE_WAITINGTIMES_ALL: buildWaitingTimesAll(); break;
		case MODE_WORKINGTIMES: buildWorkingTimes(); break;
		case MODE_POSTPROCESSINGTIMES: buildPostProcessingTimes(); break;
		case MODE_SYSTEMTIMES_SUCCESS: buildSystemTimesSuccess(); break;
		case MODE_SYSTEMTIMES_ALL: buildSystemTimesAll(); break;
		case MODE_AUTOCORRELATION: buildAutoCorrelation(); break;
		}
	}
}