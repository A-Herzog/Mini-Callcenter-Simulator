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
import org.apache.commons.math3.distribution.AbstractRealDistribution;

import mathtools.distribution.NeverDistributionImpl;
import mathtools.distribution.tools.DistributionRandomNumber;
import mathtools.distribution.tools.DistributionTools;
import simulator.editmodel.EditModel;

/**
 * Laufzeit-Modell
 * Dieses Modell wird read-only in der Simulation verwendet. Im Gegensatz zu dem Editor-Modell ist es auf konsistenz geprüft,
 * Bediensationen sind per Referenzen verknüpft, nicht mehr nur durch Freitextfelder.
 * @author Alexander Herzog
 * @see EditModel
 * @see RunModel#getRunModel(EditModel)
 */
public class RunModel {
	/**
	 * Prozentualer Anteil der Einschwingphase bezogen auf die gesamte Anzahl an Anrufen.
	 * (Die Einschwingphase wird auf die Anzahl an Anrufen aufgeschlagen, d.h. es stehen für die
	 * Statistik stets so viele Anrufe zur Verfügung, wie vom Nutzer eingestellt.)
	 */
	public static double warmUpPeriodPart=0.1;

	/**
	 * Zwischenankunftzeitverteilung
	 */
	public AbstractRealDistribution interArrivalTimeDist;

	/**
	 * Kunden treffen nicht einzeln, sondern in Batches dieser Größen ein.
	 */
	public int batchArrival;

	/**
	 * Wartezeittoleranzverteilung
	 */
	public AbstractRealDistribution waitingTimeDist;

	/**
	 * Bedienzeitverteilung
	 */
	public AbstractRealDistribution workingTimeDist;

	/**
	 * Nachbearbeitungszeitverteilung
	 */
	public AbstractRealDistribution postProcessingTimeDist;

	/**
	 * Ein Agent arbeitet steht so viele Kunden gleichzeitig ab. Sind nicht genug Kunden vorhanden, müssen die anwesenden Kunden weiter warten.
	 */
	public int batchWorking;

	/**
	 * Bedienreihenfolge
	 */
	public EditModel.QueueMode queueMode;

	/**
	 * Wiederholabständeverteilung
	 */
	public AbstractRealDistribution retryTimeDist;

	/**
	 * Anzahl an Callcenter-Agenten
	 */
	public int agents;

	/**
	 * Weiterleitungswahrscheinlichkeit
	 */
	public double callContinueProbability;

	/**
	 * Wiederholwahrscheinlichkeit (nach Besetztzeichen oder Warteabbruch)
	 */
	public double retryProbability;

	/**
	 * Anzahl an Wiederholungen der Simulation
	 */
	public int repeatCount;

	/**
	 * Anzahl der zu simulierenden Erstanrufe
	 */
	public int callsToSimulate;

	/**
	 * Größe des Warteraums (ein in Bedienung befindlicher Anrufer belegt keinen Warteraum mehr)
	 */
	public int waitingRoomSize;

	/**
	 * Anzahl an Anrufen, die nicht für die Statistik gezählt werden sollen
	 */
	public int warmUpPeriod;

	/**
	 * Erfassung der Autokorrelation der Wartezeiten
	 */
	public boolean collectCorrelation;

	/**
	 * Ein <code>RunModel</code> kann nicht direkt erzeugt werden, sondern es kann nur ein <code>EditModel</code>
	 * mittels der Funktion <code>getRunModel</code> in ein <code>RunModel</code> umgeformt werden. Dabei wird das
	 * Modell auf Konsistenz geprüft und alle notwendigen Verknüpfungen werden hergestellt.
	 * @see EditModel
	 * @see RunModel#getRunModel(EditModel)
	 */
	private RunModel() {
	}

