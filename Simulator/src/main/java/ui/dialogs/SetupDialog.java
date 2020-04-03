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
package ui.dialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import language.Language;
import mathtools.NumberTools;
import systemtools.BaseDialog;
import systemtools.MsgBox;
import tools.IconListCellRenderer;
import tools.SetupData;
import tools.SetupData.StartMode;
import ui.help.Help;
import ui.images.Images;

/**
 * Zeigt den Setup-Dialog unter Verwendung der Daten aus <code>SetupData</code> an.
 * Alle Verarbeitung erfolgt direkt in dem Dialog. Der Dialog muss vom Aufrufer nur per Konstruktor
 * erstellt werden; das Daten Laden, Pr�fen und Speichern �bernimmt der Dialog selbst.
 * @see SetupData
 * @author Alexander Herzog
 */
public class SetupDialog extends BaseDialog {
	private static final long serialVersionUID = 8167759839522880144L;

	private final JComboBox<String> languages;
	private final JComboBox<String> programStartWindow;
	private final JTextField imageSize;
	private final JCheckBox openWord;
	private final JCheckBox openODT;
	private final JCheckBox openExcel;
	private final JCheckBox openODS;

	/**
	 * Konstruktor der Klasse
	 * @param owner	�bergeordnetes Element
	 */
	public SetupDialog(Component owner) {
		super(owner,Language.tr("SettingsDialog.Title"));

		final JPanel content=createGUI(()->Help.topicModal(SetupDialog.this,Help.pageSetup));
		content.setLayout(new BorderLayout());
		final JTabbedPane tabs=new JTabbedPane();
		content.add(tabs,BorderLayout.CENTER);

		/* Dialogseiten einrichten */

		JPanel tab, mainarea, p;
		JLabel label;

		tabs.addTab(Language.tr("SettingsDialog.Tabs.GUI.ProgramStart"),tab=new JPanel(new FlowLayout(FlowLayout.LEFT))); tab.add(mainarea=new JPanel());
		mainarea.setLayout(new BoxLayout(mainarea,BoxLayout.Y_AXIS));

		mainarea.add(p=new JPanel(new FlowLayout(FlowLayout.LEFT)));
		p.add(label=new JLabel(Language.tr("SettingsDialog.Languages")+":"));
		mainarea.add(p=new JPanel(new FlowLayout(FlowLayout.LEFT)));
		p.add(languages=new JComboBox<String>(new String[]{Language.tr("SettingsDialog.Languages.English"),Language.tr("SettingsDialog.Languages.German")}));
		languages.setRenderer(new IconListCellRenderer(new Images[]{Images.LANGUAGE_EN,Images.LANGUAGE_DE}));
		languages.setToolTipText(Language.tr("SettingsDialog.Languages.Info"));
		label.setLabelFor(languages);

		mainarea.add(p=new JPanel(new FlowLayout(FlowLayout.LEFT)));
		p.add(new JLabel(Language.tr("SettingsDialog.WindowSizeProgrmStart")+":"));
		mainarea.add(p=new JPanel(new FlowLayout(FlowLayout.LEFT)));
		p.add(programStartWindow=new JComboBox<String>(new String[]{
				Language.tr("SettingsDialog.WindowSizeProgrmStart.Normal"),
				Language.tr("SettingsDialog.WindowSizeProgrmStart.FullScreen"),
				Language.tr("SettingsDialog.WindowSizeProgrmStart.LastSize")
		}));
		programStartWindow.setRenderer(new IconListCellRenderer(new Images[]{
				Images.SETUP_WINDOW_SIZE_DEFAULT,
				Images.SETUP_WINDOW_SIZE_FULL,
				Images.SETUP_WINDOW_SIZE_LAST
		}));

		tabs.add(Language.tr("SettingsDialog.Tabs.GUI.Graphics"),tab=new JPanel(new FlowLayout(FlowLayout.LEFT))); tab.add(mainarea=new JPanel());
		mainarea.setLayout(new BoxLayout(mainarea,BoxLayout.Y_AXIS));

		mainarea.add(p=new JPanel(new FlowLayout(FlowLayout.LEFT)));
		p.add(new JLabel(Language.tr("SettingsDialog.ImageResolution")+":"));
		mainarea.add(p=new JPanel(new FlowLayout(FlowLayout.LEFT)));
		p.add(imageSize=new JTextField(5));
		imageSize.addKeyListener(new KeyListener() {
			@Override public void keyTyped(KeyEvent e) {NumberTools.getPositiveLong(imageSize,true);}
			@Override public void keyReleased(KeyEvent e) {NumberTools.getPositiveLong(imageSize,true);}
			@Override public void keyPressed(KeyEvent e) {NumberTools.getPositiveLong(imageSize,true);}
		});

		tabs.add(Language.tr("SettingsDialog.Tabs.GUI.Statistics"),tab=new JPanel(new FlowLayout(FlowLayout.LEFT))); tab.add(mainarea=new JPanel());
		mainarea.setLayout(new BoxLayout(mainarea,BoxLayout.Y_AXIS));

		mainarea.add(p=new JPanel(new FlowLayout(FlowLayout.LEFT)));
		p.add(openWord=new JCheckBox(Language.tr("SettingsDialog.Tabs.Statistics.OpenWord")));

		mainarea.add(p=new JPanel(new FlowLayout(FlowLayout.LEFT)));
		p.add(openODT=new JCheckBox(Language.tr("SettingsDialog.Tabs.Statistics.OpenODT")));

		mainarea.add(p=new JPanel(new FlowLayout(FlowLayout.LEFT)));
		p.add(openExcel=new JCheckBox(Language.tr("SettingsDialog.Tabs.Statistics.OpenExcel")));

		mainarea.add(p=new JPanel(new FlowLayout(FlowLayout.LEFT)));
		p.add(openODS=new JCheckBox(Language.tr("SettingsDialog.Tabs.Statistics.OpenODS")));

		/* Icons auf den Tabreitern einf�gen */

		tabs.setIconAt(0,Images.SETUP_PAGE_APPLICATION.getIcon());
		tabs.setIconAt(1,Images.SETUP_PAGE_IMPORT_EXPORT.getIcon());
		tabs.setIconAt(2,Images.SETUP_PAGE_STATISTICS.getIcon());

		/* Daten in den Dialog laden */

		final SetupData setup=SetupData.getSetup();

		if (setup.language==null || setup.language.isEmpty() || setup.language.equalsIgnoreCase("de")) languages.setSelectedIndex(1); else languages.setSelectedIndex(0);

		switch (setup.startSizeMode) {
		case START_MODE_DEFAULT: programStartWindow.setSelectedIndex(0); break;
		case START_MODE_FULLSCREEN: programStartWindow.setSelectedIndex(1); break;
		case START_MODE_LASTSIZE: programStartWindow.setSelectedIndex(2); break;
		}

		imageSize.setText(""+Math.min(5000,Math.max(50,setup.imageSize)));

		openWord.setSelected(setup.openWord);
		openODT.setSelected(setup.openODT);
		openExcel.setSelected(setup.openExcel);
		openODS.setSelected(setup.openODS);

		/* Dialog anzeigen */

		setMinSizeRespectingScreensize(400,0);
		pack();
		setLocationRelativeTo(owner);
		setVisible(true);
	}

	@Override
	protected boolean checkData() {
		Long L=NumberTools.getPositiveLong(imageSize,true);
		if (L==null || L<50 || L>5000) {
			MsgBox.error(owner,Language.tr("SettingsDialog.ImageResolution.Invalid.Title"),Language.tr("SettingsDialog.ImageResolution.Invalid.Info"));
			return false;
		}
		return true;
	}

	@Override
	protected void storeData() {
		final SetupData setup=SetupData.getSetup();

		setup.language=(languages.getSelectedIndex()==1)?"de":"en";

		switch (programStartWindow.getSelectedIndex()) {
		case 0: setup.startSizeMode=StartMode.START_MODE_DEFAULT; break;
		case 1: setup.startSizeMode=StartMode.START_MODE_FULLSCREEN; break;
		case 2: setup.startSizeMode=StartMode.START_MODE_LASTSIZE; break;
		}

		Long L=NumberTools.getPositiveLong(imageSize,true);
		if (L!=null) setup.imageSize=(int)((long)L);

		setup.openWord=openWord.isSelected();
		setup.openODT=openODT.isSelected();
		setup.openExcel=openExcel.isSelected();
		setup.openODS=openODS.isSelected();

		setup.saveSetupWithWarning(this);
	}
}