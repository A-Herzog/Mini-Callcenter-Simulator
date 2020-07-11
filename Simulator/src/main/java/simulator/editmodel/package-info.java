/**
 * Das in diesem Package abgebildete Editor-Modell kann aus xml-Dateien geladen
 * und in diese geschrieben werden. Stationen, Eigenschaften usw. sind hier noch
 * nicht �ber Referenzen verbunden und das Modell ist noch nicht auf Konsistenz
 * gepr�ft. Dies ist das Modell, welches auch in {@link ui.EditorPanel}
 * zum Bearbeiten angeboten wird. Mit Hilfe von {@link simulator.runmodel.RunModel#getRunModel(EditModel)}
 * kann es gepr�ft und in ein simulierbares {@link simulator.runmodel.RunModel}
 * umgewandelt werden.<br><br>
 * Die wesentliche Klasse in diesem Package ist:<br>
 * {@link simulator.editmodel.EditModel}
 * @author Alexander Herzog
 * @see simulator.editmodel.EditModel
 * @see ui.EditorPanel
 * @see simulator.runmodel.RunModel#getRunModel(EditModel)
 * @see simulator.runmodel.RunModel
 */
package simulator.editmodel;