	/**
	 * Wandelt ein <code>EditModel</code> in ein <code>RunModel</code> um. Dabei wird das Modell auf Konsistenz geprüft
	 * und alle notwendigen Verknüpfungen werden hergestellt.
	 * @param editModel	Editor-Modell, welches in ein Laufzeit-Modell umgewandelt werden soll
	 * @return	Gibt im Erfolgsfall ein Objekt vom Typ <code>RunModel</code> zurück, sonst einen String mit einer Fehlermeldung.
	 * @see EditModel
	 */
	public static Object getRunModel(final EditModel editModel) {
		RunModel runModel=new RunModel();

		runModel.interArrivalTimeDist=DistributionTools.cloneDistribution(editModel.interArrivalTimeDist);
		runModel.batchArrival=editModel.batchArrival;
		runModel.waitingTimeDist=DistributionTools.cloneDistribution(editModel.waitingTimeDist);
		runModel.workingTimeDist=DistributionTools.cloneDistribution(editModel.workingTimeDist);
		runModel.postProcessingTimeDist=DistributionTools.cloneDistribution(editModel.postProcessingTimeDist);
		runModel.batchWorking=editModel.batchWorking;
		runModel.queueMode=editModel.queueMode;
		runModel.retryTimeDist=DistributionTools.cloneDistribution(editModel.retryTimeDist);
		runModel.agents=editModel.agents;
		runModel.callContinueProbability=editModel.callContinueProbability;
		runModel.retryProbability=editModel.retryProbability;

		int cores=Runtime.getRuntime().availableProcessors();
		int split=cores;

		runModel.callsToSimulate=(int)Math.round(Math.ceil(((double)editModel.callsToSimulate)/split));
		while (runModel.callsToSimulate>50000) {
			split*=2;
			runModel.callsToSimulate=(int)Math.round(Math.ceil(((double)editModel.callsToSimulate)/split));
		}

		runModel.repeatCount=split;
		runModel.warmUpPeriod=(int)Math.round(Math.ceil((runModel.callsToSimulate)*warmUpPeriodPart));
		runModel.callsToSimulate+=runModel.warmUpPeriod;
		runModel.waitingRoomSize=editModel.waitingRoomSize;
		runModel.collectCorrelation=editModel.collectCorrelation;

		return runModel;
	}

	/**
	 * Liefert eine Zufallszahl gemäß Zwischenankunftszeitverteilung
	 * (bereits umgerechnet in einen <code>long</code>-Wert für die Simulation)
	 * @return	Zufällige Zwischenankunftszeit
	 */
	public final long getInterArrivalTime() {
		return (long)(1000*DistributionRandomNumber.randomNonNegative(interArrivalTimeDist));
	}

	/**
	 * Liefert eine Zufallszahl gemäß Wartezeittoleranzverteilung
	 * (bereits umgerechnet in einen <code>long</code>-Wert für die Simulation)
	 * @return	Zufällige Wartezeittoleranz
	 */
	public final long getWaitingToleranceTime() {
		if (waitingTimeDist instanceof NeverDistributionImpl) return 1000*86400*365*100; /* Wenn der Kunde beliebig lange bereit ist zu warten: Warteabbruchzeit in 100 Jahren. */
		return (long)(1000*DistributionRandomNumber.randomNonNegative(waitingTimeDist));
	}

	/**
	 * Liefert eine Zufallszahl gemäß Bedienzeitenverteilung
	 * (bereits umgerechnet in einen <code>long</code>-Wert für die Simulation)
	 * @return	Zufällige Bedienzeit
	 */
	public final long getWorkingTime() {
		return (long)(1000*DistributionRandomNumber.randomNonNegative(workingTimeDist));
	}

	/**
	 * Liefert eine Zufallszahl gemäß Nachbearbeitungszeitenverteilung
	 * (bereits umgerechnet in einen <code>long</code>-Wert für die Simulation)
	 * @return	Zufällige Nachbearbeitungszeit
	 */
	public final long getPostProcessingTime() {
		return (long)(1000*DistributionRandomNumber.randomNonNegative(postProcessingTimeDist));
	}

	/**
	 * Liefert eine Zufallszahl gemäß Wiederholabständeverteilung
	 * (bereits umgerechnet in einen <code>long</code>-Wert für die Simulation)
	 * @return	Zufälliger Wiederholabstand
	 */
	public final long getRetryTime() {
		return (long)(1000*DistributionRandomNumber.randomNonNegative(retryTimeDist));
	}
}
