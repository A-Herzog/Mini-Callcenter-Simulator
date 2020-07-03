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
package simulator;
import java.io.File;

import simcore.SimData;
import simcore.SimulatorBase;
import simulator.editmodel.EditModel;
import simulator.runmodel.RunModel;
import simulator.runmodel.SimulationData;
import simulator.statistics.Statistics;
import tools.SetupData;

/**
 * Vollständiger Multi-Core-fähiger Simulator
 * @author Alexander Herzog
 */
public class Simulator extends SimulatorBase {
	/**
	 * Laufzeit-Modell (global für alle Threads)
	 */
	protected RunModel runModel;

	/**
	 * Editor-Modell (wird nur benötigt, da es am Ende mit in die Statistik aufgenommen wird)
	 */
	protected EditModel editModel;

	/**
	 * Steht hier ein Wert ungleich <code>null</code>, so wird in den Single-Core-Modus geschaltet und der Lauf wird in der angegebenen Log-Datei aufgezeichnet
	 */
	protected final File logFile;

	/**
	 * Da die Statistik nur einmal aus den Daten erhoben wird, wird diese für wiederholte Aufrufe von <code>getStatistic()</code> hier aufgehoben
	 * @see getStatistic
	 */
	private Statistics statistics=null;

	/**
	 * Konstruktor der Klasse <code>Simulator</code>
	 * @param multiCore	Wird hier <code>true</code> übergeben, so wird auf allen verfügbaren CPU-Kernen gerechnet. (Ausnahme: Wird in <code>logFile</code> ein Wert ungleich <code>null</code> übergeben, so wird stets nur ein Kern verwendet.)
	 * @param editModel	Editor-Modell
	 * @param logFile	Wird hier ein Wert ungleich <code>null</code> übergeben, so wird der Lauf in der angegebenen Datei aufgezeichnet; anonsten erfolgt nur die normale Aufzeichnung in der Statistik
	 */
	public Simulator(final boolean multiCore, final EditModel editModel, final File logFile) {
		super((multiCore && logFile==null)?Integer.MAX_VALUE:1,false,false);
		this.editModel=editModel;
		this.logFile=logFile;
	}

	/**
	 * Konstruktor der Klasse <code>Simulator</code>
	 * @param maxCoreCount	Gibt die maximale Anzahl an zu verwendenden Threads an. (Wird in <code>logFile</code> ein Wert ungleich <code>null</code> übergeben, so wird stets nur ein Kern verwendet.)
	 * @param editModel	Editor-Modell
	 * @param logFile	Wird hier ein Wert ungleich <code>null</code> übergeben, so wird der Lauf in der angegebenen Datei aufgezeichnet; anonsten erfolgt nur die normale Aufzeichnung in der Statistik
	 */
	public Simulator(final int maxCoreCount, final EditModel editModel, final File logFile) {
		super((logFile==null)?((maxCoreCount<1)?Integer.MAX_VALUE:maxCoreCount):1,false,false);
		this.editModel=editModel;
		this.logFile=logFile;
	}

	/**
	 * Konstruktor der Klasse <code>Simulator</code>
	 * @param editModel	Editor-Modell
	 * @param logFile	Wird hier ein Wert ungleich <code>null</code> übergeben, so wird der Lauf in der angegebenen Datei aufgezeichnet; anonsten erfolgt nur die normale Aufzeichnung in der Statistik
	 */
	public Simulator(final EditModel editModel, final File logFile) {
		this(SetupData.getSetup().useMultiCore,editModel,logFile);
	}

	/**
	 * Bereitet die Simulation vor
	 * @return	Liefert <code>null</code> zurück, wenn die Simulation erfolgreich vorbereitet werden konnte, sonst eine Fehlermeldung
	 */
	public String prepare() {
		Object obj=RunModel.getRunModel(editModel);
		if (obj instanceof String) return (String)obj;
		runModel=(RunModel)obj;
		return null;
	}

	/**
	 * Wird intern verwendet, um die Statistikdaten von den Threads einzusammeln.
	 * Diese Funktion wird von <code>getStatistic</code> aufgerufen. <code>getStatistic</code> speichert die einmal erhobenen
	 * Daten für spätere Abrufe zwischen, so dass <code>collectStatistics</code> nur einmal aufgerufen werden muss.
	 * @return	Statistik-Objekt, welches alle Daten des Simulationslaufs enthält
	 */
	protected Statistics collectStatistics() {
		Statistics statistics=new Statistics(runModel.collectCorrelation);

		/* Basisdaten zum Modell und zum Simulationslauf festhalten */
		statistics.editModel=editModel.clone();
		statistics.editModel.version=EditModel.systemVersion;
		statistics.simulationData.runTime=runTime;
		statistics.simulationData.runThreads=threadCount;
		statistics.simulationData.runEvents=getEventCount();
		statistics.simulationData.numaAwareMode=false;
		statistics.simulationData.threadRunTimes=getThreadRuntimes();

		/* Daten von den Threada einsammeln */
		for (int i=0;i<threads.length;i++)
			statistics.addData(((SimulationData)threads[i].simData).collectStatistics);

		/* Aufbereitete Daten berechnen */
		statistics.calc();

		return statistics;
	}

	/**
	 * Liefert nach Abschluss der Simulation die Statistikergebnisse zurück.
	 * @return	Statistik-Objekt, welches alle Daten des Simulationslaufs enthält
	 */
	public final Statistics getStatistic() {
		finalizeRun();
		if (statistics==null) {
			statistics=collectStatistics();
			for (int i=0;i<threads.length;i++) threads[i]=null;
			runModel=null;
		}
		return statistics;
	}

	@Override
	protected SimData getSimDataForThread(final int threadNr, final int threadCount) {
		SimData data=new SimulationData(threadNr,threadCount,runModel);
		if (logFile!=null) data.activateLogging(logFile);
		return data;
	}

	/**
	 * Startet die Simulationssthreads mit normaler Priorität.
	 * @see SimulatorBase#start(boolean)
	 */
	public final void start() {
		if (runModel==null) return;
		super.start(false);
	}

	/**
	 * Liefert die Gesamtanzahl an Wiederholungen in der Simulation.
	 * @return	Anzahl an Wiederholungen (über alle Threafs) der Simulation.
	 */
	public final int getSimDaysCount() {
		return runModel.repeatCount;
	}
}
