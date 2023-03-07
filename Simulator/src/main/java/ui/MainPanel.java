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
package ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.w3c.dom.Element;

import language.Language;
import mathtools.Table;
import mathtools.distribution.LogNormalDistributionImpl;
import mathtools.distribution.NeverDistributionImpl;
import mathtools.distribution.swing.CommonVariables;
import mathtools.distribution.tools.FileDropperData;
import simulator.Simulator;
import simulator.editmodel.EditModel;
import simulator.runmodel.RunModel;
import simulator.statistics.Statistics;
import systemtools.BaseDialog;
import systemtools.MainPanelBase;
import systemtools.MsgBox;
import systemtools.commandline.CommandLineDialog;
import systemtools.help.HelpBase;
import systemtools.help.IndexSystem;
import systemtools.statistics.StatisticsBasePanel;
import tools.ExportQSModel;
import tools.SetupData;
import ui.calculator.CalculatorDialog;
import ui.calculator.QueueingCalculatorDialog;
import ui.commandline.CommandLineSystem;
import ui.compare.ComparePanel;
import ui.compare.CompareSelectDialog;
import ui.dialogs.LicenseViewer;
import ui.dialogs.SetupDialog;
import ui.help.Help;
import ui.images.Images;
import ui.statistics.StatisticsPanel;
import ui.tools.SpecialPanel;
import ui.tools.WaitPanel;
import xml.XMLTools;

/**
 * Diese Klasse stellt den Arbeitsbereich innerhalb des Programmfensters dar.
 * @see MainPanelBase
 * @author Alexander Herzog
 */
public class MainPanel extends MainPanelBase {
	/**
	 * Serialisierungs-ID der Klasse
	 * @see Serializable
	 */
	private static final long serialVersionUID = 7636118203704616559L;

	/**
	 * Homepage-Adresse für Webbrowseraufrufe
	 */
	public static final String WEB_URL="github.com/A-Herzog/Mini-Callcenter-Simulator";

	/**
	 * Autor des Programms
	 */
	public static final String AUTHOR="Alexander Herzog";

	/**
	 * E-Mail-Adresse des Autors
	 */
	public static final String AUTHOR_EMAIL="alexander.herzog@tu-clausthal.de";

	/**
	 * Programmversion
	 */
	public static final String systemVersion="5.9.232";

	/**
	 * Bezeichnung für "ungespeichertes Modell" in der Titelzeile für ein neues Modell, welches noch keinen Namen besitzt
	 */
	public static String UNSAVED_MODEL="ungespeichertes Modell";

	/** Menüpunkte, die nur bei Anzeige des Modell-Editors aktiviert werden sollen */
	private List<JMenuItem> enabledOnEditorPanel;
	/** Schaltflächen, die nur bei Anzeige des Modell-Editors sichtbar sein sollen */
	private List<JButton> visibleOnEditorPanel;
	/** Schaltflächen, die nur bei Anzeige des Statistik-Viewers sichtbar sein sollen */
	private List<JButton> visibleOnStatisticsPanel;
	/** Schaltflächen/Menüpunkte, die bei Anzeige des Modell-Editors markiert dargestellt werden sollen */
	private List<AbstractButton> selectOnEditorPanel;
	/** Schaltflächen/Menüpunkte, die bei Anzeige des Statistik-Viewers markiert dargestellt werden sollen */
	private List<AbstractButton> selectOnStatisticsPanel;
	/** Schaltflächen/Menüpunkte, die bei aktiviert dargestellt werden sollen, wenn Statistikdaten verfügbar sind */
	private List<AbstractButton> enabledOnStatisticsAvailable;

	/** Menüpunkt "Zuletzt verwendet" */
	private JMenu menuFileModelRecentlyUsed;

	/** Menüpunkt "Aktuelles und festgehaltenes Modell vergleichen" */
	private JMenuItem menuExtrasCompareKept;

	/** Menüpunkt "Zu festgehaltenem Modell zurückkehren" */
	private JMenuItem menuModelCompareReturn;

	/**
	 * Button zum Ein- und Ausblenden der Erklärungen zu den Modell-Editor-Seiten
	 */
	private JButton buttonPageInfo;

	/**
	 * Programmeinstellungen-Singleton
	 */
	private final SetupData setup;

	/**
	 * Callback, welches ein Neuladen des Fensters veranlasst.
	 */
	private Runnable reloadWindow;

	/** Aktuell angezeigtes Panel */
	private JPanel currentPanel;

	/** Modell-Editor-Panel */
	private final EditorPanel editorPanel;

	/** Warte-Panel zum Anzeigen während der Simulation */
	private final WaitPanel waitPanel;

	/** Statistik-Panel */
	private final StatisticsPanel statisticsPanel;

	/** Panel für weitere besondere Einstellungen */
	private SpecialPanel specialPanel;

	/**
	 * Modell für den Vergleich mit einem geänderten Modell festhalten
	 */
	private EditModel pinnedModel;

	/** Statistikdaten, die sich beim Modellvergleich ergeben haben */
	private Statistics[] compareStatistics=new Statistics[2];

	/**
	 * Konstruktor der Klasse
	 * @param ownerWindow	Übergeordnetes Fenster
	 * @param programName	Name des Programms (wird dann über {@link MainPanelBase#programName} angeboten)
	 * @param isReload	Gibt an, ob es sich bei dem Aufbau des Panels um einen Programmstart (<code>false</code>) oder nur um einen Wiederaufbau z.B. nach dem Ändern der Sprache (<code>true</code>) handelt
	 */
	public MainPanel(final Window ownerWindow, final String programName, final boolean isReload) {
		super(ownerWindow,programName);
		initActions();
		initToolbar();
		setup=SetupData.getSetup();
		setAdditionalTitle(UNSAVED_MODEL);

		editorPanel=new EditorPanel();
		waitPanel=new WaitPanel();
		statisticsPanel=new StatisticsPanel(()->commandSimulation(null,null,null));
		statisticsPanel.addFileDropListener(e->{if (e.getSource() instanceof FileDropperData) dropFile((FileDropperData)e.getSource());});
		specialPanel=null;

		SwingUtilities.invokeLater(()->{
			setCurrentPanel(editorPanel);
			commandFileModelNew(0);
			if (!isReload) languageInfo();

			final IndexSystem indexSystem=IndexSystem.getInstance();
			indexSystem.addLanguage("de","pages_de");
			indexSystem.addLanguage("en","pages_en");
			indexSystem.init(Help.class);
			IndexSystem.getInstance().setLanguage(Language.getCurrentLanguage());
		});

		setup.addChangeNotifyListener(()->reloadSetup());
		reloadSetup();
	}

	/**
	 * Stellt das aktuell im {@link MainPanel} sichtbare Kind-Panel ein.
	 * @param visiblePanel	Anzuzeigendes Panel
	 */
	private void setCurrentPanel(final JPanel visiblePanel) {
		if (visiblePanel!=editorPanel) mainPanel.remove(editorPanel);
		if (visiblePanel!=waitPanel) mainPanel.remove(waitPanel);
		if (visiblePanel!=statisticsPanel) mainPanel.remove(statisticsPanel);
		if (specialPanel!=null && visiblePanel!=specialPanel) mainPanel.remove(specialPanel);

		boolean isInPanel=false;
		for (Component component : mainPanel.getComponents()) if (component==visiblePanel) {isInPanel=true; break;}
		if (!isInPanel) mainPanel.add(visiblePanel);

		currentPanel=visiblePanel;
		if (currentPanel instanceof SpecialPanel) specialPanel=(SpecialPanel)currentPanel; else specialPanel=null;

		mainPanel.repaint();

		final boolean editorPanelActive=(visiblePanel==editorPanel);
		final boolean statisticsPanelActive=(visiblePanel==statisticsPanel);
		if (enabledOnEditorPanel!=null) for (JMenuItem item: enabledOnEditorPanel) item.setEnabled(editorPanelActive);
		if (selectOnEditorPanel!=null) for (AbstractButton button: selectOnEditorPanel) button.setSelected(editorPanelActive);
		if (selectOnStatisticsPanel!=null) for (AbstractButton button: selectOnStatisticsPanel) button.setSelected(statisticsPanelActive);
		if (visibleOnEditorPanel!=null) for (JButton button: visibleOnEditorPanel) button.setVisible(editorPanelActive);
		if (visibleOnStatisticsPanel!=null) for (JButton button: visibleOnStatisticsPanel) button.setVisible(statisticsPanelActive);
	}

