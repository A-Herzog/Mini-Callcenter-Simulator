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

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Toolkit;
import java.io.File;
import java.io.Serializable;

import language.Language;
import language.LanguageStaticLoader;
import language.Messages_Java11;
import mathtools.Table;
import systemtools.MainFrameBase;
import systemtools.MsgBox;
import tools.SetupData;
import tools.SetupData.StartMode;

/**
 * Diese Klasse stellt das Programmfenster des Simulators dar.
 * @see MainFrameBase
 * @author Alexander Herzog
 */
public class MainFrame extends MainFrameBase {
	/**
	 * Serialisierungs-ID der Klasse
	 * @see Serializable
	 */
	private static final long serialVersionUID = -2208131980436341851L;

	/**
	 * Programmname
	 */
	public static final String PROGRAM_NAME="Mini Callcenter Simulator";

	static {
		Table.ExportTitle=PROGRAM_NAME;
	}

	/**
	 * Konstruktor der Klasse <code>SimulatorFrame</code>
	 * @param loadFile	Datei, die beim Start geladen werden soll. Wird <code>null</code> �bergeben, so wird nichts weiter geladen.
	 */
	public MainFrame(File loadFile) {
		super(PROGRAM_NAME,loadFile);
		setMainPanel(new MainPanel(this,PROGRAM_NAME,false));
		((MainPanel)getMainPanel()).setReloadWindow(new ReloadWindow());
		setIcon(MainFrame.class.getResource("res/Symbol.png"));
		setVisible(true);
	}

	@Override
	protected void loadWindowSize() {
		setSize(800,600);
		setMinimumSize(getSize());
		setLocationRelativeTo(null);

		final Dimension screenSize=Toolkit.getDefaultToolkit().getScreenSize();
		final SetupData setup=SetupData.getSetup();
		switch (setup.startSizeMode) {
		case START_MODE_FULLSCREEN:
			setExtendedState(Frame.MAXIMIZED_BOTH);
			break;
		case START_MODE_LASTSIZE:
			setExtendedState(setup.lastSizeMode);
			if (setup.lastSizeMode==Frame.NORMAL) {
				final Dimension minSize=getMinimumSize();
				Dimension d=setup.lastSize;
				if (d.width<minSize.width) d.width=minSize.width;
				if (d.width>screenSize.width) d.width=screenSize.width;
				if (d.height<minSize.height) d.height=minSize.height;
				if (d.height>screenSize.height) d.height=screenSize.height;
				setSize(d);

				Point point=setup.lastPosition;
				if (point.x<0) point.x=0;
				if (point.x>=screenSize.width-50) point.x=screenSize.width-50;
				if (point.y<0) point.y=0;
				if (point.y>=screenSize.height-50) point.y=screenSize.height-50;
				setLocation(point);
			}
			break;
		case START_MODE_DEFAULT:
			/* Keine Verarbeitung n�tig. */
			break;
		}
	}

	@Override
	protected void saveWindowSize() {
		final SetupData setup=SetupData.getSetup();
		if (setup.startSizeMode!=StartMode.START_MODE_LASTSIZE) return;
		setup.lastSizeMode=getExtendedState();
		setup.lastPosition=getLocation();
		setup.lastSize=getSize();
		setup.saveSetupWithWarning(this);
	}

	@Override
	protected void logException(final String info) {
		SetupData setup=SetupData.getSetup();
		setup.lastError=info;
		setup.saveSetup();
	}

	/**
	 * Wird �ber {@link MainPanel#setReloadWindow(Runnable)} aufgerufen, wenn
	 * das Fenster als solches neu geladen werden muss.
	 */
	private class ReloadWindow implements Runnable {
		/**
		 * Konstruktor der Klasse
		 */
		public ReloadWindow() {
			/*
			 * Wird nur ben�tigt, um einen JavaDoc-Kommentar f�r diesen (impliziten) Konstruktor
			 * setzen zu k�nnen, damit der JavaDoc-Compiler keine Warnung mehr ausgibt.
			 */
		}

		@Override
		public void run() {
			if (!(getMainPanel() instanceof MainPanel)) return;

			final Object[] store=((MainPanel)getMainPanel()).getAllData();

			Language.init(SetupData.getSetup().language);
			LanguageStaticLoader.setLanguage();
			if (Messages_Java11.isFixNeeded()) Messages_Java11.setupMissingSwingMessages();

			final MainPanel newMainPanel=new MainPanel(MainFrame.this,PROGRAM_NAME,true);
			setMainPanel(newMainPanel);
			newMainPanel.setReloadWindow(new ReloadWindow());
			newMainPanel.setAllData(store);
		}
	}

	@Override
	protected boolean exitProgramOnCloseWindow() {
		final SetupData setup=SetupData.getSetup();
		final File setupFile=new File(SetupData.getSetupFolder(),SetupData.SETUP_FILE_NAME);

		boolean b=setup.isLastFileSaveSuccessful();
		while (!b) {
			b=setup.saveSetup();
			if (!b) {
				if (!MsgBox.confirm(this,Language.tr("SetupFailure.Title"),String.format(Language.tr("SetupFailure.Info"),setupFile.toString()),Language.tr("SetupFailure.Retry"),Language.tr("SetupFailure.Discard"))) break;
			}
		}

		return true;
	}
}