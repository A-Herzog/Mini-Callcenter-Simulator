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
package start;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.util.Properties;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import language.Language;
import language.LanguageStaticLoader;
import language.Messages_Java11;
import mathtools.Table;
import mathtools.distribution.swing.JDistributionEditorPanel;
import mathtools.distribution.swing.JDistributionEditorPanelRecord;
import systemtools.GUITools;
import systemtools.MsgBox;
import systemtools.MsgBoxBackendTaskDialog;
import systemtools.statistics.PDFWriter;
import systemtools.statistics.StatisticsBasePanel;
import tools.SetupData;
import ui.MainFrame;
import ui.commandline.CommandLineSystem;
import ui.tools.FlatLaFHelper;
import xml.XMLTools;

/**
 * Main-Klasse des Simulators
 * Der Simulator kann über diese Klasse sowohl im GUI- als auch im Kommandozeilen-Modus gestartet werden.
 * @author Alexander Herzog
 *
 */
public class Main {
	/**
	 * Wird der Simulator mit einem einfachen Dateinamen als Parameter aufgerufen, so wird angenommen, dass es sich dabei
	 * um eine zu ladende Datei handelt. Diese wird hier gespeichert.
	 */
	private static File loadFile;

	/**
	 * Konstruktor der Klasse<br>
	 * Diese Klasse kann nicht instanziert werden.
	 * Sie stellt nur die statische Methode {@link #main(String[])} zur Verfügung.
	 */
	private Main() {
		/*
		 * Wird nur benötigt, um einen JavaDoc-Kommentar für diesen (impliziten) Konstruktor
		 * setzen zu können, damit der JavaDoc-Compiler keine Warnung mehr ausgibt.
		 */
	}

	/**
	 * Verarbeitet mögliche Kommandozeilen-Parameter
	 * @param args	Die an <code>main</code> übergebenen Parameter
	 * @return	Gibt <code>true</code> zurück, wenn alle Verarbeitungen bereits auf der Kommandozeile ausgeführt werden konnten und die grafische Oberfläche nicht gestartet werden muss
	 */
	private static boolean processCommandLineArguments(String[] args) {
		if (args.length==0) return false;

		CommandLineSystem commandLineSystem=new CommandLineSystem();
		loadFile=commandLineSystem.checkLoadFile(args);
		if (loadFile==null) {if (commandLineSystem.run(args)) return true;}

		return false;
	}

	/**
	 * Hauptroutine des gesamten Programms
	 * @param args	Kommandozeilen-Parameter
	 */
	public static void main(String[] args) {
		/* Jede Art der Log4J-Erfassung deaktivieren. */
		final Properties systemProperties=System.getProperties();
		systemProperties.setProperty("org.apache.logging.log4j.level","OFF"); /* wird von org.apache.logging.log4j.core.config.AbstractConfiguration.setToDefault() gelesen */
		systemProperties.setProperty("log4j2.formatMsgNoLookups","true"); /* wird von org.apache.logging.log4j.core.util.Constants gelesen */

		/* Sprache */
		try {
			Language.init(SetupData.getSetup().language);
			LanguageStaticLoader.setLanguage();
			if (Messages_Java11.isFixNeeded()) Messages_Java11.setupMissingSwingMessages();
		} catch (NoClassDefFoundError e) {
			if (GraphicsEnvironment.isHeadless()) {
				System.out.println("The required libraries in the \"libs\" subfolder are missing.");
				System.out.println("Therefore, the program cannot be executed.");
			} else {
				JOptionPane.showMessageDialog(null,"The required libraries in the \"libs\" subfolder are missing.\nTherefore, the program cannot be executed.","Missing libraries",JOptionPane.ERROR_MESSAGE);
			}
			return;
		}

		/* Basiseinstellungen zu den xml-Dateiformaten */
		XMLTools.homeURL="a-herzog.github.io";
		XMLTools.mediaURL="https://"+XMLTools.homeURL+"/Mini-Callcenter-Simulator/";
		XMLTools.dtd="MiniCallcenterSimulator.dtd";
		XMLTools.xsd="MiniCallcenterSimulator.xsd";

		/* Cache-Ordner für PDFWriter einstellen */
		PDFWriter.cacheFolder=SetupData.getSetupFolder();

		/* Programmname für Export */
		Table.ExportTitle=MainFrame.PROGRAM_NAME;
		StatisticsBasePanel.program_name=MainFrame.PROGRAM_NAME;

		/* Parameter verarbeiten */
		if (processCommandLineArguments(args)) return;

		/* Grafische Oberfläche verfügbar? */
		if (!GUITools.isGraphicsAvailable()) return;

		/* Grafische Oberfläche starten */
		SwingUtilities.invokeLater(new RunSimulator());
	}

	/**
	 * Ausführen der grafischen Oberfläche über ein <code>invokeLater</code>.
	 */
	private static final class RunSimulator implements Runnable {
		/**
		 * Konstruktor der Klasse
		 */
		public RunSimulator() {
			/*
			 * Wird nur benötigt, um einen JavaDoc-Kommentar für diesen (impliziten) Konstruktor
			 * setzen zu können, damit der JavaDoc-Compiler keine Warnung mehr ausgibt.
			 */
		}

		@Override
		public void run() {
			final SetupData setup=SetupData.getSetup();

			/* Look & Feel */
			FlatLaFHelper.init();
			FlatLaFHelper.setCombinedMenuBar(setup.lookAndFeelCombinedMenu);
			GUITools.setupUI(setup.lookAndFeel);
			FlatLaFHelper.setup();

			/* Meldungsdialoge */
			MsgBox.setBackend(new MsgBoxBackendTaskDialog());

			/* Filter für Verteilungsliste in Verteilungseditoren */
			JDistributionEditorPanel.registerFilterGetter(()->{
				final String s=setup.distributionListFilter.trim();
				return (s.isEmpty())?String.join("\n",JDistributionEditorPanelRecord.getDefaultHighlights()):s;
			});
			JDistributionEditorPanel.registerFilterSetter(list->{
				setup.distributionListFilter=list;
				setup.saveSetup();
			});

			/* Start */
			new MainFrame(loadFile);
		}
	}
}