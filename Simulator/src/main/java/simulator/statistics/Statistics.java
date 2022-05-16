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
package simulator.statistics;

import java.io.File;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import language.Language;
import simulator.editmodel.EditModel;
import statistics.StatisticsBase;
import statistics.StatisticsCountPerformanceIndicator;
import statistics.StatisticsDataPerformanceIndicator;
import statistics.StatisticsSimpleCountPerformanceIndicator;
import statistics.StatisticsSimulationBaseData;
import statistics.StatisticsTimePerformanceIndicator;

/**
 * Statistik über die komplette Simulation
 * Die <code>Statistics</code>-Klasse ist dabei so ausgelegt, dass sie sowohl thread-lokal Datensammeln kann als auch am Ende die globale Statistik ausweisen kann
 * @author Alexander Herzog
 */
public class Statistics extends StatisticsBase {
	/**
	 * Das Editor-Modell wird mit in der Statistik gespeichert. So ist immer nachvollziehbar, auf welches Modell sich die Statistik bezieht.
	 */
	public EditModel editModel;

	/**
	 * Technische Basisdaten zur Simulation
	 */
	public StatisticsSimulationBaseData simulationData;

	/**
	 * Anzahl an Erstanrufern
	 */
	public StatisticsSimpleCountPerformanceIndicator freshCalls;

	/**
	 * Zählt den Anteil der Anrufe, die warten mussten
	 */
	public StatisticsCountPerformanceIndicator callNeedToWait;

	/**
	 * Zählt den Anteil der Anrufe, die ein Besetztzeichen erhalten haben
	 */
	public StatisticsCountPerformanceIndicator callRejected;

	/**
	 * Zählt den Anteil der erfolgreichen Anrufe
	 */
	public StatisticsCountPerformanceIndicator callSuccessful;

	/**
	 * Zählt den Anteil der weitergeleiteten Anrufe
	 */
	public StatisticsCountPerformanceIndicator callContinued;

	/**
	 * Zählt den Anteil der Abbrecher (Warteabbrecher und Besetzt), die später einen neuen Anlauf starten
	 */
	public StatisticsCountPerformanceIndicator callRetry;

	/**
	 * Zählt den Anteil der erfolgreichen Anrufe, die weniger als 20 Sekunden warten mussten
	 */
	public StatisticsCountPerformanceIndicator callServiceLevel;

	/**
	 * Verteilung der Zwischenankunftszeiten
	 */
	public StatisticsDataPerformanceIndicator interarrivalTime;

	/**
	 * Verteilung der Zwischenabgangszeiten
	 */
	public StatisticsDataPerformanceIndicator interleaveTime;

	/**
	 * Verteilung der Bedienzeiten
	 */
	public StatisticsDataPerformanceIndicator workingTime;

	/**
	 * Verteilung der Nachbearbeitungszeiten
	 */
	public StatisticsDataPerformanceIndicator postProcessingTime;

	/**
	 * Verteilung der Warte- bzw. Abbruchzeiten über alle Anrufe
	 */
	public StatisticsDataPerformanceIndicator waitingTimeAll;

	/**
	 * Verteilung der Wartezeiten der erfolgreichen Anrufe
	 */
	public StatisticsDataPerformanceIndicator waitingTimeSuccess;

	/**
	 * Verteilung der Abbruchzeiten
	 */
	public StatisticsDataPerformanceIndicator waitingTimeCancel;

	/**
	 * Verteilung der Verweilzeiten aller Kunden
	 */
	public StatisticsDataPerformanceIndicator systemTimeAll;

	/**
	 * Verteilung der Verweilzeiten der erfolgreichen Kunden
	 */
	public StatisticsDataPerformanceIndicator systemTimeSuccess;

	/**
	 * Zeitdauern, in denen sich das System in den verschiedenen Zuständen bzgl. der freien Agenten befunden hat
	 */
	public StatisticsTimePerformanceIndicator freeAgents;

	/**
	 * Zeitdauern, in denen sich das System in den verschiedenen Zuständen bzgl. der beschäftigten Agenten befunden hat
	 */
	public StatisticsTimePerformanceIndicator busyAgents;

	/**
	 * Zeitdauern, in denen sich das System in den verschiedenen Zuständen bzgl. der Warteschlangenlänge befunden hat
	 */
	public StatisticsTimePerformanceIndicator queueLength;

	/**
	 * Zeitdauern, in denen sich das System in den verschiedenen Zuständen bzgl. der Anzahl an Kunden im System befunden hat
	 */
	public StatisticsTimePerformanceIndicator systemLength;