	@Override
	protected URL getResourceURL(final String path) {
		return MainPanel.class.getResource(path);
	}

	/**
	 * Reagiert auf Drag&amp;Drop-Aktionen auf das Editor- oder das Statistik-Panel.
	 * @param drop	Drag&amp;Drop-Element
	 * @see #editorPanel
	 * @see #statisticsPanel
	 */
	private void dropFile(final FileDropperData drop) {
		final File file=drop.getFile();
		if (file.isFile()) {
			drop.dragDropConsumed();
			SwingUtilities.invokeLater(()->{
				if (loadAnyFile(file,drop.getDropComponent(),drop.getDropPosition(),true)) {
					CommonVariables.setInitialDirectoryFromFile(file);
				}
			});
		}
	}

	/**
	 * Initiiert die Zuordnung von Aktionsnamen zu {@link Runnable}-Funktionen.
	 */
	private void initActions() {
		/* Datei */
		addAction("FileNew1",e->commandFileModelNew(0));
		addAction("FileNew2",e->commandFileModelNew(1));
		addAction("FileNew3",e->commandFileModelNew(2));
		addAction("FileNew4",e->commandFileModelNew(3));
		addAction("FileNew5",e->commandFileModelNew(4));
		addAction("FileLoad",e->commandFileModelLoad(null,null));
		addAction("FileSave",e->commandFileModelSave(false));
		addAction("FileSaveAs",e->commandFileModelSave(true));
		addAction("FileSaveCopyAs",e->commandFileModelSaveCopyAs());
		addAction("FileExportQSModel",e->commandFileExportQSModel());
		addAction("FileStatisticsLoad",e->commandFileStatisticsLoad(null,null));
		addAction("FileStatisticsSave",e->commandFileStatisticsSave());
		addAction("FileSetup",e->commandFileSetup());
		addAction("FileQuit",e->{if (allowQuitProgram()) close();});

		/* Ansicht */
		addAction("ViewEditor",e->setCurrentPanel(editorPanel));
		addAction("ViewStatistics",e->setCurrentPanel(statisticsPanel));

		/* Simulation */
		addAction("SimulationSimulation",e->commandSimulation(null,null,null));
		addAction("SimulationSimulationLog",e->commandSimulationLog());
		addAction("SimulationModel",e->commandSimulationModel());

		/* Extras */
		addAction("ExtrasCompare",e->commandExtrasCompare());
		addAction("ExtrasCompareKeep",e->commandExtrasCompareTwoInit());
		addAction("ExtrasCompareKept",e->commandExtrasCompareTwoRun(0));
		addAction("ExtrasCompareReturn",e->commandExtrasCompareReturn());
		addAction("ExtrasCalculator",e->commandExtrasCalculator(""));
		addAction("ExtrasQueueingCalculator",e->commandExtrasQueueingCalculator());
		addAction("ExtrasExecuteCommand",e->commandExtrasExecuteCommand());

		/* Hilfe */
		addAction("HelpHelp",e->commandHelpHelp());
		addAction("HelpContent",e->commandHelpContent());
		addAction("HelpBook",e->commandHelpBook());
		addAction("HelpSupport",e->commandHelpSupport());
		addAction("HelpHomepage",e->commandHelpHomepage());
		addAction("HelpErlangScripts",e->commandHelpErlangScripts());
		addAction("HelpLicense",e->commandHelpLicenseInfo());
		addAction("HelpInfo",e->commandHelpInfo());
		addAction("HelpPageInfo",e->commandHelpPageInfo());
	}

	@Override
	public JToolBar createToolBar() {
		if (visibleOnEditorPanel==null) visibleOnEditorPanel=new ArrayList<>();
		if (visibleOnStatisticsPanel==null) visibleOnStatisticsPanel=new ArrayList<>();
		if (selectOnEditorPanel==null) selectOnEditorPanel=new ArrayList<>();
		if (selectOnStatisticsPanel==null) selectOnStatisticsPanel=new ArrayList<>();
		if (enabledOnStatisticsAvailable==null) enabledOnStatisticsAvailable=new ArrayList<>();

		JToolBar toolbar=new JToolBar();
		toolbar.setFloatable(false);
		JButton button;

		visibleOnEditorPanel.add(createToolbarButton(toolbar,Language.tr("Main.Toolbar.LoadModel"),Language.tr("Main.Toolbar.LoadModel.Hint")+" ("+keyStrokeToString(KeyStroke.getKeyStroke(KeyEvent.VK_L,InputEvent.CTRL_DOWN_MASK))+")",Images.MODEL_LOAD.getIcon(),"FileLoad"));
		visibleOnEditorPanel.add(createToolbarButton(toolbar,Language.tr("Main.Toolbar.SaveModel"),Language.tr("Main.Toolbar.SaveModel.Hint")+" ("+keyStrokeToString(KeyStroke.getKeyStroke(KeyEvent.VK_S,InputEvent.CTRL_DOWN_MASK))+")",Images.MODEL_SAVE.getIcon(),"FileSave"));
		visibleOnStatisticsPanel.add(createToolbarButton(toolbar,Language.tr("Main.Toolbar.LoadStatistics"),Language.tr("Main.Toolbar.LoadStatistics.Hint")+" ("+keyStrokeToString(KeyStroke.getKeyStroke(KeyEvent.VK_L,InputEvent.CTRL_DOWN_MASK+InputEvent.SHIFT_DOWN_MASK))+")",Images.STATISTICS_LOAD.getIcon(),"FileStatisticsLoad"));
		button=createToolbarButton(toolbar,Language.tr("Main.Toolbar.SaveStatistics"),Language.tr("Main.Toolbar.SaveStatistics.Hint")+" ("+keyStrokeToString(KeyStroke.getKeyStroke(KeyEvent.VK_S,InputEvent.CTRL_DOWN_MASK+InputEvent.SHIFT_DOWN_MASK))+")",Images.STATISTICS_SAVE.getIcon(),"FileStatisticsSave");
		visibleOnStatisticsPanel.add(button);
		enabledOnStatisticsAvailable.add(button);
		toolbar.addSeparator();
		selectOnEditorPanel.add(createToolbarButton(toolbar,Language.tr("Main.Toolbar.ShowEditor"),Language.tr("Main.Toolbar.ShowEditor.Hint")+" ("+keyStrokeToString(KeyStroke.getKeyStroke(KeyEvent.VK_F3,0))+")",Images.MODEL.getIcon(),"ViewEditor"));
		selectOnStatisticsPanel.add(createToolbarButton(toolbar,Language.tr("Main.Toolbar.ShowStatistics"),Language.tr("Main.Toolbar.ShowStatistics.Hint")+" ("+keyStrokeToString(KeyStroke.getKeyStroke(KeyEvent.VK_F4,0))+")",Images.STATISTICS.getIcon(),"ViewStatistics"));
		toolbar.addSeparator();
		createToolbarButton(toolbar,Language.tr("Main.Toolbar.StartSimulation"),Language.tr("Main.Toolbar.StartSimulation.Hint")+" ("+keyStrokeToString(KeyStroke.getKeyStroke(KeyEvent.VK_F5,0))+")",Images.SIMULATION.getIcon(),"SimulationSimulation");
		button=createToolbarButton(toolbar,Language.tr("Main.Toolbar.ShowModelForTheseResults"),Language.tr("Main.Toolbar.ShowModelForTheseResults.Hint"),Images.MODEL.getIcon(),"SimulationModel");
		visibleOnStatisticsPanel.add(button);
		enabledOnStatisticsAvailable.add(button);
		toolbar.addSeparator();
		createToolbarButton(toolbar,Language.tr("Main.Toolbar.Help"),Language.tr("Main.Toolbar.Help.Hint")+" ("+keyStrokeToString(KeyStroke.getKeyStroke(KeyEvent.VK_F1,0))+")",Images.HELP.getIcon(),"HelpHelp");

		/*
		toolbar.add(button=new JButton("Test"));
		button.addActionListener(e->{ });
		 */

		toolbar.add(Box.createHorizontalGlue());

		visibleOnEditorPanel.add(buttonPageInfo=createToolbarButton(toolbar,Language.tr("Main.Toolbar.PageInfo"),Language.tr("Main.Toolbar.PageInfo.Hint"),Images.GENERAL_INFO.getIcon(),"HelpPageInfo"));

		return toolbar;
	}

