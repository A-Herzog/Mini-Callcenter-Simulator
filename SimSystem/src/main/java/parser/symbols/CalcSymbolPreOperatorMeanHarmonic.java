/**
 * Copyright 2023 Alexander Herzog
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
package parser.symbols;

import parser.MathCalcError;
import parser.coresymbols.CalcSymbolPreOperator;

/**
 * Harmonischer Mittelwert der Datenreihe, die aus den �bergebenen Parametern gebildet wird
 * @author Alexander Herzog
 */
public final class CalcSymbolPreOperatorMeanHarmonic extends CalcSymbolPreOperator {
	/**
	 * Namen f�r das Symbol
	 * @see #getNames()
	 */
	private static final String[] names=new String[]{"HarmonicMean","HarmonischerMittelwert"};

	/**
	 * Konstruktor der Klasse
	 */
	public CalcSymbolPreOperatorMeanHarmonic() {
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
	protected double calc(double[] parameters) throws MathCalcError {
		if (parameters.length==0) throw error();
		double sum=0;
		for (double d: parameters) {
			if (d==0.0) throw error();
			sum+=1/d;
		}
		return parameters.length/sum;
	}

	@Override
	protected double calcOrDefault(final double[] parameters, final double fallbackValue) {
		if (parameters.length==0) return fallbackValue;
		double sum=0;
		for (double d: parameters) {
			if (d==0.0) return fallbackValue;
			sum+=1/d;
		}
		return parameters.length/sum;
	}
}
