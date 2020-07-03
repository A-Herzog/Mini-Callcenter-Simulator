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

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.math3.distribution.ExponentialDistribution;

import language.Language;
import mathtools.ErlangC;
import mathtools.NumberTools;
import mathtools.distribution.NeverDistributionImpl;
import mathtools.distribution.OnePointDistributionImpl;
import mathtools.distribution.tools.DistributionTools;
import simulator.statistics.Statistics;
import statistics.StatisticsDataPerformanceIndicator;
import systemtools.statistics.StatisticViewerText;
import ui.help.Help;

/**
 * Dieser Viewer gibt Informationen in Textform zu den Simulationsergebnissen aus.
 * @see ViewerText
 * @author Alexander Herzog
 */
public class ViewerText extends StatisticViewerText {
	private static double[] DISPLAY_QUANTILS=new double[] {0.1,0.25,0.5,0.75,0.9};

	private final Statistics statistics;
	private final Mode mode;
	private final Consumer<Mode> modeClick;

	/**
	 * Wählt die von {@link ViewerText} auszugebende Information aus.
	 * @author Alexander Herzog
	 * @see ViewerText#ViewerText(Statistics, Mode)
	 */
	public enum Mode {
		/** Textseite "Übersicht" **/
		MODE_OVERVIEW,
		/** Textseite "Anruferanzahl" */
		MODE_CALLER,
		/** Textseite "Warteschlange" */
		MODE_QUEUE,
		/** Textseite "Kunden im System" */
		MODE_WIP,
		/** Textseite  */
		MODE_INTERARRIVALTIMES,
		/** Textseite "Zwischenankunftszeiten" */
		MODE_INTERLEAVETIMES,
		/** Textseite "Zwischenabgangszeiten" */
		MODE_WAITINGTIMES,
		/** Textseite "Wartezeiten" */
		MODE_WORKINGTIMES,
		/** Textseite "Verweilzeiten" */
		MODE_SYSTEMTIMES,
		/** Textseite "Auslastung"  */
		MODE_WORKLOAD,
		/** Textseite "Vergleich mit analytischen Modellen" */
		MODE_COMPARE,
		/** Textseite "Systemdaten" */
		MODE_SYSTEM_INFO,
		/** Autokorrelation der Wartezeiten */
		MODE_AUTOCORRELATION
	}

	/**
	 * Konstruktor der Klasse
	 * @param statistics	Statistikobjekt, aus dem die anzuzeigenden Daten entnommen werden sollen
	 * @param mode	Gibt an, welche Daten genau ausgegeben werden sollen
	 * @param modeClick	Callback, das aufgerufen wird, wenn ein "Details"-Link angeklickt wurde
	 * @see Mode
	 */
	public ViewerText(final Statistics statistics, final Mode mode, final Consumer<Mode> modeClick) {
		this.statistics=statistics;
		this.mode=mode;
		this.modeClick=modeClick;
	}

	/**
	 * Konstruktor der Klasse
	 * @param statistics	Statistikobjekt, aus dem die anzuzeigenden Daten entnommen werden sollen
	 * @param mode	Gibt an, welche Daten genau ausgegeben werden sollen
	 * @see Mode
	 */
	public ViewerText(final Statistics statistics, final Mode mode) {
		this(statistics,mode,null);
	}

	private void addModeLink(final Mode mode) {
		addLink(mode.toString(),Language.tr("Statistics.Details"));
	}

	private void addDescription(final String topic) {
		final URL url=ViewerText.class.getResource("description_"+Language.getCurrentLanguage()+"/"+topic+".html");
		addDescription(url,helpTopic->Help.topic(getViewer(false),helpTopic));
	}

