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
package simulator.runmodel;

import java.util.ArrayDeque;
import java.util.Queue;

import simulator.events.CallCancelEvent;

/**
 * Dynamische, thread-lokale Laufzeitdaten
 * @author Alexander Herzog
 */
public class RunData {
	/**
	 * Anzahl an momentan verfügbaren Agenten
	 */
	public int freeAgents;

	/**
	 * Zählung der Anzahl an Anrufern (für Ende der Simulation und Ende der Einschwingphase)
	 */
	public int calls;

	/**
	 * Letzter Zeitpunkt, an dem sich die Anzahl der verfügbaren Agenten, die Warteschlangenlänge
	 * oder die Anzahl an Kunden im System geändert hat.
	 * (Mit Hilfe dieser Variable kann eine Statistik erstellt werden, wie lange jeweils
	 * wie viele Agenten frei waren und wie lange wie viele Kunden in der Warteschlange und im System waren.)
	 */
	public long lastDataLogTime=0;

	/**
	 * Liste der wartenden Anrufer (repräsentiert durch ihre Warteabbruch-Events.
	 */
	public final Queue<CallCancelEvent> waitingCalls;

	/**
	 * Gibt an, ob sich das System noch in der Einschwingphase befindet.
	 */
	public boolean isWarmUpPeriod=true;

	/**
	 * Letzte Kundenankunftszeit (für Berechnung der Zwischenankunftszeiten)
	 */
	public long lastArrival=0;

	/**
	 * Letzter Abgangszeitpunkt (für Berechnung der Zwischenabgangszeiten)
	 */
	public long lastLeave=0;

	/**
	 * Konstruktor der Klasse <code>CallcenterDynamicSimData</code>
	 * @param agents	Die Variable <code>freeAgents</code> wird auf diesen Wert vorbelegt.
	 */

	/**
	 * Konstruktor der Klasse <code>RunData</code>
	 * @param runModel	Globales Laufzeitmodell, auf dessen Basis hier die Laufzeitdaten vorbereitet werden können (z.B. Arrays in passender Größe angelegt werden usw.)
	 */
	public RunData(final RunModel runModel) {
		freeAgents=runModel.agents;
		waitingCalls=new ArrayDeque<CallCancelEvent>(10000);
	}

	/**
	 * Vorbereitung einer einzelnen Wiederholung der Simulation
	 * @param nr	Nummer der Wiederholung (0-basierend und thread-lokal)
	 * @param simData	Objekt vom Typ <code>SimulationData</code>, welches das Laufzeitmodell (vom Typ <code>RunModel</code> im Feld <code>runModel</code>) und die Statistik (vom Typ <code>Statistics</code> im Feld <code>statistics</code>) enthält und den Zugriff auf die von <code>SimData</code> geerbten Basis-Funktionen ermöglicht
	 */
	public void initRun(final long nr, final SimulationData simData) {
		freeAgents=simData.runModel.agents;
		calls=0;
		waitingCalls.clear();

		/* Ersten Anruf(-batch) einplanen */
		simData.scheduleCall(simData.runModel.getInterArrivalTime(),true);
	}

	/**
	 * Abschluss einer einzelnen Wiederholung der Simulation
	 * @param now	Ausführungszeitpunkt des letzten Events
	 * @param simData	Objekt vom Typ <code>SimulationData</code>, welches das Laufzeitmodell (vom Typ <code>RunModel</code> im Feld <code>runModel</code>) und die Statistik (vom Typ <code>Statistics</code> im Feld <code>statistics</code>) enthält und den Zugriff auf die von <code>SimData</code> geerbten Basis-Funktionen ermöglicht
	 */
	public void doneRun(final long now, final SimulationData simData) {
		waitingCalls.clear();
		simData.logDistDataChange();
	}
}