	/**
	 * Wandelt ein Hotkey-Objekt in eine entsprechende Beschreibung um
	 * @param key	Hotkey
	 * @return	Beschreibung als Zeichenkette
	 */
	private String keyStrokeToString(final KeyStroke key) {
		final int modifiers=key.getModifiers();
		final StringBuilder text=new StringBuilder();
		if (modifiers>0) {
			text.append(InputEvent.getModifiersExText(modifiers));
			text.append('+');
		}
		text.append(KeyEvent.getKeyText(key.getKeyCode()));
		return text.toString();
	}

	@Override
	public JMenuBar createMenu() {
		if (selectOnEditorPanel==null) selectOnEditorPanel=new ArrayList<>();
		if (selectOnStatisticsPanel==null) selectOnStatisticsPanel=new ArrayList<>();
		if (enabledOnStatisticsAvailable==null) enabledOnStatisticsAvailable=new ArrayList<>();

		final JMenuBar menubar=new JMenuBar();
		JMenu menu,sub;
		JMenuItem item;

		/* Datei */

		menubar.add(menu=new JMenu(Language.tr("Main.Menu.File")));
		setMnemonic(menu,Language.tr("Main.Menu.File.Mnemonic"));

		menu.add(sub=new JMenu(Language.tr("Main.Menu.File.New")));
		createMenuItem(sub,"M/M/c/infty Modell",'\0',"FileNew1");
		createMenuItem(sub,"M/M/c/K Modell",'\0',"FileNew2");
		createMenuItem(sub,"M/M/c/K+M Modell",'\0',"FileNew3");
		createMenuItem(sub,"M/M/c/K+M+M+Weiterleitungen Modell",'\0',"FileNew4");
		createMenuItem(sub,"M/G/c/K+G+G+Weiterleitungen Modell",'\0',"FileNew5");

		createMenuItemCtrl(menu,Language.tr("Main.Menu.File.Load"),Images.MODEL_LOAD.getIcon(),Language.tr("Main.Menu.File.Load.Mnemonic"),KeyEvent.VK_L,"FileLoad");

		menu.add(menuFileModelRecentlyUsed=new JMenu(Language.tr("Main.Menu.File.RecentlyUsed")));
		setMnemonic(menuFileModelRecentlyUsed,Language.tr("Main.Menu.File.RecentlyUsed.Mnemonic"));
		updateRecentlyUsedList();

		createMenuItemCtrl(menu,Language.tr("Main.Menu.File.Save"),Images.MODEL_SAVE.getIcon(),Language.tr("Main.Menu.File.Save.Mnemonic"),KeyEvent.VK_S,"FileSave");
		createMenuItemCtrl(menu,Language.tr("Main.Menu.File.SaveAs"),Language.tr("Main.Menu.File.SaveAs.Mnemonic"),KeyEvent.VK_U,"FileSaveAs");
		createMenuItem(menu,Language.tr("Main.Menu.File.SaveCopyAs"),Language.tr("Main.Menu.File.SaveCopyAs.Mnemonic"),"FileSaveCopyAs");
		item=createMenuItem(menu,Language.tr("Main.Menu.File.ExportQSModel"),Images.EXTRAS_QUEUE.getIcon(),Language.tr("Main.Menu.File.ExportQSModel.Mnemonic"),"FileExportQSModel");
		item.setToolTipText(Language.tr("Main.Menu.File.ExportQSModel.Info"));

		menu.addSeparator();

		createMenuItemCtrlShift(menu,Language.tr("Main.Menu.File.LoadStatistics"),Images.STATISTICS_LOAD.getIcon(),Language.tr("Main.Menu.File.LoadStatistics.Mnemonic"),KeyEvent.VK_L,"FileStatisticsLoad");
		enabledOnStatisticsAvailable.add(createMenuItemCtrlShift(menu,Language.tr("Main.Menu.File.SaveStatistics"),Images.STATISTICS_SAVE.getIcon(),Language.tr("Main.Menu.File.SaveStatistics.Mnemonic"),KeyEvent.VK_U,"FileStatisticsSave"));

		menu.addSeparator();

		createMenuItemCtrl(menu,Language.tr("Main.Menu.File.Settings"),Images.GENERAL_SETUP.getIcon(),Language.tr("Main.Menu.File.Settings.Mnemonic"),KeyEvent.VK_P,"FileSetup");

		menu.addSeparator();

		createMenuItemCtrl(menu,Language.tr("Main.Menu.File.Quit"),Images.GENERAL_EXIT.getIcon(),Language.tr("Main.Menu.File.Quit.Mnemonic"),KeyEvent.VK_W,"FileQuit");

		/* Ansicht */
		menubar.add(menu=new JMenu(Language.tr("Main.Menu.View")));
		setMnemonic(menu,Language.tr("Main.Menu.View.Mnemonic"));

		selectOnEditorPanel.add(createCheckBoxMenuItemIcon(menu,Language.tr("Main.Menu.View.ModelEditor"),Images.MODEL.getIcon(),Language.tr("Main.Menu.View.ModelEditor.Mnemonic"),KeyEvent.VK_F3,"ViewEditor"));
		selectOnStatisticsPanel.add(createCheckBoxMenuItemIcon(menu,Language.tr("Main.Menu.View.SimulationResults"),Images.STATISTICS.getIcon(),Language.tr("Main.Menu.View.SimulationResults.Mnemonic"),KeyEvent.VK_F4,"ViewStatistics"));

		/* Simulation */
		menubar.add(menu=new JMenu(Language.tr("Main.Menu.Simulation")));
		setMnemonic(menu,Language.tr("Main.Menu.Simulation.Mnemonic"));

		createMenuItem(menu,Language.tr("Main.Menu.StartSimulation"),Images.SIMULATION.getIcon(),Language.tr("Main.Menu.StartSimulation.Mnemonic"),KeyEvent.VK_F5,"SimulationSimulation");
		createMenuItem(menu,Language.tr("Main.Menu.RecordSimulation"),Images.SIMULATION_LOG.getIcon(),Language.tr("Main.Menu.RecordSimulation.Mnemonic"),"SimulationSimulationLog");

		/* Extras */
		menubar.add(menu=new JMenu(Language.tr("Main.Menu.Extras")));
		setMnemonic(menu,Language.tr("Main.Menu.Extras.Mnemonic"));

		createMenuItem(menu,Language.tr("Main.Menu.Extras.CompareModels"),Images.MODEL_COMPARE.getIcon(),Language.tr("Main.Menu.Extras.CompareModels.Mnemonic"),"ExtrasCompare");
		menu.addSeparator();
		createMenuItem(menu,Language.tr("Main.Menu.Extras.KeepModel"),Images.MODEL_COMPARE_KEEP.getIcon(),Language.tr("Main.Menu.Extras.KeepModel.Mnemonic"),"ExtrasCompareKeep");
		menuExtrasCompareKept=createMenuItem(menu,Language.tr("Main.Menu.Extras.CompareWithKeptModel"),Images.MODEL_COMPARE_COMPARE.getIcon(),Language.tr("Main.Menu.Extras.CompareWithKeptModel.Mnemonic"),"ExtrasCompareKept");
		menuExtrasCompareKept.setEnabled(false);
		menuModelCompareReturn=createMenuItem(menu,Language.tr("Main.Menu.Extras.ReturnToKeptModel"),Images.MODEL_COMPARE_GO_BACK.getIcon(),Language.tr("Main.Menu.Extras.ReturnToKeptModel.Mnemonic"),"ExtrasCompareReturn");
		menuModelCompareReturn.setEnabled(false);
		menu.addSeparator();
		createMenuItem(menu,Language.tr("Main.Menu.Extras.Calculator"),Images.EXTRAS_CALCULATOR.getIcon(),Language.tr("Main.Menu.Extras.Calculator.Mnemonic"),"ExtrasCalculator");
		createMenuItem(menu,Language.tr("Main.Menu.Extras.QueueingCalculator"),Images.EXTRAS_QUEUE.getIcon(),Language.tr("Main.Menu.Extras.QueueingCalculator.Mnemonic"),"ExtrasQueueingCalculator");
		createMenuItem(menu,Language.tr("Main.Menu.Extras.ExecuteCommand"),Images.EXTRAS_COMMANDLINE.getIcon(),Language.tr("Main.Menu.Extras.ExecuteCommand.Mnemonic"),"ExtrasExecuteCommand");

		/* Hilfe */
		menubar.add(menu=new JMenu(Language.tr("Main.Menu.Help")));
		setMnemonic(menu,Language.tr("Main.Menu.Help.Mnemonic"));

		createMenuItem(menu,Language.tr("Main.Menu.Help.Help"),Images.HELP.getIcon(),Language.tr("Main.Menu.Help.Help.Mnemonic"),KeyEvent.VK_F1,"HelpHelp");
		createMenuItemShift(menu,Language.tr("Main.Menu.Help.HelpContent"),Images.HELP_CONTENT.getIcon(),Language.tr("Main.Menu.Help.HelpContent.Mnemonic"),KeyEvent.VK_F1,"HelpContent");
		menu.addSeparator();
		createMenuItem(menu,Language.tr("MainMenu.Help.Book"),Images.HELP_BOOK.getIcon(),Language.tr("MainMenu.Help.Book.Mnemonic"),"HelpBook");
		createMenuItem(menu,Language.tr("Main.Menu.Help.Support"),Images.HELP_EMAIL.getIcon(),Language.tr("Main.Menu.Help.Support.Mnemonic"),"HelpSupport");
		createMenuItem(menu,Language.tr("MainMenu.Help.Homepage"),Images.HELP_HOMEPAGE.getIcon(),Language.tr("MainMenu.Help.Homepage.Mnemonic"),"HelpHomepage");
		createMenuItem(menu,Language.tr("MainMenu.Help.ErlangScripts"),Language.tr("MainMenu.Help.ErlangScripts.Mnemonic"),"HelpErlangScripts");
		menu.addSeparator();
		createMenuItem(menu,Language.tr("Main.Menu.Help.LicenseInformation"),Language.tr("Main.Menu.Help.LicenseInformation.Mnemonic"),"HelpLicense");
		createMenuItemCtrlShift(menu,Language.tr("Main.Menu.Help.ProgramInformation"),Images.GENERAL_INFO.getIcon(),Language.tr("Main.Menu.Help.ProgramInformation.Mnemonic"),KeyEvent.VK_F1,"HelpInfo");

		return menubar;
	}

