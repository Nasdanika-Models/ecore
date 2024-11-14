package org.nasdanika.models.ecore.graph.processors;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EcorePackage;
import org.nasdanika.common.Context;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.diagram.plantuml.clazz.DiagramElement;
import org.nasdanika.diagram.plantuml.clazz.Enum;
import org.nasdanika.diagram.plantuml.clazz.EnumLiteral;
import org.nasdanika.graph.emf.EReferenceConnection;
import org.nasdanika.graph.processor.NodeProcessorConfig;
import org.nasdanika.graph.processor.OutgoingEndpoint;
import org.nasdanika.models.app.Action;
import org.nasdanika.models.app.Label;
import org.nasdanika.models.app.gen.DynamicTableBuilder;
import org.nasdanika.models.app.graph.WidgetFactory;
import org.nasdanika.models.app.graph.emf.OutgoingReferenceBuilder;

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

	@OutgoingReferenceBuilder(
			nsURI = EcorePackage.eNS_URI,
			classID = EcorePackage.EENUM,
			referenceID = EcorePackage.EENUM__ELITERALS)
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
				literalsTableBuilder.addStringColumnBuilder("literal", true, false, "Literal", endpoint -> endpoint.getValue().selectString((Selector<String>) this::getLiteral, progressMonitor));
				literalsTableBuilder.addStringColumnBuilder("value", true, true, "Value", endpoint -> endpoint.getValue().selectString((Selector<String>) this::getValue, progressMonitor));
				
				org.nasdanika.models.html.Tag operationsTable = literalsTableBuilder.build(
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
		
	private Map<Integer,WidgetFactory> eLiteralWidgetFactories = Collections.synchronizedMap(new TreeMap<>());
	
	@OutgoingEndpoint("reference.name == 'eLiterals'")
	public final void setELiteralTypeEndpoint(EReferenceConnection connection, WidgetFactory eLiteralWidgetFactory) {
		eLiteralWidgetFactories.put(connection.getIndex(), eLiteralWidgetFactory);
	}	
	
	@Override
	public org.nasdanika.diagram.plantuml.clazz.Enum generateDiagramElement(
			URI base,
			Function<EClassifier, CompletionStage<DiagramElement>> diagramElementProvider,
			ProgressMonitor progressMonitor) {
		
		Enum ret = new Enum(getTarget().getName());
				
		Selector<EnumLiteral> literalSelector = (widgetFactory, sBase, pm) -> {
			return ((EEnumLiteralNodeProcessor) widgetFactory).generateLiteral(sBase, pm);
		};
		
		for (WidgetFactory lwf: eLiteralWidgetFactories.values()) {
			EnumLiteral literal = lwf.select(literalSelector, base, progressMonitor);
			ret.getLiterals().add(literal);
		}
		
		Object link = createLink(base, progressMonitor);
		if (link instanceof Label) {
			ret.setTooltip(((Label) link).getTooltip());
		}
		if (link instanceof org.nasdanika.models.app.Link) {
			ret.setLocation(((org.nasdanika.models.app.Link) link).getLocation());
		}
		
		
		return ret;
	}	
}
