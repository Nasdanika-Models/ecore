package org.nasdanika.models.ecore.graph.processors;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Predicate;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EGenericType;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.ETypedElement;
import org.eclipse.emf.ecore.EcorePackage;
import org.nasdanika.common.Context;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.diagram.plantuml.clazz.DiagramElement;
import org.nasdanika.graph.emf.EReferenceConnection;
import org.nasdanika.graph.processor.IncomingEndpoint;
import org.nasdanika.graph.processor.NodeProcessorConfig;
import org.nasdanika.graph.processor.OutgoingEndpoint;
import org.nasdanika.html.model.app.Action;
import org.nasdanika.html.model.app.AppFactory;
import org.nasdanika.html.model.app.Label;
import org.nasdanika.html.model.app.gen.DynamicTableBuilder;
import org.nasdanika.html.model.app.graph.WidgetFactory;
import org.nasdanika.html.model.app.graph.emf.OutgoingReferenceBuilder;
import org.nasdanika.models.echarts.graph.GraphFactory;
import org.nasdanika.models.echarts.graph.Link;
import org.nasdanika.ncore.NcoreFactory;

public abstract class EClassifierNodeProcessor<T extends EClassifier> extends ENamedElementNodeProcessor<T> implements EClassifierNodeProcessorProvider {

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
	
	protected Map<Integer,WidgetFactory> eTypeParametersWidgetFactories = Collections.synchronizedMap(new TreeMap<>());
	
	@OutgoingEndpoint("reference.name == 'eTypeParameters'")
	public final void setETypeParameterEndpoint(EReferenceConnection connection, WidgetFactory eTypeParameterWidgetFactory) {
		eTypeParametersWidgetFactories.put(connection.getIndex(), eTypeParameterWidgetFactory);
	}
	
	protected Map<EGenericType, WidgetFactory> classifierReferencingGenericTypes = Collections.synchronizedMap(new HashMap<>());	
	protected Map<ETypedElement, WidgetFactory> referencingTypedElements = Collections.synchronizedMap(new HashMap<>());		
	
	@IncomingEndpoint
	public final void setEGenericTypeClassifierEndpoint(EReferenceConnection connection, WidgetFactory widgetFactory) {
		if (connection.getReference() == EcorePackage.Literals.EGENERIC_TYPE__ECLASSIFIER) {
			EGenericType genericType = (EGenericType) connection.getSource().get();
			classifierReferencingGenericTypes.put(genericType, widgetFactory);		
		} else if (connection.getReference() == EcorePackage.Literals.ETYPED_ELEMENT__ETYPE) {
			ETypedElement typeElement = (ETypedElement) connection.getSource().get();
			referencingTypedElements.put(typeElement, widgetFactory);		
		}
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
	
	@OutgoingReferenceBuilder(
			nsURI = EcorePackage.eNS_URI,
			classID = EcorePackage.ECLASSIFIER,
			referenceID = EcorePackage.ECLASSIFIER__ETYPE_PARAMETERS)
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
			Function<EClassifier, CompletionStage<DiagramElement>> diagramElementProvider,
			ProgressMonitor progressMonitor);
	
		
	// === Uses ===
	
	// TODO - type references in structural features, operation and parameter types, generic types?
	
	@Override
	protected boolean isCreateActionForUndocumented() {
		return true;
	}
	
	// --- ECharts Graph ---
	
	
	// --- Graph generation ---
		
//	private Map<EReferenceConnection, WidgetFactory> outgoingWidgetFactories = Collections.synchronizedMap(new HashMap<>());
//
//	@OutgoingEndpoint
//	public final void setOutgoingEndpoint(EReferenceConnection connection, WidgetFactory outgoingWidgetFactory) {
//		outgoingWidgetFactories.put(connection, outgoingWidgetFactory);
//	}	
//
//	private Map<EReferenceConnection, WidgetFactory> incomingWidgetFactories = Collections.synchronizedMap(new HashMap<>());
//
//	@IncomingEndpoint
//	public final void setIncomingEndpoint(EReferenceConnection connection, WidgetFactory incomingWidgetFactory) {
//		incomingWidgetFactories.put(connection, incomingWidgetFactory);
//	}	
	
