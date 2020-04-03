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

import java.util.concurrent.ThreadLocalRandom;

import language.Language;
import simcore.Event;
import simcore.SimData;
import simulator.runmodel.SimulationData;

/**
 * Ereignis, das beim Ende eines Anrufs bei der Callcenter-Simulation ausgeführt wird.<br>
 * @author Alexander Herzog
 */
public final class CallDone1Event extends Event {
	@Override
	public final void run(SimData data) {
		SimulationData simData=(SimulationData)data;
		if (data.loggingActive) data.logEventExecution(Language.tr("Simulator.Log.CallDone1Event"),"  "+String.format(Language.tr("Simulator.Log.CallDone1Event.Info"),simData.runModel.batchWorking));

		/* Weiterleitungen ?*/
		for (int i=0;i<simData.runModel.batchWorking;i++) {
			if (ThreadLocalRandom.current().nextDouble()<simData.runModel.callContinueProbability) {
				simData.statistics.callContinued.add(true);
				if (data.loggingActive) data.logEventExecution(Language.tr("Simulator.Log.CallDone1Event"),"  "+Language.tr("Simulator.Log.CallDone1Event.Forwarding"));
				simData.scheduleCall(0,false);
			} else {
				simData.statistics.callContinued.add(false);
			}
		}

		/* Zustandsänderungen für Statistik erfassen */
		simData.logDistDataChange();

		/* Erfassung der Zwischenabgangszeiten*/
		simData.statistics.interleaveTime.add(((double)(time-simData.runData.lastLeave))/1000);
		simData.runData.lastLeave=time;

		/* Nachbearbeitungszeit planen */
		long postProcessingTime=simData.runModel.getPostProcessingTime();
		if (data.loggingActive) data.logEventExecution(Language.tr("Simulator.Log.CallDone1Event"),"  "+String.format(Language.tr("Simulator.Log.CallDone1Event.PostProcessing"),SimData.formatSimTime(time+postProcessingTime)));

		simData.statistics.postProcessingTime.add(((double)postProcessingTime)/1000);

		CallDone2Event callDone2Event=(CallDone2Event)data.getEvent(CallDone2Event.class);
		callDone2Event.init(time+postProcessingTime);
		simData.eventManager.addEvent(callDone2Event);
	}
}