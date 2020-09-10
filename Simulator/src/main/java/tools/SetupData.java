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

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import language.Language;
import language.LanguageStaticLoader;
import language.Messages_Java11;
import mathtools.NumberTools;
import systemtools.GUITools;
import systemtools.SetupBase;

/**
 * Diese Klasse kapselt alle Setup-Daten des Programms und automatisiert das Laden und Speichern der Daten
 * @see SetupBase
 * @author Alexander Herzog
 */
public class SetupData extends SetupBase {
	/**
	 * @see SetupData#startSizeMode
	 */
	public enum StartMode {
		/** Starten des Programms mit Vorgabe-Fenstergröße */
		START_MODE_DEFAULT,

		/** Starten des Programms im Vollbildmodus */
		START_MODE_FULLSCREEN,

		/** Wiederherstellung der letzten Fenstergröße beim Programmstart */
		START_MODE_LASTSIZE
	}

	/**
	 * Programmsprache
	 */
	public String language;

	/**
	 * Fenstergröße beim Programmstart
	 * @see StartMode#START_MODE_DEFAULT
	 * @see StartMode#START_MODE_FULLSCREEN
	 * @see StartMode#START_MODE_LASTSIZE
	 */
	public StartMode startSizeMode;

	/**
	 * Ist startSizeMode=START_MODE_LASTSIZE gewählt, so wird hier gespeichert, ob das Fenster im Vollbildmodus dargestellt wird oder nicht
	 */
	public int lastSizeMode;

	/**
	 * Ist startSizeMode=START_MODE_LASTSIZE gewählt, so wird hier die letzte Position des Fensters gespeichert
	 */
	public Point lastPosition;

	/**
	 * Ist startSizeMode=START_MODE_LASTSIZE gewählt, so wird hier die letzte Größe des Fensters gespeichert
	 */
	public Dimension lastSize;

	/**
	 * Zu verwendendes Theme
	 * @see GUITools#listLookAndFeels()
	 */
	public String lookAndFeel;

	/**
	 * Gibt die Größe von Bildern beim Speichern an
	 */
	public int imageSize;

	/**
	 * Gibt an, ob die Bilder bei HTML-Reports inline oder als separate Dateien ausgegeben werden sollen.
	 */
	public boolean imagesInline;

	/**
	 * Gibt an, welche Einträge im Reportgenerator zuletzt aktiviert waren
	 */
	public String reportSettings;

	/**
	 * Liste der zuletzt verwendeten Dateien
	 */
	public String[] lastFiles;

	/**
	 * Option auf Statistik-Text-Viewer-Seiten: "Öffnen mit Word"
	 */
	public boolean openWord;

	/**
	 * Option auf Statistik-Text-Viewer-Seiten: "Öffnen mit OpenOffice/LibreOffice"
	 */
	public boolean openODT;

	/**
	 * Option auf Statistik-Tabellen-Viewer-Seiten: "Öffnen mit Excel"
	 */
	public boolean openExcel;

	/**
	 * Option auf Statistik-Tabellen-Viewer-Seiten: "Öffnen mit OpenOffice/LibreOffice"
	 */
	public boolean openODS;

	/**
	 * Alle Rechenkerne für die Simulation verwenden
	 */
	public boolean useMultiCore;

	/**
	 * Letzter Fehler
	 * (Hier wird die Setup-Datei als Logdatei für solche Ereignisse verwendet.)
	 */
	public String lastError;

	private static volatile SetupData setup=null;
	private static final Lock mutex=new ReentrantLock(true);

	private SetupData(final boolean loadSetupFile) {
		super();
		if (loadSetupFile) {
			loadSetupFromFile();
			autoSetLanguage();
		}
	}

	@Override
	protected void resetDataToDefaults() {
		language="";
		startSizeMode=StartMode.START_MODE_DEFAULT;
		lastSizeMode=Frame.NORMAL;
		lastPosition=new Point(0,0);
		lastSize=new Dimension(0,0);
		lookAndFeel="";
		imageSize=1000;
		imagesInline=true;
		reportSettings="";
		lastFiles=null;
		openWord=true;
		openODT=false;
		openExcel=true;
		openODS=false;
		useMultiCore=true;
		lastError=null;
	}

	private boolean autoSetLanguageActive=false;

	/**
	 * Gibt an, ob die Programmsprache beim Programmstart gemäß der Systemsprache automatisch
	 * eingestellt wurde (oder ob die Programmsprache aus dem Setup geladen wurde)
	 * @return	Gibt <code>true</code> zurück, wenn die Programmsprache automatisch eingestellt wurde
	 */
	public boolean languageWasAutomaticallySet() {
		return autoSetLanguageActive;
	}

	/**
	 * Setzt den Status "Sprache wurde automatisch gesetzt" zurück.
	 */
	public void resetLanguageWasAutomatically() {
		autoSetLanguageActive=false;
	}

