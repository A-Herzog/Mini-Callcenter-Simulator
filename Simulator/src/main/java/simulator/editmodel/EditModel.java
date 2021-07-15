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
package simulator.editmodel;

import java.io.File;

import org.apache.commons.math3.distribution.AbstractRealDistribution;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import language.Language;
import mathtools.NumberTools;
import mathtools.distribution.NeverDistributionImpl;
import mathtools.distribution.OnePointDistributionImpl;
import mathtools.distribution.tools.DistributionTools;
import simulator.runmodel.RunModel;
import ui.MainPanel;

/**
 * Editor-Modell
 * Dieses Modell kann inkonsistent sein, da Verknüpfungen zwischen Bedienstationen usw. nur per Freitext erfolgen.
 * Für diese Simulation wird aus diesem Modell ein Laufzeit-Modell in Form der Klasse <code>RunModel</code> abgeleitet.
 * Das Editor-Modell selbst wird in der Simulation nicht verwendet.
 * @author Alexander Herzog
 * @see RunModel
 * @see EditModelBase
 */
public class EditModel extends EditModelBase implements Cloneable {
	/**
	 * Bedienrreihenfolge
	 * @author Alexander Herzog
	 * @see EditModel#queueMode
	 */
	public enum QueueMode {
		/** Fist in first out - Bedienung in Ankunftsreihenfolge */
		FIFO("FIFO"),
		/** Last in first out - Bedienung in umgekehrter Ankunftsreihenfolge */
		LIFO("LIFO");

		/** Name der Bedienreihenfolge zum Laden/Speichern in xml-Dateien */
		public final String xmlName;

		/**
		 * Konstruktor des Enum
		 * @param xmlName	Name der Bedienreihenfolge zum Laden/Speichern in xml-Dateien
		 */
		QueueMode(final String xmlName) {
			this.xmlName=xmlName;
		}

		/**
		 * Liefert ein {@link QueueMode}-Objekt basierend auf einem xml-Datenfeld-Namen
		 * @param xmlName	xml-Datenfeld-Namen zu dem das {@link QueueMode}-Objekt gefunden werden soll
		 * @return	Passender Bedienreihenfolge-Modus oder Fallback-Modus (FIFO), wenn der String zu keinem Modus passt
		 */
		public static QueueMode getFromName(final String xmlName) {
			for (QueueMode queueMode: QueueMode.values()) if (queueMode.xmlName.equalsIgnoreCase(xmlName)) return queueMode;
			return QueueMode.FIFO;
		}
	}

	/**
	 * Version des Simulators diese wird in die Modell- und die Statistik-Dateien geschrieben, damit der Simulator
	 * warnen kann, wenn eine Datei, die mit einer späteren Version erstellt wurde, mit einer früheren Version, die
	 * evtl. nicht alle gespeicherten Eigenschaften darstellen kann, geöffnet wird.
	 */
	public static final String systemVersion=MainPanel.systemVersion;

	/**
	 * Konstruktor der Klasse <code>EditModel</code>
	 */
	public EditModel() {
		resetData();
	}

	/**
	 * Wurzel-Element für Modell-xml-Dateien
	 */
	@Override
	public String[] getRootNodeNames() {
		return Language.trAll("Model.XML.Root");
	}

	/**
	 * Version des Simulators, mit der dieses Editor-Modell erstellt bzw. zu letzt gespeichert wurde.
	 */
	public String version;

	/**
	 * Name des Modells (hat für die Simulation keine Bedeutung, nur zur Bezeichnung des Modells)
	 */
	public String name;

	/**
	 * Beschreibung des Modells (hat für die Simulation keine Bedeutung, nur zur Bezeichnung des Modells)
	 */
	public String description;

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
	 * @see EditModel.QueueMode
	 */
	public QueueMode queueMode;

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
	 * Anzahl der zu simulierenden Erstanrufe
	 */
	public int callsToSimulate;

	/**
	 * Zusätzliche Anrufe die als Einschwingphase simuliert werden sollen
	 */
	public int callsToSimulateWarmUp;

	/**
	 * Größe des Warteraums (ein in Bedienung befindlicher Anrufer belegt keinen Warteraum mehr)
	 */
	public int waitingRoomSize;

	/**
	 * Erfassung der Autokorrelation der Wartezeiten
	 */
	public boolean collectCorrelation;

	@Override
	protected void resetData() {
		version=systemVersion;

		name="";
		description="";

		interArrivalTimeDist=new ExponentialDistribution(null,60);
		batchArrival=1;
		waitingTimeDist=new NeverDistributionImpl();
		workingTimeDist=new ExponentialDistribution(null,180);
		postProcessingTimeDist=new OnePointDistributionImpl(0);
		batchWorking=1;
		queueMode=QueueMode.FIFO;
		retryTimeDist=new ExponentialDistribution(null,1800);
		agents=4;
		callContinueProbability=0;
		retryProbability=0;
		callsToSimulate=100000;
		callsToSimulateWarmUp=10000;
		waitingRoomSize=-1;
		collectCorrelation=false;
	}

