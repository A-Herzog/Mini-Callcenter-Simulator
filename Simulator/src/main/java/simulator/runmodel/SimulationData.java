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
import java.util.concurrent.ThreadLocalRandom;

import language.Language;
import simcore.SimData;
import simcore.eventcache.HashMapEventCache;
import simcore.eventmanager.PriorityQueueEventManager;
import simulator.editmodel.EditModel;
import simulator.events.CallCancelEvent;
import simulator.events.CallDone1Event;
import simulator.events.CallEvent;
import simulator.events.StopTestEvent;
import simulator.statistics.Statistics;

/**
 * Diese Klasse enthält alle Daten, die zur Laufzeit der Simulation von einem Simulationsthread verwendet werden.
 * @author Alexander Herzog
 */
public class SimulationData extends SimData {
	/**
	 * Instanz des Laufzeit-Modells
	 * (read-only, da zwischen allen Threads geteilt; dynamische Daten werden in <code>RunData</code>, nicht in <code>RunModel</code> abgelegt)
	 */
	public final RunModel runModel;

	/**
	 * Instanz der dynamischen Daten
	 * (thread-lokal)
	 */
	public final RunData runData;

	/**
	 * Statistik-Objekt, welches während der Simulation die Daten sammelt
	 * (thread-lokal, die Ergebnisse werden am Ende zusammengeführt)
	 */
	public final Statistics statistics;

	/**
	 * Statistik-Objekt, welches die Daten der einzelnen Läufe aufnimmt.
	 * Das obige <code>statistics</code>-Objekt wird bei jedem Lauf zurückgesetzt.
	 * (thread-lokal, die Ergebnisse werden am Ende zusammengeführt)
	 */
	public final Statistics collectStatistics;

	/**
	 * Konstruktor der Klasse <code>SimulationData</code>
	 * @param threadNr		Gibt die Nummer des Threads an, für den das <code>SimDat</code>-Objekt erstellt wird.
	 * @param threadCount	Anzahl der Rechenthreads
	 * @param runModel	Laufzeit-Modell, welches die Basis der Simulation darstellt
	 */
	public SimulationData(final int threadNr, final int threadCount, final RunModel runModel) {
		super(new PriorityQueueEventManager(),new HashMapEventCache(),threadNr,threadCount);
		this.runModel=runModel;
		this.runData=new RunData(runModel);
		statistics=new Statistics(runModel.collectCorrelation);
		collectStatistics=new Statistics(runModel.collectCorrelation);

		simDaysByOtherThreads=0;
		for (int i=0;i<threadNr;i++) {
			simDaysByOtherThreads+=runModel.repeatCount/threadCount;
			if (runModel.repeatCount%threadCount>=threadNr+1) simDaysByOtherThreads++;
		}

		simDays=runModel.repeatCount/threadCount;
		if (runModel.repeatCount%threadCount>=threadNr+1) simDays++;
	}

	@Override
	public void initDay(final long day, final long dayGlobal, final boolean backgroundMode) {
		currentTime=0;
		statistics.resetData();
		runData.initRun(day,this);
	}

	@Override
	public void terminateCleanUp(final long now) {
		runData.doneRun(now,this);
		collectStatistics.addData(statistics);
	}

	/**
	 * Legt ein <code>CallEvent</code>-Objekte an
	 * @param timeFromNow	Zeitabstand von der aktuellen Zeit an gerechnet
	 * @param newCall	Wird auf <code>true</code> gesetzt, wenn es sich um einen Erstanrufer und nicht um einen Wiederholer handelt
	 * @see CallEvent
	 */
	public final void scheduleCall(final long timeFromNow, final boolean newCall) {
		/* Den nächsten Anrufe einplanen */
		CallEvent callEvent=(CallEvent)getEvent(CallEvent.class);
		callEvent.init(currentTime+timeFromNow);
		callEvent.isNewCall=newCall;
		eventManager.addEvent(callEvent);
		if (loggingActive) {
			final String s=(newCall)?String.format(Language.tr("Simulator.Log.ScheduleCall.Info.New"),runModel.batchArrival):Language.tr("Simulator.Log.ScheduleCall.Info.Retry");
			logEventExecution(Language.tr("Simulator.Log.ScheduleCall"),"  "+String.format(Language.tr("Simulator.Log.ScheduleCall.Info"),s,formatSimTime(currentTime+timeFromNow)));
		}
	}

	/**
	 * Legt ein <code>CallCancelEvent</code> an
	 * @param timeFromNow	Zeitabstand von der aktuellen Zeit an gerechnet
	 * @see CallCancelEvent
	 */
	public final void scheduleCallCancel(long timeFromNow) {
		CallCancelEvent cancelEvent=(CallCancelEvent)getEvent(CallCancelEvent.class);
		cancelEvent.init(currentTime,currentTime+timeFromNow);
		runData.waitingCalls.add(cancelEvent);
		eventManager.addEvent(cancelEvent);
		if (loggingActive) logEventExecution(Language.tr("Simulator.Log.ScheduleCallCancel"),"  "+String.format(Language.tr("Simulator.Log.ScheduleCallCancel.Info"),formatSimTime(currentTime+timeFromNow)));
	}