	/**
	 * Aktualisiert die Liste der zuletzt verwendeten Dateien im Menü.
	 * @see #menuFileModelRecentlyUsed
	 */
	private void updateRecentlyUsedList() {
		menuFileModelRecentlyUsed.removeAll();
		menuFileModelRecentlyUsed.setEnabled(setup.lastFiles!=null && setup.lastFiles.length>0);
		if (!menuFileModelRecentlyUsed.isEnabled()) return;

		for (int i=0;i<setup.lastFiles.length; i++) {
			final JMenuItem sub=new JMenuItem(setup.lastFiles[i]);
			sub.addActionListener(actionListener);
			menuFileModelRecentlyUsed.add(sub);
		}

		if (setup.lastFiles.length>0) {
			menuFileModelRecentlyUsed.addSeparator();
			final JMenuItem sub=new JMenuItem(Language.tr("Main.Menu.File.RecentlyUsed.Delete"));
			sub.setIcon(Images.GENERAL_OFF.getIcon());
			sub.addActionListener(e->{
				setup.lastFiles=new String[0];
				setup.saveSetup();
				updateRecentlyUsedList();
			});
			menuFileModelRecentlyUsed.add(sub);
		}
	}

	/**
	 * Fügt einen Eintrag zu der Liste der zuletzt verwendeten Dateien hinzu,
	 * speichert das Setup und baut das Menü entsprechend neu auf.
	 * @param fileName	Dateiname, der zu der Liste hinzugefügt werden soll (wenn er nicht bereits enthalten ist)
	 */
	private void addFileToRecentlyUsedList(String fileName) {
		final ArrayList<String> files=(setup.lastFiles==null)?new ArrayList<>():new ArrayList<>(Arrays.asList(setup.lastFiles));

		int index=files.indexOf(fileName);
		if (index==0) return; /* Eintrag ist bereits ganz oben in der Liste, nichts zu tun */
		if (index>0) files.remove(index); /* Wenn schon in Liste: Element an alter Position entfernen */
		files.add(0,fileName); /* Element ganz vorne einfügen */
		while (files.size()>5) files.remove(files.size()-1); /* Maximal die letzten 5 Dateien merken */

		setup.lastFiles=files.toArray(new String[0]);
		setup.saveSetup();

		updateRecentlyUsedList();
	}

	/**
	 * Darf das aktuelle Modell verworfen werden?
	 * @return	Liefert <code>true</code>, wenn das aktuell im Editor befindliche Modell verworfen werden darf
	 */
	private boolean isDiscardModelOk() {
		if (!editorPanel.isModelChanged()) return true;

		switch (MsgBox.confirmSave(getOwnerWindow(),Language.tr("Window.DiscardConfirmation.Title"),Language.tr("Window.DiscardConfirmation.Info"))) {
		case JOptionPane.YES_OPTION:
			commandFileModelSave(false);
			return isDiscardModelOk();
		case JOptionPane.NO_OPTION:
			return true;
		case JOptionPane.CANCEL_OPTION:
			return false;
		default:
			return false;
		}
	}

	@Override
	public boolean allowQuitProgram() {
		if (currentPanel==null) return false;
		if (currentPanel==waitPanel) {waitPanel.abortSimulation(); return false;}
		if (currentPanel==specialPanel) {specialPanel.requestClose(); return false;}
		return isDiscardModelOk();
	}

