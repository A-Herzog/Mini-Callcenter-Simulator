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
 * Prüft, ob sich noch Kunden in der Warteschlange befinden, die nicht mehr bedient werden können
 * (weil die Anzahl zu klein ist, um noch einen Batch zu bilden.
 * In diesem Fall wird die Warteschlange geleert.
 * @author Alexander Herzog
 */
public class StopTestEvent extends Event {

	@Override
	public void run(SimData data) {
		SimulationData simData=(SimulationData)data;
		if (simData.loggingActive) simData.logEventExecution(Language.tr("Simulator.Log.StopTestEvent"),"  "+Language.tr("Simulator.Log.StopTestEvent.Info"));

		/* Keiner mehr da? - Um so besser. Nichts tun, Simulation endet. */
		if (simData.runData.waitingCalls.size()==0) return;

		if (simData.runData.freeAgents==simData.runModel.agents) {
			/* Warteschlange leeren, da Agenten frei sind, aber Kunden dennoch nicht bedient werden. */
			while (simData.runData.waitingCalls.size()>0) {
				final CallCancelEvent cancelEvent=simData.getNextFromQueue();
				simData.logWaitingTime((double)(time-cancelEvent.waitingStartTime)/1000);
			}
		} else {
			/* Später noch einmal prüfen. */
			simData.scheduleStopTest();
		}
	}
}