	private void autoSetLanguage() {
		if (!language.isEmpty()) return;
		final String userLanguage=System.getProperty("user.language");
		if (Language.isSupportedLanguage(userLanguage)) language=userLanguage.toLowerCase(); else language="en";
		autoSetLanguageActive=true;
		saveSetup();
	}

	/**
	 * Liefert das Setup-Singleton-Objekt zurück
	 * Der Aufruf wird über ein Mutex-Objekt abgesichert, ist also thread-safe
	 * @return	Setup-Objekt
	 */
	public static SetupData getSetup() {
		return getSetup(true);
	}

	/**
	 * Liefert das Setup-Singleton-Objekt zurück
	 * @param lock	Gibt an, ob das evtl. notwendige Erstellen des Setup-Objektes über ein Mutex-Objekt vor Parallelaufrufen geschützt werden soll
	 * @return	Setup-Objekt
	 */
	public static SetupData getSetup(final boolean lock) {
		if (!lock) {
			if (setup==null) setup=new SetupData(true);
			return setup;
		}

		mutex.lock();
		try {
			if (setup==null) setup=new SetupData(true);
			return setup;
		} finally {
			mutex.unlock();
		}
	}

	/**
	 * Setzt das Setup auf die Defaultwerte zurück
	 */
	public static void resetSetup() {
		setup=new SetupData(false);
		setup.saveSetup();
	}

	/**
	 * Liefert den Pfadnamen des Verzeichnisses in dem sich die jar-Programmdatei befindet.
	 * @return	Pfad der Programmdatei
	 */
	public static File getProgramFolder() {
		try {
			final File source=new File(SetupData.class.getProtectionDomain().getCodeSource().getLocation().toURI());
			if (source.toString().toLowerCase().endsWith(".jar")) return new File(source.getParent());
		} catch (URISyntaxException e1) {}
		return new File(System.getProperty("user.dir"));
	}

	/**
	 * Name für den Ordner unterhalb von %APPDATA%, der für Programmeinstellungen verwendet
	 * werden soll, wenn das Programm von innerhalb des "Programme"-Verzeichnisses ausgeführt wird.
	 */
	private final static String USER_CONFIGURATION_FOLDER_NAME="Mini Callcenter Simulator";

	/**
	 * Liefert den Pfadnamen des Verzeichnisses in dem die Einstellungsdatei abgelegt werden soll.
	 * @return	Pfad der Einstellungendatei
	 */
	public static File getSetupFolder() {
		final File programFolder=getProgramFolder();

		/* Abweichender Ordner nur unter Windows */
		final String osName=System.getProperty("os.name");
		if (osName==null) return programFolder;
		if (!osName.toLowerCase().contains("windows")) return programFolder;

		/* Programmverzeichnis ist Unterordner des home-Verzeichnisses */
		final String homeFolder=System.getProperty("user.home");
		if (homeFolder==null) return programFolder;
		final String s1=homeFolder.toString().toLowerCase();
		final String s2=programFolder.toString().toLowerCase();
		if (s1.equals(s2.substring(0,Math.min(s1.length(),s2.length())))) return programFolder;

		/* Alternativen Speicherort */
		final String appData=System.getenv("APPDATA");
		if (appData==null) return programFolder;
		final File appDataFolder=new File(appData);
		if (!appDataFolder.isDirectory()) return programFolder;
		final File folder=new File(appDataFolder,USER_CONFIGURATION_FOLDER_NAME);
		if (!folder.isDirectory()) {
			if (!folder.mkdir()) return programFolder;
		}
		if (!folder.isDirectory()) return programFolder;
		return folder;
	}

	/**
	 * Dateiname der Setup-Datei
	 */
	public static final String SETUP_FILE_NAME="MiniCallcenterSimulator.cfg";

	@Override
	protected File getSetupFile() {
		return new File(getSetupFolder(),SETUP_FILE_NAME);
	}