	/**
	 * Versucht das Modell oder Statistikdaten aus einem Stream zu laden
	 * @param file	Datei der die Daten entstammen
	 * @param stream	Input-Stream aus dem die Daten geladen werden sollen
	 * @return	Liefert <code>true</code>, wenn der Ladevorgang erfolgreich war
	 * @see #processBase64ModelData(File, String)
	 */
	private boolean loadFromStream(final File file, final InputStream stream) {
		if (!isDiscardModelOk()) return true;

		final XMLTools xml=new XMLTools(stream);
		final Element root=xml.load();
		if (root==null) {
			MsgBox.error(getOwnerWindow(),Language.tr("XML.LoadErrorTitle"),xml.getError());
			return false;
		}

		final String name=root.getNodeName();

		for (String test: new EditModel().getRootNodeNames()) if (name.equalsIgnoreCase(test)) {
			return commandFileModelLoad(root,file);
		}
		for (String test: new Statistics(false,false).getRootNodeNames()) if (name.equalsIgnoreCase(test)) {
			return commandFileStatisticsLoad(root,file);
		}

		MsgBox.error(getOwnerWindow(),Language.tr("XML.LoadErrorTitle"),Language.tr("XML.UnknownFileFormat"));
		return false;
	}

	/**
	 * Versucht das Modell oder Statistikdaten aus base64 encodierten Daten zu laden
	 * @param file	Datei der die Daten entstammen
	 * @param base64data	base64 codierte Daten
	 * @return	Liefert <code>true</code>, wenn der Ladevorgang erfolgreich war
	 * @see #tryLoadHTML(File)
	 */
	private boolean processBase64ModelData(final File file, final String base64data) {
		try {
			final ByteArrayInputStream in=new ByteArrayInputStream(Base64.getDecoder().decode(base64data));
			return loadFromStream(file,in);
		} catch (IllegalArgumentException e) {return false;}
	}

	/**
	 * Versucht das Modell oder Statistikdaten aus HTML-Daten zu laden
	 * @param file	Zu ladende Datei
	 * @return	Liefert <code>true</code>, wenn der Ladevorgang erfolgreich war
	 */
	private boolean tryLoadHTML(final File file) {
		boolean firstLine=true;
		boolean modelDataFollow=false;

		final List<String> lines=Table.loadTextLinesFromFile(file);
		if (lines==null) return false;

		for (String line: lines) {
			if (firstLine) {
				if (!line.trim().equalsIgnoreCase("<!doctype html>")) return false;
			} else {
				if (modelDataFollow) {
					if (!line.trim().startsWith("data:application/xml;base64,")) return false;
					return processBase64ModelData(file,line.trim().substring("data:application/xml;base64,".length()));
				} else {
					if (line.trim().equalsIgnoreCase("QSModel")) modelDataFollow=true;
				}
			}
			firstLine=false;
		}

		return false;
	}

	@Override
	public boolean loadAnyFile(final File file, final Component dropComponent, final Point dropPosition, final boolean errorMessageOnFail) {
		if (file==null) {
			if (errorMessageOnFail) MsgBox.error(getOwnerWindow(),Language.tr("XML.LoadErrorTitle"),Language.tr("XML.NoFileSelected"));
			return false;
		}
		if (!file.exists()) {
			if (errorMessageOnFail) MsgBox.error(getOwnerWindow(),Language.tr("XML.LoadErrorTitle"),String.format(Language.tr("XML.FileNotFound"),file.toString()));
			return false;
		}

		/* Modell aus HTML-Datei laden */
		if (tryLoadHTML(file)) return true;

		/* XML oder json laden */
		final XMLTools xml=new xml.XMLTools(file);
		final Element root=xml.load();
		if (root==null) {
			if (errorMessageOnFail) MsgBox.error(getOwnerWindow(),Language.tr("XML.LoadErrorTitle"),xml.getError());
			return false;
		}

		final String name=root.getNodeName();

		for (String test: new EditModel().getRootNodeNames()) if (name.equalsIgnoreCase(test)) return commandFileModelLoad(root,file);
		for (String test: new Statistics(false,false).getRootNodeNames()) if (name.equalsIgnoreCase(test)) return commandFileStatisticsLoad(root,file);

		if (errorMessageOnFail) MsgBox.error(getOwnerWindow(),Language.tr("XML.LoadErrorTitle"),Language.tr("XML.UnknownFileFormat"));

		return false;
	}

	/**
	 * Zeigt, wenn die Sprache gerade automatisch initial eingestellt wurde,
	 * ein entsprechendes Benachrichtigungs-Panel an.
	 */
	private void languageInfo() {
		if (!setup.languageWasAutomaticallySet()) return;
		setMessagePanel("",Language.tr("Window.LanguageAutomatic"),MessagePanelIcon.INFO).setBackground(new Color(255,255,240));
		new Timer().schedule(new TimerTask() {@Override public void run() {setMessagePanel(null,null,null);}},7500);
	}

	/**
	 * Befehl: Datei - Neu
	 * @param type	Typ des neuen Modells (0..4)
	 */
	private void commandFileModelNew(final int type) {
		if (!isDiscardModelOk()) return;

		final EditModel newModel=new EditModel();

		newModel.interArrivalTimeDist=new ExponentialDistribution(60);
		newModel.batchArrival=1;
		newModel.batchWorking=1;
		newModel.agents=4;
		newModel.callsToSimulate=1000000;

		switch (type) {
		case 0:
			newModel.name="M/M/c/infty "+Language.tr("Example.Model");
			newModel.description=Language.tr("Example.ErlangC");
			newModel.workingTimeDist=new ExponentialDistribution(180);
			newModel.waitingTimeDist=new NeverDistributionImpl();
			newModel.waitingRoomSize=-1;
			newModel.callContinueProbability=0;
			newModel.retryProbability=0;
			newModel.retryTimeDist=new ExponentialDistribution(1800);
			break;
		case 1:
			newModel.name="M/M/c/K "+Language.tr("Example.Model");
			newModel.description=Language.tr("Example.ErlangC");
			newModel.workingTimeDist=new ExponentialDistribution(180);
			newModel.waitingTimeDist=new NeverDistributionImpl();
			newModel.waitingRoomSize=5;
			newModel.callContinueProbability=0;
			newModel.retryProbability=0;
			newModel.retryTimeDist=new ExponentialDistribution(1800);
			break;
		case 2:
			newModel.name="M/M/c/K+M "+Language.tr("Example.Model");
			newModel.description=Language.tr("Example.ExtErlangC");
			newModel.workingTimeDist=new ExponentialDistribution(180);
			newModel.waitingTimeDist=new ExponentialDistribution(120);
			newModel.waitingRoomSize=15;
			newModel.callContinueProbability=0;
			newModel.retryProbability=0;
			newModel.retryTimeDist=new ExponentialDistribution(1800);
			break;
		case 3:
			newModel.name="M/M/c/K+M+M+"+Language.tr("Example.Forwarding")+" "+Language.tr("Example.Model");
			newModel.description=Language.tr("Example.ModelWithRetry");
			newModel.workingTimeDist=new ExponentialDistribution(180);
			newModel.waitingTimeDist=new ExponentialDistribution(120);
			newModel.waitingRoomSize=15;
			newModel.callContinueProbability=0.2;
			newModel.retryProbability=0.75;
			newModel.retryTimeDist=new ExponentialDistribution(1800);
			break;
		case 4:
			newModel.name="M/G/c/K+G+G+"+Language.tr("Example.Forwarding")+" "+Language.tr("Example.Model");
			newModel.description=Language.tr("Example.ModelGeneral");
			newModel.workingTimeDist=new LogNormalDistributionImpl(180,30);
			newModel.waitingTimeDist=new LogNormalDistributionImpl(120,60);
			newModel.waitingRoomSize=15;
			newModel.callContinueProbability=0.2;
			newModel.retryProbability=0.75;
			newModel.retryTimeDist=new LogNormalDistributionImpl(1800,600);
			break;
		}

		editorPanel.setModel(newModel);
		setAdditionalTitle(null);
		statisticsPanel.setStatistics(null);
		for (AbstractButton button: enabledOnStatisticsAvailable) button.setEnabled(false);
		setCurrentPanel(editorPanel);
	}

