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
package ui.commandline;

import java.io.File;
import java.io.PrintStream;

import org.w3c.dom.Element;

import language.Language;
import mathtools.NumberTools;
import simulator.Simulator;
import simulator.editmodel.EditModel;
import simulator.statistics.Statistics;
import systemtools.commandline.AbstractCommand;
import systemtools.commandline.BaseCommandLineSystem;
import xml.XMLTools;

/**
 * Abstrakte Basisklasse für Simulations-Kommandozeilenbefehle.
 * Diese Klasse stellt zusätzliche geschützte Methoden bereit, die alle Simulationsbefehle benötigen.
 * @author Alexander Herzog
 * @see AbstractCommand
 * @see CommandLineSystem
 */
public abstract class AbstractSimulationCommand extends AbstractCommand {
	/**
	 * Lädt eine xml-Datei
	 * @param file	Zu ladende Datei
	 * @return Tritt ein Fehler auf, so wird <code>null</code> zurück gegeben, ansonsten das Root-Element der Daten
	 */
	private final Element loadXMLFile(File file) {
		XMLTools xml=new XMLTools(file);
		return xml.load();
	}

	/**
	 * Prüft, ob die übergebene Datei eine Callcenter-Modell Datei ist
	 * @param file	Zu prüfende Datei
	 * @return	Gibt <code>true</code> zurück, wenn es sich um eine Callcenter-Modell Datei handelt
	 */
	protected final boolean isModelFile(File file) {
		Element root=loadXMLFile(file);
		if (root==null) return false;

		for (String test: new EditModel().getRootNodeNames()) if (root.getNodeName().equalsIgnoreCase(test)) return true;

		return false;
	}

	/**
	 * Prüft, ob die übergebene Datei eine Statistik-Datei ist
	 * @param file	Zu prüfende Datei
	 * @return	Gibt <code>true</code> zurück, wenn es sich um eine Statistik-Datei handelt
	 */
	protected final boolean isStatisticFile(File file) {
		Element root=loadXMLFile(file);
		if (root==null) return false;

		for (String test: new Statistics(false).getRootNodeNames()) if (root.getNodeName().equalsIgnoreCase(test)) return true;

		return false;
	}

	/**
	 * Wartet bis das als Parameter übergebene Simulator-Interface fertig ist und gibt ggf. Zwischenfortschrittsmeldungen aus.
	 * @param simulator	Interface auf das Simulator-Objekt, welches überwacht werden soll
	 * @param minimalOutput	Wird hier <code>false</code> übergeben, so werden Fortschrittsmeldungen ausgegeben.
	 * @param out Ein <code>PrintStream</code>-Objekt, über das Texte ausgegeben werden können.
	 */
	protected final void waitForSimulationDone(Simulator simulator, boolean minimalOutput, PrintStream out) {
		long startTime=System.currentTimeMillis();
		long lastGesamt=Integer.MAX_VALUE;

		if (!minimalOutput) out.println(Language.tr("Simulation.Started"));

		int count=0;
		while (simulator.isRunning()) {
			try {Thread.sleep(25);} catch (InterruptedException e) {}
			if (minimalOutput) continue;
			count++;
			if (count%50==0) {
				long time=System.currentTimeMillis();
				if (time-startTime>5000) {
					double gesamt=(time-startTime)/(((double)simulator.getSimDayCount())/simulator.getSimDaysCount());
					gesamt-=(time-startTime);
					if (gesamt/1000<lastGesamt) lastGesamt=(int) Math.round(gesamt/1000);
					out.println(String.format(Language.tr("CommandLine.Simulation.Status.Long"),simulator.getSimDayCount(),simulator.getSimDaysCount(),NumberTools.formatLong(lastGesamt)));
				} else {
					out.println(String.format(Language.tr("CommandLine.Simulation.Status.Short"),simulator.getSimDayCount(),simulator.getSimDaysCount()));
				}
			}
		}
	}

