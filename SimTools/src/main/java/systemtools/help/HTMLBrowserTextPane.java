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
package systemtools.help;

import java.awt.Cursor;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JTextPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLFrameHyperlinkEvent;

import systemtools.JScrollPaneTouch;

/**
 * Kapselt einen Webbrowser auf Basis eines <code>JTextPane</code>-Elements
 * Diese Klasse kann nur innerhalb dieses Package verwendet werden.
 * @author Alexander Herzog
 * @see HTMLBrowserPanel
 * @version 1.1
 */
public class HTMLBrowserTextPane extends JTextPane implements HTMLBrowserPanel {
	/**
	 * Serialisierungs-ID der Klasse
	 * @see Serializable
	 */
	private static final long serialVersionUID = 6093964682884036080L;

	/** Callback, das aufgerufen wird, wenn ein Link angeklickt wird */
	private Runnable linkClickListener;

	/** Runnable, das aufgerufen wird, wenn eine Seite geladen wurde */
	private Runnable pageLoadListener;

	/** Optionaler Pr�prozessor der geladenen Seiten */
	private Consumer<Element> pagePreProcessor;

	/**
	 * �berschriften und Positionen dieser auf der Seite
	 * @see #scanElement(Element)
	 * @see #getPageContent()
	 * @see #getPageContentLevel()
	 * @see #scrollToPageContent(int)
	 */
	private List<ElementPos> pageContentList;

	/**
	 * URL des Links, der zuletzt angeklickt wurde.
	 * @see #getLastClickedURL()
	 */
	private URL lastClickedURL;

	/**
	 * Inhalt des Linkziels des zuletzt angeklickten Links als Text.
	 * @see #getLastClickedURLDescription()
	 */
	private String lastClickedURLDescription;