	private void buildOverview() {
		addHeading(1,Language.tr("Statistics.ResultsOverview"));

		if (statistics.simulationData.emergencyShutDown) {
			beginParagraph();
			addLine(Language.tr("Statistics.EmergencyShutDown"));
			endParagraph();
		}

		addHeading(2,Language.tr("SimStatistic.NumberOfCallers"));
		beginParagraph();
		addLine(Language.tr("SimStatistic.AllCallTypes")+": "+NumberTools.formatLong(statistics.freshCalls.get()+statistics.callContinued.getSuccess()+statistics.callRetry.getSuccess()));
		addLine(Language.tr("SimStatistic.FreshCalls")+": "+NumberTools.formatLong(statistics.freshCalls.get()));
		if (statistics.callContinued.getSuccess()>0) addLine(Language.tr("SimStatistic.ForwardedCalls")+": "+NumberTools.formatLong(statistics.callContinued.getSuccess()));
		if (statistics.callRetry.getSuccess()>0) addLine(Language.tr("SimStatistic.Retryer")+": "+NumberTools.formatLong(statistics.callRetry.getSuccess()));
		addLine(Language.tr("SimStatistic.Accessibility")+" 1-P(A): "+NumberTools.formatPercent(statistics.callSuccessful.getSuccessPart())+" ("+NumberTools.formatLong(statistics.callSuccessful.getSuccess())+" "+Language.tr("SimStatistic.of")+" "+NumberTools.formatLong(statistics.callSuccessful.getAll())+" "+Language.tr("SimStatistic.of.Calls")+")");
		addLine(Language.tr("SimStatistic.CancelationRate")+" P(A): "+NumberTools.formatPercent(1-statistics.callSuccessful.getSuccessPart()));
		if (statistics.callRejected.getSuccess()>0) {
			addLine(Language.tr("SimStatistic.BusySignalQuota")+": "+NumberTools.formatPercent((double)statistics.callRejected.getSuccess()/statistics.callSuccessful.getAll())+" ("+NumberTools.formatLong(statistics.callRejected.getSuccess())+" "+Language.tr("SimStatistic.of")+" "+NumberTools.formatLong(statistics.callSuccessful.getAll())+" "+Language.tr("SimStatistic.of.Calls")+")");
		}
		addModeLink(Mode.MODE_CALLER);
		endParagraph();

		if (statistics.callSuccessful.getAll()>statistics.freshCalls.get()+statistics.callContinued.getSuccess()+statistics.callRetry.getSuccess()) {
			beginParagraph();
			addLines(Language.tr("SimStatistic.Info.MoreCallerThanFreshCalls"));
			endParagraph();
		}

		/*
		addHeading(2,Language.tr("Statistics.InterArrivalTimes"));
		beginParagraph();
		addLine(Language.tr("Distribution.AverageSomething")+" "+Language.tr("Statistics.InterArrivalTime.lower")+": E[I]="+NumberTools.formatNumber(statistics.interarrivalTime.getMean(),3));
		addLine(Language.tr("Distribution.StdDev")+": Std[I]="+NumberTools.formatNumber(statistics.interarrivalTime.getSD(),3));
		addLine(Language.tr("Distribution.CV")+": CV[I]="+NumberTools.formatNumber(statistics.interarrivalTime.getCV(),3));
		endParagraph();
		 */

		addHeading(2,Language.tr("Statistics.WaitingTimes"));
		beginParagraph();
		if (statistics.callNeedToWait.getSuccess()>0) {
			addLine(Language.tr("SimStatistic.ClientsWhoHaveToWait")+": "+NumberTools.formatPercent(statistics.callNeedToWait.getSuccessPart())+" ("+NumberTools.formatLong(statistics.callNeedToWait.getSuccess())+" "+Language.tr("SimStatistic.of")+" "+NumberTools.formatLong(statistics.callNeedToWait.getAll())+" "+Language.tr("SimStatistic.of.Calls")+")");
		}
		if (statistics.editModel.waitingRoomSize<0 && (statistics.editModel.waitingTimeDist instanceof NeverDistributionImpl)) {
			addLine(Language.tr("Distribution.AverageSomething")+" "+Language.tr("Statistics.WaitingTime.lower")+": E[W]="+NumberTools.formatNumber(statistics.waitingTimeSuccess.getMean(),3));
			addLine(Language.tr("Distribution.StdDev")+" "+Language.tr("Statistics.WaitingTime")+": Std[W]="+NumberTools.formatNumber(statistics.waitingTimeSuccess.getSD(),3));
			addLine(Language.tr("Distribution.CV")+" "+Language.tr("Statistics.WaitingTime")+": CV[W]="+NumberTools.formatNumber(statistics.waitingTimeSuccess.getCV(),3));

		} else {
			addLine(Language.tr("Distribution.AverageSomething")+" "+Language.tr("Statistics.WaitingTime.lower")+"/"+Language.tr("SimStatistic.CancelTime")+"/"+Language.tr("SimStatistic.overall")+": E[W]="+NumberTools.formatNumber(statistics.waitingTimeSuccess.getMean(),3)+" / E[A]="+NumberTools.formatNumber(statistics.waitingTimeCancel.getMean(),3)+" / "+NumberTools.formatNumber(statistics.waitingTimeAll.getMean(),3));
			addLine(Language.tr("Distribution.StdDev")+" "+Language.tr("Statistics.WaitingTime.lower")+"/"+Language.tr("SimStatistic.CancelTime.lower")+"/"+Language.tr("SimStatistic.overall")+": Std[W]="+NumberTools.formatNumber(statistics.waitingTimeSuccess.getSD(),3)+" / Std[A]="+NumberTools.formatNumber(statistics.waitingTimeCancel.getSD(),3)+" / "+NumberTools.formatNumber(statistics.waitingTimeAll.getSD(),3));
			addLine(Language.tr("Distribution.CV")+" "+Language.tr("Statistics.WaitingTime")+"/"+Language.tr("SimStatistic.CancelTime.lower")+": CV[W]="+NumberTools.formatNumber(statistics.waitingTimeSuccess.getCV(),3)+" / "+NumberTools.formatNumber(statistics.waitingTimeAll.getCV(),3));
		}
		addModeLink(Mode.MODE_WAITINGTIMES);
		endParagraph();

		/*
		addHeading(2,Language.tr("Statistics.ProcessTimes"));
		beginParagraph();
		addLine(Language.tr("Distribution.AverageSomething")+" Bediendauer: E[S]="+NumberTools.formatNumber(statistics.workingTime.getMean(),3));
		addLine(Language.tr("Distribution.StdDev")+" der Bedienzeiten: Std[S]="+NumberTools.formatNumber(statistics.workingTime.getSD(),3));
		addLine(Language.tr("Distribution.CV")+": CV[S]="+NumberTools.formatNumber(statistics.workingTime.getCV(),3));
		endParagraph();
		 */

		addHeading(2,Language.tr("Statistics.ResidenceTimes"));
		beginParagraph();
		if (statistics.editModel.waitingRoomSize<0 && (statistics.editModel.waitingTimeDist instanceof NeverDistributionImpl)) {
			addLine(Language.tr("Distribution.AverageSomething")+" "+Language.tr("SimStatistic.ResidenceTime.lower")+": E[V]="+NumberTools.formatNumber(statistics.systemTimeSuccess.getMean(),3));
			addLine(Language.tr("Distribution.StdDev")+Language.tr("SimStatistic.of.ResidenceTimes")+": Std[V]="+NumberTools.formatNumber(statistics.systemTimeSuccess.getSD(),3));
			addLine(Language.tr("Distribution.CV")+": CV[V]="+NumberTools.formatNumber(statistics.systemTimeSuccess.getCV(),3));
		} else {
			addLine(Language.tr("Distribution.AverageSomething")+" "+Language.tr("SimStatistic.ResidenceTime.successful.lower")+"/"+Language.tr("SimStatistic.overall")+": E[V]="+NumberTools.formatNumber(statistics.systemTimeSuccess.getMean(),3)+" / "+NumberTools.formatNumber(statistics.systemTimeAll.getMean(),3));
			addLine(Language.tr("Distribution.StdDev")+" "+Language.tr("SimStatistic.of.ResidenceTime.successful.lower")+"/"+Language.tr("SimStatistic.overall")+": Std[V]="+NumberTools.formatNumber(statistics.systemTimeSuccess.getSD(),3)+" / "+NumberTools.formatNumber(statistics.systemTimeAll.getSD(),3));
			addLine(Language.tr("Distribution.CV")+" "+Language.tr("SimStatistic.ResidenceTime.successful.lower")+"/"+Language.tr("SimStatistic.overall")+": CV[V]="+NumberTools.formatNumber(statistics.systemTimeSuccess.getCV(),3)+" / "+NumberTools.formatNumber(statistics.systemTimeAll.getCV(),3));
		}
		addModeLink(Mode.MODE_SYSTEMTIMES);
		endParagraph();

		addHeading(2,Language.tr("SimStatistic.Queue"));
		beginParagraph();
		addLine(Language.tr("Distribution.AverageSomething")+" "+Language.tr("Statistic.QueueLength.lower")+": E[NQ]="+NumberTools.formatNumber(statistics.queueLength.getTimeMean(),3));
		addLine(Language.tr("Distribution.StdDev")+" "+Language.tr("Statistic.of.QueueLength.lower")+": Std[NQ]="+NumberTools.formatNumber(statistics.queueLength.getTimeSD(),3));
		addLine(Language.tr("SimStatistic.Queue.EmptyPart")+": P(NQ=0)="+NumberTools.formatPercent(statistics.queueLength.getTimePartForState(0),3));
		addLine(Language.tr("SimStatistic.deMaximale")+" "+Language.tr("Statistic.QueueLength.lower")+": "+NumberTools.formatNumber(statistics.queueLength.getTimeMax()));
		if (statistics.callRejected.getSuccess()>0) {
			addLine(Language.tr("SimStatistic.Queue.Reject")+": "+NumberTools.formatPercent(statistics.callRejected.getSuccessPart())+" ("+NumberTools.formatLong(statistics.callRejected.getSuccess())+" "+Language.tr("SimStatistic.of")+" "+NumberTools.formatLong(statistics.callRejected.getAll())+" "+Language.tr("SimStatistic.of.Calls")+")");
		}
		addModeLink(Mode.MODE_QUEUE);
		endParagraph();

		if (statistics.editModel.waitingRoomSize>=0) {
			beginParagraph();
			addLines(Language.tr("SimStatistic.Queue.Reject.Info"));
			endParagraph();
		}

		addHeading(2,Language.tr("Statistics.ClientsInSystem"));
		beginParagraph();
		addLine(Language.tr("Distribution.AverageSomething")+" "+Language.tr("Statistics.NumberOfClientsInTheSystem.lower")+": E[N]="+NumberTools.formatNumber(statistics.systemLength.getTimeMean(),3));
		addLine(Language.tr("Distribution.StdDev")+" "+Language.tr("Statistics.ClientsInSystem.of")+": Std[N]="+NumberTools.formatNumber(statistics.systemLength.getTimeSD(),3));
		addLine(Language.tr("Statistics.ClientsInSystem.EmptyPart")+": P(N=0)="+NumberTools.formatPercent(statistics.systemLength.getTimePartForState(0),3));
		addLine(Language.tr("SimStatistic.deMaximale")+" "+Language.tr("Statistics.NumberOfClientsInTheSystem.lower")+": "+NumberTools.formatNumber(statistics.systemLength.getTimeMax()));
		addModeLink(Mode.MODE_WIP);
		endParagraph();

		addHeading(2,Language.tr("SimStatistic.WorkLoad"));
		beginParagraph();
		addLine(Language.tr("SimStatistic.WorkLoad.IdlePart")+": "+NumberTools.formatPercent(statistics.busyAgents.getTimePartForState(0),3)+" ("+Language.tr("SimStatistic.WorkLoad.IdlePart.Info")+")");
		addLine(Language.tr("SimStatistic.WorkLoad.FullPart")+": "+NumberTools.formatPercent(statistics.freeAgents.getTimePartForState(0),3)+" ("+Language.tr("SimStatistic.WorkLoad.FullPart.Info")+")");
		addLine(Language.tr("Distribution.AverageSomething")+" "+Language.tr("Statistics.NumberOfClientsInServiceProcess.lower")+": E[B]="+NumberTools.formatNumber(statistics.busyAgents.getTimeMean()*statistics.editModel.batchWorking,3));
		addLine(Language.tr("Distribution.AverageSomething")+" "+Language.tr("SimStatistic.NumberOfBusyAgents.lower")+": "+NumberTools.formatNumber(statistics.busyAgents.getTimeMean(),3)+" (rho="+NumberTools.formatPercent(statistics.busyAgents.getTimeMean()/statistics.editModel.agents,3)+")");
		addLine(Language.tr("Distribution.StdDev")+" "+Language.tr("SimStatistic.NumberOfBusyAgents.of")+": "+NumberTools.formatNumber(statistics.busyAgents.getTimeSD(),3));
		addLine(Language.tr("Distribution.AverageSomething")+" "+Language.tr("SimStatistic.NumberOfIdleAgents.lower")+": "+NumberTools.formatNumber(statistics.freeAgents.getTimeMean(),3));
		addModeLink(Mode.MODE_WORKLOAD);
		endParagraph();

		/* Infotext  */
		addDescription("Overview");
	}

