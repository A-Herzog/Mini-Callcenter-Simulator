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
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.w3c.dom.Element;

import language.Language;
import simulator.statistics.Statistics;
import systemtools.MsgBox;
import systemtools.statistics.ChartSetup;
import systemtools.statistics.ReportStyle;
import systemtools.statistics.StatisticNode;
import systemtools.statistics.StatisticViewer;
import systemtools.statistics.StatisticsBasePanel;
import tools.SetupData;
import ui.help.Help;
import ui.images.Images;
import ui.tools.FlatLaFHelper;
import xml.XMLTools;

/**
 * Diese Klasse erlaubt die Anzeige einer oder mehrerer Statistikdaten
 * @author Alexander Herzog
 */
public class StatisticsPanel extends StatisticsBasePanel {
	/**
	 * Serialisierungs-ID der Klasse
	 * @see Serializable
	 */
	private static final long serialVersionUID = 6515474808784376450L;

	/** Statistikdatensätze, deren Daten angezeigt werden sollen */
	private Statistics[] statistics;

	/**
	 * Konstruktor der Klasse <code>StatisticsPanel</code>
	 * @param numberOfViewers	Anzahl der nebeneinander anzuzeigenden Ergebnisse
	 * @param startSimulation	Callback, das ausgelöst wird, wenn der Nutzer auf der "Noch keine Daten"-Seite auf "Simulation jetzt starten" klickt. (Wird hier <code>null</code> übergeben, so wird diese Option nicht angezeigt.)
	 */
	public StatisticsPanel(final int numberOfViewers, final Runnable startSimulation) {
		super(numberOfViewers,Language.tr("Main.Menu.View.SimulationResults"),Images.STATISTICS_DARK.getURLs()[0],Language.trPrimary("CommandLine.Report.Name"),true);
		setCallBacks(startSimulation,()->loadStatistics(null),()->Help.topicModal(StatisticsPanel.this,Help.pageStatistics));
		setStatistics((Statistics)null);
	}

	/**
	 * Konstruktor der Klasse <code>StatisticsPanel</code>
	 * @param numberOfViewers	Anzahl der nebeneinander anzuzeigenden Ergebnisse
	 */
	public StatisticsPanel(final int numberOfViewers) {
		this(numberOfViewers,null);
	}

	/**
	 * Konstruktor der Klasse <code>StatisticsPanel</code>
	 * @param startSimulation	Callback, das ausgelöst wird, wenn der Nutzer auf der "Noch keine Daten"-Seite auf "Simulation jetzt starten" klickt. (Wird hier <code>null</code> übergeben, so wird diese Option nicht angezeigt.)
	 */
	public StatisticsPanel(final Runnable startSimulation) {
		this(1,startSimulation);
	}

	/**
	 * Textfarbe für Bookmark-Einträge im Falle des dunklen Layouts (mit dunklem Hintergrund für den Baum)
	 * @see #getBookmarkColor()
	 */
	private static final Color flatLafDarkBookmarkColor=new Color(128,128,255);

	@Override
	protected Color getBookmarkColor() {
		return FlatLaFHelper.isDark()?flatLafDarkBookmarkColor:Color.BLUE;
	}

	@Override
	protected List<String> getBookmarks() {
		return SetupData.getSetup().statisticTreeBookmarks;
	}

	@Override
	protected void setBookmarks(final List<String> newBookmarks) {
		final SetupData setup=SetupData.getSetup();
		setup.statisticTreeBookmarks.clear();
		if (newBookmarks!=null) setup.statisticTreeBookmarks.addAll(newBookmarks);
		setup.saveSetup();
	}

	/**
	 * Diese Funktion wird aufgerufen, wenn die Einstellungen, welche Report-Einträge ausgewählt sein sollen, abgefragt werden sollen.
	 * @return	Einstellungen, welche Report-Einträge selektiert sein sollen
	 */
	@Override
	protected String getReportSelectSettings() {
		return SetupData.getSetup().reportSettings;
	}