	/**
	 * Konstruktor der Klasse <code>HTMLBrowserTextPane</code>
	 */
	public HTMLBrowserTextPane() {
		lastClickedURL=null;
		lastClickedURLDescription="";
		setEditable(false);
		addHyperlinkListener(new LinkListener());
		addPropertyChangeListener("page",new PageLoadListener());
		putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES,Boolean.TRUE);
	}

	@Override
	public void init(final Runnable linkClickListener, final Consumer<Element> pagePreProcessor, final Runnable pageLoadListener) {
		this.linkClickListener=linkClickListener;
		this.pagePreProcessor=pagePreProcessor;
		this.pageLoadListener=pageLoadListener;
	}

	@Override
	public JComponent asScrollableJComponent() {
		return new JScrollPaneTouch(this);
	}

	@Override
	public JComponent asInnerJComponent() {
		return this;
	}

	@Override
	public final boolean showPage(final URL url) {
		try {setPage(url);} catch (IOException e) {return false;}

		/*
		Funktioniert leider nicht, wenn in jar-Datei. Daher muss �berall das BOM weg.
		try {
			String text=new String(Files.readAllBytes(Paths.get(url.toURI())),Charsets.UTF_8);
			if (text.length()>1 && text.charAt(0)==65279) text=text.substring(1);
			int index=text.toLowerCase().indexOf("<meta");
			while (index>=0) {
				String textNeu=(index==0)?"":text.substring(0,index);
				text=text.substring(index);
				final int index2=text.indexOf(">");
				if (index2<0) break;
				text=textNeu+text.substring(index2+1);
				index=text.toLowerCase().indexOf("<meta");
			}
			setEditorKit(new HTMLEditorKit());
			read(new ByteArrayInputStream(text.getBytes()),url);
		} catch (IOException | URISyntaxException e) {
			return false;
		}
		 */

		return true;
	}

	@Override
	public final URL getLastClickedURL() {
		return lastClickedURL;
	}

	@Override
	public final String getLastClickedURLDescription() {
		return lastClickedURLDescription;
	}

	/**
	 * html-Element innerhalb der Darstellung
	 * @see HTMLBrowserTextPane#scanElement(Element)
	 */
	private static final class ElementPos {
		/** HTML-Element */
		private final Element element;
		/** �berschriften-Ebene (h1=1, h2=2, ...) */
		private final int level;
		/** Position des Elements innerhalb des Textes */
		private final int position;

		/**
		 * Konstruktor der Klasse
		 * @param element	HTML-Element
		 */
		public ElementPos(Element element) {
			this.element=element;
			int l=0;
			for (int i=2;i<5;i++) if (element.getName().equalsIgnoreCase("h"+i)) {l=i; break;}
			level=l;
			position=element.getStartOffset();
		}

		@Override
		public String toString() {
			StringBuilder s=new StringBuilder();
			for (int i=0;i<element.getElementCount();i++) {
				if (element.getElement(i) instanceof HTMLDocument.RunElement) {
					HTMLDocument.RunElement runElement=(HTMLDocument.RunElement)element.getElement(i);
					try {
						String t=runElement.getDocument().getText(runElement.getStartOffset(),runElement.getEndOffset()-runElement.getStartOffset());
						s.append(t.replace("\n",""));
					} catch (BadLocationException e) {}
				}
			}

			for (int i=3;i<=level;i++) s.insert(0,"   ");

			return s.toString();
		}

		/**
		 * Liefert die �berschriften-Ebene (h1=1, h2=2, ...)
		 * @return	�berschriften-Ebene (h1=1, h2=2, ...)
		 */
		public int getLevel() {
			return level;
		}

		/**
		 * Liefert die Position des Elements innerhalb des Textes
		 * @return	Position des Elements innerhalb des Textes
		 */
		public int getPosition() {
			return position;
		}
	}

	/**
	 * Durchsucht ein Element nach �berschriften
	 * @param parent	�bergeordnetes HTML-Element
	 * @return	Liste mit den gefundenen �berschriften
	 * @see ElementPos
	 */
	private final List<ElementPos> scanElement(Element parent) {
		final List<ElementPos> list=new ArrayList<>();

		for (int i=0;i<parent.getElementCount();i++) {
			Element element=parent.getElement(i);

			/* �berschriften speichern */
			boolean ok=false;
			for (int j=2;j<5;j++) if (element.getName().equalsIgnoreCase("h"+j)) {ok=true; break;}
			if (ok) list.add(new ElementPos(element));

			/* Unterelemente untersuchen */
			list.addAll(scanElement(element));
		}

		return list;
	}

	@Override
	public final List<String> getPageContent() {
		final List<String> list=new ArrayList<>();
		if (pageContentList!=null) for (int i=0;i<pageContentList.size();i++) list.add(pageContentList.get(i).toString());
		return list;
	}

	@Override
	public final List<Integer> getPageContentLevel() {
		final List<Integer> list=new ArrayList<>();
		if (pageContentList!=null) for (int i=0;i<pageContentList.size();i++) list.add(pageContentList.get(i).getLevel());
		return list;
	}

	@Override
	public final boolean scrollToPageContent(int index) {
		if (pageContentList==null || index<0 || index>=pageContentList.size()) return false;
		setCaretPosition(pageContentList.get(index).getPosition());
		return true;
	}

	/**
	 * Reagiert auf Klicks und auch Bewegungen �ber Links
	 * @see HTMLBrowserTextPane#lastClickedURL
	 * @see HTMLBrowserTextPane#lastClickedURLDescription
	 * @see HTMLBrowserTextPane#linkClickListener
	 */
	private final class LinkListener implements HyperlinkListener {
		/**
		 * Konstruktor der Klasse
		 */
		public LinkListener() {
			/*
			 * Wird nur ben�tigt, um einen JavaDoc-Kommentar f�r diesen (impliziten) Konstruktor
			 * setzen zu k�nnen, damit der JavaDoc-Compiler keine Warnung mehr ausgibt.
			 */
		}

		@Override
		public void hyperlinkUpdate(HyperlinkEvent e) {
			if (e.getEventType()==HyperlinkEvent.EventType.ENTERED) {
				setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
				return;
			}

			if (e.getEventType()==HyperlinkEvent.EventType.EXITED) {
				setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				return;
			}

			if (e.getEventType()==HyperlinkEvent.EventType.ACTIVATED) {
				if (e instanceof HTMLFrameHyperlinkEvent) {
					HTMLFrameHyperlinkEvent evt = (HTMLFrameHyperlinkEvent)e;
					HTMLDocument doc=(HTMLDocument)getDocument();
					doc.processHTMLFrameHyperlinkEvent(evt);
				} else {
					lastClickedURL=e.getURL();
					lastClickedURLDescription=e.getDescription();
					if (linkClickListener!=null) linkClickListener.run();
				}
			}
		}
	}

	/**
	 * Reagiert darauf, wenn der Text vollst�ndig geladen wurde und aktualisiert dann
	 * die Liste der �berschriften und ruft weitere Listener auf.
	 * @see HTMLBrowserTextPane#pageContentList
	 * @see HTMLBrowserTextPane#pageLoadListener
	 *
	 */
	private final class PageLoadListener implements PropertyChangeListener {
		/**
		 * Konstruktor der Klasse
		 */
		public PageLoadListener() {
			/*
			 * Wird nur ben�tigt, um einen JavaDoc-Kommentar f�r diesen (impliziten) Konstruktor
			 * setzen zu k�nnen, damit der JavaDoc-Compiler keine Warnung mehr ausgibt.
			 */
		}

		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			final Element root=((HTMLDocument)getStyledDocument()).getDefaultRootElement();
			if (pagePreProcessor!=null) pagePreProcessor.accept(root);
			pageContentList=scanElement(root);
			if (pageLoadListener!=null) pageLoadListener.run();
		}
	}

	@Override
	public boolean needsLoadLock() {
		return false;
	}

	@Override
	public boolean setUserDefinedStyleSheet(String styleSheet) {
		return false;
	}

	@Override
	public boolean needsBorder() {
		return false;
	}
}