	@Override
	protected void processLinkClick(final String link) {
		for (Mode mode: Mode.values()) if (mode.toString().equals(link)) {
			if (modeClick!=null) modeClick.accept(mode);
			break;
		}
	}

	private void buildCallerCount() {
		addHeading(1,Language.tr("SimStatistic.NumberOfCallers"));

		beginParagraph();
		addLine(Language.tr("SimStatistic.FreshCalls")+": "+NumberTools.formatLong(statistics.freshCalls.get()));
		addLine(Language.tr("SimStatistic.ForwardedCalls")+": "+NumberTools.formatLong(statistics.callContinued.getSuccess()));
		addLine(Language.tr("SimStatistic.Retryer")+": "+NumberTools.formatLong(statistics.callRetry.getSuccess()));
		addLine(Language.tr("Statistic.Sum")+": "+NumberTools.formatLong(statistics.freshCalls.get()+statistics.callContinued.getSuccess()+statistics.callRetry.getSuccess()));
		endParagraph();

		addHeading(3,Language.tr("SimStatistic.ForComparison"));
		beginParagraph();
		addLine(Language.tr("SimStatistic.NumberOfCallers.byModel")+": "+NumberTools.formatLong(statistics.editModel.callsToSimulate));
		endParagraph();
		beginParagraph();
		addLines(Language.tr("SimStatistic.NumberOfCallers.byModel.Info"));
		endParagraph();

		addHeading(2,Language.tr("SimStatistic.Queue"));
		beginParagraph();
		addLine(Language.tr("SimStatistic.BusySignalQuota")+": "+NumberTools.formatPercent((double)statistics.callRejected.getSuccess()/statistics.callSuccessful.getAll())+" ("+NumberTools.formatLong(statistics.callRejected.getSuccess())+" "+Language.tr("SimStatistic.of")+" "+NumberTools.formatLong(statistics.callSuccessful.getAll())+" "+Language.tr("SimStatistic.of.Calls")+")");
		addLine(Language.tr("SimStatistic.ClientsWhoHaveToWait")+": "+NumberTools.formatPercent(statistics.callNeedToWait.getSuccessPart())+" ("+NumberTools.formatLong(statistics.callNeedToWait.getSuccess())+" "+Language.tr("SimStatistic.of")+" "+NumberTools.formatLong(statistics.callNeedToWait.getAll())+" "+Language.tr("SimStatistic.of.Calls")+")");
		endParagraph();

		addHeading(2,Language.tr("SimStatistic.Service"));
		beginParagraph();
		addLine(Language.tr("SimStatistic.Accessibility")+" 1-P(A): "+NumberTools.formatPercent(statistics.callSuccessful.getSuccessPart())+" ("+NumberTools.formatLong(statistics.callSuccessful.getSuccess())+" "+Language.tr("SimStatistic.of")+" "+NumberTools.formatLong(statistics.callSuccessful.getAll())+" "+Language.tr("SimStatistic.of.Calls")+")");
		addLine(Language.tr("SimStatistic.ForwardingRate")+": "+NumberTools.formatPercent(statistics.callContinued.getSuccessPart())+" ("+NumberTools.formatLong(statistics.callContinued.getSuccess())+" "+Language.tr("SimStatistic.of")+" "+NumberTools.formatLong(statistics.callContinued.getAll())+" "+Language.tr("SimStatistic.of.Calls")+")");
		endParagraph();

		addHeading(3,Language.tr("SimStatistic.ForComparison"));
		beginParagraph();
		addLine(Language.tr("SimStatistic.ForwardingRate.byModel")+": "+NumberTools.formatPercent(statistics.editModel.callContinueProbability));
		endParagraph();

		addHeading(2,Language.tr("SimStatistic.Retrys"));
		beginParagraph();
		addLine(Language.tr("SimStatistic.RetryRate")+": "+NumberTools.formatPercent(statistics.callRetry.getSuccessPart())+" ("+NumberTools.formatLong(statistics.callRetry.getSuccess())+" "+Language.tr("SimStatistic.of")+" "+NumberTools.formatLong(statistics.callRetry.getAll())+" "+Language.tr("SimStatistic.of.Calls")+")");
		endParagraph();

		addHeading(3,Language.tr("SimStatistic.ForComparison"));
		beginParagraph();
		addLine(Language.tr("SimStatistic.RetryRateByModel")+": "+NumberTools.formatPercent(statistics.editModel.retryProbability));
		endParagraph();

		/* Infotext  */
		addDescription("CallerCount");
	}