	/**
	 * Diese Funktion wird aufgerufen, wenn die Einstellungen, welche Report-Einträge ausgewählt sind, gespeichert werden sollen.
	 * @param settings	Neue Einstellungen, welche Report-Einträge selektiert sind
	 */
	@Override
	protected void setReportSelectSettings(String settings) {
		SetupData setup=SetupData.getSetup();
		setup.reportSettings=settings;
		setup.saveSetup();
	}

	/**
	 * Lädt die Einstellung, ob Bilder bei HTML-Reports inline ausgegeben werden sollen, aus dem Setup.
	 * @return	Gibt an, ob Bilder bei bei HTML-Reports inline ausgegeben werden sollen.
	 */
	@Override
	protected boolean getImagesInlineSetting() {
		return SetupData.getSetup().imagesInline;
	}

	/**
	 * Speichert die Einstellung, ob Bilder bei HTML-Reports inline ausgegeben werden sollen, im Setup.
	 * @param imagesInline	Gibt an, ob Bilder bei HTML-Reports inline ausgegeben werden sollen.
	 */
	@Override
	protected void setImagesInlineSetting(final boolean imagesInline) {
		SetupData setup=SetupData.getSetup();
		setup.imagesInline=imagesInline;
		setup.saveSetup();
	}

	/**
	 * Liefert das bisher eingestellte Statistik-Objekt (kann auch <code>null</code> sein)
	 * @return	Aktuelles Statistik-Objekt
	 */
	public Statistics getStatistics() {
		if (statistics==null || statistics.length==0) return null; else return statistics[0];
	}

	/**
	 * Setzt ein Statistik-Objekt für die Anzeige (kann auch <code>null</code> sein, wenn nichts ausgegeben werden soll)
	 * @param statistics	Neues, anzuzeigendes Statistik-Objekt
	 */
	public void setStatistics(final Statistics statistics) {
		this.statistics=new Statistics[]{statistics};
		super.setStatistics(statistics);
		updateViewer();
	}

	/**
	 * Setzt mehrere Statistik-Objekte für die parallele Anzeige (kann auch <code>null</code> sein, wenn nichts ausgegeben werden soll)
	 * @param data	Neue anzuzeigende Statistik-Objekte
	 * @param title	Titel über den Anzeigen
	 */
	public void setStatistics(Statistics[] data, String[] title) {
		/* Leeres Array abfangen */
		if (data==null || data.length==0) {
			data=new Statistics[Math.max(numberOfViewers,1)];
			Arrays.fill(data,null);
		}

		/* Zu lange oder zu kurze Arrays anpassen */
		Statistics[] data2=new Statistics[numberOfViewers];
		for (int i=0;i<data2.length;i++) data2[i]=(i<data.length)?data[i]:null;
		data=data2;

		statistics=data;

		/* Titel */
		String[] titleArray=new String[data.length];
		for (int i=0;i<titleArray.length;i++) {
			if (title==null || title.length<=i || title[i]==null) titleArray[i]=null; else titleArray[i]=title[i];
		}
		additionalTitle=titleArray;

		updateViewer();
	}

	/**
	 * Liefert alle Elemente der Statistikgruppe zurück. Sind keine Elemente gesetzt, so wird ein Element mit <code>null</code> als einzigem Eintrag geliefert.
	 * @return	Array aller Elemente der Statistikgruppe
	 */
	public Statistics[] getStatisticsGroup() {
		if (statistics==null || statistics.length==0) return new Statistics[]{null}; else return statistics;
	}

	/**
	 * Setzt mehrere Statistikelemente zur parallelen Anzeige
	 * @param statistics	Array mit allen anzuzeigenden Statistik-Elementen
	 */
	public void setStatisticsGroup(final Statistics[] statistics) {
		if (statistics==null || statistics.length==0) this.statistics=new Statistics[]{null}; else this.statistics=statistics;
		updateViewer();
	}

