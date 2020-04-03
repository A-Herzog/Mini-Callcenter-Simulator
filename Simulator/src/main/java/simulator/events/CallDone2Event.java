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
 * Ereignis, das beim Ende der Nachbearbeitungszeit bei der Callcenter-Simulation ausgeführt wird.<br>
 * (Durch dieses Ereignis wird u.a. geprüft, ob es wartende Kunden gibt und ggf. der nächste Bedienprozess gestartet.)
 * @author Alexander Herzog
 */
public class CallDone2Event extends Event {
	@Override
	public final void run(SimData data) {
		SimulationData simData=(SimulationData)data;
		if (data.loggingActive) data.logEventExecution(Language.tr("Simulator.Log.CallDone2Event"),"  "+Language.tr("Simulator.Log.CallDone2Event.Info"));

		/* Agent ist wieder frei */
		simData.runData.freeAgents++;

		/* Sind Kunden zum Bedienen in der Warteschlange ? */
		if (simData.runData.waitingCalls.size()>=simData.runModel.batchWorking)	{
			long workingTime=simData.runModel.getWorkingTime();

			for (int i=0;i<simData.runModel.batchWorking;i++) {
				if (simData.loggingActive) simData.logEventExecution(Language.tr("Simulator.Log.CallDone2Event"),"  "+Language.tr("Simulator.Log.CallDone2Event.WaitingClient"));
				CallCancelEvent cancelEvent=simData.runData.waitingCalls.poll();
				simData.eventManager.deleteEvent(cancelEvent,simData);
				long waitingStartTime=cancelEvent.waitingStartTime;
				simData.logWaitingTime((double)(time-waitingStartTime)/1000,((double)workingTime)/1000);
			}

			simData.startTalk(workingTime);
		}

		/* Zustandsänderungen für Statistik erfassen */
		simData.logDistDataChange();
	}
}
