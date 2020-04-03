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
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.JPanel;

import language.Language;

/**
 * Zeigt eine Übersicht zu dem Callcenter-Modell als Warteschlangenmodell in einem Panel an.
 * @author Alexander Herzog
 * @version 1.0
 */
public class CallcenterModellPanel extends JPanel {
	private static final long serialVersionUID = 8045548555129632452L;

	private final String version;

	/**
	 * Konstruktor der Klasse
	 * @param version	Versionskennung die unten im Modell angezeigt wird
	 */
	public CallcenterModellPanel(final String version) {
		super();
		this.version=version;
	}

	private void pfeil(Graphics g, int x, int y, int width, String s1, String s2) {
		g.drawLine(x,y,x+width,y);
		g.drawLine(x+width,y,x+width-width/4,y-width/4);
		g.drawLine(x+width,y,x+width-width/4,y+width/4);
		if (!s1.isEmpty()) g.drawString(s1,x,y-g.getFontMetrics().getDescent());
		if (!s2.isEmpty()) g.drawString(s2,x,y+g.getFontMetrics().getAscent()-g.getFontMetrics().getDescent());
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		String s;
		Rectangle r=getBounds();
		int x=r.width;
		int y=r.height;

		/* Seitenverhältnis festlegen */
		x=Math.min(x,(int)Math.round(y*1.2));
		y=(int)Math.round(x/1.2);

		g.setFont(new Font(g.getFont().getName(),Font.BOLD,18));
		g.drawString(Language.tr("ModelInfo.Title"),x/40,y/20);

		g.setFont(new Font(g.getFont().getName(),Font.PLAIN,11));

		/* Ankunftsstrom-Pfeil */
		pfeil(g,1*x/10,y/2,1*x/10,Language.tr("ModelInfo.ArrivalStream.Line1"),Language.tr("ModelInfo.ArrivalStream.Line2"));

		/* Kasten */
		g.drawRect(9*x/40,4*y/10,2*x/10,2*y/10);
		for (int i=1;i<=3;i++) g.drawLine(9*x/40+2*x/10*i/4,4*y/10,9*x/40+2*x/10*i/4,6*y/10);
		g.drawString(Language.tr("ModelInfo.WaitingRoomSize"),9*x/40,4*y/10-g.getFontMetrics().getDescent());

		/* Bediener */
		g.drawOval(17*x/40,7*y/20,6*x/20,6*y/20);
		s=Language.tr("ModelInfo.Operators");
		g.drawString(s,23*x/40-g.getFontMetrics().stringWidth(s)/2,y/2+g.getFontMetrics().getAscent()/2);

		/* Ende */
		pfeil(g,15*x/20,y/2,2*x/10,Language.tr("ModelInfo.ServedClients.Line1"),Language.tr("ModelInfo.ServedClients.Line2"));

		/* Weiterleitungen */

		g.drawLine(15*x/20,4*y/10,17*x/20,4*y/10);
		g.drawLine(17*x/20,4*y/10,17*x/20,2*y/10);
		g.drawLine(17*x/20,2*y/10,1*x/10,2*y/10);
		g.drawLine(1*x/10,2*y/10,1*x/10,17*y/40);
		pfeil(g,1*x/10,17*y/40,1*x/10,"","");
		s=Language.tr("ModelInfo.Forwardings");
		g.drawString(s,19*x/40-g.getFontMetrics().stringWidth(s)/2,2*y/10-g.getFontMetrics().getDescent());

		/* Wiederholer */
		g.drawLine(13*x/40,25*y/40,13*x/40,8*y/10);
		g.drawLine(13*x/40,8*y/10,1*x/10,8*y/10);
		g.drawLine(1*x/10,8*y/10,1*x/10,23*y/40);
		pfeil(g,1*x/10,23*y/40,1*x/10,"","");
		g.drawString(Language.tr("ModelInfo.Cancel.Line1"),27*x/80,14*y/20);
		g.drawString(Language.tr("ModelInfo.Cancel.Line2"),27*x/80,14*y/20+g.getFontMetrics().getHeight());
		g.drawString(Language.tr("ModelInfo.Cancel.Line3"),27*x/80,14*y/20+g.getFontMetrics().getHeight()*2);
		g.drawString(Language.tr("ModelInfo.Cancel.Line4"),27*x/80,14*y/20+g.getFontMetrics().getHeight()*3);

		/* Copyright */
		g.setFont(new Font(g.getFont().getName(),Font.PLAIN,11));
		g.drawString(MainFrame.PROGRAM_NAME+" "+Language.tr("InfoDialog.Version.Lower")+" "+version+" "+Language.tr("InfoDialog.WrittenBy")+" "+MainPanel.AUTHOR,x/40,y-g.getFontMetrics().getDescent());
	}
}
