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
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EGenericType;
import org.eclipse.emf.ecore.EModelElement;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EcorePackage;
import org.nasdanika.common.Context;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.diagram.plantuml.Link;
import org.nasdanika.diagram.plantuml.clazz.Aggregation;
import org.nasdanika.diagram.plantuml.clazz.Association;
import org.nasdanika.diagram.plantuml.clazz.Attribute;
import org.nasdanika.diagram.plantuml.clazz.Composition;
import org.nasdanika.diagram.plantuml.clazz.DiagramElement;
import org.nasdanika.diagram.plantuml.clazz.Generalization;
import org.nasdanika.diagram.plantuml.clazz.Implementation;
import org.nasdanika.diagram.plantuml.clazz.Operation;
import org.nasdanika.diagram.plantuml.clazz.Relation;
import org.nasdanika.diagram.plantuml.clazz.SuperType;
import org.nasdanika.graph.emf.EReferenceConnection;
import org.nasdanika.graph.processor.NodeProcessorConfig;
import org.nasdanika.graph.processor.OutgoingEndpoint;
import org.nasdanika.html.model.app.Action;
import org.nasdanika.html.model.app.AppFactory;
import org.nasdanika.html.model.app.Label;
import org.nasdanika.html.model.app.gen.DynamicTableBuilder;
import org.nasdanika.html.model.app.graph.WidgetFactory;
import org.nasdanika.html.model.app.graph.WidgetFactory.Selector;
import org.nasdanika.html.model.app.graph.emf.EObjectNodeProcessor;
import org.nasdanika.html.model.app.graph.emf.OutgoingReferenceBuilder;

public abstract class EClassifierNodeProcessor<T extends EClassifier> extends ENamedElementNodeProcessor<T> {

	public EClassifierNodeProcessor(
			NodeProcessorConfig<WidgetFactory, WidgetFactory> config,
			Context context,
			java.util.function.Function<ProgressMonitor, Action> prototypeProvider) {
		super(config, context, prototypeProvider);
	}	
		
	@Override
	protected boolean isCallOutgoingReferenceLabelsSuppliers(EReference eReference) {
		if (eReference == EcorePackage.Literals.ECLASSIFIER__ETYPE_PARAMETERS) {
			return true;
		}
		return super.isCallOutgoingReferenceLabelsSuppliers(eReference);
	}	
	
	private Map<Integer,WidgetFactory> eTypeParametersWidgetFactories = Collections.synchronizedMap(new TreeMap<>());
	
	@OutgoingEndpoint("reference.name == 'eTypeParameters'")
	public final void setETypeParameterEndpoint(EReferenceConnection connection, WidgetFactory eTypeParameterWidgetFactory) {
		eTypeParametersWidgetFactories.put(connection.getIndex(), eTypeParameterWidgetFactory);
	}	
	
	/**
	 * Returns attributes action, creates if necessary. Matches by location.
	 * @param parent
	 * @return
	 */
	protected Action getTypeParametersAction(Action parent) {
		Action pAction = (Action) parent;
		return pAction.getNavigation()
			.stream()
			.filter(e -> e instanceof Action && "type-parameters.html".equals(((Action) e).getLocation()))
			.findFirst()
			.map(Action.class::cast)
			.orElseGet(() -> {
				Action typeParametersAction = AppFactory.eINSTANCE.createAction();
				typeParametersAction.setText("Type Parameters");
				typeParametersAction.setIcon("https://cdn.jsdelivr.net/gh/Nasdanika-Models/ecore@master/graph/web-resources/icons/ETypeParameter.gif");
				typeParametersAction.setLocation("type-parameters.html");
				pAction.getNavigation().add(typeParametersAction);
				return typeParametersAction;
			});
	}
	
	@OutgoingReferenceBuilder(EcorePackage.ECLASSIFIER__ETYPE_PARAMETERS)
	public void buildETypeParametersOutgoingReference(
			EReference eReference,
			List<Entry<EReferenceConnection, WidgetFactory>> referenceOutgoingEndpoints, 
			Collection<Label> labels,
			Map<EReferenceConnection, Collection<Label>> outgoingLabels, 
			ProgressMonitor progressMonitor) {
		
		for (Label tLabel: labels) {
			if (tLabel instanceof Action) {
				Action typeParametersAction = getTypeParametersAction((Action) tLabel);
				EList<Action> tAnonymous = typeParametersAction.getAnonymous();
				for (Entry<EReferenceConnection, Collection<Label>> re: outgoingLabels.entrySet()) {
					for (Label childLabel: re.getValue()) {
						if (childLabel instanceof Action && !((Action) childLabel).getContent().isEmpty()) {
							tAnonymous.add((Action) childLabel);
						}
					}
				}
				
				DynamicTableBuilder<Entry<EReferenceConnection, WidgetFactory>> typeParametersTableBuilder = new DynamicTableBuilder<>("nsd-ecore-doc-table");
				buildNamedElementColumns(typeParametersTableBuilder, progressMonitor);
				// TODO - bounds
				
				org.nasdanika.html.model.html.Tag operationsTable = typeParametersTableBuilder.build(
						referenceOutgoingEndpoints,  
						"eclassifier-type-parameters", 
						"type-parameters-table", 
						progressMonitor);
				typeParametersAction.getContent().add(operationsTable);
			}
		}
	}			
	
	public abstract org.nasdanika.diagram.plantuml.clazz.Classifier generateDiagramElement(
			URI base, 
			Function<EModelElement, CompletionStage<DiagramElement>> diagramElementProvider,
			ProgressMonitor progressMonitor);
	
		
	// === Uses ===
	
	// TODO - type references in structural features, operation and parameter types, generic types?
	
	@Override
	protected boolean isCreateActionForUndocumented() {
		return true;
	}
	
	// --- ECharts Graph ---
	
	
	// --- Graph generation ---
	
	/**
	 * Generates a node for displaying on a Graph 
	 * @param base
	 * @param nodeProvider Used for wiring of nodes when both nodes to be wired are created.
	 * @return
	 */
	public org.nasdanika.models.echarts.graph.Node generateEChartsGraphNode(
			URI base, 
			Function<EModelElement, CompletionStage<org.nasdanika.models.echarts.graph.Node>> nodeProvider,
			// TODO - category provider for EPackages
			// TODO - collector of immediate dependencies - to add to class context diagrams and to package diagrams
			ProgressMonitor progressMonitor) {		
		
		org.nasdanika.models.echarts.graph.Node graphNode = org.nasdanika.models.echarts.graph.GraphFactory.eINSTANCE.createNode();
		
		// Node name
		
		// Symbol (icon), e.g. Azure VM
		
		// Group all incoming and outgoing connections by the other node: node -> (incoming, outgoing). 
		// Also do it for all contained objects in order to have dependency connections such as from a typed element to its type
		// For generic types - to their classifiers
		
		// Node size proportional to the number of attributes and operations for classes and literals for enums
		
		// Links on the other node completion if this node processor ID is less than the other node's
		// Link either color or width is proportional to its weight - the number of connection it aggregates.		
		
		return graphNode;
	}
	
	
}
