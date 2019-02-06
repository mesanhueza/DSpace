package org.dspace.ctask.general;

import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.authority.Choices;
import org.dspace.content.authority.factory.ContentAuthorityServiceFactory;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;

import java.io.IOException;
import java.util.List;

public class MetadataAuthorityQualityControl extends AbstractCurationTask {

	protected int status = Curator.CURATE_UNSET;
	private Item item;

	protected MetadataAuthorityService metadataAuthorityService = ContentAuthorityServiceFactory.getInstance()
			.getMetadataAuthorityService();
	protected ChoiceAuthorityService choiceAuthorityService = ContentAuthorityServiceFactory.getInstance()
			.getChoiceAuthorityService();

	protected boolean fixmode = false;
	protected boolean fixvariants = false;

	@Override
	public void init(Curator curator, String taskId) throws IOException {
		super.init(curator, taskId);
		fixmode = taskBooleanProperty("fixmode", false);
		if (fixmode) {
			fixvariants = taskBooleanProperty("fixvariants", false);
		}
	}

	@Override
	public int perform(DSpaceObject dso) throws IOException {
		StringBuilder resultReport = new StringBuilder();
		if (dso instanceof Item) {
			item = (Item) dso;
			List<MetadataValue> values = itemService.getMetadata(item, Item.ANY, Item.ANY, Item.ANY, Item.ANY);
			for (MetadataValue value : values) {
				if (metadataAuthorityService.isAuthorityControlled(value.getMetadataField())) {
					resultReport.append("\n\n");
					resultReport.append("Analizando metadata " + value.getMetadataField().toString() + ":\n");
					if (value.getAuthority() == null
							&& metadataAuthorityService.isAuthorityRequired(value.getMetadataField())) {
						updateMetadataAuthority(value, resultReport);
					} else if (value.getAuthority() == null) {
						updateConfidenceWithoutAuthority(value, resultReport);
					} else {
						updateMetadataValue(value, resultReport);
					}
				}
			}
			resultReport.append("\n\n");
			status = Curator.CURATE_SUCCESS;
		} else {
			resultReport.append("No es un item");
			status = Curator.CURATE_SKIP;
		}
		setResult(resultReport.toString());
		report(resultReport.toString());
		return status;
	}

	private void updateMetadataAuthority(MetadataValue value, StringBuilder resultReport) {
		Choices choices = choiceAuthorityService.getBestMatch(value.getMetadataField().toString(), value.getValue(),
				item.getOwningCollection(), null);
		resultReport.append(
				"Es authority required pero la authority key está en null con text_value " + value.getValue() + "\n");
		if (choices.values.length > 0) {
			String newAuthority = choices.values[0].authority;
			String label = choices.values[0].label;
			int newConfidence = choices.confidence;
			if (label.equals(value.getValue())) {
				if (fixmode) {
					value.setAuthority(newAuthority);
					value.setConfidence(choices.confidence);
					resultReport.append("Se cambió la authority key por " + newAuthority + "\n");
					resultReport.append("Se cambió el confidence a " + newConfidence + "\n");
				} else {
					resultReport.append("Existe una authority key válida: " + newAuthority + "\n");
				}
			} else {
				resultReport.append("Existe una posible authority key válida: " + newAuthority + ", con confidence "
						+ newConfidence + "\n");
			}
		} else {
			resultReport.append("No se encontró ninguna authority key válida \n");
			if (value.getConfidence() != Choices.CF_NOTFOUND) {
				if (fixmode) {
					value.setConfidence(Choices.CF_NOTFOUND);
					resultReport.append("Se cambió el confidence a " + Choices.CF_NOTFOUND + "\n");
				} else {
					resultReport.append("El confidence es incorrecto: " + value.getConfidence() + "\n");
				}
			}
		}
	}

	private void updateMetadataValue(MetadataValue value, StringBuilder resultReport) {
		String label = choiceAuthorityService.getLabel(value.getMetadataField().toString(), value.getAuthority(), null);
		if (label == null || label.isEmpty()) {
			resultReport.append("La authority key es inválida \n");
			if (fixmode) {
				value.setConfidence(Choices.CF_NOTFOUND);
				resultReport.append("Se cambió el confidence a " + Choices.CF_NOTFOUND);
			} else if (value.getConfidence() != Choices.CF_NOTFOUND) {
				resultReport.append("Se debería cambiar el confidence a " + Choices.CF_NOTFOUND);
			}
		} else if (!label.equals(value.getValue())) {
			resultReport.append("La authority key no coincide con el label \n");
			if (fixvariants) {
				value.setValue(label);
				value.setConfidence(Choices.CF_UNCERTAIN);
				resultReport.append("Se cambió el label a " + label + "\n");
				resultReport.append("Se cambió el confidence a " + Choices.CF_UNCERTAIN + "\n");
			}
		} else if (value.getConfidence() < Choices.CF_UNCERTAIN) {
			resultReport.append("El confidence es incorrecto \n");
			if (fixmode) {
				value.setConfidence(Choices.CF_UNCERTAIN);
				resultReport.append("Se cambió el confidence a " + Choices.CF_UNCERTAIN + "\n");
			}
		} else {
			resultReport.append("No se encontraron inconsistencias \n");
		}
	}

	private void updateConfidenceWithoutAuthority(MetadataValue value, StringBuilder resultReport) {
		resultReport.append("La authority key está en null pero el metadato es authority optional \n");
		if (value.getConfidence() > Choices.CF_NOVALUE) {
			resultReport.append("El confidence es incorrecto \n");
			if (fixmode) {
				value.setConfidence(Choices.CF_NOVALUE);
				resultReport.append("Se cambió el confidence a " + Choices.CF_NOVALUE + "\n");
			}
		} else {
			resultReport.append("No se encontraron inconsistencias \n");
		}
	}

}