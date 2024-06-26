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
package tools;

import java.awt.Component;
import java.awt.Point;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.math3.distribution.AbstractRealDistribution;

import language.Language;
import mathtools.NumberTools;
import mathtools.Table;
import mathtools.distribution.NeverDistributionImpl;
import mathtools.distribution.OnePointDistributionImpl;
import mathtools.distribution.swing.CommonVariables;
import mathtools.distribution.tools.DistributionTools;
import simulator.editmodel.EditModel;
import systemtools.MsgBox;
import xml.XMLTools;

/**
 * Diese Klasse erm�glicht es, das Modell f�r den Warteschlangensimulator zu exportieren.
 * @author Alexander Herzog
 * @see EditModel
 * <a href="https://github.com/A-Herzog/Warteschlangensimulator">https://github.com/A-Herzog/Warteschlangensimulator</a>
 */
public class ExportQSModel {
	/** Zu exportierendes Modell */
	private final EditModel model;

	/**
	 * Konstruktor der Klasse
	 * @param model	 Zu exportierendes Modell
	 * @see #work(File)
	 */
	public ExportQSModel(final EditModel model) {
		this.model=model;
	}

	/**
	 * Gibt den Kopfbereich des xml-Modells aus
	 * @param result	Ausgabe-{@link StringBuilder}
	 * @param arrivalCount	Anzahl an zu simulierenden Kundenank�nften
	 * @see #work(File)
	 */
	private void addHeader(final StringBuilder result, final long arrivalCount) {
		final String root=Language.tr("QSExport.xml.Model");
		result.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		result.append("<!DOCTYPE "+root+" SYSTEM \"https://a-herzog.github.io/Warteschlangensimulator/Simulator.dtd\">\n");
		result.append("<"+root+" xmlns=\"https://a-herzog.github.io\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"https://a-herzog.github.io https://a-herzog.github.io/Warteschlangensimulator/Simulator.xsd\">\n");

		result.append("<"+Language.tr("QSExport.xml.ModelClients")+" "+Language.tr("QSExport.xml.Active")+"=\"1\">"+arrivalCount+"</"+Language.tr("QSExport.xml.ModelClients")+">\n");
		result.append("<"+Language.tr("QSExport.xml.ModelWarmUpPhase")+">0.01</"+Language.tr("QSExport.xml.ModelWarmUpPhase")+">\n");
		if (model.collectCorrelation) {
			result.append("<"+Language.tr("QSExport.xml.ModelAutocorrelation")+">1000</"+Language.tr("QSExport.xml.ModelAutocorrelation")+">\n");
		}
		result.append("<"+Language.tr("QSExport.xml.ModelElements")+">\n");
	}

	/**
	 * Gibt den Fu�bereich des xml-Modells aus
	 * @param result	Ausgabe-{@link StringBuilder}
	 * @param agents	Anzahl an verf�gbaren Bedienern
	 */
	private void addFooter(final StringBuilder result, final int agents) {
		result.append("</"+Language.tr("QSExport.xml.ModelElements")+">\n");
		result.append("<"+Language.tr("QSExport.xml.Resources")+" "+Language.tr("QSExport.xml.SecondaryResourcePriority")+"=\""+Language.tr("QSExport.xml.SecondaryResourcePriority.Random")+"\">\n");
		result.append("  <"+Language.tr("QSExport.xml.Resource")+" "+Language.tr("QSExport.xml.Name")+"=\""+Language.tr("QSExport.Name.Agents")+"\" "+Language.tr("QSExport.xml.Type")+"=\""+Language.tr("QSExport.xml.Count")+"\" "+Language.tr("QSExport.xml.Value")+"=\""+agents+"\"/>\n");
		result.append("</"+Language.tr("QSExport.xml.Resources")+">\n");

		result.append("</"+Language.tr("QSExport.xml.Model")+">\n");
	}

	/**
	 * F�gt eine Verbindkundskante in das xml-Modell ein
	 * @param result	Ausgabe-{@link StringBuilder}
	 * @param id	ID der Kante
	 * @param id1	ID des Elemente von dem die Kante ausgeht
	 * @param id2	ID des Elements zu dem die Kante f�hrt
	 * @see #buildStations(StringBuilder)
	 */
	private void addEdge(final StringBuilder result, final int id, final int id1, final int id2) {
		result.append("  <"+Language.tr("QSExport.xml.Element.Edge")+" "+Language.tr("QSExport.xml.id")+"=\""+id+"\">\n");
		result.append("    <"+Language.tr("QSExport.xml.Element.Connection")+" "+Language.tr("QSExport.xml.Element.Connection.Element1")+"=\""+id1+"\" "+Language.tr("QSExport.xml.Element.Connection.Element2")+"=\""+id2+"\" "+Language.tr("QSExport.xml.Type")+"=\""+Language.tr("QSExport.xml.Edge")+"\"/>\n");
		result.append("  </"+Language.tr("QSExport.xml.Element.Edge")+">\n");
	}


	/**
	 * Gibt die Stationsdaten f�r das xml-Modell aus
	 * @param result	Ausgabe-{@link StringBuilder}
	 * @see #work(File)
	 */
	private void buildStations(final StringBuilder result) {
		StationVertex vertex1, vertex2;

		/* Modelleigenschaften */
		final boolean hasWaitingRoomLimitation=(model.waitingRoomSize>=0);
		final boolean hasLimitedWaitingTimes=(model.waitingTimeDist!=null && !(model.waitingTimeDist instanceof NeverDistributionImpl));
		final boolean hasPostProcessing=(model.postProcessingTimeDist!=null) && ((!(model.postProcessingTimeDist instanceof OnePointDistributionImpl)) || ((OnePointDistributionImpl)model.postProcessingTimeDist).point>0);
		final boolean hasRetry=(hasWaitingRoomLimitation || hasLimitedWaitingTimes) && (model.retryProbability>0) && (model.retryTimeDist!=null) && (!(model.retryTimeDist instanceof NeverDistributionImpl));
		final boolean hasForwarding=(model.callContinueProbability>0);

		/* Vorbereitungen */
		final List<StationBase> stations=new ArrayList<>();
		int nextId=1;
		int x=50;

		/* Title */
		stations.add(new StationTitle(new Point(50,hasForwarding?25:50),nextId++,(model.name==null || model.name.isBlank())?Language.tr("QSExport.Name.Title"):model.name));

		/* Quelle */
		final StationSource source=new StationSource(new Point(x,100),nextId++,model.interArrivalTimeDist,model.batchArrival);
		stations.add(source);
		x+=200;

		/* Warteschlange voll? */
		StationDecide queueLimit=null;
		if (hasWaitingRoomLimitation) {
			queueLimit=new StationDecide(new Point(x,100),nextId++,Language.tr("QSExport.xml.Info.QueueFull"),StationDecide.Mode.CONDITION);
			stations.add(queueLimit);
			x+=200;
		}

		/* Bedienstation */
		final StationProcess process=new StationProcess(new Point(x,100),nextId++,model.queueMode==EditModel.QueueMode.LIFO,hasLimitedWaitingTimes?model.waitingTimeDist:null,model.workingTimeDist,hasPostProcessing?model.postProcessingTimeDist:null,model.batchWorking);
		stations.add(process);

		/* Wiederholung ? */
		StationDecide retryTest=null;
		StationDelay retryDelay=null;
		if (hasRetry) {
			retryTest=new StationDecide(new Point(x,300),nextId++,Language.tr("QSExport.xml.Info.Retry"),StationDecide.Mode.CHANCE);
			stations.add(retryTest);
			final int xPos=(queueLimit!=null)?queueLimit.pos.x:source.pos.x;
			retryDelay=new StationDelay(new Point(xPos,retryTest.pos.y),nextId++,null,model.retryTimeDist);
			stations.add(retryDelay);
		}

		x+=200;

		/* Weiterleitung ? */
		StationDecide forward=null;
		if (hasForwarding) {
			forward=new StationDecide(new Point(x,100),nextId++,Language.tr("QSExport.xml.Info.Forward"),StationDecide.Mode.CHANCE);
			stations.add(forward);
			x+=200;
		}

		/* Z�hler */
		StationCounter counterSuccess=null;
		StationCounter counterCancel=null;
		if (hasWaitingRoomLimitation || hasLimitedWaitingTimes) {
			stations.add(counterSuccess=new StationCounter(new Point(x,100),nextId++,Language.tr("QSExport.xml.Info.Counter.Success"),Language.tr("QSExport.xml.Info.Counter.Type")));
			stations.add(counterCancel=new StationCounter(new Point(x,200),nextId++,Language.tr("QSExport.xml.Info.Counter.Cancel"),Language.tr("QSExport.xml.Info.Counter.Type")));
			x+=200;
		}

		/* Ausgang */
		final StationExit exit=new StationExit(new Point(x,(counterCancel==null)?100:150),nextId++);
		stations.add(exit);

		/* Stationen verkn�pfen */

		/* Stationen verkn�pfen: Quelle->QueueLimit->Process/RetryTest oder Quelle->QueueLimit->Process/Exit oder Quelle->Process */
		if (queueLimit!=null) {
			source.addEdgeTo(queueLimit);
			queueLimit.addEdgeTo(process,""+Language.tr("QSExport.xml.Condition")+"=\"NQ("+process.id+")&lt;"+model.waitingRoomSize+"\"");
			if (hasRetry) {
				queueLimit.addEdgeTo(retryTest);
			} else {
				if (counterCancel!=null) {
					int xPos=queueLimit.pos.x+45;
					if (hasLimitedWaitingTimes) xPos=process.pos.x+45;
					stations.add(vertex1=new StationVertex(new Point(xPos,counterCancel.pos.y+20),nextId++));
					queueLimit.addEdgeTo(vertex1);
					vertex1.addEdgeTo(counterCancel);
				}
			}
		} else {
			source.addEdgeTo(process);
		}

		/* Stationen verkn�pfen: (Process->Forward oder Process->Exit) und evtl. (Process->RetryTest oder Process->Exit) */
		if (forward!=null) {
			process.addEdgeTo(forward);
		} else {
			if (counterSuccess!=null) {
				process.addEdgeTo(counterSuccess);
			} else {
				process.addEdgeTo(exit);
			}
		}
		if (hasLimitedWaitingTimes) {
			if (hasRetry) {
				process.addEdgeTo(retryTest,Language.tr("QSExport.xml.Element.Connection.Status")+"=\""+Language.tr("QSExport.xml.Element.Connection.Status.WaitingCancelation")+"\"");
			} else {
				if (queueLimit!=null) {
					process.addEdgeTo(queueLimit.edgesTo.get(1).destinationStationID,Language.tr("QSExport.xml.Element.Connection.Status")+"=\""+Language.tr("QSExport.xml.Element.Connection.Status.WaitingCancelation")+"\"");
				} else {
					if (counterCancel!=null) {
						stations.add(vertex1=new StationVertex(new Point(process.pos.x+45,counterCancel.pos.y+20),nextId++));
						process.addEdgeTo(vertex1,Language.tr("QSExport.xml.Element.Connection.Status")+"=\""+Language.tr("QSExport.xml.Element.Connection.Status.WaitingCancelation")+"\"");
						vertex1.addEdgeTo(counterCancel);
					}
				}
			}
		}

		/* Stationen verkn�pfen: RetryTest->(RetryDelay->...)/Exit */
		if (retryTest!=null && retryDelay!=null) {

			retryTest.addEdgeTo(retryDelay,Language.tr("QSExport.xml.Element.Connection.Rate")+"=\""+NumberTools.formatSystemNumber(model.retryProbability)+"\"");
			retryTest.addEdgeTo(counterCancel,Language.tr("QSExport.xml.Element.Connection.Rate")+"=\""+NumberTools.formatSystemNumber(1-model.retryProbability)+"\"");
			if (queueLimit!=null) {
				retryDelay.addEdgeTo(queueLimit);
			} else {
				retryDelay.addEdgeTo(source);
			}
		}

		/* Stationen verkn�pfen: Forward->... */
		if (forward!=null) {
			if (counterSuccess!=null) {
				forward.addEdgeTo(counterSuccess,Language.tr("QSExport.xml.Element.Connection.Rate")+"=\""+NumberTools.formatSystemNumber(model.callContinueProbability)+"\"");
			} else {
				forward.addEdgeTo(exit,Language.tr("QSExport.xml.Element.Connection.Rate")+"=\""+NumberTools.formatSystemNumber(1-model.callContinueProbability)+"\"");
			}
			stations.add(vertex1=new StationVertex(new Point(forward.pos.x+45,forward.pos.y-40),nextId++));
			forward.addEdgeTo(vertex1,Language.tr("QSExport.xml.Element.Connection.Rate")+"=\""+NumberTools.formatSystemNumber(model.callContinueProbability)+"\"");
			if (queueLimit!=null) {
				stations.add(vertex2=new StationVertex(new Point(queueLimit.pos.x+45,forward.pos.y-40),nextId++));
				vertex1.addEdgeTo(vertex2);
				vertex2.addEdgeTo(queueLimit);
			} else {
				stations.add(vertex2=new StationVertex(new Point(source.pos.x+45,forward.pos.y-40),nextId++));
				vertex1.addEdgeTo(vertex2);
				vertex2.addEdgeTo(source);
			}
		}

		/* Stationen verkn�pfen: Z�hler->Ende */
		if (counterSuccess!=null) counterSuccess.addEdgeTo(exit);
		if (counterCancel!=null) counterCancel.addEdgeTo(exit);

		/* Ergebnisse ausgeben */
		for (StationBase station: stations) nextId=station.write(result,nextId,stations);
		for (StationBase station: stations) for (EdgeData edge: station.edgesTo) if (edge.edgeId>=0) addEdge(result,edge.edgeId,station.id,edge.destinationStationID);
	}

	/**
	 * Exportiert das Modell.
	 * @param file	Zieldatei
	 * @return	Gibt an, ob der Export erfolgreich verlaufen ist.
	 */
	public boolean work(final File file) {
		if (file==null) return false;

		final StringBuilder result=new StringBuilder();

		addHeader(result,model.callsToSimulate);
		buildStations(result);
		addFooter(result,model.agents);

		return Table.saveTextToFile(result.toString(),file);
	}

	/**
	 * Zeigt einen Dateiauswahldialog f�r den Export an.
	 * @param owner	�bergeordnetes Element
	 * @return	Gew�hlte Datei zum Speichern oder <code>null</code>, wenn die Auswahl abgebrochen wrude.
	 */
	public static File selectFile(final Component owner) {
		final JFileChooser fc=new JFileChooser();
		CommonVariables.initialDirectoryToJFileChooser(fc);
		fc.setDialogTitle(Language.tr("QSExport.SelectFile"));
		final FileFilter xml=new FileNameExtensionFilter(XMLTools.fileTypeXML+" (*.xml)","xml");
		fc.addChoosableFileFilter(xml);
		fc.setFileFilter(xml);
		fc.setAcceptAllFileFilterUsed(false);

		if (fc.showSaveDialog(owner)!=JFileChooser.APPROVE_OPTION) return null;
		CommonVariables.initialDirectoryFromJFileChooser(fc);
		File file=fc.getSelectedFile();

		if (file.getName().indexOf('.')<0) {
			if (fc.getFileFilter()==xml) file=new File(file.getAbsoluteFile()+".xml");
		}

		if (file.exists()) {
			if (!MsgBox.confirmOverwrite(owner,file)) return null;
		}

		return file;
	}

	/**
	 * Datensatz f�r eine Verbindungskante
	 */
	private static class EdgeData {
		/** ID der Zielstation */
		public final int destinationStationID;
		/** Zus�tzliche Daten die bei der xml-Ausgabe in den Verbindungskante-Knoten geschrieben werden */
		public final String info;
		/** ID der Verbindungskante */
		public int edgeId;

		/**
		 * Konstruktor der Klasse
		 * @param destinationStationID	ID der Zielstation
		 */
		public EdgeData(final int destinationStationID) {
			this.destinationStationID=destinationStationID;
			info=null;
			edgeId=-1;
		}

		/**
		 * Konstruktor der Klasse
		 * @param destinationStationID	ID der Zielstation
		 * @param info	Zus�tzliche Daten die bei der xml-Ausgabe in den Verbindungskante-Knoten geschrieben werden
		 */
		public EdgeData(final int destinationStationID, final String info) {
			this.destinationStationID=destinationStationID;
			this.info=info;
			edgeId=-1;
		}
	}

	/**
	 * Datensatz f�r eine Station
	 */
	private static class StationBase {
		/** Position auf der Zeichenfl�che */
		protected final Point pos;
		/** Gr��e auf der Zeichenfl�che */
		protected final Point size;
		/** Typ der Station */
		protected final String type;
		/** ID der Station */
		public final int id;
		/** Auslaufende Verbindkungskanten */
		public final List<EdgeData> edgesTo;

		/**
		 * Konstruktor der Klasse
		 * @param pos	Position auf der Zeichenfl�che
		 * @param size	Gr��e auf der Zeichenfl�che
		 * @param type	Typ der Station
		 * @param id	ID der Station
		 */
		public StationBase(final Point pos, final Point size, final String type, final int id) {
			this.pos=new Point(pos);
			this.size=new Point(size);
			this.type=type;
			this.id=id;
			edgesTo=new ArrayList<>();
		}

		/**
		 * Konstruktor der Klasse
		 * @param pos	Position auf der Zeichenfl�che
		 * @param type	Typ der Station
		 * @param id	ID der Station
		 */
		public StationBase(final Point pos, final String type, final int id) {
			this(pos,new Point(100,50),type,id);
		}

		/**
		 * Schreibt die Daten zu der Station in ein xml-Modell
		 * @param result	H�lt das xml-Modell als Zeichenkette vor
		 * @param nextId	Aktuell n�chste freie ID
		 * @param allStations	Liste mit allen Stationen des Modells (zur Verbindung mit einlaufenden Kanten)
		 * @return	N�chste freie ID nach Verarbeitung durch diese Methode
		 */
		public final int write(final StringBuilder result, int nextId, final List<StationBase> allStations) {
			result.append("  <"+type+" "+Language.tr("QSExport.xml.id")+"=\""+id+"\">\n");
			result.append("    <"+Language.tr("QSExport.xml.Size")+" h=\""+size.y+"\" w=\""+size.x+"\" x=\""+pos.x+"\" y=\""+pos.y+"\"/>\n");

			/* Auslaufende Kanten */
			for (EdgeData edge: edgesTo) {
				if (edge.edgeId<0) {edge.edgeId=nextId; nextId++;}
				String info=edge.info;
				if (info==null || info.isBlank()) {
					info="";
				} else {
					info=" "+info;
				}
				result.append("    <"+Language.tr("QSExport.xml.Element.Connection")+" "+Language.tr("QSExport.xml.Element.Connection.Element")+"=\""+edge.edgeId+"\" "+Language.tr("QSExport.xml.Type")+"=\""+Language.tr("QSExport.xml.Element.Connection.Out")+"\""+info+"/>\n");
			}

			/* Einlaufende Kanten */
			for (StationBase station: allStations) if (station!=this) for (EdgeData edge: station.edgesTo) if (edge.destinationStationID==id) {
				if (edge.edgeId<0) {edge.edgeId=nextId; nextId++;}
				result.append("    <"+Language.tr("QSExport.xml.Element.Connection")+" "+Language.tr("QSExport.xml.Element.Connection.Element")+"=\""+edge.edgeId+"\" "+Language.tr("QSExport.xml.Type")+"=\""+Language.tr("QSExport.xml.Element.Connection.In")+"\"/>\n");
			}

			writeData(result);
			result.append("  </"+type+">\n");

			return nextId;
		}

		/**
		 * Kann in abgeleiteten Klassen �berschrieben werden, um weitere Inhalte als
		 * Unterelemente in den xml-Knoten f�r die Station zu schreiben.
		 * @param result	H�lt das xml-Modell als Zeichenkette vor
		 */
		protected void writeData(final StringBuilder result) {
		}

		/**
		 * F�gt eine neue Verbindungskante zu der Liste der aus der Station auslaufenden Kanten hinzu
		 * @param station	Zielstation
		 * @param info	Zus�tzliche Daten die bei der xml-Ausgabe in den Verbindungskante-Knoten geschrieben werden
		 */
		public void addEdgeTo(final StationBase station, final String info) {
			edgesTo.add(new EdgeData(station.id,info));
		}

		/**
		 * F�gt eine neue Verbindungskante zu der Liste der aus der Station auslaufenden Kanten hinzu
		 * @param station	Zielstation
		 */
		public void addEdgeTo(final StationBase station) {
			edgesTo.add(new EdgeData(station.id));
		}

		/**
		 * F�gt eine neue Verbindungskante zu der Liste der aus der Station auslaufenden Kanten hinzu
		 * @param stationId	ID der Zielstation
		 * @param info	Zus�tzliche Daten die bei der xml-Ausgabe in den Verbindungskante-Knoten geschrieben werden
		 */
		public void addEdgeTo(final int stationId, final String info) {
			edgesTo.add(new EdgeData(stationId,info));
		}
	}

	/**
	 * Datensatz f�r eine Textausgabe
	 * @see StationBase
	 */
	private static class StationTitle extends StationBase {
		/** Anzuzeigender Text */
		private final String text;

		/**
		 * Konstruktor der Klasse
		 * @param pos	Position auf der Zeichenfl�che
		 * @param id	ID der Station
		 * @param text	Anzuzeigender Text
		 */
		public StationTitle(final Point pos, final int id, final String text) {
			super(pos,new Point(119,19),Language.tr("QSExport.xml.Element.Text"),id);
			this.text=text;
		}

		@Override
		protected void writeData(final StringBuilder result) {
			super.writeData(result);
			result.append("    <"+Language.tr("QSExport.xml.Element.Text.Line")+">"+text+"</"+Language.tr("QSExport.xml.Element.Text.Line")+">\n");
			result.append("    <"+Language.tr("QSExport.xml.Element.Text.FontSize")+" "+Language.tr("QSExport.xml.Element.Text.FontBold")+"=\"1\">14</"+Language.tr("QSExport.xml.Element.Text.FontSize")+">\n");
			result.append("    <"+Language.tr("QSExport.xml.Color")+">0,0,0</"+Language.tr("QSExport.xml.Color")+">\n");
		}
	}

	/**
	 * Datensatz f�r eine Station mit explizit eingestelltem Namen
	 * @see StationBase
	 */
	private static class StationWithName extends StationBase {
		/** Name der Station */
		protected final String name;

		/**
		 * Konstruktor der Klasse
		 * @param pos	Position auf der Zeichenfl�che
		 * @param type	Typ der Station
		 * @param id	ID der Station
		 * @param name	Name der Station
		 */
		public StationWithName(final Point pos, final String type, final int id, final String name) {
			super(pos,type,id);
			this.name=name;
		}

		@Override
		protected void writeData(final StringBuilder result) {
			super.writeData(result);
			if (name!=null) {
				result.append("    <"+Language.tr("QSExport.xml.ModelElementName")+">"+name+"</"+Language.tr("QSExport.xml.ModelElementName")+">\n");
			}
		}
	}

	/**
	 * Datensatz f�r eine Kundenquelle
	 * @see StationWithName
	 */
	private static class StationSource extends StationWithName {
		/** Verteilung der Zwischenankunftszeiten */
		private final AbstractRealDistribution dist;
		/** Ankunfts-Batch-Gr��e */
		private final int batch;

		/**
		 * Konstruktor der Klasse
		 * @param pos	Position auf der Zeichenfl�che
		 * @param id	ID der Station
		 * @param dist	Verteilung der Zwischenankunftszeiten
		 * @param batch	Ankunfts-Batch-Gr��e
		 */
		public StationSource(final Point pos, final int id, final AbstractRealDistribution dist, final int batch) {
			super(pos,Language.tr("QSExport.xml.Element.Source"),id,Language.tr("QSExport.Name.Caller"));
			this.dist=dist;
			this.batch=batch;
		}

		@Override
		protected void writeData(final StringBuilder result) {
			super.writeData(result);
			result.append("    <"+Language.tr("QSExport.xml.ModelElementDistribution")+" "+Language.tr("QSExport.xml.TimeBase")+"=\""+Language.tr("QSExport.xml.TimeBase.Seconds")+"\">"+DistributionTools.distributionToString(dist)+"</"+Language.tr("QSExport.xml.ModelElementDistribution")+">\n");
			result.append("    <"+Language.tr("QSExport.xml.ModelElementBatchData")+" "+Language.tr("QSExport.xml.ModelElementBatchData.Size")+"=\""+batch+"\"/>\n");
		}
	}

	/**
	 * Datensatz f�r eine Ausgang-Station
	 * @see StationWithName
	 */
	private static class StationExit extends StationWithName {
		/**
		 * Konstruktor der Klasse
		 * @param pos	Position auf der Zeichenfl�che
		 * @param id	ID der Station
		 */
		public StationExit(final Point pos, final int id) {
			super(pos,Language.tr("QSExport.xml.Element.Dispose"),id,Language.tr("QSExport.Name.Exit"));
		}
	}

	/**
	 * Datensatz f�r eine Bedienstation
	 * @see StationWithName
	 */
	private static class StationProcess extends StationWithName {
		/** Wartezeittoleranzverteilung (kann <code>null</code> sein) */
		private final AbstractRealDistribution cancelDist;
		/** Bedienzeitenverteilung */
		private final AbstractRealDistribution dist;
		/** Nachbearbeitungszeizenverteilung (kann <code>null</code> sein) */
		private final AbstractRealDistribution postProcessDist;
		/** Bedien-Batch-Gr��e */
		private final int batch;
		/** LIFO (<code>true</code>) statt normalem FIFO (<code>false</code>) Modus? */
		private final boolean LIFO;

		/**
		 * Konstruktor der Klasse
		 * @param pos	Position auf der Zeichenfl�che
		 * @param id	ID der Station
		 * @param LIFO	LIFO (<code>true</code>) statt normalem FIFO (<code>false</code>) Modus?
		 * @param cancelDist	Wartezeittoleranzverteilung (kann <code>null</code> sein)
		 * @param dist	Bedienzeitenverteilung
		 * @param postProcessDist	Nachbearbeitungszeizenverteilung (kann <code>null</code> sein)
		 * @param batch	Bedien-Batch-Gr��e
		 */
		public StationProcess(final Point pos, final int id, final boolean LIFO, final AbstractRealDistribution cancelDist, final AbstractRealDistribution dist, final AbstractRealDistribution postProcessDist, final int batch) {
			super(pos,Language.tr("QSExport.xml.Element.Process"),id,Language.tr("QSExport.Name.CallCenter"));
			this.LIFO=LIFO;
			this.cancelDist=cancelDist;
			this.dist=dist;
			this.postProcessDist=postProcessDist;
			this.batch=batch;
		}

		@Override
		protected void writeData(final StringBuilder result) {
			super.writeData(result);
			result.append("    <"+Language.tr("QSExport.xml.ModelElementBatchData")+" "+Language.tr("QSExport.xml.ModelElementBatchData.Maximum")+"=\""+batch+"\" "+Language.tr("QSExport.xml.ModelElementBatchData.Minimum")+"=\""+batch+"\"/>\n");
			result.append("    <"+Language.tr("QSExport.xml.ModelElementDistribution")+" "+Language.tr("QSExport.xml.ModelElementDistribution.Status")+"=\""+Language.tr("QSExport.xml.ModelElementDistribution.Status.ProcessTime")+"\" "+Language.tr("QSExport.xml.Type")+"=\""+Language.tr("QSExport.xml.Type.ProcessingTime")+"\" "+Language.tr("QSExport.xml.TimeBase")+"=\""+Language.tr("QSExport.xml.TimeBase.Seconds")+"\">"+DistributionTools.distributionToString(dist)+"</"+Language.tr("QSExport.xml.ModelElementDistribution")+">\n");
			if (postProcessDist!=null) {
				result.append("    <"+Language.tr("QSExport.xml.ModelElementDistribution")+" "+Language.tr("QSExport.xml.Type")+"=\""+Language.tr("QSExport.xml.Type.PostProcessingTime")+"\">"+DistributionTools.distributionToString(postProcessDist)+"</"+Language.tr("QSExport.xml.ModelElementDistribution")+">\n");
			}
			if (cancelDist!=null) {
				result.append("    <"+Language.tr("QSExport.xml.ModelElementDistribution")+" "+Language.tr("QSExport.xml.Type")+"=\""+Language.tr("QSExport.xml.Type.CancelationTime")+"\">"+DistributionTools.distributionToString(cancelDist)+"</"+Language.tr("QSExport.xml.ModelElementDistribution")+">\n");
			}
			final String priority;
			if (LIFO) priority="-w"; else priority="w";
			result.append("    <"+Language.tr("QSExport.xml.ModellElementPrioritaet")+" "+Language.tr("QSExport.xml.ModellElementPrioritaet.ClientType")+"=\""+Language.tr("QSExport.Name.Caller")+"\">"+priority+"</"+Language.tr("QSExport.xml.ModellElementPrioritaet")+">\n");
			result.append("    <"+Language.tr("QSExport.xml.ModelElementOperators")+" "+Language.tr("QSExport.xml.ModelElementOperators.Alternative")+"=\"1\" "+Language.tr("QSExport.xml.ModelElementOperators.Count")+"=\"1\" "+Language.tr("QSExport.xml.ModelElementOperators.Group")+"=\""+Language.tr("QSExport.Name.Agents")+"\"/>\n");
			result.append("    <"+Language.tr("QSExport.xml.ModelElementOperatorsPriority")+">1</"+Language.tr("QSExport.xml.ModelElementOperatorsPriority")+">\n");
		}
	}

	/**
	 * Datensatz f�r eine Verzweigen-Station
	 * @see StationWithName
	 */
	private static class StationDecide extends StationWithName {
		/** Verzweigemodus */
		public enum Mode {
			/** Verzweigen gem�� Bedingung */
			CONDITION,
			/** Zuf�llig verzweigen */
			CHANCE
		}

		/**
		 * Verzweigemodus
		 * @see StationDecide.Mode
		 */
		private Mode mode;

		/**
		 * Konstruktor der Klasse
		 * @param pos	Position auf der Zeichenfl�che
		 * @param id	ID der Station
		 * @param name	Name der Station
		 * @param mode	Verzweigemodus
		 */
		public StationDecide(final Point pos, final int id, final String name, final Mode mode) {
			super(pos,Language.tr("QSExport.xml.Element.Decide"),id,name);
			this.mode=mode;
		}

		@Override
		protected void writeData(final StringBuilder result) {
			super.writeData(result);

			final String modeString;
			switch (mode) {
			case CHANCE: modeString=Language.tr("QSExport.xml.Element.Decide.Mode.Random"); break;
			case CONDITION: modeString=Language.tr("QSExport.xml.Element.Decide.Mode.Condition"); break;
			default: modeString=Language.tr("QSExport.xml.Element.Decide.Mode.Random"); break;
			}
			result.append("    <"+Language.tr("QSExport.xml.Element.Decide.Mode")+">"+modeString+"</"+Language.tr("QSExport.xml.Element.Decide.Mode")+">\n");
		}
	}

	/**
	 * Datensatz f�r eine Ecke
	 * @see StationBase
	 */
	private static class StationVertex extends StationBase {
		/**
		 * Konstruktor der Klasse
		 * @param pos	Position auf der Zeichenfl�che
		 * @param id	ID der Station
		 */
		public StationVertex(final Point pos, final int id) {
			super(pos,new Point(10,10),Language.tr("QSExport.xml.Element.Vertex"),id);
		}
	}

	/**
	 * Datensatz f�r eine Verz�gerung-Station
	 * @see StationWithName
	 */
	private static class StationDelay extends StationWithName {
		/** Verz�gerungszeitenverteilung */
		private final AbstractRealDistribution dist;

		/**
		 * Konstruktor der Klasse
		 * @param pos	Position auf der Zeichenfl�che
		 * @param id	ID der Station
		 * @param name	Name der Station
		 * @param dist	Verz�gerungszeitenverteilung
		 */
		public StationDelay(final Point pos, final int id, final String name, final AbstractRealDistribution dist) {
			super(pos,Language.tr("QSExport.xml.Element.Delay"),id,name);
			this.dist=dist;
		}

		@Override
		protected void writeData(final StringBuilder result) {
			super.writeData(result);
			result.append("    <"+Language.tr("QSExport.xml.ModelElementDistribution")+" "+Language.tr("QSExport.xml.ModelElementDistribution.Status")+"=\""+Language.tr("QSExport.xml.ModelElementDistribution.Status.ProcessTime")+"\" "+Language.tr("QSExport.xml.Type")+"=\""+Language.tr("QSExport.xml.Type.ProcessingTime")+"\" "+Language.tr("QSExport.xml.TimeBase")+"=\""+Language.tr("QSExport.xml.TimeBase.Seconds")+"\">"+DistributionTools.distributionToString(dist)+"</"+Language.tr("QSExport.xml.ModelElementDistribution")+">\n");
		}
	}

	/**
	 * Datensatz f�r eine Z�hler-Station
	 * @see StationWithName
	 */
	private static class StationCounter extends StationWithName {
		/** Name der Z�hlergruppe */
		private final String group;

		/**
		 * Konstruktor der Klasse
		 * @param pos	Position auf der Zeichenfl�che
		 * @param id	ID der Station
		 * @param name	Name der Station
		 * @param group	Name der Z�hlergruppe
		 */
		public StationCounter(final Point pos, final int id, final String name, final String group) {
			super(pos,Language.tr("QSExport.xml.Element.Counter"),id,name);
			this.group=group;
		}

		@Override
		protected void writeData(final StringBuilder result) {
			super.writeData(result);
			result.append("    <"+Language.tr("QSExport.xml.Element.Counter.Group")+">"+group+"</"+Language.tr("QSExport.xml.Element.Counter.Group")+">\n");
		}
	}
}