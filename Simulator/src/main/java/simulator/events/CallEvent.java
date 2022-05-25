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
 * Anruf-Ereignis w�hrend der Callcenter-Simulation
 * @author Alexander Herzog
 * @version 1.0
 */
public final class CallEvent extends Event {
	/**
	 * Legt fest, ob es sich um einen neuen Anruf oder um eine Wiederholung handelt (relevant f�r die Statistik)
	 */
	public boolean isNewCall;

	/**
	 * Konstruktor der Klasse
	 */
	public CallEvent() {
		/*
		 * Wird nur ben�tigt, um einen JavaDoc-Kommentar f�r diesen (impliziten) Konstruktor
		 * setzen zu k�nnen, damit der JavaDoc-Compiler keine Warnung mehr ausgibt.
		 */
	}

	/**
	 * Z�hlt den Anruf aus Simulationssicht, d.h. beendet ggf. die Einschwingphase (und setzt dabei die Statistik zur�ck)
	 * und plant entweder den n�chsten Anruf ein oder ab f�gt das "Simulationsende"-Ereignis in die Ereignisliste ein.
	 * @param simData	<code>SimulationData</code>-Objekt
	 * @param count	Anzahl der Ank�nfte (bei Batch &gt;1)
	 */
	private void simCallCount(final SimulationData simData, final int count) {
		/* Z�hlung des Anrufs in Bezug zu dem <code>simData.runModel.callsToSimulate</code>-Wert */
		simData.runData.calls+=count;

		/* Einschwingphase zu Ende? */
		if (simData.runData.isWarmUpPeriod && simData.runData.calls>simData.runModel.warmUpPeriod) {
			if (simData.loggingActive) simData.logEventExecution(Language.tr("Simulator.Log.CallEvent"),-1,"  "+Language.tr("Simulator.Log.CallEvent.EndOfWarmUp"));
			simData.runData.isWarmUpPeriod=false;
			simData.statistics.resetData();
			simData.initDistDataChange();
		}

		/* Simulation zu Ende oder aber n�chsten Anruf einplanen? */
		if (simData.runData.calls>=simData.runData.callsToSimulate) {
			simData.scheduleStopTest();
		} else {
			simData.scheduleCall(simData.runModel.getInterArrivalTime(),true);
		}
	}

	@Override
	public void run(SimData data) {
		SimulationData simData=(SimulationData)data;
		if (data.loggingActive) data.logEventExecution(Language.tr("Simulator.Log.CallEvent"),-1,"  "+(isNewCall?Language.tr("Simulator.Log.CallEvent.FreshCall"):Language.tr("Simulator.Log.CallEvent.Retryer")));

		int newCallCount=(isNewCall)?simData.runModel.batchArrival:1;

		/* Erfassung von Daten in der Statistik */
		if (isNewCall) {
			simCallCount(simData,newCallCount);
			for (int i=0;i<simData.runModel.batchArrival;i++) simData.statistics.freshCalls.add();
		}

		/* Kunden zu Agenten schicken oder in Warteschlange stellen oder abweisen */
		simData.tryStartCall(newCallCount);

		/* Zustands�nderungen f�r Statistik erfassen */
		simData.logDistDataChange();
	}
}