	/**
	 * Lädt die Statistikdaten aus einer Datei
	 * @param file	Datei, aus der die Statistikdaten geladen werden sollen. Wird hier <code>null</code> übergeben, so wird ein Dateiauswahl-Dialog angezeigt.
	 * @return	Gibt im Erfolgsfall <code>null</code> zurück, sonst eine Fehlermeldung.
	 */
	public String loadStatistics(File file) {
		if (file==null) {
			file=XMLTools.showLoadDialog(getParent(),Language.tr("Main.Toolbar.LoadStatistics"));
			if (file==null) return null;
		}

		Statistics newStatistics=new Statistics(false,false);
		String error=newStatistics.loadFromFile(file);
		if (error!=null) return error;

		setStatistics(newStatistics);

		return null;
	}

	/**
	 * Lädt die Statistikdaten aus einem XML-Element
	 * @param root	XML-Wurzelelement, aus dem die Statistikdaten geladen werden sollen.
	 * @return	Gibt im Erfolgsfall <code>null</code> zurück, sonst eine Fehlermeldung.
	 */
	public String loadStatisticsFromXML(final Element root) {
		Statistics newStatistics=new Statistics(false,false);
		String error=newStatistics.loadFromXML(root);
		if (error!=null) return error;

		setStatistics(newStatistics);

		return null;
	}

	/**
	 * Speichert die Statistikdaten in einer Datei
	 * @param file	Datei, in die die Statistikdaten geschrieben werden sollen. Wird hier <code>null</code> übergeben, so wird ein Dateiauswahl-Dialog angezeigt.
	 * @return	Gibt im Erfolgsfall <code>null</code> zurück, sonst eine Fehlermeldung.
	 */
	public String saveStatistics(File file) {
		if (statistics==null || statistics.length==0 || statistics[0]==null) return Language.tr("Main.Statistic.NoStatisticsAvailable");

		if (file==null) {
			file=XMLTools.showSaveDialog(
					getParent(),Language.tr("Main.Toolbar.SaveStatistics"),
					null,
					new String[] {StatisticsBasePanel.fileTypeHTMLJS+" (*.html)"},
					new String[] {"html"},
					XMLTools.DefaultSaveFormat.XML);
			if (file==null) return null;
		}

		if (file.exists()) {
			if (!MsgBox.confirmOverwrite(getTopLevelAncestor(),file)) return null;
		}

		final String fileString=file.toString().toUpperCase();
		if (fileString.endsWith(".HTML") || fileString.endsWith(".HTM")) {
			if (!runReportGeneratorHTMLApp(file,true)) return Language.tr("Main.Statistic.ErrorSaving");
		} else {
			if (!statistics[0].saveToFile(file)) return Language.tr("Main.Statistic.ErrorSaving");
		}

		return null;
	}

	/**
	 * Aktualisiert die gesamte Darstellung (Baumstruktur und Inhalte).
	 * @see #setStatistics(Statistics)
	 * @see #setStatistics(Statistics[], String[])
	 * @see #setStatisticsGroup(Statistics[])
	 */
	private void updateViewer() {
		StatisticNode root=new StatisticNode();
		String modelName=null;
		if (statistics!=null && statistics.length>0 && statistics[0]!=null) {
			addNodesToTree(root);
			modelName=statistics[0].editModel.name;
		}
		setData(root,modelName);
	}

	/**
	 * Sind in den Statistikdaten Autokorrelationsdaten enthalten?
	 * @return	Autokorrelationsdaten vorhanden?
	 */
	private boolean hasAutocorrelation() {
		for(Statistics statistic : statistics) {
			if (statistic.waitingTimeAll.isCorrelationAvailable()) return true;
		}
		return false;
	}

