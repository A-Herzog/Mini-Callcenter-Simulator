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
package systemtools.statistics;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.Point;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.print.PrinterException;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.AbstractDocument.LeafElement;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

import org.apache.commons.math3.distribution.TDistribution;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.odftoolkit.simple.TextDocument;
import org.odftoolkit.simple.style.StyleTypeDefinitions;
import org.odftoolkit.simple.text.Paragraph;

import mathtools.NumberTools;
import mathtools.Table;
import mathtools.TimeTools;
import mathtools.distribution.swing.CommonVariables;
import systemtools.GUITools;
import systemtools.MsgBox;
import systemtools.images.SimToolsImages;

/**
 * Diese Klasse stellt Implementierung des {@link StatisticViewer}-Interfaces zur
 * Anzeige von Text dar.
 * @author Alexander Herzog
 * @version 2.4
 */
public abstract class StatisticViewerText implements StatisticViewer {
	/**
	 * Ausgabe-Text-Panel
	 * @see #initTextPane()
	 */
	private JTextPane textPane=null;

	/**
	 * Auszugebende Zeilen
	 */
	private final List<String> lines;

	/**
	 * Linkziele oder Hinweise zu den Zeilen
	 * @see #lines
	 */
	private final List<String> hints;

	/**
	 * Bedeutung der jeweiligen Zeile:
	 * 0=Text, 1,2,3,...=�berschriften, -1=Absatzbeginn, -2=Absatzende, -3=Link
	 * @see #lines
	 */
	private final List<Integer> lineTypes;

	/**
	 * St�rke der Einr�ckung der jeweiligen Zeile
	 * @see #lines
	 */
	private final List<Integer> indentLevel;

	/**
	 * html-Seite mit einer zus�tzlichen Erkl�rung zu dieser Statistikseite
	 * @see #addDescription(URL, Consumer)
	 */
	private URL descriptionURL=null;

	/**
	 * Handler, der Themennamen (angegeben �ber "help:..."-Links) zum Aufruf normaler Hilfeseiten entgegen nimmt
	 * @see #addDescription(URL, Consumer)
	 */
	private Consumer<String> descriptionHelpCallback=null;

	/**
	 * Darstellung der Hilfe-Seite {@link #descriptionURL}
	 * @see #initDescriptionPane()
	 * @see #addDescription(URL, Consumer)
	 */
	private DescriptionViewer descriptionPane=null;

	/**
	 * Erfolgt die Darstellung im Dark-Modus?
	 */
	private final boolean isDark;

	/**
	 * Konstruktor der Klasse
	 */
	public StatisticViewerText() {
		lines=new ArrayList<>();
		hints=new ArrayList<>();
		lineTypes=new ArrayList<>();
		indentLevel=new ArrayList<>();

		final Color textBackground=UIManager.getColor("TextField.background");
		isDark=(textBackground!=null && !textBackground.equals(Color.WHITE));
	}

	/**
	 * L�scht alle bisherigen Ausgaben
	 * @see #saveHtml(BufferedWriter, File, int, boolean)
	 * @see #saveLaTeX(BufferedWriter, File, int)
	 */
	private void reset() {
		lines.clear();
		hints.clear();
		lineTypes.clear();
		indentLevel.clear();
	}

	/**
	 * Wurden bereits Ausgabezeilen erzeugt?
	 * @return	Liefert <code>true</code>, wenn noch keinerlei Ausgaben angelegt wurden
	 */
	protected final boolean isEmpty() {
		return lines.size()==0;
	}

	@Override
	public ViewerType getType() {
		return ViewerType.TYPE_TEXT;
	}

	@Override
	public ViewerImageType getImageType() {
		return ViewerImageType.IMAGE_TYPE_NOIMAGE;
	}

	@Override
	public boolean getCanDo(final CanDoAction canDoType) {
		switch (canDoType) {
		case CAN_DO_UNZOOM: return false;
		case CAN_DO_COPY: return true;
		case CAN_DO_PRINT: return true;
		case CAN_DO_SAVE: return true;
		case CAN_DO_SEARCH: return true;
		default: return false;
		}
	}

	/**
	 * �bertr�gt den Text aus {@link #lines} usw. in {@link #textPane}.
	 * @see #lines
	 * @see #hints
	 * @see #lineTypes
	 * @see #indentLevel
	 * @see #textPane
	 */
	private void initTextPane() {
		if (textPane!=null) return;

		textPane=new JTextPane();
		textPane.setEditable(false);
		if (!isDark) textPane.setBackground(new Color(0xFF,0xFF,0xF8));
		textPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES,Boolean.TRUE);

		/* Styles zusammenstellen */

		final Style oldDefaultStyle=textPane.getStyle("default");
		final String defaultFontFamily=StyleConstants.getFontFamily(oldDefaultStyle);

		final FastDefaultStyledDocument doc=new FastDefaultStyledDocument(); /* Neues Dokument erstellen; bisheriges in textPane l�st bei jeder Texteinf�gung ein Event aus */