	private void buildQueue() {
		addHeading(1,Language.tr("SimStatistic.Queue"));
		beginParagraph();
		addLine(Language.tr("Distribution.AverageSomething")+" "+Language.tr("Statistic.QueueLength.lower")+": E[NQ]="+NumberTools.formatNumber(statistics.queueLength.getTimeMean(),3));
		addLine(Language.tr("Distribution.StdDev")+" "+Language.tr("Statistic.QueueLength.of")+": Std[NQ]="+NumberTools.formatNumber(statistics.queueLength.getTimeSD(),3));
		addLine(Language.tr("Distribution.CV")+" "+Language.tr("Statistic.QueueLength.of")+": CV[NQ]="+NumberTools.formatNumber(statistics.queueLength.getTimeCV(),3));
		endParagraph();

		beginParagraph();
		addLine(Language.tr("SimStatistic.Queue.EmptyPart")+": P(NQ=0)="+NumberTools.formatPercent(statistics.queueLength.getTimePartForState(0),3));
		addLine(Language.tr("SimStatistic.deMaximale")+" "+Language.tr("Statistic.QueueLength.lower")+": "+NumberTools.formatNumber(statistics.queueLength.getTimeMax()));
		endParagraph();

		beginParagraph();
		addLine(Language.tr("SimStatistic.Queue.Reject")+": "+NumberTools.formatPercent(statistics.callRejected.getSuccessPart())+" ("+NumberTools.formatLong(statistics.callRejected.getSuccess())+" "+Language.tr("SimStatistic.of")+" "+NumberTools.formatLong(statistics.callRejected.getAll())+" "+Language.tr("SimStatistic.of.Calls")+")");
		addLine(Language.tr("SimStatistic.ClientsWhoHaveToWait")+": "+NumberTools.formatPercent(statistics.callNeedToWait.getSuccessPart())+" ("+NumberTools.formatLong(statistics.callNeedToWait.getSuccess())+" "+Language.tr("SimStatistic.of")+" "+NumberTools.formatLong(statistics.callNeedToWait.getAll())+" "+Language.tr("SimStatistic.of.Calls")+")");
		endParagraph();

		beginParagraph();
		for (double p: DISPLAY_QUANTILS) addLine(String.format("%s-"+Language.tr("Statistic.Quantile")+" "+Language.tr("Statistic.QueueLength.of")+": %d",NumberTools.formatPercent(p),statistics.queueLength.getQuantil(p)));
		endParagraph();

		/* Infotext  */
		addDescription("Queue");
	}

	private void buildWIP() {
		addHeading(1,Language.tr("Statistics.ClientsInSystem"));
		beginParagraph();
		addLine(Language.tr("Distribution.AverageSomething")+" "+Language.tr("Statistics.NumberOfClientsInTheSystem.lower")+": E[N]="+NumberTools.formatNumber(statistics.systemLength.getTimeMean(),3));
		addLine(Language.tr("Distribution.StdDev")+" "+Language.tr("Statistics.ClientsInSystem.of")+": Std[N]="+NumberTools.formatNumber(statistics.systemLength.getTimeSD(),3));
		addLine(Language.tr("Distribution.CV")+" "+Language.tr("Statistics.ClientsInSystem.of")+": CV[N]="+NumberTools.formatNumber(statistics.systemLength.getTimeCV(),3));
		endParagraph();

		beginParagraph();
		addLine(Language.tr("Statistics.ClientsInSystem.EmptyPart")+": P(N=0)="+NumberTools.formatPercent(statistics.systemLength.getTimePartForState(0),3));
		addLine(Language.tr("SimStatistic.deMaximale")+" "+Language.tr("Statistics.NumberOfClientsInTheSystem.lower")+": "+NumberTools.formatNumber(statistics.systemLength.getTimeMax()));
		endParagraph();

		beginParagraph();
		for (double p: DISPLAY_QUANTILS) addLine(String.format("%s-"+Language.tr("Statistic.Quantile")+" "+Language.tr("Statistics.ClientsInSystem.of")+": %d",NumberTools.formatPercent(p),statistics.systemLength.getQuantil(p)));
		endParagraph();

		/* Infotext  */
		addDescription("WIP");
	}

	private void buildInterArrivalTimes() {
		addHeading(1,Language.tr("Statistics.InterArrivalTimes"));
		beginParagraph();
		addLine(Language.tr("Distribution.AverageSomething")+" "+Language.tr("Statistics.InterArrivalTime.lower")+": E[I]="+NumberTools.formatNumber(statistics.interarrivalTime.getMean(),3));
		addLine(Language.tr("Distribution.StdDev")+": Std[I]="+NumberTools.formatNumber(statistics.interarrivalTime.getSD(),3));
		addLine(Language.tr("Distribution.CV")+": CV[I]="+NumberTools.formatNumber(statistics.interarrivalTime.getCV(),3));
		addLine(Language.tr("SimStatistic.deMinimale")+" "+Language.tr("Statistics.InterArrivalTime")+": "+NumberTools.formatNumber(statistics.interarrivalTime.getMin(),3));
		addLine(Language.tr("SimStatistic.deMaximale")+" "+Language.tr("Statistics.InterArrivalTime")+": "+NumberTools.formatNumber(statistics.interarrivalTime.getMax(),3));
		endParagraph();

		addHeading(3,Language.tr("SimStatistic.ForComparison"));
		beginParagraph();
		addLine(Language.tr("Statistics.InterArrivalTimes.Distribution.byModel")+": "+DistributionTools.getDistributionName(statistics.editModel.interArrivalTimeDist));
		addLine("("+DistributionTools.getDistributionLongInfo(statistics.editModel.interArrivalTimeDist)+")");
		endParagraph();

		/* Infotext  */
		addDescription("InterArrivalTimes");
	}

	private void buildInterLeaveTimes() {
		addHeading(1,"Statistics.InterLeaveTimes");
		beginParagraph();
		addLine(Language.tr("Distribution.AverageSomething")+" "+Language.tr("Statistics.InterLeaveTime.lower")+": E[L]="+NumberTools.formatNumber(statistics.interleaveTime.getMean(),3));
		addLine(Language.tr("Distribution.StdDev")+": Std[L]="+NumberTools.formatNumber(statistics.interleaveTime.getSD(),3));
		addLine(Language.tr("Distribution.CV")+": CV[L]="+NumberTools.formatNumber(statistics.interleaveTime.getCV(),3));
		addLine(Language.tr("SimStatistic.deMinimale")+" "+Language.tr("Statistics.InterLeaveTime.lower")+": "+NumberTools.formatNumber(statistics.interleaveTime.getMin(),3));
		addLine(Language.tr("SimStatistic.deMaximale")+" "+Language.tr("Statistics.InterLeaveTime.lower")+": "+NumberTools.formatNumber(statistics.interleaveTime.getMax(),3));
		endParagraph();

		/* Infotext  */
		addDescription("InterLeaveTimes");
	}

