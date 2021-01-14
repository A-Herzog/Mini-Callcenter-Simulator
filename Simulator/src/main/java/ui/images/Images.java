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

import java.awt.Image;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;
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

	/** Symbol "Aus" */
	GENERAL_OFF("cross.png"),

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

	/** Symbol "Zoom" (allgemein) */
	ZOOM("zoom.png"),

	/** Symbol "Bearbeiten - Kopieren" */
	COPY("page-copy.png"),

	/* Modell */

	/** Symbol "Modell" */
	MODEL("brick.png"),

	/** Symbol "Modell - neu" */
	MODEL_NEW("brick_add.png"),

	/** Symbol "Modell - laden" */
	MODEL_LOAD("brick_go.png"),

	/** Symbol "Modell - speichern" */
	MODEL_SAVE("disk.png"),

	/** Symbol "Drucken" */
	MODEL_PRINT("printer.png"),

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
	SIMULATION_LOG("Text.png"),

	/* Extras */

	/** Symbol "Kommandozeile" */
	EXTRAS_COMMANDLINE("application_xp_terminal.png"),

	/** Symbol "Rechner" */
	EXTRAS_CALCULATOR("calculator.png"),

	/** Symbol "Rechner - Funktionsplotter" */
	EXTRAS_CALCULATOR_PLOTTER("chart_curve.png"),

	/** Symbol "Rechner - Wahrscheinlichkeitsverteilungen" */
	EXTRAS_CALCULATOR_DISTRIBUTION("chart_curve.png"),

	/** Symbol "Rechner - Funktionsplotter - Funktion löschen" */
	EXTRAS_CALCULATOR_PLOTTER_CLEAR("chart_curve_delete.png"),

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
	HELP_EMAIL("email.png"),

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
	SETUP_PAGE_IMPORT_EXPORT("image.png"),

	/** Symbol im Einstellungendialog "Simulation" */
	SETUP_PAGE_SIMULATION("action_go.gif"),

	/** Symbol im Einstellungendialog "Statistik" */
	SETUP_PAGE_STATISTICS("sum2.png"),

	/** Symbol im Einstellungendialog "Fenstergröße - Vorgabe" */
	SETUP_WINDOW_SIZE_DEFAULT("application_double.png"),

	/** Symbol im Einstellungendialog "Fenstergröße - Vollbild" */
	SETUP_WINDOW_SIZE_FULL("application.png"),

	/** Symbol im Einstellungendialog "Fenstergröße - Letzte wiederherstellen" */
	SETUP_WINDOW_SIZE_LAST("application_go.png");

	/**
	 * Dateiname des Icons
	 */
	private final String name;

	/**
	 * URLs des Icons
	 */
	private URL[] urls;

	/**
	 * Bild
	 */
	private Image image;

	/**
	 * Icon
	 */
	private Icon icon;

	/**
	 * Konstruktor des Enum
	 * @param name	Dateiname des Icons
	 */
	Images(final String name) {
		this.name=name;
	}

	/**
	 * Sucht ein Bild in einem Ordner und fügt es, wenn gefunden, zu einer Liste hinzu.
	 * @param list	Liste mit URLs zu der die neue URL hinzugefügt werden soll
	 * @param folder	Ordner in dem das Bild gesucht werden soll
	 * @param name	Name des Bildes
	 */
	private void addURL(final List<URL> list, final String folder, final String name) {
		URL url;

		url=getClass().getResource(folder+"/"+name);
		if (url!=null) {
			list.add(url);
		} else {
			url=getClass().getResource(folder+"/"+name.replace('_','-'));
			if (url!=null) list.add(url);
		}
	}

	/**
	 * Liefert die URL des Icons
	 * @return	URL des Icons
	 */
	public URL[] getURLs() {
		if (urls==null) {
			List<URL> list=new ArrayList<>();
			addURL(list,"res",name);
			addURL(list,"res24",name);
			addURL(list,"res32",name);
			addURL(list,"res48",name);
			urls=list.toArray(new URL[0]);
		}
		assert(urls!=null);
		return urls;
	}

	/**
	 * Wird das Programm unter Java 9 oder höher ausgeführt, so wird
	 * der Konstruktor der Multi-Resolution-Bild-Objektes geliefert, sonst <code>null</code>.
	 * @return	Multi-Resolution-Bild-Konstruktor oder <code>null</code>
	 */
	@SuppressWarnings("unchecked")
	private static Constructor<Object> getMultiImageConstructor() {
		try {
			final Class<?> cls=Class.forName("java.awt.image.BaseMultiResolutionImage");
			return (Constructor<Object>)cls.getDeclaredConstructor(int.class,Image[].class);
		} catch (ClassNotFoundException | NoSuchMethodException | SecurityException e) {
			return null;
		}
	}

	/**
	 * Liefert das Icon.
	 * @return	Icon
	 */
	public Icon getIcon() {
		if (icon==null) {
			final Image image=getImage();
			if (image!=null) icon=new ImageIcon(image);
		}
		assert(icon!=null);
		return icon;
	}

	/**
	 * Liefert basierend auf einer oder mehreren URLs das Standardbild (das Bild für die erste URL)
	 * @param urls	Liste mit URLs
	 * @return	Bild für die erste URL
	 */
	private Image getDefaultImage(final URL[] urls) {
		if (urls==null || urls.length==0) return null;
		try {
			return ImageIO.read(urls[0]);
		} catch (IOException e) {
			assert(false);
			return null;
		}
	}

	/**
	 * Liefert das Bild.
	 * @return	Bild
	 */
	public Image getImage() {
		if (image!=null) return image;

		final URL[] urls=getURLs();
		assert(urls.length>0);

		if (urls.length==1) return image=getDefaultImage(urls);

		final Constructor<Object> multiConstructor=getMultiImageConstructor();
		if (multiConstructor==null) return image=getDefaultImage(urls);

		final Image[] images=Arrays.asList(urls).stream().map(url->{
			try {
				return ImageIO.read(url);
			} catch (IOException e) {
				return image=getDefaultImage(urls);
			}
		}).toArray(Image[]::new);

		try {
			image=(Image)multiConstructor.newInstance(0,images);
			assert(image!=null);
			return image;
		} catch (InstantiationException|IllegalAccessException|IllegalArgumentException|InvocationTargetException e) {
			return image=getDefaultImage(urls);
		}
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
