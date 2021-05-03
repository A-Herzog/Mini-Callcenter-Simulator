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
import java.io.File;

import javax.swing.SwingUtilities;

import language.Language;
import language.LanguageStaticLoader;
import language.Messages_Java11;
import mathtools.Table;
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
 * Der Simulator kann �ber diese Klasse sowohl im GUI- als auch im Kommandozeilen-Modus gestartet werden.
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
	 * Verarbeitet m�gliche Kommandozeilen-Parameter
	 * @param args	Die an <code>main</code> �bergebenen Parameter
	 * @return	Gibt <code>true</code> zur�ck, wenn alle Verarbeitungen bereits auf der Kommandozeile ausgef�hrt werden konnten und die grafische Oberfl�che nicht gestartet werden muss
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
		/* Sprache */
		Language.init(SetupData.getSetup().language);
		LanguageStaticLoader.setLanguage();
		if (Messages_Java11.isFixNeeded()) Messages_Java11.setupMissingSwingMessages();

		/* Basiseinstellungen zu den xml-Dateiformaten */
		XMLTools.homeURL="a-herzog.github.io";
		XMLTools.mediaURL="https://"+XMLTools.homeURL+"/Mini-Callcenter-Simulator/";
		XMLTools.dtd="MiniCallcenterSimulator.dtd";
		XMLTools.xsd="MiniCallcenterSimulator.xsd";

		/* Cache-Ordner f�r PDFWriter einstellen */
		PDFWriter.cacheFolder=SetupData.getSetupFolder();

		/* Programmname f�r Export */
		Table.ExportTitle=MainFrame.PROGRAM_NAME;
		StatisticsBasePanel.program_name=MainFrame.PROGRAM_NAME;

		/* Parameter verarbeiten */
		if (processCommandLineArguments(args)) return;

		/* Grafische Oberfl�che verf�gbar? */
		if (!GUITools.isGraphicsAvailable()) return;

		/* Grafische Oberfl�che starten */
		SwingUtilities.invokeLater(new RunSimulator());
	}

	/**
	 * Ausf�hren der grafischen Oberfl�che �ber ein <code>invokeLater</code>.
	 */
	private static final class RunSimulator implements Runnable {
		@Override
		public void run() {
			SetupData setup=SetupData.getSetup();
			FlatLaFHelper.init();
			FlatLaFHelper.setCombinedMenuBar(setup.lookAndFeelCombinedMenu);
			GUITools.setupUI(setup.lookAndFeel);
			FlatLaFHelper.setup();
			MsgBox.setBackend(new MsgBoxBackendTaskDialog());
			new MainFrame(loadFile);
		}
	}
}