	private void buildWaitingTimes() {
		addHeading(1,Language.tr("Statistics.WaitingTimes"));

		beginParagraph();
		addLine(Language.tr("SimStatistic.ClientsWhoHaveToWait")+": "+NumberTools.formatPercent(statistics.callNeedToWait.getSuccessPart())+" ("+NumberTools.formatLong(statistics.callNeedToWait.getSuccess())+" "+Language.tr("SimStatistic.of")+" "+NumberTools.formatLong(statistics.callNeedToWait.getAll())+" "+Language.tr("SimStatistic.of.Calls")+")");
		endParagraph();

		addHeading(2,Language.tr("Statistics.WaitingTimes.Successful"));
		beginParagraph();
		addLine(Language.tr("Statistics.Number.Successful")+": "+NumberTools.formatLong(statistics.waitingTimeSuccess.getCount()));
		addLine(Language.tr("Distribution.AverageSomething")+" "+Language.tr("Statistics.WaitingTime.lower")+": E[W]="+NumberTools.formatNumber(statistics.waitingTimeSuccess.getMean(),3));
		addLine(Language.tr("Distribution.StdDev")+": Std[W]="+NumberTools.formatNumber(statistics.waitingTimeSuccess.getSD(),3));
		addLine(Language.tr("Distribution.CV")+": CV[W]="+NumberTools.formatNumber(statistics.waitingTimeSuccess.getCV()));
		addLine(Language.tr("SimStatistic.deMaximale")+" "+Language.tr("SimStatistic.WaitingTime.lower")+": "+NumberTools.formatNumber(statistics.waitingTimeSuccess.getMax()));
		endParagraph();

		addHeading(2,Language.tr("Statistics.WaitingTimes.NotSuccessful"));
		beginParagraph();
		addLine(Language.tr("Statistics.Number.Cancelation")+": "+NumberTools.formatLong(statistics.waitingTimeCancel.getCount()));
		addLine(Language.tr("Distribution.AverageSomething")+" "+Language.tr("SimStatistic.CancelTime.lower")+": E[A]="+NumberTools.formatNumber(statistics.waitingTimeCancel.getMean(),3));
		addLine(Language.tr("Distribution.StdDev")+": Std[A]="+NumberTools.formatNumber(statistics.waitingTimeCancel.getSD(),3));
		addLine(Language.tr("Distribution.CV")+": CV[A]="+NumberTools.formatNumber(statistics.waitingTimeCancel.getCV()));
		addLine(Language.tr("SimStatistic.deMaximale")+" "+Language.tr("SimStatistic.CancelTime")+": "+NumberTools.formatNumber(statistics.waitingTimeCancel.getMax()));
		endParagraph();

		addHeading(2,Language.tr("Statistics.WaitingTimes.All"));
		beginParagraph();
		addLine(Language.tr("Statistics.Number.All")+": "+NumberTools.formatLong(statistics.waitingTimeAll.getCount()));
		addLine(Language.tr("Distribution.AverageSomething")+" "+Language.tr("Statistics.WaitingTimes.All.lower")+": "+NumberTools.formatNumber(statistics.waitingTimeAll.getMean(),3));
		addLine(Language.tr("Distribution.StdDev")+": "+NumberTools.formatNumber(statistics.waitingTimeAll.getSD(),3));
		addLine(Language.tr("Distribution.CV")+": "+NumberTools.formatNumber(statistics.waitingTimeAll.getCV()));
		addLine(Language.tr("SimStatistic.deMaximale")+" "+Language.tr("Statistics.WaitingTimes.All.lower")+": "+NumberTools.formatNumber(statistics.waitingTimeAll.getMax()));
		endParagraph();

		addHeading(2,Language.tr("SimStatistic.ServiceLevel"));
		beginParagraph();
		addLine(Language.tr("SimStatistic.ServiceLevel.20Seconds")+": "+NumberTools.formatPercent(statistics.callServiceLevel.getSuccessPart())+" ("+NumberTools.formatLong(statistics.callServiceLevel.getSuccess())+" "+Language.tr("SimStatistic.of")+" "+NumberTools.formatLong(statistics.callServiceLevel.getAll())+" "+Language.tr("SimStatistic.of.Calls")+")");
		endParagraph();

		beginParagraph();
		for (double p: DISPLAY_QUANTILS) addLine(String.format("%s-"+Language.tr("Statistic.Quantile")+" "+Language.tr("Statistics.WaitingTimes.All.of")+": %s",NumberTools.formatPercent(p),NumberTools.formatNumber(statistics.waitingTimeAll.getQuantil(p))));
		endParagraph();

		/* Infotext  */
		addDescription("WaitingTimes");
	}

	private void buildWorkingTimes() {
		addHeading(1,Language.tr("SimStatistic.ServiceAndPostProcessingTimes"));

		beginParagraph();
		addLine(Language.tr("Distribution.AverageSomething")+" "+Language.tr("SimStatistic.ServiceTime.lower")+": E[S]="+NumberTools.formatNumber(statistics.workingTime.getMean(),3));
		addLine(Language.tr("Distribution.StdDev")+" "+Language.tr("SimStatistic.ServiceTime.of")+": Std[S]="+NumberTools.formatNumber(statistics.workingTime.getSD(),3));
		addLine(Language.tr("Distribution.CV")+": CV[S]="+NumberTools.formatNumber(statistics.workingTime.getCV(),3));
		addLine(Language.tr("SimStatistic.Shortest")+" "+Language.tr("SimStatistic.ServiceTime.lower")+": "+NumberTools.formatNumber(statistics.workingTime.getMin(),3));
		addLine(Language.tr("SimStatistic.Longest")+" "+Language.tr("SimStatistic.ServiceTime.lower")+": "+NumberTools.formatNumber(statistics.workingTime.getMax(),3));
		endParagraph();

		beginParagraph();
		addLine(Language.tr("Distribution.AverageSomething")+" "+Language.tr("SimStatistic.PostProcessingTime.lower")+": E[S2]="+NumberTools.formatNumber(statistics.postProcessingTime.getMean(),3));
		addLine(Language.tr("Distribution.StdDev")+" "+Language.tr("SimStatistic.PostProcessingTime.of")+": Std[S2]="+NumberTools.formatNumber(statistics.postProcessingTime.getSD(),3));
		addLine(Language.tr("Distribution.CV")+": CV[S2]="+NumberTools.formatNumber(statistics.postProcessingTime.getCV(),3));
		addLine(Language.tr("SimStatistic.Shortest")+" "+Language.tr("SimStatistic.PostProcessingTime")+": "+NumberTools.formatNumber(statistics.postProcessingTime.getMin(),3));
		addLine(Language.tr("SimStatistic.Longest")+" "+Language.tr("SimStatistic.PostProcessingTime")+": "+NumberTools.formatNumber(statistics.postProcessingTime.getMax(),3));
		endParagraph();

		beginParagraph();
		for (double p: DISPLAY_QUANTILS) addLine(String.format("%s-"+Language.tr("Statistic.Quantile")+" "+Language.tr("SimStatistic.ServiceTime.of")+": %s",NumberTools.formatPercent(p),NumberTools.formatNumber(statistics.workingTime.getQuantil(p))));
		endParagraph();

		beginParagraph();
		for (double p: DISPLAY_QUANTILS) addLine(String.format("%s-"+Language.tr("Statistic.Quantile")+" "+Language.tr("SimStatistic.PostProcessingTime.of")+": %s",NumberTools.formatPercent(p),NumberTools.formatNumber(statistics.postProcessingTime.getQuantil(p))));
		endParagraph();

		/* Infotext  */
		addDescription("WorkingTimes");
	}