	/**
	 * Erstellt eine Kopie des <code>EditModel</code>-Objektes
	 */
	@Override
	public EditModel clone() {
		EditModel clone=new EditModel();

		clone.version=version;

		clone.name=name;
		clone.description=description;

		clone.interArrivalTimeDist=DistributionTools.cloneDistribution(interArrivalTimeDist);
		clone.batchArrival=batchArrival;
		clone.waitingTimeDist=DistributionTools.cloneDistribution(waitingTimeDist);
		clone.workingTimeDist=DistributionTools.cloneDistribution(workingTimeDist);
		clone.postProcessingTimeDist=DistributionTools.cloneDistribution(postProcessingTimeDist);
		clone.batchWorking=batchWorking;
		clone.queueMode=queueMode;
		clone.retryTimeDist=DistributionTools.cloneDistribution(retryTimeDist);
		clone.agents=agents;
		clone.callContinueProbability=callContinueProbability;
		clone.retryProbability=retryProbability;
		clone.callsToSimulate=callsToSimulate;
		clone.callsToSimulateWarmUp=callsToSimulateWarmUp;
		clone.waitingRoomSize=waitingRoomSize;
		clone.collectCorrelation=collectCorrelation;

		return clone;
	}

	/**
	 * Vergleicht das Editor-Modell mit einem anderen Editor-Modell
	 * (z.B. um zu prüfen, ob das Modell vor dem Verlassen des Programms gespeichert werden muss)
	 * @param otherModel	Editor-Modell, mit dem dieses Modell verglichen werden soll
	 * @return	Liefert <code>true</code> zurück, wenn die beiden Modelle identisch sind
	 */
	public boolean equalsEditModel(final EditModel otherModel) {
		if (!version.equalsIgnoreCase(otherModel.version)) return false;

		if (!name.equals(otherModel.name)) return false;
		if (!description.equals(otherModel.description)) return false;

		if (!DistributionTools.compare(interArrivalTimeDist,otherModel.interArrivalTimeDist)) return false;
		if (batchArrival!=otherModel.batchArrival) return false;
		if (!DistributionTools.compare(interArrivalTimeDist,otherModel.interArrivalTimeDist)) return false;
		if (!DistributionTools.compare(workingTimeDist,otherModel.workingTimeDist)) return false;
		if (!DistributionTools.compare(postProcessingTimeDist,otherModel.postProcessingTimeDist)) return false;
		if (batchWorking!=otherModel.batchWorking) return false;
		if (queueMode!=otherModel.queueMode) return false;
		if (!DistributionTools.compare(retryTimeDist,otherModel.retryTimeDist)) return false;
		if (agents!=otherModel.agents) return false;
		if (callContinueProbability!=otherModel.callContinueProbability) return false;
		if (retryProbability!=otherModel.retryProbability) return false;
		if (callsToSimulate!=otherModel.callsToSimulate) return false;
		if (callsToSimulateWarmUp!=otherModel.callsToSimulateWarmUp) return false;
		if (waitingRoomSize!=otherModel.waitingRoomSize) return false;
		if (collectCorrelation!=otherModel.collectCorrelation) return false;

		return true;
	}