	/**
	 * Befehl: Modell - Laden
	 * @param rootOptional	XML-Root-Element (kann <code>null</code> sein)
	 * @param file	Zu ladende Datei (wird <code>null</code> übergeben, so wird ein Dateiauswahldialog angezeigt)
	 * @return	Liefert <code>true</code> wenn ein Model lgeladen wurde
	 */
	private boolean commandFileModelLoad(final Element rootOptional, final File file) {
		if (!isDiscardModelOk()) return true;
		final String error;
		if (rootOptional!=null) {
			error=editorPanel.loadModel(rootOptional,file);
		} else {
			error=editorPanel.loadModel(file);
		}
		if (error==null) {
			statisticsPanel.setStatistics(null);
			for (AbstractButton button: enabledOnStatisticsAvailable) button.setEnabled(false);
			setCurrentPanel(editorPanel);
		} else {
			MsgBox.error(getOwnerWindow(),Language.tr("XML.LoadErrorTitle"),error);
		}
		if (editorPanel.getLastFile()!=null) {
			addFileToRecentlyUsedList(editorPanel.getLastFile().toString());
			setAdditionalTitle(editorPanel.getLastFile().getName());
			CommonVariables.setInitialDirectoryFromFile(editorPanel.getLastFile());
		}
		return error==null;
	}


	/**
	 * Befehl: Datei - Speichern und Speichern unter
	 * @param saveAs	Wird <code>true</code> übergeben, so wird immer nach einem neuen Dateinamen gefragt, sonst nur, wenn noch keine Name festgelegt ist
	 * @return	Liefert <code>true</code>, wenn das Modell gespeichert wurde
	 */
	private boolean commandFileModelSave(final boolean saveAs) {
		final File file=(saveAs)?null:editorPanel.getLastFile();
		final String error=editorPanel.saveModel(file);
		if (error!=null) MsgBox.error(getOwnerWindow(),Language.tr("XML.SaveErrorTitle"),error);
		if (editorPanel.getLastFile()!=null) {
			addFileToRecentlyUsedList(editorPanel.getLastFile().toString());
			setAdditionalTitle(editorPanel.getLastFile().getName());
		}
		return error==null;
	}

	/**
	 * Befehl: Datei - Kopie speichern unter
	 * @return	Liefert <code>true</code>, wenn das Modell gespeichert wurde
	 */
	private boolean commandFileModelSaveCopyAs() {
		final String error=editorPanel.saveModelCopy();
		if (error!=null) {
			MsgBox.error(getOwnerWindow(),Language.tr("XML.SaveErrorTitle"),error);
		} else {
			if (editorPanel.getLastFile()!=null) {
				addFileToRecentlyUsedList(editorPanel.getLastFile().toString());
			}
		}
		return error==null;
	}

	/**
	 * Befehl: Datei - Als Warteschlangensimulator-Modell exportieren
	 */
	private void commandFileExportQSModel() {
		final File file=ExportQSModel.selectFile(this);
		if (file==null) return;

		final ExportQSModel exporter=new ExportQSModel(editorPanel.getModel());
		if (!exporter.work(file)) {
			MsgBox.error(this,Language.tr("QSExport.Error.Title"),String.format(Language.tr("QSExport.Error.Info"),file.toString()));
		}
	}

	/**
	 * Befehl: Datei - Statistik laden
	 * @param rootOptional	XML-Root-Element (kann <code>null</code> sein)
	 * @param file	Zu ladende Datei; wird <code>null</code> übergeben, so wird ein Dateiauswahldialog angezeigt
	 * @return	Liefert <code>true</code>, wenn eine Datei geladen wurde
	 */
	private boolean commandFileStatisticsLoad(final Element rootOptional, final File file) {
		final String error;
		if (rootOptional!=null) {
			error=statisticsPanel.loadStatisticsFromXML(rootOptional);
		} else {
			error=statisticsPanel.loadStatistics(file);
		}
		if (error==null) {
			for (AbstractButton button: enabledOnStatisticsAvailable) button.setEnabled(true);
			setCurrentPanel(statisticsPanel);
		} else {
			MsgBox.error(getOwnerWindow(),Language.tr("XML.LoadErrorTitle"),error);
		}
		return error==null;
	}

	/**
	 * Befehl: Datei - Statistik speichern unter
	 * @return	Liefert <code>true</code>, wenn die Statistikdaten gespeichert wurden
	 */
	private boolean commandFileStatisticsSave() {
		String error=statisticsPanel.saveStatistics(null);
		if (error!=null) MsgBox.error(getOwnerWindow(),Language.tr("XML.SaveErrorTitle"),error);
		return error==null;
	}

	/**
	 * Befehl: Datei - Einstellungen
	 */
	private void commandFileSetup() {
		new SetupDialog(this);
		reloadSetup();
	}

	/**
	 * Befehl: Simulation - Simulation starten
	 * @param simModel	Zu simulierendes Modell (wird <code>null</code> übergeben, so wird das Modell aus dem Editor geladen)
	 * @param logFile	Logdatei (kann <code>null</code> sein)
	 * @param whenDone	Runnable, das nach Abschluss der Simulation ausgeführt werden soll
	 */
	private void commandSimulation(final EditModel simModel, final File logFile, final Runnable whenDone) {
		final EditModel editModel=(simModel==null)?editorPanel.getModel():simModel;
		final Simulator simulator=new Simulator(editModel,logFile);

		String error=simulator.prepare();
		if (error!=null) {
			MsgBox.error(getOwnerWindow(),Language.tr("Window.Simulation.ModelIsFaulty"),"<html>"+Language.tr("Window.Simulation.ErrorInitializatingSimulation")+":<br>"+error+"</html>");
			return;
		}

		simulator.start();
		enableMenuBar(false);

		waitPanel.setSimulator(simulator,()->{
			if (waitPanel.isSimulationSuccessful()) {
				statisticsPanel.setStatistics(simulator.getStatistic());
				for (AbstractButton button: enabledOnStatisticsAvailable) button.setEnabled(true);
				setCurrentPanel(statisticsPanel);
			} else {
				setCurrentPanel(editorPanel);
			}
			enableMenuBar(true);
			if (whenDone!=null) whenDone.run();
		});
		setCurrentPanel(waitPanel);
	}

	/**
	 * Befehl: Simulation - Simulation in Logdatei aufzeichnen
	 */
	private void commandSimulationLog() {
		final JFileChooser fc=new JFileChooser();
		CommonVariables.initialDirectoryToJFileChooser(fc);
		fc.setDialogTitle(Language.tr("Main.Menu.RecordSimulation.LogFile"));
		final FileFilter txt=new FileNameExtensionFilter(Language.tr("FileType.Text")+" (*.txt)","txt");
		fc.addChoosableFileFilter(txt);
		fc.setFileFilter(txt);
		fc.setAcceptAllFileFilterUsed(false);

		if (fc.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION) return;
		CommonVariables.initialDirectoryFromJFileChooser(fc);
		File file=fc.getSelectedFile();

		if (file.getName().indexOf('.')<0) {
			if (fc.getFileFilter()==txt) file=new File(file.getAbsoluteFile()+".txt");
		}

		if (file.exists()) {
			if (!MsgBox.confirmOverwrite(this,file)) return;
		}

		commandSimulation(null,file,null);
	}