		final Style defaultStyle=StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);
		StyleConstants.setFontFamily(defaultStyle,defaultFontFamily);
		Style style;

		style=doc.addStyle("default",defaultStyle);
		StyleConstants.setFontSize(style,(int)Math.round((StyleConstants.getFontSize(style)+1)*GUITools.getScaleFactor()));
		if (isDark) StyleConstants.setForeground(style,Color.LIGHT_GRAY);

		style=doc.addStyle("h1",defaultStyle);
		StyleConstants.setFontSize(style,(int)Math.round((StyleConstants.getFontSize(style)+4)*GUITools.getScaleFactor()));
		StyleConstants.setBold(style,true);
		if (isDark) StyleConstants.setForeground(style,Color.LIGHT_GRAY);

		style=doc.addStyle("h2",defaultStyle);
		StyleConstants.setBold(style,true);
		StyleConstants.setFontSize(style,(int)Math.round((StyleConstants.getFontSize(style)+2)*GUITools.getScaleFactor()));
		if (isDark) StyleConstants.setForeground(style,Color.LIGHT_GRAY);

		style=doc.addStyle("h3",defaultStyle);
		StyleConstants.setFontSize(style,(int)Math.round((StyleConstants.getFontSize(style)+1)*GUITools.getScaleFactor()));
		StyleConstants.setUnderline(style,true);
		if (isDark) StyleConstants.setForeground(style,Color.LIGHT_GRAY);

		style=doc.addStyle("link",defaultStyle);
		StyleConstants.setFontSize(style,(int)Math.round((StyleConstants.getFontSize(style)-1)*GUITools.getScaleFactor()));
		if (isDark) StyleConstants.setForeground(style,new Color(128,128,225)); else StyleConstants.setForeground(style,Color.BLUE);

		/* Text einf�gen */

		final int size=lines.size();
		final Style defaultStyle2=doc.getStyle("default");
		for (int i=0;i<size;i++) {
			final String line=lines.get(i);
			final String hint=hints.get(i);
			final int type=lineTypes.get(i);

			if (type==-1) {
				/* Absatzanfang */
				continue;
			}
			if (type==-2) {
				/* Absatzende */
				doc.addText(defaultStyle2,"\n");
				continue;
			}
			if (type==0) {
				/* Normaler Text */
				if (hint!=null && !hint.trim().isEmpty()) {
					final SimpleAttributeSet attrSet=new SimpleAttributeSet(defaultStyle2);
					attrSet.addAttribute("Hint",hint);
					doc.addText(attrSet,line+"\n");
				} else {
					doc.addText(defaultStyle2,line+"\n");
				}
				continue;
			}
			if (type==-3) {
				/* Link */
				final SimpleAttributeSet attrSet=new SimpleAttributeSet(doc.getStyle("link"));
				if (hint!=null && !hint.trim().isEmpty()) attrSet.addAttribute("URL",hint);
				doc.addText(attrSet,line+"\n");
				continue;
			}
			if (type>0) {
				/* �berschriften */
				if (i>0 && lineTypes.get(i-1)!=-2) doc.addText(defaultStyle2,"\n");
				doc.addText(doc.getStyle("h"+type),line+"\n");
				continue;
			}
		}

		doc.finalizeText();

		textPane.setStyledDocument(doc); /* Neues Dokument setzen */

		textPane.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseMoved(java.awt.event.MouseEvent e) {
				/*
				Funktioniert aufgrund der in pointToLink genannten Einschr�nkung nur, wenn geklickt wurde
				final int cursor=(pointToLink(e.getPoint())==null)?Cursor.DEFAULT_CURSOR:Cursor.HAND_CURSOR;
				System.out.println(pointToLink(e.getPoint()));
				textPane.setCursor(new Cursor(cursor));
				 */
			}
		});
		textPane.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (SwingUtilities.isRightMouseButton(e)) {
					/* Caret wird durch Rechtsklick nicht verschoben, daher m�ssen wir das nachholen, damit pointToHint den richtigen Wert liefert. */
					final MouseEvent e2=new MouseEvent((Component)e.getSource(),e.getID(),e.getWhen(),e.getModifiersEx(),e.getX(),e.getY(),e.getXOnScreen(),e.getYOnScreen(),e.getClickCount(),false,MouseEvent.BUTTON1);
					textPane.dispatchEvent(e2);

					final String hint=pointToHint();
					if (hint!=null) {
						final JPopupMenu menu=processContextClick(hint);
						if (menu!=null) menu.show(e.getComponent(),e.getX(),e.getY());
					}
				}
				if (SwingUtilities.isLeftMouseButton(e)) {
					final String url=pointToLink(e.getPoint());
					if (url!=null) processLinkClick(url);
				}
			}
		});

		textPane.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				if (e.isControlDown() && !e.isAltDown() && !e.isShiftDown() && e.getKeyCode()==KeyEvent.VK_F) {
					search(SwingUtilities.getWindowAncestor(textPane));
					e.consume();
				}
			}
		});
	}

	/**
	 * Wurden in {@link #addLine(String, String)} oder {@link #addLine(int, String, String)} f�r bestimmte
	 * Zeilen Zusatztexte �bergeben, so kann auf dieser Basis hier f�r diese ein Popup-Men� ge�ffnet werden.
	 * @param hint	Zus�tzlicher Text
	 * @return	Zu �ffnendes Kontextmen� oder <code>null</code>, wenn f�r diesen Text kein Kontextmen� ge�ffnet werden soll
	 * @see #addLine(String, String)
	 * @see #addLine(int, String, String)
	 */
	protected JPopupMenu processContextClick(final String hint) {
		return null;
	}

	/**
	 * Initialisiert die Anzeige der zus�tzlichen Beschreibung.
	 * @see #addDescription(URL, Consumer)
	 * @see #descriptionURL
	 * @see #descriptionHelpCallback
	 * @see #descriptionPane
	 */
	private void initDescriptionPane() {
		if (descriptionPane!=null) return;
		if (descriptionURL==null) return;

		descriptionPane=new DescriptionViewer(descriptionURL,link->{
			if (link.toLowerCase().startsWith("help:") && descriptionHelpCallback!=null) {
				descriptionHelpCallback.accept(link.substring("help:".length()));
			}
		});
	}

	/**
	 * Konkretes Anzeigeobjekt, das �ger {@link #getViewer(boolean)} geliefert wird.
	 * @see #getViewer(boolean)
	 */
	private Container viewer=null;

	@Override
	public Container getViewer(final boolean needReInit) {
		if (viewer!=null && !needReInit) return viewer;

		if (textPane==null || needReInit) {
			textPane=null;
			lines.clear();
			hints.clear();
			lineTypes.clear();
			indentLevel.clear();
			buildText();
			initTextPane();
			initDescriptionPane();
		}

		final JScrollPane textScroller=new JScrollPane(textPane);
		textPane.setSelectionStart(0);
		textPane.setSelectionEnd(0);

		if (descriptionPane==null) return viewer=textScroller;

		return viewer=descriptionPane.getSplitPanel(textScroller);
	}

	@Override
	public boolean isViewerGenerated() {
		return viewer!=null;
	}

	/**
	 * Liefert den Text des Viewers ohne Formatierungen.
	 * @return	Text des Viewers ohne Formatierungen
	 * @see #copyToClipboard(Clipboard)
	 * @see #save(Component, File)
	 */
	private String getPlainText() {
		StringBuilder result=new StringBuilder();

		for (int i=0;i<lines.size();i++) {
			final String line=lines.get(i);
			final int type=lineTypes.get(i);
			final int indent=indentLevel.get(i);

			if (type==-1) {
				/* Absatzanfang */
				continue;
			}
			if (type==-2) {
				/* Absatzende */
				result.append('\n');
				continue;
			}
			if (type==0) {
				/* Normaler Text */
				for (int j=0;j<indent;j++) result.append("  ");
				result.append(line+"\n");
				continue;
			}
			if (type>0) {
				/* �berschriften */
				if (i>0 && lineTypes.get(i-1)!=-2) result.append('\n');
				for (int j=0;j<type-2;j++) result.append("  ");
				result.append(line+"\n");
				continue;
			}
		}
		return result.toString();
	}

	/**
	 * Liefert den Text des Viewer mit RTF-Formatierung.
	 * @return	Text des Viewer mit RTF-Formatierung
	 * @see #copyToClipboard(Clipboard)
	 * @see #save(Component, File)
	 */
	private String getRTFText() {
		StringBuilder result=new StringBuilder();

		for (int i=0;i<lines.size();i++) {
			final String line=lines.get(i);
			final int type=lineTypes.get(i);
			final int indent=indentLevel.get(i);

			if (type==-1) {
				/* Absatzanfang */
				continue;
			}
			if (type==-2) {
				/* Absatzende */
				result.append("\\par\n");
				continue;
			}
			if (type==0) {
				/* Normaler Text */
				for (int j=0;j<indent;j++) result.append("  ");
				result.append(convertLineToRTF(line)+"\\line\n");
				continue;
			}
			if (type>0) {
				/* �berschriften */
				String fs="22";
				switch (type) {
				case 1: fs="34"; break;
				case 2: fs="28"; break;
				}
				if (i>0 && lineTypes.get(i-1)!=-2) result.append("\\par\n");
				result.append("\\fs"+fs+" "+convertLineToRTF(line)+"\\fs22\\par\n");
				continue;
			}
		}

		return "{\\rtf1\\ansi\\ansicpg1252\\deff0{\\fonttbl\\f0\\fswiss Helvetica;}\\f0\n"+result.toString()+"\n}\n";
	}

	/**
	 * Liefert den Text des Viewer mit HTML-Formatierung.
	 * @return	Text des Viewer mit HTML-Formatierung
	 * @see #getFullHTMLText()
	 */
	private String getHTMLText() {
		StringBuilder result=new StringBuilder();

		boolean inParagraph=false;

		for (int i=0;i<lines.size();i++) {
			final String line=lines.get(i);
			final int type=lineTypes.get(i);
			final int indent=indentLevel.get(i);

			if (type==-1) {
				/* Absatzanfang */
				if (inParagraph) result.append("</p>\n");
				result.append("<p>\n");
				inParagraph=true;
				continue;
			}
			if (type==-2) {
				/* Absatzende */
				if (inParagraph) result.append("</p>\n");
				inParagraph=false;
				continue;
			}
			if (type==0) {
				/* Normaler Text */
				if (!inParagraph) {result.append("<p>\n"); inParagraph=true;}
				for (int j=0;j<indent;j++) result.append("  ");
				result.append(line+"<br>\n");
				continue;
			}
			if (type>0) {
				/* �berschriften */
				if (inParagraph) {result.append("</p>\n"); inParagraph=false;}
				result.append("<h"+type+">"+line+"</h"+type+">\n");
				continue;
			}
		}

		return result.toString();
	}

	/**
	 * Liefert den Text des Viewer mit LaTeX-Formatierung.
	 * @return	Text des Viewer mit LaTeX-Formatierung
	 * @see #save(Component, File)
	 * @see #saveLaTeX(BufferedWriter, File, int)
	 */
	private String getLaTeXText() {
		StringBuilder result=new StringBuilder();

		boolean inParagraph=false;

		for (int i=0;i<lines.size();i++) {
			final String line=lines.get(i);
			final int type=lineTypes.get(i);
			final int indent=indentLevel.get(i);

			if (type==-1) {
				/* Absatzanfang */
				if (inParagraph) result.append("\n");
				result.append("\n");
				inParagraph=true;
				continue;
			}
			if (type==-2) {
				/* Absatzende */
				if (inParagraph) result.append("\n");
				inParagraph=false;
				continue;
			}
			if (type==0) {
				/* Normaler Text */
				if (!inParagraph) {result.append("\n"); inParagraph=true;}
				for (int j=0;j<indent;j++) result.append("  ");
				result.append(line.replace("%","\\%")+"\\\\\n");
				continue;
			}
			if (type>0) {
				/* �berschriften */
				if (inParagraph) {result.append("\n"); inParagraph=false;}
				switch (type) {
				case 1: result.append("\\section{"+line+"}\n"); break;
				case 2: result.append("\\subsection{"+line+"}\n"); break;
				case 3: result.append("\\subsubsection{"+line+"}\n"); break;
				default: result.append("\\paragraph{"+line+"}~\\\n"); break;
				}
				continue;
			}
		}

		return result.toString();
	}

	/**
	 * Liefert den Text des Viewer mit HTML-Formatierung und inkl. HTML-Vor- und Abspann.
	 * @return	Text des Viewer mit LaTeHTML-Formatierung
	 * @see #save(Component, File)
	 */
	private String getFullHTMLText() {
		final StringBuilder result=new StringBuilder();

		result.append("<!DOCTYPE html>\n");
		result.append("<html>\n");
		result.append("<head>\n");
		result.append("  <meta charset=\"utf-8\">");
		result.append("  <title>"+StatisticsBasePanel.program_name+"</title>\n");
		result.append("  <meta name=\"author\" content=\"Alexander Herzog\">\n");
		result.append("  <style type=\"text/css\">\n");
		result.append("    body {font-family: Verdana, Lucida, sans-serif;}\n");
		result.append("    table {border: 1px solid black; border-collapse: collapse;}\n");
		result.append("    td {border: 1px solid black; padding: 2px 5px;}\n");
		result.append("  </style>\n");
		result.append("</head>\n");
		result.append("<body>\n");
		result.append(getHTMLText());
		result.append("</body>\n</html>\n");

		return result.toString();
	}

	/**
	 * Liefert den Text des Viewer mit Markdown-Formatierung.
	 * @return	Text des Viewer mit Markdown-Formatierung
	 * @see #save(Component, File)
	 */
	private String getMarkdownText() {
		StringBuilder result=new StringBuilder();

		boolean inParagraph=false;

		for (int i=0;i<lines.size();i++) {
			final String line=lines.get(i);
			final int type=lineTypes.get(i);
			/* final int indent=indentLevel.get(i); - wird in Markdown nicht unterst�tzt */

			if (type==-1) {
				/* Absatzanfang */
				if (inParagraph) result.append("\n");
				inParagraph=true;
				continue;
			}
			if (type==-2) {
				/* Absatzende */
				if (inParagraph) result.append("\n");
				inParagraph=false;
				continue;
			}
			if (type==0) {
				/* Normaler Text */
				inParagraph=true;
				result.append(line+"  \n");
				continue;
			}
			if (type>0) {
				/* �berschriften */
				if (inParagraph) {result.append("\n"); inParagraph=false;}
				for (int j=1;j<=type;j++) result.append('#');
				result.append(' ');
				result.append(line+"  \n");
				continue;
			}
		}

		return result.toString();
	}

	/**
	 * Wird unmittelbar vor der ersten Verwendung der Textdaten aufgerufen, sofern der Text leer ist.
	 */
	protected abstract void buildText();

	@Override
	public void copyToClipboard(final Clipboard clipboard) {
		if (textPane==null) {
			buildText();
			initTextPane();
			initDescriptionPane();
		}

		final DataFlavor RTF_FLAVOR=new DataFlavor("text/rtf", "Rich Formatted Text");
		final String transferPlain=getPlainText();
		final Object transferRTF=new ByteArrayInputStream(getRTFText().getBytes());

		final Transferable transfer=new Transferable() {
			private final DataFlavor[] flavors=new DataFlavor[]{DataFlavor.stringFlavor,RTF_FLAVOR};

			@Override
			public boolean isDataFlavorSupported(DataFlavor flavor) {
				for (DataFlavor test: flavors) if (test.equals(flavor)) return true;
				return false;
			}

			@Override
			public DataFlavor[] getTransferDataFlavors() {
				return flavors;
			}

			@Override
			public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
				if (flavor.equals(RTF_FLAVOR)) return transferRTF;
				if (flavor.equals(DataFlavor.stringFlavor)) return transferPlain;
				return null;
			}
		};

		clipboard.setContents(transfer,null);
	}

	@Override
	public boolean print() {
		if (textPane==null) {
			buildText();
			initTextPane();
			initDescriptionPane();
		}

		try {textPane.print();} catch (PrinterException e) {return false;}
		return true;
	}

	@Override
	public void save(final Component owner) {
		final JFileChooser fc=new JFileChooser();
		CommonVariables.initialDirectoryToJFileChooser(fc);
		fc.setDialogTitle(StatisticsBasePanel.viewersSaveText);
		final FileFilter docx=new FileNameExtensionFilter(StatisticsBasePanel.fileTypeDOCX+" (*.docx)","docx");
		final FileFilter odt=new FileNameExtensionFilter(StatisticsBasePanel.fileTypeODT+" (*.odt)","odt");
		final FileFilter rtf=new FileNameExtensionFilter(StatisticsBasePanel.fileTypeRTF+" (*.rtf)","rtf");
		final FileFilter html=new FileNameExtensionFilter(StatisticsBasePanel.fileTypeHTML+" (*.html, *.htm)","html","htm");
		final FileFilter txt=new FileNameExtensionFilter(StatisticsBasePanel.fileTypeTXT+" (*.txt)","txt");
		final FileFilter pdf=new FileNameExtensionFilter(StatisticsBasePanel.fileTypePDF+" (*.pdf)","pdf");
		final FileFilter md=new FileNameExtensionFilter(StatisticsBasePanel.fileTypeMD+" (*.md)","md");
		final FileFilter tex=new FileNameExtensionFilter(StatisticsBasePanel.fileTypeTEX+" (*.tex)","tex");
		fc.addChoosableFileFilter(docx);
		fc.addChoosableFileFilter(odt);
		fc.addChoosableFileFilter(rtf);
		fc.addChoosableFileFilter(html);
		fc.addChoosableFileFilter(pdf);
		fc.addChoosableFileFilter(txt);
		fc.addChoosableFileFilter(md);
		fc.addChoosableFileFilter(tex);
		fc.setFileFilter(docx);
		fc.setAcceptAllFileFilterUsed(false);

		if (fc.showSaveDialog(owner)!=JFileChooser.APPROVE_OPTION) return;
		CommonVariables.initialDirectoryFromJFileChooser(fc);
		File file=fc.getSelectedFile();

		if (file.getName().indexOf('.')<0) {
			if (fc.getFileFilter()==docx) file=new File(file.getAbsoluteFile()+".docx");
			if (fc.getFileFilter()==odt) file=new File(file.getAbsoluteFile()+".odt");
			if (fc.getFileFilter()==rtf) file=new File(file.getAbsoluteFile()+".rtf");
			if (fc.getFileFilter()==html) file=new File(file.getAbsoluteFile()+".html");
			if (fc.getFileFilter()==txt) file=new File(file.getAbsoluteFile()+".txt");
			if (fc.getFileFilter()==pdf) file=new File(file.getAbsoluteFile()+".pdf");
			if (fc.getFileFilter()==md) file=new File(file.getAbsoluteFile()+".md");
			if (fc.getFileFilter()==tex) file=new File(file.getAbsoluteFile()+".tex");
		}

		if (file.exists()) {
			if (!MsgBox.confirmOverwrite(owner,file)) return;
		}

		save(owner,file);
	}

	/**
	 * Speichert den Text als PDF-Datei.
	 * @param owner	�bergeordnete Komponente f�r die eventuelle Anzeige von Dialogen
	 * @param file	Datei, in der die Statistikdaten gespeichert werden soll. Es darf hier <b>nicht</b> <code>null</code> �bergeben werden.
	 * @return	Liefert <code>true</code> zur�ck, wenn die Daten erfolgreich gespeichert werden konnten.
	 * @see #save(Component, File)
	 */
	private boolean savePDF(final Component owner, final File file) {
		PDFWriter pdf=new PDFWriter(owner,15,10);
		if (!pdf.systemOK) return false;
		if (!savePDF(pdf)) return false;
		return pdf.save(file);
	}

	/**
	 * Speichert den Text als DOCX-Datei.
	 * @param file	Datei, in der die Statistikdaten gespeichert werden soll. Es darf hier <b>nicht</b> <code>null</code> �bergeben werden.
	 * @return	Liefert <code>true</code> zur�ck, wenn die Daten erfolgreich gespeichert werden konnten.
	 * @see #save(Component, File)
	 */
	private boolean saveDOCX(final File file) {
		try(XWPFDocument doc=new XWPFDocument()) {
			if (!saveDOCX(doc)) return false;
			try (FileOutputStream out=new FileOutputStream(file)) {doc.write(out);}
			return true;
		} catch (IOException e) {return false;}
	}

	/**
	 * Speichert den Text als ODT-Datei.
	 * @param odt	Ausgabe-Text-Dokument
	 * @return	Liefert <code>true</code> zur�ck, wenn die Daten erfolgreich gespeichert werden konnten.
	 * @see #saveODT(File)
	 */
	private boolean saveODT(final TextDocument odt) {
		if (textPane==null) {
			buildText();
			initTextPane();
			initDescriptionPane();
		}

		Paragraph p=null;

		for (int i=0;i<lines.size();i++) {
			final String line=lines.get(i);
			final int type=lineTypes.get(i);

			if (type==-1) {
				/* Absatzanfang */
				p=odt.addParagraph(null);
				continue;
			}
			if (type==-2) {
				/* Absatzende */
				p=null;
				continue;
			}
			if (type==0) {
				/* Normaler Text */
				if (p==null) p=odt.addParagraph(null);
				p.appendTextContent(line);
				p.appendTextContent("\n");
				continue;
			}
			if (type>0) {
				/* �berschriften */
				p=odt.addParagraph(null);
				int fs=12;
				switch (type) {
				case 1: fs=18; break;
				case 2: fs=15; break;
				}
				p.setFont(new org.odftoolkit.simple.style.Font("Arial",StyleTypeDefinitions.FontStyle.BOLD,fs));
				p.appendTextContent(line);
				p=null;
				continue;
			}
		}

		return true;
	}

	/**
	 * Speichert den Text als ODT-Datei.
	 * @param file	Datei, in der die Statistikdaten gespeichert werden soll. Es darf hier <b>nicht</b> <code>null</code> �bergeben werden.
	 * @return	Liefert <code>true</code> zur�ck, wenn die Daten erfolgreich gespeichert werden konnten.
	 * @see #save(Component, File)
	 */
	private boolean saveODT(final File file) {
		try (TextDocument odt=TextDocument.newTextDocument()) {
			if (!saveODT(odt)) return false;
			odt.save(file);
			return true;

		} catch (Exception e) {return false;}
	}

	@Override
	public boolean save(final Component owner, final File file) {
		if (textPane==null) {
			buildText();
			initTextPane();
			initDescriptionPane();
		}

		String filename=file.getName().toUpperCase();

		if (filename.endsWith(".DOCX")) return saveDOCX(file);
		if (filename.endsWith(".ODT")) return saveODT(file);
		if (filename.endsWith(".PDF")) return savePDF(owner,file);

		String text="";
		if (filename.endsWith(".RTF")) text=getRTFText();
		if (filename.endsWith(".HTML") || filename.endsWith(".HTM")) text=getFullHTMLText();
		if (filename.endsWith(".MD")) text=getMarkdownText();
		if (filename.endsWith(".TEX")) text=getLaTeXText();
		if (text.isEmpty()) text=getPlainText();

		return Table.saveTextToFile(text,file);
	}

	@Override
	public int saveHtml(final BufferedWriter bw, final File mainFile, final int nextImageNr, final boolean imagesInline) throws IOException {
		if (textPane==null) {
			buildText();
			bw.write(getHTMLText());
			reset();
		} else {
			bw.write(getHTMLText());
		}
		return nextImageNr;
	}

	@Override
	public int saveLaTeX(final BufferedWriter bw, final File mainFile, final int nextImageNr) throws IOException {
		if (textPane==null) {
			buildText();
			bw.write(getLaTeXText());
			reset();
		} else {
			bw.write(getLaTeXText());
		}
		return nextImageNr;
	}

	@Override
	public void unZoom() {}

	@Override
	public JButton[] getAdditionalButton() {
		final boolean word=StatisticsBasePanel.viewerPrograms.contains(StatisticsBasePanel.ViewerPrograms.WORD);
		final boolean odt=StatisticsBasePanel.viewerPrograms.contains(StatisticsBasePanel.ViewerPrograms.ODT);
		final boolean pdf=StatisticsBasePanel.viewerPrograms.contains(StatisticsBasePanel.ViewerPrograms.PDF);
		int count=0;
		if (word) count++;
		if (odt) count++;
		if (pdf) count++;

		if (count>1) {
			final JButton button=new JButton(StatisticsBasePanel.viewersToolbarOpenText);
			button.setToolTipText(StatisticsBasePanel.viewersToolbarOpenTextHint);
			button.setIcon(SimToolsImages.OPEN.getIcon());
			button.addActionListener(e->{
				final JPopupMenu menu=new JPopupMenu();
				JMenuItem item;
				if (word) {
					menu.add(item=new JMenuItem(StatisticsBasePanel.viewersToolbarWord));
					item.setIcon(SimToolsImages.SAVE_TEXT_WORD.getIcon());
					item.setToolTipText(StatisticsBasePanel.viewersToolbarWordHint);
					item.addActionListener(ev->openWord());
				}
				if (odt) {
					menu.add(item=new JMenuItem(StatisticsBasePanel.viewersToolbarODT));
					item.setIcon(SimToolsImages.SAVE_TEXT.getIcon());
					item.setToolTipText(StatisticsBasePanel.viewersToolbarODTHint);
					item.addActionListener(ev->openODT());
				}
				if (pdf) {
					menu.add(item=new JMenuItem(StatisticsBasePanel.viewersToolbarPDF));
					item.setIcon(SimToolsImages.SAVE_PDF.getIcon());
					item.setToolTipText(StatisticsBasePanel.viewersToolbarPDFHint);
					item.addActionListener(ev->openPDF(SwingUtilities.getWindowAncestor(textPane)));
				}
				menu.show(button,0,button.getHeight());

			});
			return new JButton[]{button};
		}

		if (word) {
			final JButton button=new JButton(StatisticsBasePanel.viewersToolbarWord);
			button.setToolTipText(StatisticsBasePanel.viewersToolbarWordHint);
			button.setIcon(SimToolsImages.SAVE_TEXT_WORD.getIcon());
			button.addActionListener(e->openWord());
			return new JButton[]{button};
		}

		if (odt) {
			final JButton button=new JButton(StatisticsBasePanel.viewersToolbarODT);
			button.setToolTipText(StatisticsBasePanel.viewersToolbarODTHint);
			button.setIcon(SimToolsImages.SAVE_TEXT.getIcon());
			button.addActionListener(e->openODT());
			return new JButton[]{button};
		}

		if (pdf) {
			final JButton button=new JButton(StatisticsBasePanel.viewersToolbarPDF);
			button.setToolTipText(StatisticsBasePanel.viewersToolbarPDFHint);
			button.setIcon(SimToolsImages.SAVE_PDF.getIcon());
			button.addActionListener(e->openPDF(SwingUtilities.getWindowAncestor(textPane)));
			return new JButton[]{button};
		}

		return null;
	}

	/**
	 * Wird aufgerufen, um eine externe Datei (mit dem Standardprogramm) zu �ffnen.
	 * @param file	Zu �ffnende Datei
	 * @throws IOException	Kann ausgel�st werden, wenn die Datei nicht ge�ffnet werden konnte
	 */
	protected void openExternalFile(final File file) throws IOException {
		Desktop.getDesktop().open(file);
	}

	/**
	 * �ffnet den Text (�ber eine tempor�re Datei) mit Word
	 */
	private void openWord() {
		try {
			final File file=File.createTempFile(StatisticsBasePanel.viewersToolbarExcelPrefix+"_",".docx");
			if (saveDOCX(file)) {
				file.deleteOnExit();
				openExternalFile(file);
			}
		} catch (IOException e1) {
			MsgBox.error(getViewer(false),StatisticsBasePanel.viewersToolbarExcelSaveErrorTitle,StatisticsBasePanel.viewersToolbarExcelSaveErrorInfo);
		}
	}

	/**
	 * �ffnet den Text (�ber eine tempor�re Datei) mit OpenOffice/LibreOffice
	 */
	private void openODT() {
		try {
			final File file=File.createTempFile(StatisticsBasePanel.viewersToolbarExcelPrefix+"_",".odt");
			if (saveODT(file)) {
				file.deleteOnExit();
				openExternalFile(file);
			}
		} catch (IOException e1) {
			MsgBox.error(getViewer(false),StatisticsBasePanel.viewersToolbarExcelSaveErrorTitle,StatisticsBasePanel.viewersToolbarExcelSaveErrorInfo);
		}
	}

	/**
	 * �ffnet den Text (�ber eine tempor�re Datei) als pdf
	 * @param owner	�bergeordnete Komponente f�r die eventuelle Anzeige von Dialogen
	 */
	private void openPDF(final Component owner) {
		try {
			final File file=File.createTempFile(StatisticsBasePanel.viewersToolbarExcelPrefix+"_",".pdf");
			if (savePDF(owner,file)) {
				file.deleteOnExit();
				openExternalFile(file);
			}
		} catch (IOException e1) {
			MsgBox.error(getViewer(false),StatisticsBasePanel.viewersToolbarExcelSaveErrorTitle,StatisticsBasePanel.viewersToolbarExcelSaveErrorInfo);
		}
	}

	/**
	 * Wandelt eine Zahl im Bereich von 0 bis 15 in die entsprechende Hexadezimal-Darstellung um.
	 * @param b	Umzuwandelnde Zahl (0..15)
	 * @return	Hexadezimal-Darstellung
	 * @see #convertLineToRTF(String)
	 */
	private char hex(final int b) {
		if (b<10) return (char)(b+((short)'0')); else return (char)(b-10+((short)'a'));
	}

	@Override
	public String[] ownSettingsName() {
		return null;
	}

	@Override
	public Icon[] ownSettingsIcon() {
		return null;
	}

	@Override
	public boolean ownSettings(final StatisticsBasePanel owner, final int nr) {
		return false;
	}

	/**
	 * Wandelt eine Textzeile in eine RTF-Zeichenkette um
	 * @param line	Umzuwandelnde Zeile
	 * @return	RTF-Zeichenkette
	 * @see #getRTFText()
	 */
	private String convertLineToRTF(final String line) {
		StringBuilder result=new StringBuilder();
		for (int i=0;i<line.length();i++) {
			char c=line.charAt(i);
			if ((short)c<=127) {result.append(c); continue;}
			short b=(short)c;

			result.append("\\'");
			result.append(hex(b/16));
			result.append(hex(b%16));

		}
		return result.toString();
	}

	@Override
	public boolean saveDOCX(final XWPFDocument doc) {
		if (textPane==null) {
			buildText();
			initTextPane();
			initDescriptionPane();
		}

		XWPFParagraph p=null;

		for (int i=0;i<lines.size();i++) {
			final String line=lines.get(i);
			final int type=lineTypes.get(i);

			if (type==-1) {
				/* Absatzanfang */
				p=doc.createParagraph();
				continue;
			}
			if (type==-2) {
				/* Absatzende */
				p=null;
				continue;
			}
			if (type==0) {
				/* Normaler Text */
				if (p==null) p=doc.createParagraph();
				p.createRun().setText(line);
				p.createRun().addBreak();
				continue;
			}
			if (type>0) {
				/* �berschriften */
				p=doc.createParagraph();
				XWPFRun r=p.createRun();
				r.setBold(true);
				int fs=12;
				switch (type) {
				case 1: fs=18; break;
				case 2: fs=15; break;
				}
				r.setFontSize(fs);
				r.setText(line);
				p=null;
				continue;
			}
		}

		return true;
	}

	@Override
	public boolean savePDF(final PDFWriter pdf) {
		if (textPane==null) {
			buildText();
			initTextPane();
			initDescriptionPane();
		}

		boolean newParagraph=true;

		for (int i=0;i<lines.size();i++) {
			final String line=lines.get(i);
			final int type=lineTypes.get(i);

			if (type==-1) {
				/* Absatzanfang */
				newParagraph=true;
				continue;
			}
			if (type==-2) {
				/* Absatzende */
				newParagraph=true;
				continue;
			}
			if (type==0) {
				/* Normaler Text */
				if (newParagraph) {pdf.writeEmptySpace(10); newParagraph=false;}
				if (!pdf.writeText(line,11,false,0)) return false;
				continue;
			}
			if (type>0) {
				/* �berschriften */
				int fs=12;
				switch (type) {
				case 1: fs=18; break;
				case 2: fs=15; break;
				}
				pdf.writeEmptySpace(20);
				if (!pdf.writeText(line,fs,true,0)) return false;
				newParagraph=true;
				continue;
			}
		}
		pdf.writeEmptySpace(25);

		return true;
	}

	/**
	 * Berechnet aus einigem Messreihen-Kenngr��en die Standardabweichung
	 * @param x2	Quadrierte Summe der Werte
	 * @param x	Summe der Werte
	 * @param n	Anzahl der Werte
	 * @return	Standardabweichung der Messreihe
	 */
	protected static long calcStd(final long x2, final long x, final long n) {
		if (n>1) return Math.round(Math.sqrt(((double)x2)/(n-1)-((double)(x*x)/n/(n-1)))); else return 0;
	}

	/**
	 * Berechnet aus Messreihen-Kenngr��en die Standardabweichung
	 * @param x2	Quadrierte Summe der Werte
	 * @param x	Summe der Werte
	 * @param n	Anzahl der Werte
	 * @return	Standardabweichung der Messreihe
	 */
	protected static double calcStd(final double x2, final double x, final double n) {
		if (n>1) return Math.sqrt(x2/(n-1)-x*x/n/(n-1)); else return 0;
	}

	/**
	 * Berechnet aus Messreihen-Kenngr��en ein Konfidenzintervall
	 * @param x2	Quadrierte Summe der Werte
	 * @param x	Summe der Werte
	 * @param n	Anzahl der Werte
	 * @param p	Wahrscheinlichkeit, zu der das Konfidenzintervall bestimmt werden soll
	 * @return	Konfidenzintervall der Messreihe zur Wahrscheinlichkeit <code>p</code>
	 */
	protected static double[] calcConfidence(final double x2, final double x, final double n, final double p) {
		final double[] interval=new double[2];

		final double mean=x/n;
		final double sd=calcStd(x2,x,n);

		/* x +- t(n-1;1-alpha/2)*sd/sqrt(n) */
		final TDistribution dist=new TDistribution(n-1);
		final double t=dist.inverseCumulativeProbability(1-p/2);
		final double half=t*sd/Math.sqrt(n);

		interval[0]=mean-half;
		interval[1]=mean+half;

		return interval;
	}

	/**
	 * F�gt eine �berschrift zu der Ausgabe hinzu
	 * @param level	Ebene der �berschrift; g�ltige Werte: 1,2,3
	 * @param s	Textzeile, die ausgegeben werden soll
	 */
	protected void addHeading(final int level, final String s) {
		lines.add(s);
		hints.add(null);
		lineTypes.add(level);
		indentLevel.add(0);
	}

	/**
	 * F�gt eine Textzeile an die Ausgabe an
	 * @param indentLevel	Einr�ck-Level (0=keine Einr�ckung; 1=2 Leerzeichen, 2=4 Leerzeichen usw.)
	 * @param line	Textzeile, die ausgegeben werden soll
	 */
	protected void addLine(final int indentLevel, final String line) {
		lines.add(line);
		hints.add(null);
		lineTypes.add(0);
		this.indentLevel.add(indentLevel);
	}

	/**
	 * F�gt eine Textzeile an die Ausgabe an
	 * @param indentLevel	Einr�ck-Level (0=keine Einr�ckung; 1=2 Leerzeichen, 2=4 Leerzeichen usw.)
	 * @param line	Textzeile, die ausgegeben werden soll
	 * @param hint	Optionaler zus�tzlicher Hinweis f�r das Kontextmen�
	 * @see #processContextClick(String)
	 */
	protected void addLine(final int indentLevel, final String line, final String hint) {
		lines.add(line);
		hints.add(hint);
		lineTypes.add(0);
		this.indentLevel.add(indentLevel);
	}

	/**
	 * F�gt einen Link an die Ausgabe an
	 * @param indentLevel	Einr�ck-Level (0=keine Einr�ckung; 1=2 Leerzeichen, 2=4 Leerzeichen usw.)
	 * @param link	Bezeichner des Links
	 * @param text	Anzuzeigender Text
	 * @see StatisticViewerText#processLinkClick(String)
	 */
	protected void addLink(final int indentLevel, final String link, final String text) {
		lines.add(text);
		hints.add(link);
		lineTypes.add(-3);
		this.indentLevel.add(indentLevel);
	}

	/**
	 * Beginnt einen neuen Absatz in der Ausgabe
	 */
	protected void beginParagraph() {
		lines.add("");
		hints.add(null);
		lineTypes.add((-1));
		indentLevel.add(0);
	}

	/**
	 * Beendet den aktuellen Absatz in der Ausgabe
	 */
	protected void endParagraph() {
		lines.add("");
		hints.add(null);
		lineTypes.add((-2));
		indentLevel.add(0);
	}

	/**
	 * F�gt eine Textzeile (ohne Einr�ckung) an die Ausgabe an
	 * @param line	Textzeile, die ausgegeben werden soll
	 */
	protected void addLine(final String line) {
		addLine(0,line);
	}

	/**
	 * F�gt eine Textzeile (ohne Einr�ckung) an die Ausgabe an
	 * @param line	Textzeile, die ausgegeben werden soll
	 * @param hint	Optionaler zus�tzlicher Hinweis f�r das Kontextmen�
	 */
	protected void addLine(final String line, final String hint) {
		addLine(0,line,hint);
	}

	/**
	 * F�gt einen Link (ohne Einr�ckung) an die Ausgabe an
	 * @param link	Bezeichner des Links
	 * @param text	Anzuzeigender Text
	 * @see StatisticViewerText#processLinkClick(String)
	 */
	protected void addLink(final String link, final String text) {
		addLink(0,link,text);
	}

	/**
	 * F�gt mehrere Textzeilen (ohne Einr�ckung) an die Ausgabe an
	 * An jedem "\n" erfolgt ein Zeilenumbruch
	 * @param s	Textzeilen, die ausgegeben werden sollen
	 */
	protected void addLines(final String s) {
		String[] list=s.split("\n");
		for (int i=0;i<list.length;i++) addLine(0,list[i]);
	}

	/**
	 * F�gt eine Textzeile mit einem Zahlenwert an die Ausgabe an
	 * @param indentLevel	Einr�ck-Level (0=keine Einr�ckung; 1=2 Leerzeichen, 2=4 Leerzeichen usw.)
	 * @param label	Beschriftung des Wertes, der ausgegeben werden soll
	 * @param data	Wert, der durch <code>days</code> geteilt ausgegeben werden soll
	 * @param days	Anzahl der Tage, durch die der summierte Wert <code>data</code> geteilt werden soll
	 * @param sum	Ist dieser Wert &gt;0, so wird zus�tzlich ein Prozentwert, der sich aus dem Bruch <code>data/sum</code> ergibt, ausgegeben
	 */
	protected void addLine(final int indentLevel, final String label, final int data, final int days, final int sum) {
		String s=label+": ";
		s+=NumberTools.formatNumber((double)data/days,3);
		if (sum>0) s+=" ("+NumberTools.formatPercent((double)data/sum)+")";
		addLine(indentLevel,s);
	}

	/**
	 * F�gt eine Textzeile mit einem Zahlenwert an die Ausgabe an
	 * @param indentLevel	Einr�ck-Level (0=keine Einr�ckung; 1=2 Leerzeichen, 2=4 Leerzeichen usw.)
	 * @param label	Beschriftung des Wertes, der ausgegeben werden soll
	 * @param data	Wert, der durch <code>div</code> geteilt ausgegeben werden soll
	 * @param div	Wert, durch den der summierte Wert <code>data</code> geteilt werden soll
	 */
	protected void addLineDiv(final int indentLevel, final String label, final double data, final double div) {
		addLine(indentLevel,label+": "+NumberTools.formatNumber(data/div,3));
	}

	/**
	 * F�gt eine Textzeile mit einem Zahlenwert an die Ausgabe an
	 * @param indentLevel	Einr�ck-Level (0=keine Einr�ckung; 1=2 Leerzeichen, 2=4 Leerzeichen usw.)
	 * @param label	Beschriftung des Wertes, der ausgegeben werden soll
	 * @param data	Wert, der durch <code>div</code> geteilt ausgegeben werden soll
	 * @param div	Wert, durch den der summierte Wert <code>data</code> geteilt werden soll
	 */
	protected void addLineDiv2(final int indentLevel, final String label, final double data, final double div) {
		addLine(indentLevel,label+": "+NumberTools.formatNumber(data/div,2));
	}

	/**
	 * F�gt eine Textzeile mit einem Zahlenwert an die Ausgabe an
	 * @param indentLevel	Einr�ck-Level (0=keine Einr�ckung; 1=2 Leerzeichen, 2=4 Leerzeichen usw.)
	 * @param label	Beschriftung des Wertes, der ausgegeben werden soll
	 * @param data	Wert, der ausgegeben werden soll
	 * @param digits	Anzahl an anzuzeigenden Nachkommastellen
	 */
	protected void addLine(final int indentLevel, final String label, final double data, final int digits) {
		addLine(indentLevel,label+": "+NumberTools.formatNumber(data,digits));
	}

	/**
	 * F�gt eine Textzeile mit einem Zahlenwert an die Ausgabe an.
	 * Es wird dabei eine Nachkommastelle ausgegeben.
	 * @param indentLevel	Einr�ck-Level (0=keine Einr�ckung; 1=2 Leerzeichen, 2=4 Leerzeichen usw.)
	 * @param label	Beschriftung des Wertes, der ausgegeben werden soll
	 * @param data	Wert, der ausgegeben werden soll
	 */
	protected void addLine(final int indentLevel, final String label, final double data) {
		addLine(indentLevel,label,data,1);
	}

	/**
	 * F�gt eine Textzeile mit einem Zahlenwert in Prozent-Schreibweise an die Ausgabe an
	 * @param indentLevel	Einr�ck-Level (0=keine Einr�ckung; 1=2 Leerzeichen, 2=4 Leerzeichen usw.)
	 * @param label	Beschriftung des Wertes, der ausgegeben werden soll
	 * @param value	Wert, der in Prozentdarstellung ausgegeben werden soll
	 * @param digits	Anzahl an anzuzeigenden Nachkommastellen
	 */
	protected void addPercentLine(final int indentLevel, final String label, final double value, final int digits) {
		addLine(indentLevel,label+": "+NumberTools.formatPercent(value,digits));
	}

	/**
	 * F�gt eine Textzeile mit einem Zahlenwert in Prozent-Schreibweise an die Ausgabe an.
	 * Es wird dabei eine Nachkommastelle ausgegeben.
	 * @param indentLevel	Einr�ck-Level (0=keine Einr�ckung; 1=2 Leerzeichen, 2=4 Leerzeichen usw.)
	 * @param label	Beschriftung des Wertes, der ausgegeben werden soll
	 * @param value	Wert, der in Prozentdarstellung ausgegeben werden soll
	 */
	protected void addPercentLine(final int indentLevel, final String label, final double value) {
		addLine(indentLevel,label,value,1);
	}

	/**
	 * F�gt eine Textzeile mit einem Zahlenwert in Prozent-Schreibweise an die Ausgabe an
	 * @param indentLevel	Einr�ck-Level (0=keine Einr�ckung; 1=2 Leerzeichen, 2=4 Leerzeichen usw.)
	 * @param label	Beschriftung des Wertes, der ausgegeben werden soll
	 * @param zaehler	Ausgegeben wird die Zahl zaehler/nenner. Hier wird der Z�hler des Bruches �bergeben.
	 * @param nenner	Ausgegeben wird die Zahl zaehler/nenner. Hier wird der Nenner des Bruches �bergeben.
	 * @param digits	Anzahl an anzuzeigenden Nachkommastellen
	 */
	protected void addPercentLineParts(final int indentLevel, final String label, final long zaehler, final long nenner, final int digits) {
		if (nenner==0) return;
		addPercentLine(indentLevel,label,((double)zaehler)/nenner,digits);
	}

	/**
	 * F�gt eine Textzeile mit einem Zahlenwert in Prozent-Schreibweise an die Ausgabe an.
	 * Es wird dabei eine Nachkommastelle ausgegeben.
	 * @param indentLevel	Einr�ck-Level (0=keine Einr�ckung; 1=2 Leerzeichen, 2=4 Leerzeichen usw.)
	 * @param label	Beschriftung des Wertes, der ausgegeben werden soll
	 * @param zaehler	Ausgegeben wird die Zahl zaehler/nenner. Hier wird der Z�hler des Bruches �bergeben.
	 * @param nenner	Ausgegeben wird die Zahl zaehler/nenner. Hier wird der Nenner des Bruches �bergeben.
	 */
	protected void addPercentLineParts(final int indentLevel, final String label, final long zaehler, final long nenner) {
		addPercentLineParts(indentLevel,label,zaehler,nenner,1);
	}

	/**
	 * F�gt eine Textzeile mit einer Zeitangabe in Sekunden an die Ausgabe an.
	 * @param indentLevel	Einr�ck-Level (0=keine Einr�ckung; 1=2 Leerzeichen, 2=4 Leerzeichen usw.)
	 * @param label	Beschriftung des Wertes, der ausgegeben werden soll
	 * @param time	Zeitangabe (in Sekunden), die ausgegeben werden soll.
	 */
	protected void addShortTime(final int indentLevel, final String label, final double time) {
		addLine(indentLevel,label+": "+NumberTools.formatNumber(time,1)+" "+StatisticsBasePanel.viewersTextSeconds+" (="+TimeTools.formatTime((int)Math.round(time))+")");
	}

	/**
	 * F�gt eine Textzeile mit einer Zeitangabe in Sekunden an die Ausgabe an.
	 * @param indentLevel	Einr�ck-Level (0=keine Einr�ckung; 1=2 Leerzeichen, 2=4 Leerzeichen usw.)
	 * @param label	Beschriftung des Wertes, der ausgegeben werden soll
	 * @param zaehler	Ausgegeben wird die Zeit zaehler/nenner. Hier wird der Z�hler des Bruches �bergeben.
	 * @param nenner	Ausgegeben wird die Zeit zaehler/nenner. Hier wird der Nenner des Bruches �bergeben.
	 */
	protected void addShortTimeParts(final int indentLevel, final String label, final long zaehler, final long nenner) {
		if (nenner==0) return;
		addShortTime(indentLevel,label,((double)zaehler)/nenner);
	}

	/**
	 * F�gt eine Textzeile mit einer Zeitangabe an die Ausgabe an.
	 * @param indentLevel	Einr�ck-Level (0=keine Einr�ckung; 1=2 Leerzeichen, 2=4 Leerzeichen usw.)
	 * @param label	Beschriftung des Wertes, der ausgegeben werden soll
	 * @param time	Zeitangabe (in Sekunden), die ausgegeben werden soll.
	 * @param div	Wert, durch den der summierte Wert <code>time</code> geteilt werden soll
	 * @param sum	Ist dieser Wert &gt;0, so wird zus�tzlich ein Prozentwert, der sich aus dem Bruch <code>time/sum</code> ergibt, ausgegeben
	 */
	protected void addPercentTime(final int indentLevel, final String label, final long time, final long div, final long sum) {
		String s=label+": ";
		s+=TimeTools.formatTime(time/div);
		if (sum>0) s+=" = "+NumberTools.formatPercent((double)time/sum);
		addLine(indentLevel,s);
	}

	/**
	 * Wandelt einen Punkt und eine Cursorposition in {@link #textPane} in einen zugeh�rigen Link um
	 * @param point	Angeklickter Punkt innerhalb von {@link #textPane}
	 * @return	Liefert im Erfolgsfall die URL als Text, sonst <code>null</code>
	 * @see #initTextPane()
	 */
	private String pointToLink(final Point point) {
		/*
		Urspr�nglich:
		offset=textPane.viewToModel(point);
		aber das ist deprecated seit Java 9.
		Alternative "viewToModel2D" gibt's erst seit Java 9.
		Bei Code Style Level = 1.8 ist das ein Problem.
		 */

		if (point.x>100) return null; /* Der "Details"-Link ist eher 20 Pixel breit. Bei 100 sind wir definitiv au�erhalb des Textes. */

		final int offset=textPane.getCaretPosition();

		if (offset<0) return null;
		final Element element=textPane.getStyledDocument().getCharacterElement(offset);
		if (element==null) return null;
		final Object obj=element.getAttributes().getAttribute("URL");
		if (!(obj instanceof String)) return null;
		return (String)obj;
	}

	/**
	 * Liefert zu der aktuellen Cursorposition in {@link #textPane} den hinterlegten Hinweistext
	 * @return	Liefert im Erfolgsfall den hinterlegten Hinweistext, sonst <code>null</code>
	 * @see #initTextPane()
	 */
	private String pointToHint() {
		/*
		Urspr�nglich:
		offset=textPane.viewToModel(point);
		aber das ist deprecated seit Java 9.
		Alternative "viewToModel2D" gibt's erst seit Java 9.
		Bei Code Style Level = 1.8 ist das ein Problem.
		 */
		final int offset=textPane.getCaretPosition();

		if (offset<0) return null;
		final Element element=textPane.getStyledDocument().getCharacterElement(offset);
		if (element==null) return null;
		final Object obj=element.getAttributes().getAttribute("Hint");
		if (!(obj instanceof String)) return null;
		return (String)obj;
	}

	/**
	 * Wird aufgerufen, wenn ein Link im Text angeklickt wurde.<br>
	 * Muss von abgeleiteten Klassen �berschrieben werden, um eine Behandlung des Linkklicks durchzuf�hren.
	 * @param link	Bezeichner des Links
	 * @see StatisticViewerText#addLink(String, String)
	 * @see StatisticViewerText#addLink(int, String, String)
	 */
	protected void processLinkClick(final String link) {
	}

	@Override
	public void setRequestImageSize(final IntSupplier getImageSize) {}

	@Override
	public void setUpdateImageSize(final IntConsumer setImageSize) {}

	@Override
	public void setRequestChartSetup(Supplier<ChartSetup> getChartSetup) {}

	@Override
	public void setUpdateChartSetup(Consumer<ChartSetup> setChartSetup) {	}

	/**
	 * Zeigt im Fu�bereich der Hilfeseite eine "Erkl�rung einblenden"-Schaltfl�che, die,
	 * wenn sie angeklickt wird, die html-Seite der angegebenen Adresse anzeigt.
	 * @param descriptionURL	html-Seite mit einer zus�tzlichen Erkl�rung zu dieser Statistikseite
	 * @param descriptionHelpCallback	Handler, der Themennamen (angegeben �ber "help:..."-Links) zum Aufruf normaler Hilfeseiten entgegen nimmt
	 */
	protected final void addDescription(final URL descriptionURL, final Consumer<String> descriptionHelpCallback) {
		this.descriptionURL=descriptionURL;
		this.descriptionHelpCallback=descriptionHelpCallback;
	}

	/**
	 * Soll f�r diese Komponente der Standard-FileDrop-Listener des {@link StatisticsBasePanel} verwendet werden?
	 * @return	�bergeordneten FileDrop-Listener verwenden (<code>false</code>) oder eigenen (<code>true</code>)
	 */
	@Override
	public boolean hasOwnFileDropListener() {
		return false;
	}

	/**
	 * Sucht einen Text in einem Element und seinen Unterelementen
	 * @param element	Element von dem die Suche ausgehen soll
	 * @param searchLower	Suchtext in Kleinschreibung
	 * @param hits	Liste mit den Fundstellen (Cursorpositionen)
	 * @see #getCaretPositions(String)
	 */
	private void searchInElement(final Element element, final String searchLower, final List<Integer> hits) {
		for (int i=0;i<element.getElementCount();i++) {
			searchInElement(element.getElement(i),searchLower,hits);
		}

		if (element instanceof LeafElement) {
			final LeafElement leaf=(LeafElement)element;
			try {
				final int start=leaf.getStartOffset();
				final String textLower=textPane.getText(start,leaf.getEndOffset()-start).toLowerCase();

				int index=-1;
				while (true) {
					index=textLower.indexOf(searchLower,index+1);
					if (index<0) break;
					hits.add(start+index);
				}
			} catch (BadLocationException e) {}
		}
	}

	/**
	 * Sucht in dem Viewer nach einem Text und liefert die Cursorpositionen der Fundstellen
	 * @param search	Zu suchender Text
	 * @return	Liste mit den Cursorpositionen der Fundstellen (kann leer sein, ist aber nie <code>null</code>)
	 * @see #search(Component)
	 */
	private List<Integer> getCaretPositions(final String search) {
		final List<Integer> hits=new ArrayList<>();
		final String searchLower=search.toLowerCase();

		searchInElement(textPane.getStyledDocument().getDefaultRootElement(),searchLower,hits);

		return hits;
	}

	/**
	 * Markiert die Fundstellen einer Suche.
	 * @param owner	�bergeordnetes Element um optional den "Keine Treffer"-Dialog auszurichten
	 * @param search	Suchtext
	 * @param hits	Liste mit den Treffern
	 * @see #search(Component)
	 */
	private void processSearchResults(final Component owner, final String search, final List<Integer> hits) {
		textPane.getHighlighter().removeAllHighlights();

		if (hits==null || hits.isEmpty()) {
			MsgBox.info(owner,StatisticsBasePanel.viewersToolbarSearch,String.format(StatisticsBasePanel.viewersToolbarSearchNotFound,search));
			return;
		}

		final DefaultHighlighter.DefaultHighlightPainter highlightPainter=new DefaultHighlighter.DefaultHighlightPainter(Color.YELLOW);
		for (Integer hit: hits) {
			try {
				textPane.getHighlighter().addHighlight(hit,hit+search.length(),highlightPainter);
			} catch (BadLocationException e) {}
		}
		textPane.setCaretPosition(hits.get(0));
	}

	@Override
	public void search(final Component owner) {
		if (textPane==null) {
			buildText();
			initTextPane();
			initDescriptionPane();
		}

		final String search=JOptionPane.showInputDialog(owner,StatisticsBasePanel.viewersToolbarSearchTitle);
		if (search==null) {
			textPane.getHighlighter().removeAllHighlights();
			return;
		}

		final List<Integer> hits=getCaretPositions(search);
		processSearchResults(owner,search,hits);
	}

	/**
	 * Dokument f�r die Anzeige in {@link StatisticViewerText#textPane}
	 * @see StatisticViewerText#textPane
	 */
	private class FastDefaultStyledDocument extends DefaultStyledDocument {
		/**
		 * Serialisierungs-ID der Klasse
		 * @see Serializable
		 */
		private static final long serialVersionUID=1626728203320204205L;

		/**
		 * Liste der anzuzeigenden Einzelelemente
		 */
		private List<ElementSpec> buffer;

		/**
		 * Konstruktor der Klasse
		 */
		public FastDefaultStyledDocument() {
			buffer=new ArrayList<>();
		}

		/**
		 * Standard-Element zur Darstellung eines Abschnitts-Endes
		 * @see #addText(AttributeSet, String)
		 */
		private final ElementSpec endTag=new ElementSpec(null,ElementSpec.EndTagType);

		/**
		 * Standard-Element zur Darstellung eines Abschnitts-Beginns
		 * @see #addText(AttributeSet, String)
		 */
		private final ElementSpec startTag=new ElementSpec(null,ElementSpec.StartTagType);

		/**
		 * F�gt ein neues Ausgabeelement zu der Liste der auszugebenden Elemente hinzu.
		 * @param attr	Formatierungsattribute
		 * @param text	Auszugebender Text
		 * @see #finalizeText()
		 */
		public void addText(final AttributeSet attr, final String text)  {
			buffer.add(endTag);
			buffer.add(startTag);
			buffer.add(new ElementSpec(attr,ElementSpec.ContentType,text.toCharArray(),0,text.length()));
		}

		/**
		 * �bertr�gt die in {@link #addText(AttributeSet, String)} gesammelten Elemente
		 * in das Dokument.
		 * @return	Liefert <code>true</code>, wenn die Elemente erfolgreich in das Dokument �bertragen werden konnten
		 * @see #addText(AttributeSet, String)
		 */
		public boolean finalizeText() {
			try {
				insert(getLength(),buffer.toArray(new ElementSpec[0]));
			} catch (BadLocationException e) {
				return false;
			}
			return true;
		}
	}
}