	/**
	 * Generates a node for displaying on a Graph 
	 * @param base
	 * @param nodeProvider Used for wiring of nodes when both nodes to be wired are created.
	 * @return
	 */
	public org.nasdanika.models.echarts.graph.Node generateEChartsGraphNode(
			URI base, 
			Function<EClassifier, CompletionStage<org.nasdanika.models.echarts.graph.Node>> nodeProvider,
			Function<EPackage, org.nasdanika.models.echarts.graph.Item> categoryProvider,
			ProgressMonitor progressMonitor) {		
		
		GraphFactory graphFactory = org.nasdanika.models.echarts.graph.GraphFactory.eINSTANCE;
		org.nasdanika.models.echarts.graph.Node graphNode = graphFactory.createNode();
		graphNode.setId(getTarget().getName() + "@" + getTarget().getEPackage().getNsURI());
		
		org.nasdanika.ncore.Map vMap = NcoreFactory.eINSTANCE.createMap();
		
		Set<WidgetFactory> traversed = new HashSet<>();
		
		Object link = createLink(base, progressMonitor);
		if (link instanceof Label) {
			Label label = (Label) link;
			vMap.put("description", label.getTooltip());
			String icon = label.getIcon();
			if (icon != null && icon.contains("/")) { // URL
				graphNode.setSymbol("image://" + icon);
			}
		}
		if (link instanceof org.nasdanika.html.model.app.Link) {
			vMap.put("externalLink", ((org.nasdanika.html.model.app.Link) link).getLocation());
		}
		
		if (!vMap.getValue().isEmpty()) {
			graphNode.getValue().add(vMap);
		}
		graphNode.setName(getTarget().getName());
		
		if (categoryProvider != null) {
			graphNode.setCategory(categoryProvider.apply(getTarget().getEPackage()));
		}
		
		Collection<EClassifierNodeProcessor<?>> dependencies = getEClassifierNodeProcessors(1, traversed::add, progressMonitor);
		for (EClassifierNodeProcessor<?> dep: dependencies) {
			if (dep != this) {
				nodeProvider.apply((EClassifier) dep.getTarget()).thenAccept(targetNode -> {
					Link graphLink = graphFactory.createLink();
					graphNode.getOutgoingLinks().add(graphLink);
					graphLink.setTarget(targetNode);
				});
			}
		}
		
//		record NeighborRecord(List<Connection> incomingConnections, List<Connection> outgoingConnections) {};		
//		Map<Node, NeighborRecord> neighbors = new HashMap<>();	
//		Function<Node, NeighborRecord> neighborRecordProvider = node -> neighbors.computeIfAbsent(node, n -> new NeighborRecord(new ArrayList<>(), new ArrayList<>()));
//
//		for (Connection ic: config.getElement().getIncomingConnections()) {
//			Node src = ic.getSource();
//			neighborRecordProvider.apply(src).incomingConnections().add(ic);
//		}
//
//		for (Connection oc: config.getElement().getOutgoingConnections()) {
//			Node target = oc.getTarget();
//			neighborRecordProvider.apply(target).outgoingConnections().add(oc);
//		}
//		
		
		// Group all incoming and outgoing connections by the other node: node -> (incoming, outgoing). 
		// Also do it for all contained objects in order to have dependency connections such as from a typed element to its type
		// For generic types - to their classifiers
		
		// Node size proportional to the number of attributes and operations for classes and literals for enums
		
		// Links on the other node completion if this node processor ID is less than the other node's
		// Link either color or width is proportional to its weight - the number of connection it aggregates.		
		
		return graphNode;
	}

	/**
	 * Returns self. EClassNodeProcessor overrides to return also dependency classifiers from features, operations, and supertypes.
	 */
	@Override
	public Collection<EClassifierNodeProcessor<?>> getEClassifierNodeProcessors(int depth, Predicate<WidgetFactory> predicate, ProgressMonitor progressMonitor) {		
		return Collections.singleton(this);
	}
	
}