	/**
	 * Befehl: (Toolbar bei Statistikansicht) - Modell zu diesen Ergebnissen
	 */
	private void commandSimulationModel() {
		final Statistics statistics=statisticsPanel.getStatistics();
		if (statistics==null) {
			MsgBox.error(getOwnerWindow(),Language.tr("Window.CannotShowModel.Title"),Language.tr("Window.CannotShowModel.Info"));
			return;
		}

		final ModelViewerFrame viewer=new ModelViewerFrame(getOwnerWindow(),statistics.editModel,null,()->{
			if (!isDiscardModelOk()) return;
			editorPanel.setModel(statistics.editModel);
			setCurrentPanel(editorPanel);
		});
		viewer.setVisible(true);
	}

	/**
	 * Befehl: Extras - Simulationergebnisse verschiedener Modelle vergleichen
	 */
	private void commandExtrasCompare() {
		CompareSelectDialog dialog=new CompareSelectDialog(getOwnerWindow(),5);
		if (dialog.getClosedBy()!=BaseDialog.CLOSED_BY_OK) return;

		File[] files=dialog.getSelectedFiles();
		Statistics[] statistics=ComparePanel.getStatisticFiles(files);
		String[] title=new String[statistics.length];
		for (int i=0;i<statistics.length;i++) {
			if (statistics[i]==null) {
				MsgBox.error(getOwnerWindow(),Language.tr("Window.Compare.NotAValidStatisticsFile.Title"),String.format(Language.tr("Window.Compare.NotAValidStatisticsFile.Info"),""+(i+1),files[i].toString()));
				return;
			}
			title[i]=statistics[i].editModel.name;
		}

		enableMenuBar(false);
		setCurrentPanel(new ComparePanel(getOwnerWindow(),statistics,title,true,()->{
			if (currentPanel instanceof ComparePanel) {
				ComparePanel comparePanel=(ComparePanel)currentPanel;
				EditModel model=comparePanel.getModelForEditor();
				if (model!=null) {
					if (!isDiscardModelOk()) return;
					editorPanel.setModel(model);
				}
			}
			setCurrentPanel(editorPanel);
			enableMenuBar(true);
		}));
	}

	/**
	 * Befehl: Extras - Aktuelles Modell für späteren Vergleich festhalten
	 */
	private void commandExtrasCompareTwoInit() {
		EditModel model=editorPanel.getModel();
		Object obj=RunModel.getRunModel(model);
		if (obj instanceof String) {
			MsgBox.error(getOwnerWindow(),Language.tr("Compare.Error.ModelError.Title"),Language.tr("Compare.Error.ModelError.CannotCompare"));
			return;
		}

		if (pinnedModel!=null) {
			if (!MsgBox.confirm(getOwnerWindow(),Language.tr("Compare.ReplaceKeptModel.Title"),Language.tr("Compare.ReplaceKeptModel.Info"),Language.tr("Compare.ReplaceKeptModel.YesInfo"),Language.tr("Compare.ReplaceKeptModel.NoInfo"))) return;
		}

		pinnedModel=model;
		MsgBox.info(getOwnerWindow(),Language.tr("Compare.Kept.Title"),Language.tr("Compare.Kept.Info"));

		menuExtrasCompareKept.setEnabled(true);
		menuModelCompareReturn.setEnabled(true);
	}

	/**
	 * Befehl Extras - Aktuelles und festgehaltenes Modell vergleichen
	 * @param level	0: festgehaltenes Modell simulieren; 1: aktuelles Modell simulieren; 2: Ergebnisse anzeigen
	 */
	private void commandExtrasCompareTwoRun(final int level) {
		if (level==0) {
			if (pinnedModel==null) {
				MsgBox.error(getOwnerWindow(),Language.tr("Compare.Error.NoModelKept.Title"),Language.tr("Compare.Error.NoModelKept.Info"));
				return;
			}

			EditModel model=editorPanel.getModel();
			Object obj=RunModel.getRunModel(model);
			if (obj instanceof String) {
				MsgBox.error(getOwnerWindow(),Language.tr("Compare.Error.ModelError.Title"),Language.tr("Compare.Error.ModelError.CannotKeep"));
				return;
			}

			if (pinnedModel.equalsEditModel(model)) {
				MsgBox.error(getOwnerWindow(),Language.tr("Compare.Error.IdenticalModels.Title"),Language.tr("Compare.Error.IdenticalModels.Info"));
				return;
			}

			commandSimulation(pinnedModel,null,()->{
				compareStatistics[0]=statisticsPanel.getStatistics();
				commandExtrasCompareTwoRun(1);
			});
			return;
		}

		if (level==1) {
			commandSimulation(null,null,()->{
				compareStatistics[1]=statisticsPanel.getStatistics();
				commandExtrasCompareTwoRun(2);
			});
			return;
		}

		if (level==2) {
			enableMenuBar(false);
			setCurrentPanel(new ComparePanel(getOwnerWindow(),compareStatistics,new String[] {Language.tr("Compare.Models.Base"),Language.tr("Compare.Models.Changed")},true,()->{
				if (currentPanel instanceof ComparePanel) {
					ComparePanel comparePanel=(ComparePanel) currentPanel;
					EditModel model=comparePanel.getModelForEditor();
					if (model!=null) {if (!isDiscardModelOk()) return; editorPanel.setModel(model);}
				}
				setCurrentPanel(editorPanel);
				enableMenuBar(true);
			}));
			return;
		}
	}

	/**
	 * Befehl: Extras - Zu festgehaltenem Modell zurückkehren
	 */
	private void commandExtrasCompareReturn() {
		if (pinnedModel==null) {
			MsgBox.error(getOwnerWindow(),Language.tr("Compare.Error.NoModelKept.Title"),Language.tr("Compare.Error.NoModelKept.Info2"));
			return;
		}

		EditModel model=editorPanel.getModel();

		if (pinnedModel.equalsEditModel(model)) {
			MsgBox.error(getOwnerWindow(),Language.tr("Compare.Error.IdenticalModels.Title"),Language.tr("Compare.Error.IdenticalModels.Info"));
			return;
		}

		if (editorPanel.isModelChanged()) {
			if (!MsgBox.confirm(getOwnerWindow(),Language.tr("Compare.ReturnConfirm.Title"),Language.tr("Compare.ReturnConfirm.Info"),Language.tr("Compare.ReturnConfirm.InfoYes"),Language.tr("Compare.ReturnConfirm.InfoNo"))) return;
		}

		editorPanel.setModel(pinnedModel);
	}

	/**
	 * Befehl: Extras - Rechner
	 * @param initialExpression	Initial anzuzeigende Eingabe (kann <code>null</code> sein)
	 */
	private void commandExtrasCalculator(final String initialExpression) {
		final CalculatorDialog dialog=new CalculatorDialog(this,initialExpression);
		dialog.setVisible(true);
	}

	/**
	 * Befehl: Extras - Warteschlangenrechner
	 */
	private void commandExtrasQueueingCalculator() {
		final QueueingCalculatorDialog dialog=new QueueingCalculatorDialog(this);
		dialog.setVisible(true);
	}

	/**
	 * Befehl: Extras - Kommandozeilenbefehl ausführen
	 */
	private void commandExtrasExecuteCommand() {
		new CommandLineDialog(this,stream->new CommandLineSystem(null,stream),window->Help.topicModal(window,"CommandLineDialog"));
	}