	/**
	 * Prüft bei einem Warteabbrecher, ob dieser evtl. später einen neuen Versuch starten möchte und plant diesen ggf. ein.
	 */
	public final void testAndScheduleCallRetry() {
		if (ThreadLocalRandom.current().nextDouble()<runModel.retryProbability) {
			statistics.callRetry.add(true);
			long retryTime=runModel.getRetryTime();
			scheduleCall(retryTime,false);
			if (loggingActive) logEventExecution(Language.tr("Simulator.Log.TestAndScheduleCallRetry"),"  "+String.format(Language.tr("Simulator.Log.TestAndScheduleCallRetry.Retry"),formatSimTime(currentTime+retryTime)));
		} else {
			statistics.callRetry.add(false);
			if (loggingActive) logEventExecution(Language.tr("Simulator.Log.TestAndScheduleCallRetry"),"  "+Language.tr("Simulator.Log.TestAndScheduleCallRetry.FinalCancelation"));
		}
	}

	/**
	 * Plant ein Event zur Beendung der Simulation ein.
	 */
	public final void scheduleStopTest() {
		StopTestEvent stopTestEvent=(StopTestEvent)getEvent(StopTestEvent.class);
		stopTestEvent.init(currentTime+1000); /* eine Sekunde warten */
		eventManager.addEvent(stopTestEvent);
	}

	/**
	 * Prüft, ob es freie Agenten und wartende Kunden gibt und startet ggf. ein Gespräch.
	 * @param newCalls	Anzahl an neu an der Warteschlange eingetroffenen Kunden. Dies können Erstanrufer oder weitergeleitete Kunden sein.
	 */
	public final void tryStartCall(int newCalls) {
		/* Abstände der Anrufe erfassen */
		for (int i=0;i<newCalls;i++) {
			statistics.interarrivalTime.add(((double)(currentTime-runData.lastArrival))/1000);
			runData.lastArrival=currentTime;
		}

		if (loggingActive) logEventExecution(Language.tr("Simulator.Log.TryStartCall"),"  "+String.format(Language.tr("Simulator.Log.TryStartCall.Info"),runData.waitingCalls.size(),newCalls,runData.freeAgents));

		while (runData.freeAgents>0) {
			int availableClients=runData.waitingCalls.size()+newCalls;
			if (availableClients<runModel.batchWorking) break;

			long waitingStartTime;
			long workingTime=runModel.getWorkingTime();

			for (int i=0;i<runModel.batchWorking;i++) {
				if (runData.waitingCalls.size()>0) {
					if (loggingActive) logEventExecution(Language.tr("Simulator.Log.TryStartCall"),"  "+Language.tr("Simulator.Log.TryStartCall.StartWaiting"));
					final CallCancelEvent cancelEvent=getNextFromQueue();
					waitingStartTime=cancelEvent.waitingStartTime;
				} else {
					if (loggingActive) logEventExecution(Language.tr("Simulator.Log.TryStartCall"),"  "+Language.tr("Simulator.Log.TryStartCall.StartNew"));
					newCalls--;
					waitingStartTime=currentTime;
				}

				logWaitingTime((double)(currentTime-waitingStartTime)/1000,((double)workingTime)/1000);
			}

			startTalk(workingTime);
		}

		/* Kunden, die nicht sofort bedient werden können, an die Warteschlange anstellen (oder abweisen, wenn diese voll ist) */
		for (int i=0;i<newCalls;i++) {
			trySendCallToQueue();
		}
	}

	/**
	 * Versucht, einen Kunden an die Warteschlange an zu stellen (bzw. weist ihn ab, wenn die Warteschlange voll ist).
	 */
	public final void trySendCallToQueue() {
		if (runModel.waitingRoomSize>=0 && runData.waitingCalls.size()>=runModel.waitingRoomSize) {
			if (loggingActive) logEventExecution(Language.tr("Simulator.Log.TryStartCall"),"  "+Language.tr("Simulator.Log.TryStartCall.WaitingRoomFull"));
			statistics.interleaveTime.add(((double)(currentTime-runData.lastLeave))/1000);
			runData.lastLeave=currentTime;
			logWaitingTime(0.0);
			statistics.callRejected.add(true);
			testAndScheduleCallRetry();
		} else {
			if (loggingActive) logEventExecution(Language.tr("Simulator.Log.TryStartCall"),"  "+Language.tr("Simulator.Log.TryStartCall.QueueingClient"));
			statistics.callRejected.add(false);
			long waitingTimeTolerance=runModel.getWaitingToleranceTime();
			scheduleCallCancel(waitingTimeTolerance);
		}
	}