	/**
	 * Wandelt die Statistikknoten in Baumeinträge um.
	 * @param root	Wurzelelement der Statistikknoten
	 * @see #updateViewer()
	 */
	private void addNodesToTree(final StatisticNode root) {
		List<StatisticViewer> viewer;
		StatisticNode group;

		/* Ergebnisübersicht */

		viewer=new ArrayList<>();
		for(Statistics statistic : statistics) viewer.add(new ViewerText(statistic,ViewerText.Mode.MODE_OVERVIEW,mode->modeClick(mode)));
		root.addChild(new StatisticNode(Language.tr("Statistics.ResultsOverview"),viewer));

		/* Vergleich mit analytischen Modellen */

		viewer=new ArrayList<>();
		for(Statistics statistic : statistics) viewer.add(new ViewerText(statistic,ViewerText.Mode.MODE_COMPARE));
		root.addChild(new StatisticNode(Language.tr("Statistics.AnalyticModelCompare"),viewer));

		/* Anruferzahlen */

		root.addChild(group=new StatisticNode(Language.tr("SimStatistic.NumberOfCallers")));

		viewer=new ArrayList<>();
		for(Statistics statistic : statistics) viewer.add(new ViewerText(statistic,ViewerText.Mode.MODE_CALLER));
		group.addChild(new StatisticNode(Language.tr("SimStatistic.Overview"),viewer));

		viewer=new ArrayList<>();
		for(Statistics statistic : statistics) viewer.add(new ViewerTable(statistic,ViewerTable.Mode.MODE_CALLER));
		group.addChild(new StatisticNode(Language.tr("SimStatistic.CallerType"),viewer));

		viewer=new ArrayList<>();
		for(Statistics statistic : statistics) viewer.add(new ViewerPieChart(statistic,ViewerPieChart.Mode.MODE_CALLER));
		group.addChild(new StatisticNode(Language.tr("SimStatistic.CallerType"),viewer));

		/* Warteschlange */

		root.addChild(group=new StatisticNode(Language.tr("SimStatistic.Queue")));

		viewer=new ArrayList<>();
		for(Statistics statistic : statistics) viewer.add(new ViewerText(statistic,ViewerText.Mode.MODE_QUEUE));
		group.addChild(new StatisticNode(Language.tr("SimStatistic.Overview"),viewer));

		viewer=new ArrayList<>();
		for(Statistics statistic : statistics) viewer.add(new ViewerTable(statistic,ViewerTable.Mode.MODE_QUEUE));
		group.addChild(new StatisticNode(Language.tr("SimStatistic.Queue.Distribution"),viewer));

		viewer=new ArrayList<>();
		for(Statistics statistic : statistics) viewer.add(new ViewerBarChart(statistic,ViewerBarChart.Mode.MODE_QUEUE));
		group.addChild(new StatisticNode(Language.tr("SimStatistic.Queue.Distribution"),viewer));

		/* Kunden im System */

		root.addChild(group=new StatisticNode(Language.tr("Statistics.ClientsInSystem")));

		viewer=new ArrayList<>();
		for(Statistics statistic : statistics) viewer.add(new ViewerText(statistic,ViewerText.Mode.MODE_WIP));
		group.addChild(new StatisticNode(Language.tr("SimStatistic.Overview"),viewer));

		viewer=new ArrayList<>();
		for(Statistics statistic : statistics) viewer.add(new ViewerTable(statistic,ViewerTable.Mode.MODE_WIP));
		group.addChild(new StatisticNode(Language.tr("Statistics.NumberOfClientsInTheSystem.Distribution"),viewer));

		viewer=new ArrayList<>();
		for(Statistics statistic : statistics) viewer.add(new ViewerBarChart(statistic,ViewerBarChart.Mode.MODE_WIP));
		group.addChild(new StatisticNode(Language.tr("Statistics.NumberOfClientsInTheSystem.Distribution"),viewer));

		/* Zwischenankunftszeiten */

		root.addChild(group=new StatisticNode(Language.tr("Statistics.InterArrivalTimes")));

		viewer=new ArrayList<>();
		for(Statistics statistic : statistics) viewer.add(new ViewerText(statistic,ViewerText.Mode.MODE_INTERARRIVALTIMES));
		group.addChild(new StatisticNode(Language.tr("SimStatistic.Overview"),viewer));

		viewer=new ArrayList<>();
		for(Statistics statistic : statistics) viewer.add(new ViewerTable(statistic,ViewerTable.Mode.MODE_INTERARRIVALTIMES));
		group.addChild(new StatisticNode(Language.tr("SimStatistic.Distribution"),viewer));

		viewer=new ArrayList<>();
		for(Statistics statistic : statistics) viewer.add(new ViewerLineChart(statistic,ViewerLineChart.Mode.MODE_INTERARRIVALTIMES));
		group.addChild(new StatisticNode(Language.tr("SimStatistic.Distribution"),viewer));

		/* Zwischenabgangszeiten */

		root.addChild(group=new StatisticNode(Language.tr("Statistics.InterLeaveTimes")));

		viewer=new ArrayList<>();
		for(Statistics statistic : statistics) viewer.add(new ViewerText(statistic,ViewerText.Mode.MODE_INTERLEAVETIMES));
		group.addChild(new StatisticNode(Language.tr("SimStatistic.Overview"),viewer));

		viewer=new ArrayList<>();
		for(Statistics statistic : statistics) viewer.add(new ViewerTable(statistic,ViewerTable.Mode.MODE_INTERLEAVETIMES));
		group.addChild(new StatisticNode(Language.tr("SimStatistic.Distribution"),viewer));

		viewer=new ArrayList<>();
		for(Statistics statistic : statistics) viewer.add(new ViewerLineChart(statistic,ViewerLineChart.Mode.MODE_INTERLEAVETIMES));
		group.addChild(new StatisticNode(Language.tr("SimStatistic.Distribution"),viewer));

		/* Wartezeiten */

		root.addChild(group=new StatisticNode(Language.tr("SimStatistic.WaitingTimes")));

		viewer=new ArrayList<>();
		for(Statistics statistic : statistics) viewer.add(new ViewerText(statistic,ViewerText.Mode.MODE_WAITINGTIMES));
		group.addChild(new StatisticNode(Language.tr("SimStatistic.Overview"),viewer));

		viewer=new ArrayList<>();
		for(Statistics statistic : statistics) viewer.add(new ViewerTable(statistic,ViewerTable.Mode.MODE_WAITINGTIMES_SUCCESS));
		group.addChild(new StatisticNode(Language.tr("SimStatistic.WaitingTimes.Distribution"),viewer));

		viewer=new ArrayList<>();
		for(Statistics statistic : statistics) viewer.add(new ViewerTable(statistic,ViewerTable.Mode.MODE_WAITINGTIMES_CANCEL));
		group.addChild(new StatisticNode(Language.tr("SimStatistic.CancelationTimes.Distribution"),viewer));

		viewer=new ArrayList<>();
		for(Statistics statistic : statistics) viewer.add(new ViewerTable(statistic,ViewerTable.Mode.MODE_WAITINGTIMES_ALL));
		group.addChild(new StatisticNode(Language.tr("SimStatistic.WaitingTimes.Distribution.All"),viewer));

		viewer=new ArrayList<>();
		for(Statistics statistic : statistics) viewer.add(new ViewerLineChart(statistic,ViewerLineChart.Mode.MODE_WAITINGTIMES_SUCCESS));
		group.addChild(new StatisticNode(Language.tr("SimStatistic.WaitingTimes.Distribution"),viewer));

		viewer=new ArrayList<>();
		for(Statistics statistic : statistics) viewer.add(new ViewerLineChart(statistic,ViewerLineChart.Mode.MODE_WAITINGTIMES_CANCEL));
		group.addChild(new StatisticNode(Language.tr("SimStatistic.CancelationTimes.Distribution"),viewer));

		viewer=new ArrayList<>();
		for(Statistics statistic : statistics) viewer.add(new ViewerLineChart(statistic,ViewerLineChart.Mode.MODE_WAITINGTIMES_ALL));
		group.addChild(new StatisticNode(Language.tr("SimStatistic.WaitingTimes.Distribution.All"),viewer));

		if (hasAutocorrelation()) {
			viewer=new ArrayList<>();
			for(Statistics statistic : statistics) viewer.add(new ViewerText(statistic,ViewerText.Mode.MODE_AUTOCORRELATION));
			group.addChild(new StatisticNode(Language.tr("SimStatistic.AutoCorrelation"),viewer));

			viewer=new ArrayList<>();
			for(Statistics statistic : statistics) viewer.add(new ViewerTable(statistic,ViewerTable.Mode.MODE_AUTOCORRELATION));
			group.addChild(new StatisticNode(Language.tr("SimStatistic.AutoCorrelation"),viewer));

			viewer=new ArrayList<>();
			for(Statistics statistic : statistics) viewer.add(new ViewerLineChart(statistic,ViewerLineChart.Mode.MODE_AUTOCORRELATION));
			group.addChild(new StatisticNode(Language.tr("SimStatistic.AutoCorrelation"),viewer));
		}

		/* Bedienzeiten */

		root.addChild(group=new StatisticNode(Language.tr("Statistics.ProcessTimes")));

		viewer=new ArrayList<>();
		for(Statistics statistic : statistics) viewer.add(new ViewerText(statistic,ViewerText.Mode.MODE_WORKINGTIMES));
		group.addChild(new StatisticNode(Language.tr("SimStatistic.Overview"),viewer));

		viewer=new ArrayList<>();
		for(Statistics statistic : statistics) viewer.add(new ViewerTable(statistic,ViewerTable.Mode.MODE_WORKINGTIMES));
		group.addChild(new StatisticNode(Language.tr("Statistics.DistributionOfTheProcessTimes"),viewer));

		viewer=new ArrayList<>();
		for(Statistics statistic : statistics) viewer.add(new ViewerTable(statistic,ViewerTable.Mode.MODE_POSTPROCESSINGTIMES));
		group.addChild(new StatisticNode(Language.tr("Statistics.DistributionOfThePostProcessTimes"),viewer));

		viewer=new ArrayList<>();
		for(Statistics statistic : statistics) viewer.add(new ViewerLineChart(statistic,ViewerLineChart.Mode.MODE_WORKINGTIMES));
		group.addChild(new StatisticNode(Language.tr("Statistics.DistributionOfTheProcessTimes"),viewer));

		viewer=new ArrayList<>();
		for(Statistics statistic : statistics) viewer.add(new ViewerLineChart(statistic,ViewerLineChart.Mode.MODE_POSTPROCESSINGTIMES));
		group.addChild(new StatisticNode(Language.tr("Statistics.DistributionOfThePostProcessTimes"),viewer));

		/* Verweilzeiten */

		root.addChild(group=new StatisticNode(Language.tr("Statistics.ResidenceTimes")));

		viewer=new ArrayList<>();
		for(Statistics statistic : statistics) viewer.add(new ViewerText(statistic,ViewerText.Mode.MODE_SYSTEMTIMES));
		group.addChild(new StatisticNode(Language.tr("SimStatistic.Overview"),viewer));

		viewer=new ArrayList<>();
		for(Statistics statistic : statistics) viewer.add(new ViewerTable(statistic,ViewerTable.Mode.MODE_SYSTEMTIMES_SUCCESS));
		group.addChild(new StatisticNode(Language.tr("Statistics.ResidenceTimes.Distribution.Successful"),viewer));

		viewer=new ArrayList<>();
		for(Statistics statistic : statistics) viewer.add(new ViewerTable(statistic,ViewerTable.Mode.MODE_SYSTEMTIMES_ALL));
		group.addChild(new StatisticNode(Language.tr("Statistics.ResidenceTimes.Distribution.All"),viewer));

		viewer=new ArrayList<>();
		for(Statistics statistic : statistics) viewer.add(new ViewerLineChart(statistic,ViewerLineChart.Mode.MODE_SYSTEMTIMES_SUCCESS));
		group.addChild(new StatisticNode(Language.tr("Statistics.ResidenceTimes.Distribution.Successful"),viewer));

		viewer=new ArrayList<>();
		for(Statistics statistic : statistics) viewer.add(new ViewerLineChart(statistic,ViewerLineChart.Mode.MODE_SYSTEMTIMES_ALL));
		group.addChild(new StatisticNode(Language.tr("Statistics.ResidenceTimes.Distribution.All"),viewer));

		/* Auslastung */

		root.addChild(group=new StatisticNode(Language.tr("SimStatistic.WorkLoad")));

		viewer=new ArrayList<>();
		for(Statistics statistic : statistics) viewer.add(new ViewerText(statistic,ViewerText.Mode.MODE_WORKLOAD));
		group.addChild(new StatisticNode(Language.tr("SimStatistic.Overview"),viewer));

		viewer=new ArrayList<>();
		for(Statistics statistic : statistics) viewer.add(new ViewerTable(statistic,ViewerTable.Mode.MODE_WORKLOAD));
		group.addChild(new StatisticNode(Language.tr("SimStatistic.WorkLoad.LoadShares"),viewer));

		viewer=new ArrayList<>();
		for(Statistics statistic : statistics) viewer.add(new ViewerBarChart(statistic,ViewerBarChart.Mode.MODE_WORKLOAD));
		group.addChild(new StatisticNode(Language.tr("SimStatistic.WorkLoad.LoadShares"),viewer));

		viewer=new ArrayList<>();
		for(Statistics statistic : statistics) viewer.add(new ViewerPieChart(statistic,ViewerPieChart.Mode.MODE_WORKLOAD));
		group.addChild(new StatisticNode(Language.tr("SimStatistic.WorkLoad.LoadShares"),viewer));

		/* Systemdaten */

		viewer=new ArrayList<>();
		for(Statistics statistic : statistics) viewer.add(new ViewerText(statistic,ViewerText.Mode.MODE_SYSTEM_INFO));
		root.addChild(new StatisticNode(Language.tr("Statistics.SystemData"),viewer));
	}