	@Override
	protected void loadSetupFromXML(final Element root) {
		List<String> files=new ArrayList<String>();

		NodeList l=root.getChildNodes();
		for (int i=0; i<l.getLength();i++) {
			if (!(l.item(i) instanceof Element)) continue;
			Element e=(Element)l.item(i);
			String s=e.getNodeName();

			if (s.equalsIgnoreCase("language")) {
				String t=e.getTextContent().toLowerCase();
				if (Language.isSupportedLanguage(t)) language=t.toLowerCase();
				continue;
			}

			if (s.equalsIgnoreCase("Fullscreen")) {
				Integer j=NumberTools.getInteger(e.getTextContent());
				if (j!=null) {
					if (j==1) startSizeMode=StartMode.START_MODE_FULLSCREEN;
					if (j==2) startSizeMode=StartMode.START_MODE_LASTSIZE;
				}
				continue;
			}

			if (s.equalsIgnoreCase("LastWindowSize")) {
				Integer j=NumberTools.getInteger(e.getAttribute("Mode"));
				if (j!=null && (j==Frame.NORMAL || j==Frame.MAXIMIZED_HORIZ || j==Frame.MAXIMIZED_VERT || j==Frame.MAXIMIZED_BOTH)) lastSizeMode=j;
				j=NumberTools.getNotNegativeInteger(e.getAttribute("X"));
				if (j!=null) lastPosition.x=j;
				j=NumberTools.getNotNegativeInteger(e.getAttribute("Y"));
				if (j!=null) lastPosition.y=j;
				j=NumberTools.getNotNegativeInteger(e.getAttribute("Width"));
				if (j!=null) lastSize.width=j;
				j=NumberTools.getNotNegativeInteger(e.getAttribute("Height"));
				if (j!=null) lastSize.height=j;
			}

			if (s.equalsIgnoreCase("LookAndFeel")) {
				lookAndFeel=e.getTextContent();
				continue;
			}

			if (s.equalsIgnoreCase("Images")) {
				Integer j=NumberTools.getInteger(e.getTextContent());
				if (j!=null) imageSize=Math.min(5000,Math.max(50,j));
				imagesInline=loadBoolean(e.getAttribute("Inline"),true);
				continue;
			}

			if (s.equalsIgnoreCase("Report")) {
				reportSettings=e.getTextContent();
				continue;
			}

			if (s.equalsIgnoreCase("LastFiles")) {
				files.add(e.getTextContent());
				continue;
			}

			if (s.equalsIgnoreCase("OpenStatistics")) {
				openWord=loadBoolean(e.getAttribute("docx"),true);
				openODT=loadBoolean(e.getAttribute("odt"),true);
				openExcel=loadBoolean(e.getAttribute("xlsx"),true);
				openODS=loadBoolean(e.getAttribute("ods"),true);
				continue;
			}

			if (s.equalsIgnoreCase("MultiCore")) {
				useMultiCore=loadBoolean(e.getTextContent(),true);
			}
		}

		lastFiles=addToArray(lastFiles,files);
	}

	@Override
	protected void saveSetupToXML(final Document doc, final Element root) {
		Element node;

		root.appendChild(node=doc.createElement("Language"));
		node.setTextContent(language.toLowerCase());

		if (startSizeMode!=StartMode.START_MODE_DEFAULT) {
			root.appendChild(node=doc.createElement("Fullscreen"));
			if (startSizeMode==StartMode.START_MODE_FULLSCREEN) node.setTextContent("1");
			if (startSizeMode==StartMode.START_MODE_LASTSIZE) node.setTextContent("2");
		}

		if (startSizeMode==StartMode.START_MODE_LASTSIZE) {
			root.appendChild(node=doc.createElement("LastWindowSize"));
			node.setAttribute("Mode",""+lastSizeMode);
			node.setAttribute("X",""+lastPosition.x);
			node.setAttribute("Y",""+lastPosition.y);
			node.setAttribute("Width",""+lastSize.width);
			node.setAttribute("Height",""+lastSize.height);
		}

		if (lookAndFeel!=null && !lookAndFeel.trim().isEmpty()) {
			root.appendChild(node=doc.createElement("LookAndFeel"));
			node.setTextContent(lookAndFeel);
		}

		if (imageSize!=1000 || !imagesInline) {
			root.appendChild(node=doc.createElement("Images"));
			node.setTextContent(""+imageSize);
			if (!imagesInline) node.setAttribute("Inline","0");
		}

		if (reportSettings!=null && !reportSettings.isEmpty()) {
			root.appendChild(node=doc.createElement("Report"));
			node.setTextContent(reportSettings);
		}

		if (lastFiles!=null && lastFiles.length>0) for (int i=0;i<lastFiles.length;i++) {
			root.appendChild(node=doc.createElement("LastFiles"));
			node.setTextContent(lastFiles[i]);
		}

		if (!openWord || openODT || !openExcel || openODS) {
			root.appendChild(node=doc.createElement("OpenStatistics"));
			node.setAttribute("docx",openWord?"1":"0");
			node.setAttribute("odt",openODT?"1":"0");
			node.setAttribute("xlsx",openExcel?"1":"0");
			node.setAttribute("ods",openODS?"1":"0");
		}

		if (!useMultiCore) {
			root.appendChild(node=doc.createElement("MultiCore"));
			node.setTextContent("0");
		}

		if (lastError!=null && !lastError.isEmpty()) {
			root.appendChild(node=doc.createElement("LastError"));
			node.setTextContent(lastError);
		}
	}


	/**
	 * Stellt die Systemsprache ein und reinitialisiert
	 * die <code>Language</code>- und <code>LanguageStaticLoader</code>-Systeme.
	 * @param langName	Sprache, "de" oder "en"
	 */
	public void setLanguage(final String langName) {
		language=langName;
		saveSetup();
		Language.init(language);
		LanguageStaticLoader.setLanguage();
		if (Messages_Java11.isFixNeeded()) Messages_Java11.setupMissingSwingMessages();
	}
}