	private void buildSystemTimes() {
		addHeading(1,Language.tr("Statistics.ResidenceTimes"));

		addHeading(2,Language.tr("Statistics.ResidenceTimes.successful"));
		beginParagraph();
		addLine(Language.tr("Distribution.AverageSomething")+" "+Language.tr("SimStatistic.ResidenceTime.lower")+": E[V]="+NumberTools.formatNumber(statistics.systemTimeSuccess.getMean(),3));
		addLine(Language.tr("Distribution.StdDev")+" "+Language.tr("SimStatistic.ResidenceTime.of")+": Std[V]="+NumberTools.formatNumber(statistics.systemTimeSuccess.getSD(),3));
		addLine(Language.tr("Distribution.CV")+": CV[V]="+NumberTools.formatNumber(statistics.systemTimeSuccess.getCV(),3));
		addLine(Language.tr("SimStatistic.Shortest")+" "+Language.tr("Statistics.ResidenceTimes.successful.lower")+": "+NumberTools.formatNumber(statistics.systemTimeSuccess.getMin(),3));
		addLine(Language.tr("SimStatistic.Longest")+" "+Language.tr("Statistics.ResidenceTimes.successful.lower")+": "+NumberTools.formatNumber(statistics.systemTimeSuccess.getMax(),3));
		endParagraph();

		addHeading(2,Language.tr("Statistics.ResidenceTimes.notsuccessful"));
		beginParagraph();
		addLines(Language.tr("Statistics.ResidenceTimes.notsuccessful.info"));
		endParagraph();

		addHeading(2,Language.tr("Statistics.ResidenceTimes.all"));
		beginParagraph();
		addLine(Language.tr("Distribution.AverageSomething")+" "+Language.tr("SimStatistic.ResidenceTime.lower")+": E[V]="+NumberTools.formatNumber(statistics.systemTimeAll.getMean(),3));
		addLine(Language.tr("Distribution.StdDev")+" "+Language.tr("SimStatistic.ResidenceTime.of")+": Std[V]="+NumberTools.formatNumber(statistics.systemTimeAll.getSD(),3));
		addLine(Language.tr("Distribution.CV")+": CV[V]="+NumberTools.formatNumber(statistics.systemTimeAll.getCV(),3));
		addLine(Language.tr("SimStatistic.Shortest")+" "+Language.tr("SimStatistic.ResidenceTime.lower")+": "+NumberTools.formatNumber(statistics.systemTimeAll.getMin(),3));
		addLine(Language.tr("SimStatistic.Longest")+" "+Language.tr("SimStatistic.ResidenceTime.lower")+": "+NumberTools.formatNumber(statistics.systemTimeAll.getMax(),3));
		endParagraph();

		beginParagraph();
		for (double p: DISPLAY_QUANTILS) addLine(String.format("%s-"+Language.tr("Statistic.Quantile")+" "+Language.tr("Statistics.ResidenceTimes.all.of")+": %s",NumberTools.formatPercent(p),NumberTools.formatNumber(statistics.systemTimeAll.getQuantil(p))));
		endParagraph();

		addHeading(3,Language.tr("Statistics.ResidenceTimes.info.title"));
		beginParagraph();
		addLines(Language.tr("Statistics.ResidenceTimes.info"));
		endParagraph();

		/* Infotext  */
		addDescription("SystemTimes");
	}

	private void buildWorkLoad() {
		addHeading(1,Language.tr("SimStatistic.WorkLoad"));

		beginParagraph();
		addLine(Language.tr("SimStatistic.WorkLoad.IdlePart")+": "+NumberTools.formatPercent(statistics.busyAgents.getTimePartForState(0),3)+" ("+Language.tr("SimStatistic.WorkLoad.IdlePart.Info")+")");
		addLine(Language.tr("SimStatistic.WorkLoad.FullPart")+": "+NumberTools.formatPercent(statistics.freeAgents.getTimePartForState(0),3)+" ("+Language.tr("SimStatistic.WorkLoad.FullPart.Info")+")");
		if (statistics.editModel.batchWorking>1) addLine(Language.tr("SimStatistic.BatchService")+": "+statistics.editModel.batchWorking);
		addLine(Language.tr("SimStatistic.WorkLoad")+" rho="+NumberTools.formatPercent(statistics.busyAgents.getTimeMean()/statistics.editModel.agents,3));
		addLine(Language.tr("Distribution.AverageSomething")+" "+Language.tr("Statistics.NumberOfClientsInServiceProcess.lower")+": E[B]="+NumberTools.formatNumber(statistics.busyAgents.getTimeMean()*statistics.editModel.batchWorking,3));
		endParagraph();

		addHeading(2,Language.tr("Statistic.NumberOfWorkingAgents"));
		beginParagraph();
		addLine(Language.tr("Distribution.AverageSomething")+" "+Language.tr("SimStatistic.NumberOfBusyAgents.lower")+": "+NumberTools.formatNumber(statistics.busyAgents.getTimeMean(),3));
		addLine(Language.tr("Distribution.StdDev")+" "+Language.tr("SimStatistic.NumberOfBusyAgents.of")+": "+NumberTools.formatNumber(statistics.busyAgents.getTimeSD(),3));
		addLine(Language.tr("Distribution.CV")+" "+Language.tr("SimStatistic.NumberOfBusyAgents.of")+": "+NumberTools.formatNumber(statistics.busyAgents.getTimeCV(),3));
		endParagraph();

		addHeading(2,Language.tr("SimStatistic.WorkLoad.IdleAgents"));
		beginParagraph();
		addLine(Language.tr("Distribution.AverageSomething")+" "+Language.tr("SimStatistic.NumberOfIdleAgents.lower")+": "+NumberTools.formatNumber(statistics.freeAgents.getTimeMean(),3));
		addLine(Language.tr("Distribution.StdDev")+" "+Language.tr("SimStatistic.NumberOfIdleAgents.of")+": "+NumberTools.formatNumber(statistics.freeAgents.getTimeSD(),3));
		addLine(Language.tr("Distribution.CV")+" "+Language.tr("SimStatistic.NumberOfIdleAgents.of")+": "+NumberTools.formatNumber(statistics.freeAgents.getTimeCV(),3));
		endParagraph();

		/* Infotext  */
		addDescription("WorkLoad");
	}

	private double powerFactorial(double a, long c) {
		/* a^c/c! */
		double result=1;
		for (int i=1;i<=c;i++) result*=(a/i);
		return result;
	}