	@Override
	protected String loadProperty(final String name, final String text, final Element node) {
		if (Language.trAll("Model.XML.Version",name)) {
			version=text;
			if (version.isEmpty()) version=systemVersion;
			return null;
		}

		if (Language.trAll("Model.XML.Name",name)) {
			this.name=text;
			return null;
		}

		if (Language.trAll("Model.XML.Description",name)) {
			description=text;
			return null;
		}

		if (Language.trAll("Model.XML.InterArrivalTimes",name)) {
			final AbstractRealDistribution dist=DistributionTools.distributionFromString(text,3600);
			if (dist==null) return Language.tr("Model.XML.InterArrivalTimes.Error");
			interArrivalTimeDist=dist;
			return null;
		}

		if (Language.trAll("Model.XML.BatchArrival",name)) {
			final Integer J=NumberTools.getNotNegativeInteger(text);
			if (J==null || J==0) return String.format(Language.tr("Model.XML.BatchArrival.Error"),text);
			batchArrival=J;
			return null;
		}

		if (Language.trAll("Model.XML.WaitingTimeTolerances",name)) {
			final AbstractRealDistribution dist=DistributionTools.distributionFromString(text,3600);
			if (dist==null) return Language.tr("Model.XML.WaitingTimeTolerances.Error");
			waitingTimeDist=dist;
			return null;
		}

		if (Language.trAll("Model.XML.ServiceTimes",name)) {
			final AbstractRealDistribution dist=DistributionTools.distributionFromString(text,3600);
			if (dist==null) return Language.tr("Model.XML.ServiceTimes.Error");
			workingTimeDist=dist;
			return null;
		}

		if (Language.trAll("Model.XML.PostProcessingTimes",name)) {
			final AbstractRealDistribution dist=DistributionTools.distributionFromString(text,3600);
			if (dist==null) return Language.tr("Model.XML.PostProcessingTimes.Error");
			postProcessingTimeDist=dist;
			return null;
		}

		if (Language.trAll("Model.XML.BatchService",name)) {
			final Integer J=NumberTools.getNotNegativeInteger(text);
			if (J==null || J==0) return String.format(Language.tr("Model.XML.BatchService.Error"),text);
			batchWorking=J;
			return null;
		}

		if (Language.trAll("Model.XML.QueueMode",name)) {
			queueMode=QueueMode.getFromName(text);
			return null;
		}

		if (Language.trAll("Model.XML.RetryDistances",name)) {
			final AbstractRealDistribution dist=DistributionTools.distributionFromString(text,3600);
			if (dist==null) return Language.tr("Model.XML.RetryDistances.Error");
			retryTimeDist=dist;
			return null;
		}

		if (Language.trAll("Model.XML.NumberOfAgents",name)) {
			final Integer J=NumberTools.getNotNegativeInteger(text);
			if (J==null || J==0) return String.format(Language.tr("Model.XML.NumberOfAgents.Error"),text);
			agents=J;
			return null;
		}

		if (Language.trAll("Model.XML.ForwardingProbability",name)) {
			final Double D=NumberTools.getSystemProbability(text);
			if (D==null) return String.format(Language.tr("Model.XML.ForwardingProbability.Error"),text);
			callContinueProbability=D;
			return null;
		}

		if (Language.trAll("Model.XML.RetryProbability",name)) {
			final Double D=NumberTools.getSystemProbability(text);
			if (D==null) return String.format(Language.tr("Model.XML.RetryProbability.Error"),text);
			retryProbability=D;
			return null;
		}

		if (Language.trAll("Model.XML.ClientCount",name)) {
			final Integer J=NumberTools.getNotNegativeInteger(text);
			if (J==null || J==0) return String.format(Language.tr("Model.XML.ClientCount.Error"),text);
			callsToSimulate=J;

			final String warmUpString=Language.trAllAttribute("Model.XML.ClientCountWarmUp",node);
			if (warmUpString!=null && !warmUpString.trim().isEmpty()) {
				final Integer I=NumberTools.getNotNegativeInteger(warmUpString);
				if (I==null) return String.format(Language.tr("Model.XML.ClientCountWarmUp.Error"),warmUpString);
				callsToSimulateWarmUp=I;
			}
			return null;
		}

		if (Language.trAll("Model.XML.WaitingRoomSize",name)) {
			final Integer J=NumberTools.getInteger(text);
			if (J==null) return String.format(Language.tr("Model.XML.WaitingRoomSize.Error"),text);
			waitingRoomSize=J;
			return null;
		}

		if (language.Language.trAll("Model.XML.CollectCorrelation",name)) {
			collectCorrelation=(text.equals("1"));
			return null;
		}

		return null;
	}

	@Override
	protected void addDataToXML(final Document doc, final Element node, final boolean isPartOfOtherFile, final File file) {
		Element sub;

		if (!isPartOfOtherFile) version=systemVersion; /* Versionskennung aktualisieren, sofern das Modell direkt gespeichert wird und nicht nur Teil einer Statistikdatei ist. */
		addTextToXML(doc,node,Language.tr("Model.XML.Version"),version);

		addTextToXML(doc,node,Language.tr("Model.XML.Name"),name);
		addTextToXML(doc,node,Language.tr("Model.XML.Description"),description);

		addTextToXML(doc,node,Language.tr("Model.XML.InterArrivalTimes"),interArrivalTimeDist);
		addTextToXML(doc,node,Language.tr("Model.XML.BatchArrival"),batchArrival);
		addTextToXML(doc,node,Language.tr("Model.XML.WaitingTimeTolerances"),waitingTimeDist);
		addTextToXML(doc,node,Language.tr("Model.XML.ServiceTimes"),workingTimeDist);
		addTextToXML(doc,node,Language.tr("Model.XML.PostProcessingTimes"),postProcessingTimeDist);
		addTextToXML(doc,node,Language.tr("Model.XML.BatchService"),batchWorking);
		addTextToXML(doc,node,Language.tr("Model.XML.QueueMode"),queueMode.xmlName);
		addTextToXML(doc,node,Language.tr("Model.XML.RetryDistances"),retryTimeDist);
		addTextToXML(doc,node,Language.tr("Model.XML.NumberOfAgents"),agents);
		addTextToXML(doc,node,Language.tr("Model.XML.ForwardingProbability"),callContinueProbability);
		addTextToXML(doc,node,Language.tr("Model.XML.RetryProbability"),retryProbability);
		sub=addTextToXML(doc,node,Language.tr("Model.XML.ClientCount"),callsToSimulate);
		sub.setAttribute(Language.tr("Model.XML.ClientCountWarmUp"),""+callsToSimulateWarmUp);
		addTextToXML(doc,node,Language.tr("Model.XML.WaitingRoomSize"),waitingRoomSize);
		if (collectCorrelation) addTextToXML(doc,node,Language.tr("Model.XML.CollectCorrelation"),"1");
	}
}