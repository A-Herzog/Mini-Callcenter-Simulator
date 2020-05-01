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
package ui.images;

import java.net.URL;

import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 * Diese Enumerations-Klasse hält die Icons für Toolbars und Menüs vor.
 * @author Alexander Herzog
 */
public enum Images {
	/* Allgemeine Icons */

	/** Symbol "Einstellungen" (Programm-Setup) */
	GENERAL_SETUP("wrench.png"),

	/** Symbol "Ende" */
	GENERAL_EXIT("door_in.png"),

	/** Symbol "Information" */
	GENERAL_INFO("information.png"),

	/** Symbol "Abbruch" */
	GENERAL_CANCEL("cancel.png"),

	/** Symbol "Dialog-Button 'Ok'" */
	MSGBOX_OK("accept.png"),

	/** Symbol "Dialog-Button 'Ja'" */
	MSGBOX_YES("tick.png"),

	/** Symbol "Dialog-Button 'Ja, speichern'" */
	MSGBOX_YES_SAVE("disk.png"),

	/** Symbol "Dialog-Button 'Nein'" */
	MSGBOX_NO("cancel.png"),

	/** Symbol "Dialog-Button 'Abbruch/Zurück'" */
	MSGBOX_CANCEL("arrow_redo2.png"),

	/* Modell */

	/** Symbol "Modell" */
	MODEL("brick.png"),

	/** Symbol "Modell - neu" */
	MODEL_NEW("brick_add.png"),

	/** Symbol "Modell - laden" */
	MODEL_LOAD("brick_go.png"),

	/** Symbol "Modell - speichern" */
	MODEL_SAVE("disk.png"),

	/* Vergleichen */

	/** Symbol "Vergleichen - mehrere Statistikdaten" */
	MODEL_COMPARE("application_tile_horizontal.png"),

	/** Symbol "Vergleichen - Modell festhalten" */
	MODEL_COMPARE_KEEP("basket_put.png"),

	/** Symbol "Vergleichen - festgehaltenes und aktuelles Modell vergleichen" */
	MODEL_COMPARE_COMPARE("basket_go.png"),

	/** Symbol "Vergleichen - Zu festgehaltenem Modell zurückkehren" */
	MODEL_COMPARE_GO_BACK("basket_remove.png"),

	/* Modell-Editor */

	/** Symbol für Modell-Editor "Modelldarstellung" */
	MODEL_EDITOR_MODEL("Symbol.png"),
	/** Symbol für Modell-Editor "Allgemein" */
	MODEL_EDITOR_GENERAL("brick.png"),
	/** Symbol für Modell-Editor "Ankünfte" */
	MODEL_EDITOR_ARRIVALS("user.png"),
	/** Symbol für Modell-Editor "Wartezeittoleranz" */
	MODEL_EDITOR_WAITING("door_in.png"),
	/** Symbol für Modell-Editor "Agenten" */
	MODEL_EDITOR_SERVICE("group.png"),
	/** Symbol für Modell-Editor "Nachbearbeitung" */
	MODEL_EDITOR_POST_PROCESSING("server_go.png"),
	/** Symbol für Modell-Editor "Wiederholungen" */
	MODEL_EDITOR_RETRY("arrow_redo2.png"),

	/* Statistik */

	/** Symbol "Statistik" */
	STATISTICS("sum.png"),

	/** Symbol "Statistik" (dunkler) */
	STATISTICS_DARK("sum2.png"),

	/** Symbol "Statistik - laden" */
	STATISTICS_LOAD("icon_package_open.gif"),

	/** Symbol "Statistik - speichern" */
	STATISTICS_SAVE("icon_package_get.gif"),

	/** Symbol "Statistik - Modell in Editor laden */
	STATISTICS_SHOW_MODEL("brick.png"),

	/* Simulation */

	/** Symbol "Simulation" */
	SIMULATION("action_go.gif"),

	/** Symbol "Simulation - in Logfile aufzeichnen" */
	SIMULATION_LOG("Text.gif"),

	/* Extras */

	/** Symbol "Kommandozeile" */
	EXTRAS_COMMANDLINE("application_xp_terminal.png"),

	/** Symbol "Warteschlangenrechner" */
	EXTRAS_QUEUE("Symbol.png"),

	/** Symbol "Warteschlangenrechner (Tab-Icons)" */
	EXTRAS_QUEUE_FUNCTION("fx.png"),

	/* Hilfe */

	/** Symbol "Hilfe" */
	HELP("help.png"),

	/** Symbol "Hilfeinhalt" */
	HELP_CONTENT("book_open.png"),

	/** Symbol "Programminformation" */
	HELP_INFORMATION("information.png"),

	/** Symbol "Lehrbuch" */
	HELP_BOOK("book.png"),

	/** Symbol "E-Mail" */
	HELP_EMAIL("icon_mail.gif"),

	/** Symbol "Homepage" */
	HELP_HOMEPAGE("world.png"),

	/* Sprache */

	/** Symbol "Sprache - Englisch" */
	LANGUAGE_EN("flag_gb.png"),

	/** Symbol "Sprache - Deutsch" */
	LANGUAGE_DE("flag_de.png"),

	/* Einstellungen */

	/** Symbol im Einstellungendialog "Programmoberfläche" */
	SETUP_PAGE_APPLICATION("application_go.png"),

	/** Symbol im Einstellungendialog "Export" */
	SETUP_PAGE_IMPORT_EXPORT("image.gif"),

	/** Symbol im Einstellungendialog "Statistik" */
	SETUP_PAGE_STATISTICS("sum2.png"),

	/** Symbol im Einstellungendialog "Fenstergröße - Vorgabe" */
	SETUP_WINDOW_SIZE_DEFAULT("application_double.png"),

	/** Symbol im Einstellungendialog "Fenstergröße - Vollbild" */
	SETUP_WINDOW_SIZE_FULL("application.png"),

	/** Symbol im Einstellungendialog "Fenstergröße - Letzte wiederherstellen" */
	SETUP_WINDOW_SIZE_LAST("application_go.png");

	private final String name;
	private URL url;
	private Icon icon;

	Images(final String name) {
		this.name=name;
	}

	/**
	 * Liefert die URL des Icons
	 * @return	URL des Icons
	 */
	public URL getURL() {
		if (url==null) url=getClass().getResource("res/"+name);
		assert(url!=null);
		return url;
	}

	/**
	 * Liefert das Icon
	 * @return	Icon
	 */
	public Icon getIcon() {
		if (icon==null) {
			final URL url=getURL();
			if (url!=null) icon=new ImageIcon(url);
		}
		assert(icon!=null);
		return icon;
	}

	/**
	 * Prüft, ob alle Icons vorhanden sind.
	 */
	public static void checkAll() {
		for (Images image: values()) {
			System.out.print(image.name+": ");
			if (image.getIcon()==null) System.out.println("missing"); else System.out.println("ok");
		}
	}
}