	/**
	 * Befehl: Hilfe - Hilfe
	 */
	private void commandHelpHelp() {
		if (currentPanel==editorPanel) {Help.topic(this,Help.pageEditor); return;}
		if (currentPanel==statisticsPanel) {Help.topic(this,Help.pageStatistics); return;}
		Help.topic(this,"");
	}

	/**
	 * Befehl: Hilfe - Hilfe-Inhalt
	 */
	private void commandHelpContent() {
		Help.topic(this,"");
	}

	/**
	 * Befehl: Hilfe - Lehrbuch "Callcenter - Analyse &amp; Management"
	 */
	private void commandHelpBook() {
		try {
			final URI uri=new URI("https://www.springer.com/de/book/9783658183080");
			if (!MsgBox.confirmOpenURL(this,uri)) return;
			Desktop.getDesktop().browse(uri);
		} catch (IOException | URISyntaxException e) {
			MsgBox.error(this,Language.tr("Window.Info.NoInternetConnection"),String.format(Language.tr("Window.Info.NoInternetConnection.Address"),"https://www.springer.com/de/book/9783658183080"));
		}
	}

	/**
	 * Befehl: Hilfe - Unterstützung &amp; Support
	 */
	private void commandHelpSupport() {
		try {
			Desktop.getDesktop().mail(new URI("mailto:"+MainPanel.AUTHOR_EMAIL));
		} catch (IOException | URISyntaxException e1) {
			MsgBox.error(getOwnerWindow(),Language.tr("Window.Info.NoEMailProgram.Title"),String.format(Language.tr("Window.Info.NoEMailProgram.Info"),"mailto:"+MainPanel.AUTHOR_EMAIL));
		}
		return;
	}

	/**
	 * Befehl: Hilfe - Homepage
	 */
	private void commandHelpHomepage() {
		try {
			final URI uri=new URI("https://"+WEB_URL);
			if (!MsgBox.confirmOpenURL(this,uri)) return;
			Desktop.getDesktop().browse(uri);
		} catch (IOException | URISyntaxException e) {
			MsgBox.error(this,Language.tr("Window.Info.NoInternetConnection"),String.format(Language.tr("Window.Info.NoInternetConnection.Address"),"https://github.com/A-Herzog/Mini-Callcenter-Simulator"));
		}
	}

	/**
	 * Befehl: Hilfe - Skripte für Erlang-Formeln
	 */
	private void commandHelpErlangScripts() {
		final File folder=new File(SetupData.getProgramFolder(),"tools");
		try {
			Desktop.getDesktop().open(folder);
		} catch (Exception e) {
			MsgBox.error(this,Language.tr("Window.Info.FolderError"),String.format(Language.tr("Window.Info.FolderError.Location"),folder.toString()));
		}
	}

	/**
	 * Befehl: Hilfe - Lizenzinformationen
	 */
	private void commandHelpLicenseInfo() {
		new LicenseViewer(this);
	}

	/**
	 * Befehl: Hilfe - Programminformation
	 */
	private void commandHelpInfo() {
		MsgBox.info(
				this,
				Language.tr("InfoDialog.Title"),
				"<html><b>"+programName+"</b><br>"+Language.tr("InfoDialog.Version")+" "+EditModel.systemVersion+"<br>"+Language.tr("InfoDialog.WrittenBy")+" "+AUTHOR+"</html>"
				);
	}

	/**
	 * Befehl: (Toolbar) - Erklärung
	 */
	private void commandHelpPageInfo() {
		buttonPageInfo.setSelected(!buttonPageInfo.isSelected());
		editorPanel.setInfoPanelVisible(buttonPageInfo.isSelected());
	}

	@Override
	protected void action(final Object sender) {
		/* Datei - Letzte Dokumente */
		final Component[] sub=menuFileModelRecentlyUsed.getMenuComponents();
		for (int i=0;i<sub.length;i++) if (sender==sub[i]) {commandFileModelLoad(null,new File(setup.lastFiles[i])); return;}
	}

	/**
	 * Über diese Methode kann dem Panal ein Callback mitgeteilt werden,
	 * das aufgerufen wird, wenn das Fenster neu geladen werden soll.
	 * @param reloadWindow	Callback, welches ein Neuladen des Fensters veranlasst.
	 */
	public void setReloadWindow(final Runnable reloadWindow) {
		this.reloadWindow=reloadWindow;
	}

	/**
	 * Wird nach dem Verändern des Setups aufgerufen, um die neuen
	 * Einstellungen in das Programmfenster zu übernehmen.
	 * @see #commandFileSetup()
	 */
	private void reloadSetup() {
		/* Sprache neu laden? */
		if (!setup.language.equals(Language.getCurrentLanguage())) {
			setup.resetLanguageWasAutomatically();
			HelpBase.hideHelpFrame();
			if (reloadWindow!=null) SwingUtilities.invokeLater(reloadWindow);
		} else {
			invalidate();
			if (reloadWindow!=null) SwingUtilities.invokeLater(()->repaint());
		}

		/* "Öffnen"-Buttons in Statistik */
		StatisticsBasePanel.viewerPrograms.clear();
		if (setup.openWord) StatisticsBasePanel.viewerPrograms.add(StatisticsBasePanel.ViewerPrograms.WORD);
		if (setup.openODT) StatisticsBasePanel.viewerPrograms.add(StatisticsBasePanel.ViewerPrograms.ODT);
		if (setup.openExcel) StatisticsBasePanel.viewerPrograms.add(StatisticsBasePanel.ViewerPrograms.EXCEL);
		if (setup.openODS) StatisticsBasePanel.viewerPrograms.add(StatisticsBasePanel.ViewerPrograms.ODS);
		if (setup.openPDF) StatisticsBasePanel.viewerPrograms.add(StatisticsBasePanel.ViewerPrograms.PDF);
	}

	/**
	 * Liefert alle Daten innerhalb dieses Panels als Objekt-Array
	 * um dann das Panel neu laden und die Daten wiederherstellen
	 * zu können.
	 * @return	5-elementiges Objekt-Array mit allen Daten des Panels
	 * @see #setAllData(Object[])
	 */
	public Object[] getAllData() {
		return new Object[]{
				editorPanel.getModel(),
				editorPanel.isModelChanged(),
				editorPanel.getLastFile(),
				statisticsPanel.getStatistics(),
				Integer.valueOf((currentPanel==statisticsPanel)?1:0)
		};
	}

	/**
	 * Reinitialisiert die Daten in dem Panel wieder aus einem
	 * zuvor erstellten Objekt-Array.
	 * @param data	5-elementiges Objekt-Array mit allen Daten des Panels
	 * @return	Gibt an, ob die Daten aus dem Array erfolgreich geladen werden konnten
	 * @see #getAllData()
	 */
	public boolean setAllData(Object[] data) {
		if (data==null || data.length!=5) return false;
		if (!(data[0] instanceof EditModel)) return false;
		if (!(data[1] instanceof Boolean)) return false;

		if (data[2]!=null && !(data[2] instanceof File)) return false;
		if (data[3]!=null && !(data[3] instanceof Statistics)) return false;
		if (data[4]==null || !(data[4] instanceof Integer)) return false;

		editorPanel.setModel((EditModel)data[0]);
		editorPanel.setModelChanged((Boolean)data[1]);
		editorPanel.setLastFile((File)data[2]); if (data[2]!=null) setAdditionalTitle(((File)data[3]).getName());
		statisticsPanel.setStatistics((Statistics)data[3]);
		if ((Integer)data[4]==1) setCurrentPanel(statisticsPanel); else setCurrentPanel(editorPanel);

		return true;
	}
}