	private void buildCompare() {
		addHeading(1,Language.tr("Statistics.AnalyticModelCompare"));

		/* Allgemeine Vorberechnungen */

		double inputContinueProbability=statistics.editModel.callContinueProbability;
		double inputRetryProbability=statistics.editModel.retryProbability;
		int inputWaitingRoomSize=(statistics.editModel.waitingRoomSize<0)?Integer.MAX_VALUE:statistics.editModel.waitingRoomSize;
		int inputAgents=statistics.editModel.agents;

		double inverseLambda=DistributionTools.getMean(statistics.editModel.interArrivalTimeDist);
		double cvIB=DistributionTools.getCV(statistics.editModel.interArrivalTimeDist);
		double inverseMu=DistributionTools.getMean(statistics.editModel.workingTimeDist);
		double cvSB=DistributionTools.getCV(statistics.editModel.workingTimeDist);
		double inverseNu=DistributionTools.getMean(statistics.editModel.waitingTimeDist);

		boolean LambdaIsExp=(statistics.editModel.interArrivalTimeDist instanceof ExponentialDistribution);
		boolean MuIsExp=(statistics.editModel.workingTimeDist instanceof ExponentialDistribution);
		boolean hasPostProcessing=(!(statistics.editModel.postProcessingTimeDist instanceof OnePointDistributionImpl) || ((OnePointDistributionImpl)statistics.editModel.postProcessingTimeDist).point>0);
		boolean NuIsExp=(statistics.editModel.waitingTimeDist instanceof ExponentialDistribution) || (statistics.editModel.waitingTimeDist instanceof NeverDistributionImpl);

		int batchArrival=statistics.editModel.batchArrival;
		int batchWorking=statistics.editModel.batchWorking;

		/* Analyse des Modells */

		addHeading(2,Language.tr("Statistics.AnalyticModelCompare.Analysis"));

		List<String> properties=new ArrayList<>();
		if (inputContinueProbability>0) properties.add("* "+Language.tr("Statistics.AnalyticModelCompare.Analysis.NotAnalytic.Forwarding"));
		if (inputRetryProbability>0) properties.add("* "+Language.tr("Statistics.AnalyticModelCompare.Analysis.NotAnalytic.Retry"));
		if (!LambdaIsExp) properties.add("* "+Language.tr("Statistics.AnalyticModelCompare.Analysis.NotAnalytic.NonExponentialInterArrival"));
		if (!MuIsExp) properties.add("* "+Language.tr("Statistics.AnalyticModelCompare.Analysis.NotAnalytic.NonExponentialService"));
		if (!NuIsExp) properties.add("* "+Language.tr("Statistics.AnalyticModelCompare.Analysis.NotAnalytic.NonExponentialWaitingTimeTolerances"));
		if (batchArrival>1) properties.add("* "+Language.tr("Statistics.AnalyticModelCompare.Analysis.NotAnalytic.BatchArrival"));
		if (batchWorking>1) properties.add("* "+Language.tr("Statistics.AnalyticModelCompare.Analysis.NotAnalytic.BatchService"));
		if (hasPostProcessing) properties.add("* "+Language.tr("Statistics.AnalyticModelCompare.Analysis.NotAnalytic.PostProcessing"));

		beginParagraph();
		if (properties.size()==0) {
			addLine(Language.tr("Statistics.AnalyticModelCompare.Analysis.Analytic"));
		} else {
			addLine(Language.tr("Statistics.AnalyticModelCompare.Analysis.NotAnalytic"));
			addLines(String.join("\n",properties));
		}
		endParagraph();

		/* Erlang-C-Modell */

		addHeading(2,Language.tr("Statistics.AnalyticModelCompare.ErlangC"));

		int K; if (inputWaitingRoomSize==Integer.MAX_VALUE) K=Integer.MAX_VALUE; else K=inputWaitingRoomSize+inputAgents;
		double lambda=1/inverseLambda*batchArrival;
		double mu=1/inverseMu*batchWorking;
		double nu=1/inverseNu;
		double rho=lambda/mu/inputAgents;

		beginParagraph();
		addLine(Language.tr("Distribution.AverageSomething")+" "+Language.tr("SimStatistic.WorkLoad")+" rho="+NumberTools.formatPercent(rho,3));
		if (!(statistics.editModel.waitingTimeDist instanceof NeverDistributionImpl)) {
			addLine(Language.tr("Distribution.AverageSomething")+" "+Language.tr("Statistics.WaitingTime.lower")+" ("+Language.tr("Statistics.WaitingTime.lower.AnalyticNoCancelation")+"): E[W]="+NumberTools.formatNumber(ErlangC.waitingTimeExt(lambda,mu,0,inputAgents,K),3));
			addLine(Language.tr("Distribution.AverageSomething")+" "+Language.tr("Statistics.WaitingTime.lower")+" ("+Language.tr("Statistics.WaitingTime.lower.AnalyticWithCancelation")+"): "+NumberTools.formatNumber(ErlangC.waitingTimeExt(lambda,mu,nu,inputAgents,K),3));
		} else {
			final double EW=ErlangC.waitingTimeExt(lambda,mu,0,inputAgents,K);
			final double ENQ=EW*lambda;
			addLine(Language.tr("Distribution.AverageSomething")+" "+Language.tr("Statistic.QueueLength.lower")+": E[NQ]="+NumberTools.formatNumber(ENQ,3));
			addLine(Language.tr("Distribution.AverageSomething")+" "+Language.tr("Statistics.WaitingTime.lower")+" ("+Language.tr("Statistics.WaitingTime.lower.AnalyticNoCancelation")+"): E[W]="+NumberTools.formatNumber(EW,3));
		}
		addLine(Language.tr("Distribution.AverageSomething")+" "+Language.tr("Statistics.ProcessTime.lower")+": E[S]="+NumberTools.formatNumber(1/mu,3));
		if (inputContinueProbability>0) addLine(Language.tr("Distribution.AverageSomething")+" "+Language.tr("Statistics.WaitingTime.lower")+" ("+Language.tr("Statistics.WaitingTime.lower.AnalyticWithCancelationAndForwardingApproximation")+"): "+NumberTools.formatNumber(ErlangC.waitingTimeExt(lambda*(1+inputContinueProbability),mu,nu,inputAgents,K),3));
		endParagraph();

		/* A-C-Modell */

		addHeading(2,Language.tr("Statistics.AnalyticModelCompare.AllenCunnen"));

		long bI=batchArrival;
		double muInv=inverseMu;
		long c=inputAgents;
		long b=batchWorking;

		/* Umrechnung von Arrival-Batches auf einzelne Kunden */
		lambda=lambda*bI;
		double cvI=Math.sqrt(bI*cvIB*cvIB+bI-1);

		rho=lambda*muInv/(b*c);

		/*
		PC1=(c*rho)^c/(c!(1-rho));
		PC=PC1/(PC1+sum(k=0...c-1; (c*rho)^k/k!))
		E[NQ]=rho/(1-rho)*PC*(SCV[I]+b*SCV[S])/2+(b-1)/2
		E[N]=E[NQ]+b*c*rho
		 */

		double PC1=powerFactorial(c*rho,c)/(1-rho);
		double PC=0; for(int i=0;i<=c-1;i++) PC+=powerFactorial(c*rho,i);
		PC=PC1/(PC1+PC);

		double ENQ=rho/(1-rho)*PC*(cvI*cvI+b*cvSB*cvSB)/2+(((double)b)-1)/2;
		double EN=ENQ+((double)b)*((double)c)*rho;
		double EW=ENQ/lambda;
		double EV=EW+muInv;

		beginParagraph();
		addLine(Language.tr("Distribution.AverageSomething")+" "+Language.tr("SimStatistic.WorkLoad")+" rho="+NumberTools.formatPercent(rho,3));
		addLine(Language.tr("Distribution.AverageSomething")+" "+Language.tr("Statistics.NumberOfClientsInTheSystem.lower")+": E[N]="+NumberTools.formatNumber(EN,3));
		addLine(Language.tr("Distribution.AverageSomething")+" "+Language.tr("Statistic.QueueLength.lower")+": E[NQ]="+NumberTools.formatNumber(ENQ,3));
		addLine(Language.tr("Distribution.AverageSomething")+" "+Language.tr("Statistics.NumberOfClientsInServiceProcess.lower")+": E[B]="+NumberTools.formatNumber(rho*c*b,3));
		addLine(Language.tr("Distribution.AverageSomething")+" "+Language.tr("SimStatistic.ResidenceTime.lower")+": E[V]="+NumberTools.formatNumber(EV,3));
		addLine(Language.tr("Distribution.AverageSomething")+" "+Language.tr("Statistics.WaitingTime.lower")+": E[W]="+NumberTools.formatNumber(EW,3));
		addLine(Language.tr("Distribution.AverageSomething")+" "+Language.tr("Statistics.ProcessTime.lower")+": E[S]="+NumberTools.formatNumber(muInv,3));
		endParagraph();

		/* Simulationsergebnisse */

		addHeading(2,Language.tr("Statistics.AnalyticModelCompare.SimulationResults"));

		beginParagraph();
		addLine(Language.tr("Distribution.AverageSomething")+" "+Language.tr("SimStatistic.WorkLoad")+" rho="+NumberTools.formatPercent(statistics.busyAgents.getTimeMean()/statistics.editModel.agents,3));
		addLine(Language.tr("Distribution.AverageSomething")+" "+Language.tr("Statistics.NumberOfClientsInTheSystem.lower")+": E[N]="+NumberTools.formatNumber(statistics.systemLength.getTimeMean(),3));
		addLine(Language.tr("Distribution.AverageSomething")+" "+Language.tr("Statistic.QueueLength.lower")+": E[NQ]="+NumberTools.formatNumber(statistics.queueLength.getTimeMean(),3));
		addLine(Language.tr("Distribution.AverageSomething")+" "+Language.tr("Statistics.NumberOfClientsInServiceProcess.lower")+": E[B]="+NumberTools.formatNumber(statistics.busyAgents.getTimeMean()*statistics.editModel.batchWorking,3));
		addLine(Language.tr("Distribution.AverageSomething")+" "+Language.tr("SimStatistic.ResidenceTime.lower")+": E[V]="+NumberTools.formatNumber(statistics.systemTimeSuccess.getMean(),3));
		if (!(statistics.editModel.waitingTimeDist instanceof NeverDistributionImpl) || statistics.editModel.waitingRoomSize>=0) {
			addLine(Language.tr("Distribution.AverageSomething")+" "+Language.tr("Statistics.WaitingTime.lower")+" ("+Language.tr("Statistics.WaitingTime.lower.successful")+"): E[W]="+NumberTools.formatNumber(statistics.waitingTimeSuccess.getMean(),3));
			addLine(Language.tr("Distribution.AverageSomething")+" "+Language.tr("Statistics.WaitingTime.lower")+" ("+Language.tr("Statistics.WaitingTime.lower.all")+"): "+NumberTools.formatNumber(statistics.waitingTimeAll.getMean(),3));
		} else {
			addLine(Language.tr("Distribution.AverageSomething")+" "+Language.tr("Statistics.WaitingTime.lower")+": E[W]="+NumberTools.formatNumber(statistics.waitingTimeAll.getMean(),3));
		}
		addLine(Language.tr("Distribution.AverageSomething")+" "+Language.tr("Statistics.ProcessTime.lower")+": E[S]="+NumberTools.formatNumber(statistics.workingTime.getMean(),3));
		if (statistics.postProcessingTime.getMean()>0) addLine(Language.tr("Distribution.AverageSomething")+" "+Language.tr("SimStatistic.PostProcessingTime.lower")+": E[S2]="+NumberTools.formatNumber(statistics.postProcessingTime.getMean(),3));

		endParagraph();

		/* Infotext  */
		addDescription("Compare");
	}

