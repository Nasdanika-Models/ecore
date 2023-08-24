package org.nasdanika.models.ecore.graph.processors;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EModelElement;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EcorePackage;
import org.nasdanika.common.Context;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.diagram.plantuml.clazz.DiagramElement;
import org.nasdanika.graph.emf.EReferenceConnection;
import org.nasdanika.graph.processor.NodeProcessorConfig;
import org.nasdanika.html.model.app.Action;
import org.nasdanika.html.model.app.Label;
import org.nasdanika.html.model.app.gen.DynamicTableBuilder;
import org.nasdanika.html.model.app.graph.WidgetFactory;
import org.nasdanika.html.model.app.graph.emf.OutgoingReferenceBuilder;

public class EEnumNodeProcessor extends EDataTypeNodeProcessor<EEnum> {

	public EEnumNodeProcessor(
			NodeProcessorConfig<WidgetFactory, WidgetFactory> config,
			Context context,
			java.util.function.Function<ProgressMonitor, Action> prototypeProvider) {
		super(config, context, prototypeProvider);
	}	
	
	@Override
	protected boolean isCallOutgoingReferenceLabelsSuppliers(EReference eReference) {
		if (eReference == EcorePackage.Literals.EENUM__ELITERALS) {
			return true;
		}
		return super.isCallOutgoingReferenceLabelsSuppliers(eReference);
	}	

	@OutgoingReferenceBuilder(EcorePackage.EENUM__ELITERALS)
	public void buildELiteralsOutgoingReference(
			EReference eReference,
			List<Entry<EReferenceConnection, WidgetFactory>> referenceOutgoingEndpoints, 
			Collection<Label> labels,
			Map<EReferenceConnection, Collection<Label>> outgoingLabels, 
			ProgressMonitor progressMonitor) {
		
		for (Label tLabel: labels) {
			if (tLabel instanceof Action) {
				Action action = (Action) tLabel;
				EList<Action> tAnonymous = action.getAnonymous();
				for (Entry<EReferenceConnection, Collection<Label>> re: outgoingLabels.entrySet()) {
					for (Label childLabel: re.getValue()) {
						if (childLabel instanceof Action && !((Action) childLabel).getContent().isEmpty()) {
							tAnonymous.add((Action) childLabel);
						}
					}
				}
				
				DynamicTableBuilder<Entry<EReferenceConnection, WidgetFactory>> literalsTableBuilder = new DynamicTableBuilder<>("nsd-ecore-doc-table");
				buildNamedElementColumns(literalsTableBuilder, progressMonitor);
				literalsTableBuilder.addStringColumnBuilder("literal", true, false, "Literal", endpoint -> endpoint.getValue().createWidgetString((Selector<String>) this::getLiteral, progressMonitor));
				literalsTableBuilder.addStringColumnBuilder("value", true, true, "Value", endpoint -> endpoint.getValue().createWidgetString((Selector<String>) this::getValue, progressMonitor));
				
				org.nasdanika.html.model.html.Tag operationsTable = literalsTableBuilder.build(
						referenceOutgoingEndpoints,  
						"eenum-literals", 
						"literals-table", 
						progressMonitor);
				action.getContent().add(operationsTable);				
			}
		}
	}	
	
	protected String getLiteral(WidgetFactory widgetFactory, URI base, ProgressMonitor progressMonitor) {
		return ((EEnumLiteralNodeProcessor) widgetFactory).getTarget().getLiteral();
	}
	
	protected String getValue(WidgetFactory widgetFactory, URI base, ProgressMonitor progressMonitor) {
		return String.valueOf(((EEnumLiteralNodeProcessor) widgetFactory).getTarget().getValue());		
	}
	
	@Override
	public org.nasdanika.diagram.plantuml.clazz.Enum generateDiagramElement(
			URI base,
			Function<EModelElement, CompletionStage<DiagramElement>> diagramElementProvider,
			ProgressMonitor progressMonitor) {
		throw new UnsupportedOperationException();
	}
	
}