	/**
	 * Konstruktor der Klasse
	 * @param collectCorrelation	Erfassung der Autokorrelation der Wartezeiten der Kunden
	 */
	public Statistics(final boolean collectCorrelation) {
		final int correlationRange=collectCorrelation?1000:-1;
		final boolean useWelford=false;

		editModel=new EditModel();

		addPerformanceIndicator(simulationData=new StatisticsSimulationBaseData(Language.trAll("Statistics.XML.BaseData")));

		addPerformanceIndicator(freshCalls=new StatisticsSimpleCountPerformanceIndicator(Language.trAll("Statistics.XML.FreshCalls")));
		addPerformanceIndicator(callNeedToWait=new StatisticsCountPerformanceIndicator(Language.trAll("Statistics.XML.CallsNeedToWait")));
		addPerformanceIndicator(callRejected=new StatisticsCountPerformanceIndicator(Language.trAll("Statistics.XML.Rejected")));
		addPerformanceIndicator(callSuccessful=new StatisticsCountPerformanceIndicator(Language.trAll("Statistics.XML.Successful")));
		addPerformanceIndicator(callContinued=new StatisticsCountPerformanceIndicator(Language.trAll("Statistics.XML.Continued")));
		addPerformanceIndicator(callRetry=new StatisticsCountPerformanceIndicator(Language.trAll("Statistics.XML.Retry")));
		addPerformanceIndicator(callServiceLevel=new StatisticsCountPerformanceIndicator(Language.trAll("Statistics.XML.ServiceLevel")));

		addPerformanceIndicator(interarrivalTime=new StatisticsDataPerformanceIndicator(Language.trAll("Statistics.XML.InterArrivalTimes"),7200,7200,-1,1,useWelford));
		addPerformanceIndicator(interleaveTime=new StatisticsDataPerformanceIndicator(Language.trAll("Statistics.XML.InterLeaveTimes"),7200,7200,-1,1,useWelford));
		addPerformanceIndicator(waitingTimeAll=new StatisticsDataPerformanceIndicator(Language.trAll("Statistics.XML.WaitingTimesAll"),7200,7200,correlationRange,1,useWelford));
		addPerformanceIndicator(waitingTimeSuccess=new StatisticsDataPerformanceIndicator(Language.trAll("Statistics.XML.WaitingTimesSuccess"),7200,7200,-1,1,useWelford));
		addPerformanceIndicator(waitingTimeCancel=new StatisticsDataPerformanceIndicator(Language.trAll("Statistics.XML.WaitingTimesCancel"),7200,7200,-1,1,useWelford));
		addPerformanceIndicator(workingTime=new StatisticsDataPerformanceIndicator(Language.trAll("Statistics.XML.WorkingTimes"),7200,7200,-1,1,useWelford));
		addPerformanceIndicator(postProcessingTime=new StatisticsDataPerformanceIndicator(Language.trAll("Statistics.XML.PostProcessingTime"),7200,7200,-1,1,useWelford));
		addPerformanceIndicator(systemTimeAll=new StatisticsDataPerformanceIndicator(Language.trAll("Statistics.XML.SystemTimeAll"),7200,7200,-1,1,useWelford));
		addPerformanceIndicator(systemTimeSuccess=new StatisticsDataPerformanceIndicator(Language.trAll("Statistics.XML.SystemTimeSuccess"),7200,7200,-1,1,useWelford));

		addPerformanceIndicator(freeAgents=new StatisticsTimePerformanceIndicator(Language.trAll("Statistics.XML.FreeAgents")));
		addPerformanceIndicator(busyAgents=new StatisticsTimePerformanceIndicator(Language.trAll("Statistics.XML.BusyAgents")));
		addPerformanceIndicator(queueLength=new StatisticsTimePerformanceIndicator(Language.trAll("Statistics.XML.QueueLength")));
		addPerformanceIndicator(systemLength=new StatisticsTimePerformanceIndicator(Language.trAll("Statistics.XML.SystemLength")));

		resetData();
	}

	/**
	 * Wurzel-Element für Statistik-xml-Dateien
	 */
	@Override
	public String[] getRootNodeNames() {
		return Language.trAll("Statistics.XML.Root");
	}

	@Override
	protected String loadProperty(final String name, final String text, final Element node) {
		for (String test: editModel.getRootNodeNames()) if (name.equalsIgnoreCase(test)) return editModel.loadFromXML(node);
		String error=super.loadProperty(name,text,node); if (error!=null) return error;
		return null;
	}

	@Override
	protected void addDataToXML(final Document doc, final Element node, final boolean isPartOfOtherFile, final File file) {
		editModel.saveToXML(node,true);
		super.addDataToXML(doc,node,isPartOfOtherFile,file);
	}
}