	/**
	 * Simulator-Objekt über das die Simulation durchgeführt wird
	 */
	private volatile Simulator simulator;

	/**
	 * Führt eine Simulation aus und liefert das Ergebnis-Statistik-Objekt zurück
	 * @param editModel	Zu simulierendes Modell
	 * @param minimalOutput	Wird hier <code>false</code> übergeben, so werden Fortschrittsmeldungen ausgegeben.
	 * @param maxThreads Gibt an, wie viele Threads maximal verwendet werden sollen.
	 * @param out	Ein <code>PrintStream</code>-Objekt, über das Texte ausgegeben werden können.
	 * @return	Gibt im Erfolgsfalls das Statistik-Objekt zurück, sonst <code>null</code>
	 */
	protected final Statistics singleSimulation(EditModel editModel, boolean minimalOutput, int maxThreads, PrintStream out) {
		/* Modell vorbereiten */
		if (EditModel.isNewerVersionSystem(editModel.version,EditModel.systemVersion)) {
			out.println(Language.tr("Dialog.Title.Warning").toUpperCase()+": "+Language.tr("Editor.NewerVersion.Info.Short"));
		}
		simulator=new Simulator(editModel,null);
		String s=simulator.prepare();
		if (s!=null) {out.println(BaseCommandLineSystem.errorBig+": "+Language.tr("CommandLine.Error.PreparationOfModel")+": "+s); return null;}

		/* Simulation starten */
		simulator.start();

		/* Auf Ende der Simulation warten */
		waitForSimulationDone(simulator,minimalOutput,out);

		Statistics statistics=simulator.getStatistic();
		if (!minimalOutput) out.println(String.format(Language.tr("CommandLine.Simulation.Done"),NumberTools.formatLong(statistics.simulationData.runTime)));

		simulator=null;

		return statistics;
	}

	/**
	 * Führt eine Simulation aus und liefert das Ergebnis-Statistik-Objekt zurück
	 * @param editModel	Zu simulierendes Modell
	 * @param minimalOutput	Wird hier <code>false</code> übergeben, so werden Fortschrittsmeldungen ausgegeben.
	 * @param out Ein <code>PrintStream</code>-Objekt, über das Texte ausgegeben werden können.
	 * @return	Gibt im Erfolgsfalls das Statistik-Objekt zurück, sonst <code>null</code>
	 */
	protected final Statistics singleSimulation(EditModel editModel, boolean minimalOutput, PrintStream out) {
		return singleSimulation(editModel,minimalOutput,Integer.MAX_VALUE,out);
	}

	/**
	 * Speichert die Statistikdaten in einer Datei und gibt im Fehlerfall eine Meldung auf der Konsole aus
	 * @param statistics	Zu speichernde Statistikdaten
	 * @param statisticsFile	Datei, in der die Statistik gespeichert werden soll
	 * @param out Ein <code>PrintStream</code>-Objekt, über das Texte ausgegeben werden können.
	 * @return	Gibt zurück, ob das Speichern erfolgreich verlief
	 */
	protected final boolean saveStatistics(Statistics statistics, File statisticsFile, PrintStream out) {
		if (!statistics.saveToFile(statisticsFile)) {out.println(BaseCommandLineSystem.errorBig+": "+Language.tr("CommandLine.Error.UnableToSaveStatistic")); return false;}
		return true;
	}

	/**
	 * Wird von {@link #setQuit()} auf <code>true</code> gesetzt, wenn die Simulation
	 * abgebrochen werden soll.
	 */
	private volatile boolean canceled=false;

	/**
	 * Gibt an, ob {@link #setQuit()} aufgerufen wurde.
	 * @return	Abbruchstatus
	 */
	protected final boolean isCanceled() {
		return canceled;
	}

	@Override
	public void setQuit() {
		canceled=true;
		if (simulator!=null) simulator.cancel();
	}
}