	private void buildSystemInfo() {
		addHeading(1,Language.tr("Statistics.SystemData"));
		beginParagraph();
		addLine(Language.tr("SimStatistic.SystemData.Version")+": "+statistics.editModel.version);
		addLine(Language.tr("Statistics.SystemData.RunDate")+": "+statistics.simulationData.runDate);
		addLine(Language.tr("Statistics.SystemData.RunThreads")+": "+statistics.simulationData.runThreads);
		addLine(Language.tr("Statistics.SystemData.RunOS")+": "+statistics.simulationData.runOS);
		addLine(Language.tr("Statistics.SystemData.RunUser")+": "+statistics.simulationData.runUser);
		endParagraph();

		beginParagraph();
		addLine(Language.tr("SimStatistic.SystemData.SimulationTime")+": "+NumberTools.formatLong(statistics.simulationData.runTime)+" ms");
		addLine(Language.tr("SimStatistic.SystemData.SimulatedEvents")+": "+NumberTools.formatLong(statistics.simulationData.runEvents));
		addLine(Language.tr("Statistics.SystemData.CountedClients")+": "+NumberTools.formatLong(statistics.freshCalls.get()+statistics.callContinued.getSuccess()+statistics.callRetry.getSuccess()));
		endParagraph();

		beginParagraph();
		addLine(Language.tr("Statistics.SystemData.EventsPerSecond")+": "+NumberTools.formatLong(statistics.simulationData.runEvents*1000/statistics.simulationData.runTime));
		addLine(Language.tr("Statistics.SystemData.TimePerEvent")+" (*): "+NumberTools.formatNumber(((double)statistics.simulationData.runTime)*statistics.simulationData.runThreads*1000/statistics.simulationData.runEvents,2)+" µs");
		endParagraph();

		beginParagraph();
		addLines(Language.tr("SimStatistic.SystemData.MultiThreadInfo"));
		endParagraph();
	}

	/**
	 * Korrelationslevels zu denen angegeben werden soll, ab welchem
	 * Abstand dieser Wert erreicht bzw. unterschritten wird.
	 */
	public final static double[] AUTOCORRELATION_LEVELS=new double[]{0.1,0.05,0.01,0.005,0.001};

	private void outputAutocorrelationData(final StatisticsDataPerformanceIndicator indicator, final int[] maxDistance) {
		beginParagraph();
		final int maxSize=(indicator.getCorrelationData().length-1)*StatisticsDataPerformanceIndicator.CORRELATION_RANGE_STEPPING;
		for (int i=0;i<AUTOCORRELATION_LEVELS.length;i++) {
			final double level=AUTOCORRELATION_LEVELS[i];
			final int distance=indicator.getCorrelationLevelDistance(level);
			maxDistance[i]=Math.max(maxDistance[i],distance);
			if (distance>maxSize) {
				addLine(String.format(Language.tr("Statistics.AutoCorrelation.LineMoreThan"),NumberTools.formatPercent(level),NumberTools.formatLong(maxSize)));
			} else {
				addLine(String.format(Language.tr("Statistics.AutoCorrelation.Line"),NumberTools.formatPercent(level),NumberTools.formatLong(distance)));
			}
		}
		endParagraph();
	}

	private void buildAutoCorrelation() {
		addHeading(1,Language.tr("Statistics.AutoCorrelation.WaitingTimes"));

		int[] maxDistance=new int[AUTOCORRELATION_LEVELS.length];

		/* Keine Daten vorhanden? */

		if (!statistics.waitingTimeAll.isCorrelationAvailable()) {
			beginParagraph();
			addLine(Language.tr("Statistics.AutoCorrelation.NoData"));
			endParagraph();
			return;
		}

		/* Autokorrelation über die Wartezeit über alle Kunden */

		outputAutocorrelationData(statistics.waitingTimeAll,maxDistance);

		/* Allgemeine Informationen zu den Autokorrelationsdaten */

		beginParagraph();
		addLines(Language.tr("Statistics.AutoCorrelation.Step"));
		endParagraph();

		/* Infotext  */
		addDescription("Autocorrleation"); // XXX
	}

	/**
	 * Liefert den im Konstruktor angegebenen Modus, welche Daten ausgegeben werden sollen.
	 * @return	Anzeige-Modus
	 * @see Mode
	 */
	public Mode getMode() {
		return mode;
	}

	@Override
	protected void buildText() {
		switch (mode) {
		case MODE_OVERVIEW: buildOverview(); break;
		case MODE_CALLER: buildCallerCount(); break;
		case MODE_QUEUE: buildQueue(); break;
		case MODE_WIP: buildWIP(); break;
		case MODE_INTERARRIVALTIMES: buildInterArrivalTimes(); break;
		case MODE_INTERLEAVETIMES: buildInterLeaveTimes(); break;
		case MODE_WAITINGTIMES: buildWaitingTimes(); break;
		case MODE_WORKINGTIMES: buildWorkingTimes(); break;
		case MODE_SYSTEMTIMES: buildSystemTimes(); break;
		case MODE_WORKLOAD: buildWorkLoad(); break;
		case MODE_COMPARE: buildCompare(); break;
		case MODE_SYSTEM_INFO: buildSystemInfo(); break;
		case MODE_AUTOCORRELATION: buildAutoCorrelation(); break;
		}
	}
}