	/**
	 * Startet die Arbeit eines Agenten<br>
	 * (Fügt ein <code>CallDoneEvent</code>-Ereignis ein und verringert den Zähler der freien Agenten)
	 * @param workingTime	Bedienzeit
	 */
	public final void startTalk(final long workingTime) {
		if (loggingActive) logEventExecution(Language.tr("Simulator.Log.StartTalk"),"  "+String.format(Language.tr("Simulator.Log.StartTalk.Info"),SimData.formatSimTime(currentTime+workingTime)));

		statistics.workingTime.add(((double)workingTime)/1000);

		CallDone1Event callDone1Event=(CallDone1Event)getEvent(CallDone1Event.class);
		callDone1Event.init(currentTime+workingTime);
		eventManager.addEvent(callDone1Event);

		runData.freeAgents--;
	}

	/**
	 * Setzt nach einem Reset der Statistik die Startzeit der Simulation (für die Statistik)
	 * auf die aktuelle Zeit, um erfassen zu können, wie lange sich das System in welchem
	 * Zustand befunden hat.
	 */
	public final void initDistDataChange() {
		statistics.freeAgents.setTime(currentTime);
		statistics.busyAgents.setTime(currentTime);
		statistics.queueLength.setTime(currentTime);
		statistics.systemLength.setTime(currentTime);
		runData.lastArrival=currentTime;
		runData.lastLeave=currentTime;
	}

	/**
	 * Erfasst, wenn sich die Anzahl an freien Agenten oder wartenden Kunden ändert in der Statistik
	 * (Ist aufzurufen unmittelbar <b>nachdem</b> sich der Wert geändert hat.)
	 */
	public final void logDistDataChange() {
		int freeAgents=runData.freeAgents;
		int busyAgents=runModel.agents-runData.freeAgents;
		int queueLength=runData.waitingCalls.size();
		int systemLength=queueLength+busyAgents*runModel.batchWorking;

		statistics.freeAgents.set(currentTime,freeAgents);
		statistics.busyAgents.set(currentTime,busyAgents);
		statistics.queueLength.set(currentTime,queueLength);
		statistics.systemLength.set(currentTime,systemLength);
	}

	/**
	 * Erfasst einen erfolglosen Anruf (Warteabbruch oder Abweisung) in der Statistik
	 * @param waitingTime	Abbruchzeit des Anrufers
	 */
	public final void logWaitingTime(final double waitingTime) {
		statistics.callSuccessful.add(false);
		statistics.callNeedToWait.add(waitingTime>0);

		statistics.waitingTimeAll.add(waitingTime);
		statistics.waitingTimeCancel.add(waitingTime);
		statistics.systemTimeAll.add(waitingTime);
	}

	/**
	 * Erfasst einen erfolgreichen Anruf in der Statistik
	 * @param waitingTime	Wartezeit des Anrufers
	 * @param workingTime	Bedienzeit für den Anruf
	 */
	public final void logWaitingTime(final double waitingTime, final double workingTime) {
		statistics.callSuccessful.add(true);
		statistics.callNeedToWait.add(waitingTime>0);

		statistics.waitingTimeAll.add(waitingTime);
		statistics.waitingTimeSuccess.add(waitingTime);
		statistics.systemTimeAll.add(waitingTime+workingTime);
		statistics.systemTimeSuccess.add(waitingTime+workingTime);

		statistics.callServiceLevel.add(waitingTime<=20);
	}

	/**
	 * Bricht die Simulation sofort ab.
	 * @param message	Meldung, die in Logdatei und in die Warnungen der Statistik aufgenommen werden soll.
	 */
	public void doEmergencyShutDown(final String message) {
		statistics.simulationData.emergencyShutDown=true;
		statistics.simulationData.addWarning(message);
		logEventExecution(Language.tr("Simulation.Log.Abort"),message);
		if (eventManager!=null) eventManager.deleteAllEvents();
	}

	@Override
	public void catchException(final String text) {
		doEmergencyShutDown(text);
	}

	@Override
	public void catchOutOfMemory(final String text) {
		doEmergencyShutDown(Language.tr("Simulation.OutOfMemory")+"\n"+text);
	}

	/**
	 * Liefert den jeweils nächsten Kunden aus der Warteschlange.
	 * @return	Nächster zu bedienender Kunde aus der Warteschlange
	 * @see RunModel#queueMode
	 */
	public CallCancelEvent getNextFromQueue() {
		final CallCancelEvent cancelEvent;
		if (runModel.queueMode==EditModel.QueueMode.LIFO) {
			/* LIFO */
			cancelEvent=runData.waitingCalls.pollLast();
		} else {
			/* FIFO */
			cancelEvent=runData.waitingCalls.poll();
		}
		eventManager.deleteEvent(cancelEvent,this);
		return cancelEvent;
	}
}