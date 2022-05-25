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
package simulator.events;

import language.Language;
import simcore.Event;
import simcore.SimData;
import simulator.runmodel.SimulationData;

/**
 * Warteabbruch-Ereignis bei der Callcenter-Simulation
 * @author Alexander Herzog
 */
public final class CallCancelEvent extends Event {
	/**
	 * Beginn der Wartezeit (relevant für die Statistik)
	 */
	public long waitingStartTime;

	/**
	 * Konstruktor der Klasse
	 */
	public CallCancelEvent() {
		/*
		 * Wird nur benötigt, um einen JavaDoc-Kommentar für diesen (impliziten) Konstruktor
		 * setzen zu können, damit der JavaDoc-Compiler keine Warnung mehr ausgibt.
		 */
	}

	/**
	 * (Re-)Initialisierung des Warteabbruch-Ereignisses
	 * @param now	Aktuelle Systemzeit
	 * @param time	Zeitpunkt, zu dem der Warteabbruch erfolgen soll
	 */
	public void init(long now, long time) {
		super.init(time);
		waitingStartTime=now;
	}

	@Override
	public void run(SimData data) {
		SimulationData simData=(SimulationData)data;
		if (simData.loggingActive) simData.logEventExecution(Language.tr("Simulator.Log.CallCancelEvent"),-1,"  "+String.format(Language.tr("Simulator.Log.CallCancelEvent.Info"),SimData.formatSimTime(time-waitingStartTime)));

		/* Erfassung von Daten in der Statistik */
		simData.logWaitingTime((double)(time-waitingStartTime)/1000);

		/* Erfassung der Zwischenabgangszeiten*/
		simData.statistics.interleaveTime.add(((double)(time-simData.runData.lastLeave))/1000);
		simData.runData.lastLeave=time;

		simData.runData.waitingCalls.remove(this);

		/* Evtl. später neuer Versuch */
		simData.testAndScheduleCallRetry();

		/* Zustandsänderungen für Statistik erfassen */
		simData.logDistDataChange();
	}
}