	/**
	 * Wird aufgerufen, wenn ein "Details"-Link auf einer Textseite angeklickt wurde.
	 * @param mode	Anzuzeigende Textseite
	 */
	private void modeClick(final ViewerText.Mode mode) {
		selectNode(node->{
			if (node.viewer.length<1) return false;
			if (!(node.viewer[0] instanceof ViewerText)) return false;
			return ((ViewerText)node.viewer[0]).getMode()==mode;
		});
	}

	@Override
	protected int getImageSize() {
		return SetupData.getSetup().imageSize;
	}

	@Override
	protected void setImageSize(int newSize) {
		final SetupData setup=SetupData.getSetup();
		setup.imageSize=newSize;
		setup.saveSetupWithWarning(this);
	}

	@Override
	protected ChartSetup getChartSetup() {
		return SetupData.getSetup().chartSetup;
	}

	@Override
	protected void setChartSetup(final ChartSetup chartSetup) {
		super.setChartSetup(chartSetup);
		final SetupData setup=SetupData.getSetup();
		setup.chartSetup.copyFrom(chartSetup);
		setup.saveSetupWithWarning(this);
	}

	@Override
	protected ReportStyle getReportStyle() {
		return SetupData.getSetup().reportStyle;
	}

	@Override
	protected void setReportStyle(final ReportStyle reportStyle) {
		final SetupData setup=SetupData.getSetup();
		setup.reportStyle=reportStyle;
		setup.saveSetupWithWarning(this);
	}
}