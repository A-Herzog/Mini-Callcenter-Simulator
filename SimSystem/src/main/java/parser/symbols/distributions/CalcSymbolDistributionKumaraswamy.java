/**
 * Copyright 2024 Alexander Herzog
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
package parser.symbols.distributions;

import org.apache.commons.math3.distribution.AbstractRealDistribution;

import mathtools.distribution.KumaraswamyDistribution;
import mathtools.distribution.ReciprocalDistribution;

/**
 * Kumaraswamy-Verteilung
 * @author Alexander Herzog
 * @see ReciprocalDistribution
 */
public class CalcSymbolDistributionKumaraswamy extends CalcSymbolDistribution {
	/**
	 * Namen f�r das Symbol
	 * @see #getNames()
	 */
	private static final String[] names=new String[]{"KumaraswamyDist","KumaraswamyDistribution","KumaraswamyVerteilung"};

	/**
	 * Konstruktor der Klasse
	 */
	public CalcSymbolDistributionKumaraswamy() {
		/*
		 * Wird nur ben�tigt, um einen JavaDoc-Kommentar f�r diesen (impliziten) Konstruktor
		 * setzen zu k�nnen, damit der JavaDoc-Compiler keine Warnung mehr ausgibt.
		 */
	}

	@Override
	public String[] getNames() {
		return names;
	}

	@Override
	protected int getParameterCount() {
		return 4;
	}

	@Override
	protected AbstractRealDistribution getDistribution(double[] parameters) {
		return new KumaraswamyDistribution(parameters[0],parameters[1],parameters[2],parameters[3]);
	}
}
