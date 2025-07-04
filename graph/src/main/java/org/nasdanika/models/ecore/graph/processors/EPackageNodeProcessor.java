package org.nasdanika.models.ecore.graph.processors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EcorePackage;
import org.icepear.echarts.charts.graph.GraphCircular;
import org.icepear.echarts.charts.graph.GraphEdgeLineStyle;
import org.icepear.echarts.charts.graph.GraphEmphasis;
import org.icepear.echarts.charts.graph.GraphForce;
import org.icepear.echarts.charts.graph.GraphSeries;
import org.icepear.echarts.components.series.SeriesLabel;
import org.icepear.echarts.render.Engine;
import org.json.JSONObject;
import org.nasdanika.common.Consumer;
import org.nasdanika.common.Context;
import org.nasdanika.common.DiagramGenerator;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.common.Supplier;
import org.nasdanika.common.Util;
import org.nasdanika.diagram.plantuml.clazz.ClassDiagram;
import org.nasdanika.diagram.plantuml.clazz.DiagramElement;
import org.nasdanika.graph.emf.EReferenceConnection;
import org.nasdanika.graph.processor.NodeProcessorConfig;
import org.nasdanika.graph.processor.OutgoingEndpoint;
import org.nasdanika.html.forcegraph3d.ForceGraph3D;
import org.nasdanika.html.forcegraph3d.ForceGraph3DFactory;
import org.nasdanika.models.app.Action;
import org.nasdanika.models.app.AppFactory;
import org.nasdanika.models.app.Label;
import org.nasdanika.models.app.gen.DynamicTableBuilder;
import org.nasdanika.models.app.graph.WidgetFactory;
import org.nasdanika.models.app.graph.emf.EObjectNodeProcessor;
import org.nasdanika.models.app.graph.emf.OutgoingReferenceBuilder;
import org.nasdanika.models.echarts.graph.Graph;
import org.nasdanika.models.echarts.graph.GraphFactory;
import org.nasdanika.models.echarts.graph.Item;
import org.nasdanika.models.echarts.graph.util.GraphUtil;

public class EPackageNodeProcessor extends ENamedElementNodeProcessor<EPackage> {
	
	static final String TARGET_KEY = "target";
	static final String SOURCE_KEY = "source";
	static final String ROTATION_KEY = "rotation";
	static final String CURVATURE_KEY = "curvature";

	static final String ON_NODE_DRAG_END = """
	node => {
	          node.fx = node.x;
	          node.fy = node.y;
	          node.fz = node.z;
	        }					
	""";

	static final String NODE_THREE_OBJECT = """
	node => {
	        const nodeEl = document.createElement('div');
	        nodeEl.textContent = node.name;
	        nodeEl.style.color = node.color;
	        nodeEl.className = 'node-label';
	        return new CSS2DObject(nodeEl);
	      }					
	""";

	static final String LINK_THREE_OBJECT = """
	link => {				
          // extend link with text sprite
          if (link.name && !link.curvature) {
            const sprite = new SpriteText(link.name);
            sprite.color = 'lightgrey';
            sprite.textHeight = 1.5;
            return sprite;
          }
        }					
	""";
	
	static final String LINK_POSITION_UPDATE = """
	(sprite, { start, end }) => {
			if (sprite) {
	          const pos = Object.assign(...['x', 'y', 'z'].map(c => ({
	            [c]: start[c] + (end[c] - start[c]) / 1.3 
	          })));
	
	          // Position sprite
	          Object.assign(sprite.position, pos);
	        }
        }					
	""";	

	public EPackageNodeProcessor(
			NodeProcessorConfig<WidgetFactory, WidgetFactory> config, 
			Context context,
			java.util.function.Function<ProgressMonitor, Action> prototypeProvider) {
		super(config, context, prototypeProvider);
	}	
	
	@Override
	public Supplier<Collection<Label>> createLabelsSupplier() {
		@SuppressWarnings("resource")
		Consumer<Collection<Label>> loadSpecificationConsumer = new Consumer<Collection<Label>>() {

			@Override
			public double size() {
				return 1;
			}

			@Override
			public String name() {
				return "Generating load specification";
			}

			@Override
			public void execute(Collection<Label> labels, ProgressMonitor progressMonitor) {
				generateDiagramAndGraphActions(labels, progressMonitor);				
			}
		}; 
		return super.createLabelsSupplier().then(loadSpecificationConsumer.asFunction()).then(this::sortLabels);
	}
	
	protected Collection<Label> sortLabels(Collection<Label> labels) {
		return labels
			.stream()
			.sorted((a,b) -> a.getText().compareTo(b.getText()))
			.toList();		
	}
	
	@Override
	protected boolean isCallOutgoingReferenceLabelsSuppliers(EReference eReference) {
		if (eReference == EcorePackage.Literals.EPACKAGE__ECLASSIFIERS) {
			return true;
		}
		if (eReference == EcorePackage.Literals.EPACKAGE__ESUBPACKAGES) {
			return true;
		}
		return super.isCallOutgoingReferenceLabelsSuppliers(eReference);
	}
	 
	@Override
	protected boolean isCreateActionForUndocumented() {
		return true;
	}
	
	/**
	 * Returns classifiers action, creates if necessary. Matches by location.
	 * @param parent
	 * @return
	 */
	protected Action getClassifiersAction(Action parent) {
		Action pAction = (Action) parent;
		return pAction.getNavigation()
			.stream()
			.filter(e -> e instanceof Action && "classifiers.html".equals(((Action) e).getLocation()))
			.findFirst()
			.map(Action.class::cast)
			.orElseGet(() -> {
				Action classifiersAction = AppFactory.eINSTANCE.createAction();
				classifiersAction.setText("Classifiers");
//				attributesAction.setIcon("https://cdn.jsdelivr.net/gh/Nasdanika-Models/ecore@master/graph/web-resources/icons/EAttribute.gif");
				classifiersAction.setLocation("classifiers.html");
				pAction.getNavigation().add(classifiersAction);
				return classifiersAction;
			});
	}
	
	/**
	 * Returns sub-packages action, creates if necessary. Matches by location.
	 * @param parent
	 * @return
	 */
	protected Action getSubPackagesAction(Action parent) {
//		Action pAction = (Action) parent;
		return parent.getNavigation()
			.stream()
			.filter(e -> e instanceof Action && "sub-packages.html".equals(((Action) e).getLocation()))
			.findFirst()
			.map(Action.class::cast)
			.orElseGet(() -> {
				Action subPackagesAction = AppFactory.eINSTANCE.createAction();
				subPackagesAction.setText("Sub-packages");
				subPackagesAction.setIcon("https://cdn.jsdelivr.net/gh/Nasdanika-Models/ecore@master/graph/web-resources/icons/EPackage.gif");
				subPackagesAction.setLocation("sub-packages.html");
				parent.getNavigation().add(subPackagesAction);
				return subPackagesAction;
			});
	}
	
	@OutgoingReferenceBuilder(
			nsURI = EcorePackage.eNS_URI,
			classID = EcorePackage.EPACKAGE,
			referenceID = EcorePackage.EPACKAGE__ECLASSIFIERS)
	public void buildEClassifiersOutgoingReference(
			EReference eReference,
			List<Entry<EReferenceConnection, WidgetFactory>> referenceOutgoingEndpoints, 
			Collection<Label> labels,
			Map<EReferenceConnection, Collection<Label>> outgoingLabels, 
			ProgressMonitor progressMonitor) {

		List<Entry<EReferenceConnection, Collection<Label>>> sorted = outgoingLabels.entrySet().stream()
				.sorted((a,b) -> ((ENamedElement) a.getKey().getTarget().get()).getName().compareTo(((ENamedElement) b.getKey().getTarget().get()).getName()))
				.toList();		

		for (Label tLabel: labels) {
			for (Entry<EReferenceConnection, Collection<Label>> re: sorted) {
				tLabel.getChildren().addAll(re.getValue());
			}
			if (tLabel instanceof Action) {										
				DynamicTableBuilder<Entry<EReferenceConnection, WidgetFactory>> classifiersTableBuilder = new DynamicTableBuilder<>("nsd-ecore-doc-table");
				buildClassifierColumns(classifiersTableBuilder, progressMonitor);
				
				org.nasdanika.models.html.Tag classifiersTable = classifiersTableBuilder.build(
						referenceOutgoingEndpoints.stream().sorted((a,b) -> {
							ENamedElement ane = (ENamedElement) a.getKey().getTarget().get();
							ENamedElement bne = (ENamedElement) b.getKey().getTarget().get();
							return ane.getName().compareTo(bne.getName());
						}).toList(),  
						"epackage-classifiers", 
						"classifiers-table", 
						progressMonitor);
				getClassifiersAction((Action) tLabel).getContent().add(classifiersTable);
			}
		}
	}
	
	@OutgoingReferenceBuilder(
			nsURI = EcorePackage.eNS_URI,
			classID = EcorePackage.EPACKAGE,
			referenceID = EcorePackage.EPACKAGE__ESUBPACKAGES)
	public void buildESubPackagesOutgoingReference(
			EReference eReference,
			List<Entry<EReferenceConnection, WidgetFactory>> referenceOutgoingEndpoints, 
			Collection<Label> labels,
			Map<EReferenceConnection, Collection<Label>> outgoingLabels, 
			ProgressMonitor progressMonitor) {

		List<Entry<EReferenceConnection, Collection<Label>>> sorted = outgoingLabels.entrySet().stream()
				.sorted((a,b) -> ((ENamedElement) a.getKey().getTarget().get()).getName().compareTo(((ENamedElement) b.getKey().getTarget().get()).getName()))
				.toList();		

		// A page with a dynamic sub-packages table and links to sub-package pages.
		for (Label label: labels) {
			for (Entry<EReferenceConnection, Collection<Label>> re: sorted) {
				label.getChildren().addAll(re.getValue());
			}
			if (label instanceof Action) {										
				DynamicTableBuilder<Entry<EReferenceConnection, WidgetFactory>> subPackagesTableBuilder = new DynamicTableBuilder<>("nsd-ecore-doc-table");
				buildNamedElementColumns(subPackagesTableBuilder, progressMonitor);
				// TODO
				// getNsURI()
//				subPackagesTableBuilder.addStringColumnBuilder("nsUri", true, false, "Namespace URI", endpoint -> endpoint.get description(endpoint.getKey(), endpoint.getValue(), progressMonitor));
				
				org.nasdanika.models.html.Tag subPackagesTable = subPackagesTableBuilder.build(
						referenceOutgoingEndpoints.stream().sorted((a,b) -> {
							ENamedElement ane = (ENamedElement) a.getKey().getTarget().get();
							ENamedElement bne = (ENamedElement) b.getKey().getTarget().get();
							return ane.getName().compareTo(bne.getName());
						}).toList(),  
						"epackage-sub-packages", 
						"sub-packages-table", 
						progressMonitor);
				getSubPackagesAction((Action) label).getContent().add(subPackagesTable);
			}
		}
	}	
	
	protected void generateDiagramAndGraphActions(Collection<Label> labels, ProgressMonitor progressMonitor) {
		for (Label label: labels) {
			if (label instanceof Action) {
				Action action = (Action) label;
				Action diagramAction = createDiagramAction(progressMonitor);
				if (diagramAction != null) {
					action.getNavigation().add(diagramAction);
				}
				
				Label graphsLabel = createGraphsLabel(progressMonitor);
				if (graphsLabel != null) {
					action.getNavigation().add(graphsLabel);
				}
			}
		}				
	}
		
	private Map<String, WidgetFactory> eClassifierWidgetFactories = Collections.synchronizedMap(new TreeMap<>());
	
	public Map<String, WidgetFactory> getEClassifierWidgetFactories() {
		return eClassifierWidgetFactories;
	}

	@OutgoingEndpoint("reference.name == 'eClassifiers'")
	public final void setEClassifierEndpoint(EReferenceConnection connection, WidgetFactory eClassifierWidgetFactory) {
		eClassifierWidgetFactories.put(((ENamedElement) connection.getTarget().get()).getName(), eClassifierWidgetFactory);
	}
		
	private Map<String, WidgetFactory> eSubpackageWidgetFactories = Collections.synchronizedMap(new TreeMap<>());
	
	public Map<String, WidgetFactory> getESubpackageWidgetFactories() {
		return eSubpackageWidgetFactories;
	}
	
	@OutgoingEndpoint("reference.name == 'eSubpackages'")
	public final void setESubpackageEndpoint(EReferenceConnection connection, WidgetFactory eSubpackageWidgetFactory) {
		eSubpackageWidgetFactories.put(((ENamedElement) connection.getTarget().get()).getName(), eSubpackageWidgetFactory);
	}		
	
	protected Action createDiagramAction(ProgressMonitor progressMonitor) {
		DiagramGenerator diagramGenerator = context.get(DiagramGenerator.class);
		if (diagramGenerator == null || !diagramGenerator.isSupported(DiagramGenerator.UML_DIALECT)) {
			return null;
		}
		
		ClassDiagram classDiagram = new ClassDiagram();
		
		record EClassifierKey(String nsURI, String name) {}
		Map<EClassifierKey, CompletableFuture<DiagramElement>> diagramElementsMap = new HashMap<>();
		Function<EClassifier, CompletableFuture<DiagramElement>> diagramElementProvider = k -> diagramElementsMap.computeIfAbsent(new EClassifierKey(k.getEPackage().getNsURI(), k.getName()), kk -> new CompletableFuture<>());
		Function<EClassifier, CompletionStage<DiagramElement>> diagramElementCompletionStageProvider = k -> diagramElementProvider.apply(k);

		Selector<DiagramElement> eClassifierDiagramElementSelector = (widgetFactory, sBase, pm) -> {
			return ((EClassifierNodeProcessor<?>) widgetFactory).generateDiagramElement(sBase, diagramElementCompletionStageProvider, pm);
		};
		
		// Classifiers
		for (WidgetFactory cwf: eClassifierWidgetFactories.values()) {
			EClassifier eClassifier = (EClassifier) cwf.select(EObjectNodeProcessor.TARGET_SELECTOR, progressMonitor); 
			CompletableFuture<DiagramElement> eccf = diagramElementProvider.apply(eClassifier);
			if (!eccf.isDone()) {
				DiagramElement ecde = cwf.select(eClassifierDiagramElementSelector, getUri(), progressMonitor);
				classDiagram.getDiagramElements().add(ecde);
				eccf.complete(ecde);
			}
		}		
		
		String diagram = diagramGenerator.generateUmlDiagram(classDiagram.toString());
		
		Action diagramAction = AppFactory.eINSTANCE.createAction();
		diagramAction.setText("Diagram");
		diagramAction.setIcon("fas fa-project-diagram");
		diagramAction.setLocation("diagram.html");
		diagramAction.setDecorator(
				createHelpDecorator(
						"A class diagram showing package classifiers and relationships between them", 
						null, 
						null, 
						null, 
						"Hover mouse over diagram elements to display tooltips. Click on diagram elements to navigate go documentation",  
						null));
		addContent(diagramAction, diagram); 		
		return diagramAction;
	}
	
	final static String GRAPH_TEMPLATE = 
			"""
			<div id="graph-container-${graphContainerId}" class="row" style="height:70vh;width:100%">
			</div>
			<script type="text/javascript">
				$(document).ready(function() {
					var dom = document.getElementById("graph-container-${graphContainerId}");
					var myChart = echarts.init(dom, null, {
						render: "canvas",
						useDirtyRect: false
					});		
					var option = ${chart};
					option.tooltip = {};
					option.series[0].tooltip = {
						formatter: function(arg) { 
							return arg.value ? arg.value.description : null; 
						}
					};
					myChart.setOption(option);
					myChart.on("dblclick", function(params) {
						if (params.value) {
							if (params.value.link) {
								window.open(params.value.link, "_self");
							} else if (params.value.externalLink) {
								window.open(params.value.externalLink);
							}
						}
					});
					window.addEventListener("resize", myChart.resize);
				});		
			</script>
			""";
	
	static AtomicInteger GRAPH_CONTAINER_COUNTER = new AtomicInteger();
		
	protected Label createGraphsLabel(ProgressMonitor progressMonitor) {		
		Label graphAction = AppFactory.eINSTANCE.createAction();
		graphAction.setText("Graphs");
		graphAction.setIcon("https://img.icons8.com/external-dreamstale-lineal-dreamstale/16/external-diagram-seo-media-dreamstale-lineal-dreamstale.png");
		EList<EObject> children = graphAction.getChildren();
		children.add(createDefaultGraphAction(progressMonitor));
		children.add(createCircularGraphAction(progressMonitor));
		children.add(createForceGraphAction(progressMonitor));
		children.add(createForceGraph3DAction(progressMonitor));
		children.add(createForceGraph3DActionDetailed(progressMonitor));
		
		// With dependencies & sub-packages separator
		
		Label header = AppFactory.eINSTANCE.createLabel();
		header.setTooltip("With dependencies and sub-packages");
		children.add(header);

		children.add(createDefaultGraphActionWithDependenciesAndSubpackages(progressMonitor));
		children.add(createCircularGraphActionWithDependenciesAndSubpackages(progressMonitor));
		children.add(createForceGraphActionWithDependenciesAndSubpackages(progressMonitor));
		children.add(createForceGraph3DActionWithDependenciesAndSubpackages(progressMonitor));
		children.add(createForceGraph3DActionWithDependenciesAndSubpackagesDetailed(progressMonitor));
		
		graphAction.setDecorator(
				createHelpDecorator(
						"Graphs of package classifiers showing relationships between them", 
						null, 
						null, 
						null, 
						(String) null,  
						null));
		
		return graphAction;
	}
	
	protected org.nasdanika.models.echarts.graph.Graph generateGraph(boolean withDependencies, boolean withSubpackages, ProgressMonitor progressMonitor) {
		GraphFactory graphFactory = org.nasdanika.models.echarts.graph.GraphFactory.eINSTANCE;
		org.nasdanika.models.echarts.graph.Graph graph = graphFactory.createGraph();
		
		Map<EClassifier, CompletableFuture<org.nasdanika.models.echarts.graph.Node>> nodeMap = new HashMap<>();
		Function<EClassifier, CompletableFuture<org.nasdanika.models.echarts.graph.Node>> nodeProvider = k -> nodeMap.computeIfAbsent(k, kk -> new CompletableFuture<>());
		Function<EClassifier, CompletionStage<org.nasdanika.models.echarts.graph.Node>> nodeCompletionStageProvider = k -> nodeProvider.apply(k);

		Map<EPackage, Item> categoryMap = new HashMap<>();
		Function<EPackage, Item> categoryProvider = k -> categoryMap.computeIfAbsent(k, kk -> {
			Item category = GraphFactory.eINSTANCE.createItem();
			category.setName(kk.getName());
			graph.getCategories().add(category);
			return category;
		});							
		
		Selector<org.nasdanika.models.echarts.graph.Node> eClassifierNodeSelector = (widgetFactory, sBase, pm) -> {
			return ((EClassifierNodeProcessor<?>) widgetFactory).generateEChartsGraphNode(
					sBase, 
					nodeCompletionStageProvider, 
					withDependencies ? categoryProvider : null, 
					progressMonitor);
		};
		
		Set<WidgetFactory> traversed = new HashSet<>();
		
		for (WidgetFactory pcwf: getEClassifierWidgetFactories(withSubpackages, progressMonitor)) {
			for (WidgetFactory cwf: withDependencies(pcwf, withDependencies ? 1 : 0, traversed::add, progressMonitor)) {
				EClassifier eClassifier = (EClassifier) cwf.select(EObjectNodeProcessor.TARGET_SELECTOR, progressMonitor); 
				CompletableFuture<org.nasdanika.models.echarts.graph.Node> eccf = nodeProvider.apply(eClassifier);
				if (!eccf.isDone()) {
					org.nasdanika.models.echarts.graph.Node ecn = cwf.select(eClassifierNodeSelector, getUri(), progressMonitor);
					graph.getNodes().add(ecn);
					eccf.complete(ecn);
				}
			}
		}
		
		forceLayout(graph);
		return graph;
	}
		
	protected void generateDrawioDiagram(
			Function<EPackage, org.nasdanika.drawio.Layer> layerProvider,
			boolean withDependencies, 
			boolean withSubpackages, 
			ProgressMonitor progressMonitor) {
		
		Map<EClassifier, CompletableFuture<org.nasdanika.drawio.Node>> nodeMap = new HashMap<>();
		Function<EClassifier, CompletableFuture<org.nasdanika.drawio.Node>> nodeProvider = k -> nodeMap.computeIfAbsent(k, kk -> new CompletableFuture<>());
		Function<EClassifier, CompletionStage<org.nasdanika.drawio.Node>> nodeCompletionStageProvider = k -> nodeProvider.apply(k);

		Selector<org.nasdanika.drawio.Node> eClassifierNodeSelector = (widgetFactory, sBase, pm) -> {
			return ((EClassifierNodeProcessor<?>) widgetFactory).generateDrawioDiagramNode(
					sBase, 
					nodeCompletionStageProvider, 
					layerProvider,
					progressMonitor);
		};
		
		Set<WidgetFactory> traversed = new HashSet<>();
		
		for (WidgetFactory pcwf: getEClassifierWidgetFactories(withSubpackages, progressMonitor)) {
			for (WidgetFactory cwf: withDependencies(pcwf, withDependencies ? 1 : 0, traversed::add, progressMonitor)) {
				EClassifier eClassifier = (EClassifier) cwf.select(EObjectNodeProcessor.TARGET_SELECTOR, progressMonitor); 
				CompletableFuture<org.nasdanika.drawio.Node> eccf = nodeProvider.apply(eClassifier);
				if (!eccf.isDone()) {
					org.nasdanika.drawio.Node ecn = cwf.select(eClassifierNodeSelector, getUri(), progressMonitor);
					eccf.complete(ecn);
				}
			}
		}
	}
		
	protected ForceGraph3D generate3DGraph(
			boolean withDependencies, 
			boolean withSubpackages, 
			ProgressMonitor progressMonitor) {
		
		ForceGraph3D forceGraph3D = ForceGraph3DFactory.INSTANCE.create();
		List<JSONObject> links = new ArrayList<>();
		
		Map<EClassifier, CompletableFuture<JSONObject>> nodeMap = new HashMap<>();
		Function<EClassifier, CompletableFuture<JSONObject>> nodeProvider = k -> nodeMap.computeIfAbsent(k, kk -> new CompletableFuture<>());
		Function<EClassifier, CompletionStage<JSONObject>> nodeCompletionStageProvider = k -> nodeProvider.apply(k);
	
		Selector<JSONObject> eClassifierNodeSelector = (widgetFactory, sBase, pm) -> {
			JSONObject node = ((EClassifierNodeProcessor<?>) widgetFactory).generate3DNode(
					sBase, 
					nodeCompletionStageProvider, 
					link -> {
						links.add(link);
						forceGraph3D.addLink(link);
					},
					progressMonitor);
			forceGraph3D.addNode(node);
			return node;
		};
		
		Set<WidgetFactory> traversed = new HashSet<>();
		
		for (WidgetFactory pcwf: getEClassifierWidgetFactories(withSubpackages, progressMonitor)) {
			for (WidgetFactory cwf: withDependencies(pcwf, withDependencies ? 1 : 0, traversed::add, progressMonitor)) {
				EClassifier eClassifier = (EClassifier) cwf.select(EObjectNodeProcessor.TARGET_SELECTOR, progressMonitor); 
				CompletableFuture<JSONObject> eccf = nodeProvider.apply(eClassifier);
				if (!eccf.isDone()) {
					JSONObject ecn = cwf.select(eClassifierNodeSelector, getUri(), progressMonitor);
					eccf.complete(ecn);
				}
			}
		}
		
		// Curvatures and rotations
		for (Entry<String, List<JSONObject>> lse: Util.groupBy(links, l -> l.getString(SOURCE_KEY)).entrySet()) {
			for (Entry<String, List<JSONObject>> lte: Util.groupBy(lse.getValue(), l -> l.getString(TARGET_KEY)).entrySet()) {
				long incomingLinksCount = links
					.stream()
					.filter(l -> l.getString(TARGET_KEY).equals(lse.getKey()) && l.getString(SOURCE_KEY).equals(lte.getKey()))
					.count();
				
				List<JSONObject> outgoingLinks = lte.getValue();
				double totalLinkCount = outgoingLinks.size() + incomingLinksCount; 
				for (JSONObject link: outgoingLinks) {
					if (outgoingLinks.size() > 1 || incomingLinksCount > 1) {
						link.put(CURVATURE_KEY, 0.2);
						link.put(ROTATION_KEY, Math.PI * 2 * lte.getValue().indexOf(link) / totalLinkCount);
					} else {
						link.put(CURVATURE_KEY, 0.0);
						link.put(ROTATION_KEY, 0.0);			
					}
				}
			}
			
		}		
		
		return forceGraph3D;
	}		
	
	public Collection<WidgetFactory> getEClassifierWidgetFactories(boolean recursive, ProgressMonitor progressMonitor) {
		Collection<WidgetFactory> ret = new HashSet<>(getEClassifierWidgetFactories().values());
		if (recursive) {
			for (WidgetFactory eSubpackageWidgetFactory: getESubpackageWidgetFactories().values()) {
				EPackageNodeProcessor subpackageNodeProcessor = (EPackageNodeProcessor) eSubpackageWidgetFactory.select(EObjectNodeProcessor.SELF_SELECTOR, progressMonitor);
				ret.addAll(subpackageNodeProcessor.getEClassifierWidgetFactories(recursive, progressMonitor));
			}
		}
		return ret;
	}
	
	protected Collection<? extends WidgetFactory> withDependencies(WidgetFactory wf, int depth, Predicate<WidgetFactory> predicate, ProgressMonitor progressMonitor) {
		EClassifier eClassifier = (EClassifier) wf.select(EObjectNodeProcessor.TARGET_SELECTOR, progressMonitor); 
		if (eClassifier instanceof EClass) {
			EClassNodeProcessor ecnp = (EClassNodeProcessor) wf.select(EObjectNodeProcessor.SELF_SELECTOR, progressMonitor);
			return ecnp.getEClassifierNodeProcessors(depth, predicate, progressMonitor);
		}
		return Collections.singleton(wf);
	}	
	
	protected Action createDefaultGraphAction(ProgressMonitor progressMonitor) {
		Action graphAction = AppFactory.eINSTANCE.createAction();
		graphAction.setText("Default Graph");
		graphAction.setLocation("default-graph.html");
		
		GraphSeries graphSeries = new org.icepear.echarts.charts.graph.GraphSeries()
				.setSymbolSize(ICON_SIZE)
				.setDraggable(true)				
				.setLayout("none")
                .setLabel(new SeriesLabel().setShow(true).setPosition("right"))
                .setLineStyle(new GraphEdgeLineStyle().setColor(SOURCE_KEY).setCurveness(0))
                .setRoam(true)
                .setEmphasis(new GraphEmphasis().setFocus("adjacency")); // Line style width 10?
				
		generateGraph(false, false, progressMonitor).configureGraphSeries(graphSeries);
    	org.icepear.echarts.Graph echartsGraph = new org.icepear.echarts.Graph().addSeries(graphSeries);
    	
	    Engine engine = new Engine();
	    String chartJSON = engine.renderJsonOption(echartsGraph);
	    
		String chartHTML = Context
				.singleton("chart", chartJSON)
				.compose(Context.singleton("graphContainerId", GRAPH_CONTAINER_COUNTER.incrementAndGet()))
				.interpolateToString(GRAPH_TEMPLATE);
		addContent(graphAction, chartHTML);
	    
		graphAction.setDecorator(
				createHelpDecorator(
						"A fixed force-layout graph of package classifiers showing relationships between them", 
						null, 
						null, 
						null, 
						"""
						Hover mouse over nodes elements to display tooltips. 
						Double-click on nodes to navigate go documentation. 
						Drag to rearrange.
						""",  
						null));
		
		return graphAction;		
	}
		
	protected Action createCircularGraphAction(ProgressMonitor progressMonitor) {
		Action graphAction = AppFactory.eINSTANCE.createAction();
		graphAction.setText("Circular Graph");
		graphAction.setLocation("circular-layout-graph.html");
		
		GraphSeries graphSeries = new org.icepear.echarts.charts.graph.GraphSeries()
				.setSymbolSize(ICON_SIZE)
				.setDraggable(true)				
				.setLayout("circular")
				.setCircular(new GraphCircular().setRotateLabel(true))
                .setLabel(new SeriesLabel().setShow(true).setPosition("right"))
                .setLineStyle(new GraphEdgeLineStyle().setColor(SOURCE_KEY).setCurveness(0.3))
                .setRoam(true)
                .setEmphasis(new GraphEmphasis().setFocus("adjacency")); // Line style width 10?
				
		generateGraph(false, false, progressMonitor).configureGraphSeries(graphSeries);
    	org.icepear.echarts.Graph echartsGraph = new org.icepear.echarts.Graph().addSeries(graphSeries);
    	
	    Engine engine = new Engine();
	    String chartJSON = engine.renderJsonOption(echartsGraph);
	    
		String chartHTML = Context
				.singleton("chart", chartJSON)
				.compose(Context.singleton("graphContainerId", GRAPH_CONTAINER_COUNTER.incrementAndGet()))
				.interpolateToString(GRAPH_TEMPLATE);
		addContent(graphAction, chartHTML);
	    
		graphAction.setDecorator(
				createHelpDecorator(
						"A circular layout graph of package classifiers showing relationships between them", 
						null, 
						null, 
						null, 
						"""
						Hover mouse over nodes elements to display tooltips. 
						Double-click on nodes to navigate go documentation. 
						Drag to rearrange.
						""",  
						null));
		
		return graphAction;		
	}
		
	protected Action createForceGraphAction(ProgressMonitor progressMonitor) {
		Action graphAction = AppFactory.eINSTANCE.createAction();
		graphAction.setText("Force Graph");
		graphAction.setLocation("force-layout-graph.html");
		
		GraphSeries graphSeries = new org.icepear.echarts.charts.graph.GraphSeries()
				.setSymbolSize(ICON_SIZE)
				.setDraggable(true)				
				.setLayout("force")
				.setForce(new GraphForce().setRepulsion(200).setGravity(0.1).setEdgeLength(200))
                .setLabel(new SeriesLabel().setShow(true).setPosition("right"))
                .setLineStyle(new GraphEdgeLineStyle().setColor(SOURCE_KEY).setCurveness(0))
                .setRoam(true)
//                .setScaleLimit(scaleLimit)
                .setEmphasis(new GraphEmphasis().setFocus("adjacency")); // Line style width 10?
		
		generateGraph(false, false, progressMonitor).configureGraphSeries(graphSeries);
    	org.icepear.echarts.Graph echartsGraph = new org.icepear.echarts.Graph()
//                .setTooltip("item")
//                .setLegend()
                .addSeries(graphSeries);
    	
	    Engine engine = new Engine();
	    String chartJSON = engine.renderJsonOption(echartsGraph);
	    
		String chartHTML = Context
				.singleton("chart", chartJSON)
				.compose(Context.singleton("graphContainerId", GRAPH_CONTAINER_COUNTER.incrementAndGet()))
				.interpolateToString(GRAPH_TEMPLATE);
		addContent(graphAction, chartHTML);
	    
		graphAction.setDecorator(
				createHelpDecorator(
						"A live force-layout graph of package classifiers showing relationships between them", 
						null, 
						null, 
						null, 
						"""
						Hover mouse over nodes elements to display tooltips. 
						Double-click on nodes to navigate go documentation. 
						Drag to rearrange, nodes will not stay in place once released.
						""",  
						null));
		
		return graphAction;		
	}
		
	protected Action createForceGraph3DAction(ProgressMonitor progressMonitor) {
		Action graphAction = AppFactory.eINSTANCE.createAction();
		graphAction.setText("Force Graph 3D");
		graphAction.setLocation("force-layout-graph-3d.html");
		
		Graph graph = generateGraph(false, false, progressMonitor);
		
		ForceGraph3D forceGraph3D = GraphUtil.asForceGraph3D(graph);
		String forceGraphContainerId = "force-graph-" + GRAPH_CONTAINER_COUNTER.incrementAndGet();
		forceGraph3D
			.elementId(forceGraphContainerId)
			.nodeAutoColorBy("'group'")
			.nodeVal("'size'")
			.linkDirectionalArrowLength(2.5)
			.linkDirectionalArrowRelPos(1)
			.linkCurvature("'curvature'")
			.linkCurveRotation("'rotation'")			
			.addExtraRederer("new CSS2DRenderer()")
			.onNodeClick(ON_NODE_CLICK)
			.nodeThreeObject(NODE_THREE_OBJECT)
			.nodeThreeObjectExtend(true)
			.onNodeDragEnd(ON_NODE_DRAG_END);
			    
		String graphHTML = Context
				.singleton("graph", forceGraph3D.produce(4))
				.compose(Context.singleton("graphContainerId", forceGraphContainerId))
				.interpolateToString(GRAPH_3D_TEMPLATE);
		addContent(graphAction, graphHTML);
	    
		graphAction.setDecorator(
				createHelpDecorator(
						"A live force-layout graph of package classifiers showing relationships between them", 
						null, 
						null, 
						null, 
						"""
						Hover mouse over nodes elements to display tooltips. 
						Double-click on nodes to navigate go documentation. 
						Left click - rotate.
						Mouse wheel - zoom.
						Right click - pan.
						Drag to rearrange.
						""",  
						null));
		
		return graphAction;		
	}	
	
	protected Action createForceGraph3DActionDetailed(ProgressMonitor progressMonitor) {
		Action graphAction = AppFactory.eINSTANCE.createAction();
		graphAction.setText("Force Graph 3D Detailed");
		graphAction.setLocation("force-layout-graph-3d-detailed.html");
		
		ForceGraph3D forceGraph3D = generate3DGraph(false, false, progressMonitor);
		String forceGraphContainerId = "force-graph-" + GRAPH_CONTAINER_COUNTER.incrementAndGet();
		forceGraph3D
			.elementId(forceGraphContainerId)
			.nodeAutoColorBy("'group'")
			.nodeVal("'size'")
			.linkDirectionalArrowLength(2.5)
			.linkDirectionalArrowRelPos(1)
			.linkCurvature("'curvature'")
			.linkCurveRotation("'rotation'")	
			.linkAutoColorBy("'group'")
			.addExtraRederer("new CSS2DRenderer()")
			.onNodeClick(ON_NODE_CLICK)
			.nodeThreeObject(NODE_THREE_OBJECT)
			.nodeThreeObjectExtend(true)
			.linkThreeObject(LINK_THREE_OBJECT)
			.linkPositionUpdate(LINK_POSITION_UPDATE)
			.linkThreeObjectExtend(true)
			.onNodeDragEnd(ON_NODE_DRAG_END);
			    
		String graphHTML = Context
				.singleton("graph", forceGraph3D.produce(4))
				.compose(Context.singleton("graphContainerId", forceGraphContainerId))
				.interpolateToString(GRAPH_3D_TEMPLATE);
		addContent(graphAction, graphHTML);
	    
		graphAction.setDecorator(
				createHelpDecorator(
						"A live force-layout graph of package classifiers showing detailed relationships between them", 
						null, 
						null, 
						null, 
						"""
						Hover mouse over nodes elements to display tooltips. 
						Double-click on nodes to navigate go documentation. 
						Left click - rotate.
						Mouse wheel - zoom.
						Right click - pan.
						Drag to rearrange.
						""",  
						null));
		
		return graphAction;		
	}	
	
	// With dependencies and sub-packages
	
	protected Action createDefaultGraphActionWithDependenciesAndSubpackages(ProgressMonitor progressMonitor) {
		Action graphAction = AppFactory.eINSTANCE.createAction();
		graphAction.setText("Default Graph");
		graphAction.setLocation("default-graph-with-dependencies-and-subpackages.html");
		
		GraphSeries graphSeries = new org.icepear.echarts.charts.graph.GraphSeries()
				.setSymbolSize(ICON_SIZE)
				.setDraggable(true)				
				.setLayout("none")
                .setLabel(new SeriesLabel().setShow(true).setPosition("right"))
                .setLineStyle(new GraphEdgeLineStyle().setColor(SOURCE_KEY).setCurveness(0))
                .setRoam(true)
                .setEmphasis(new GraphEmphasis().setFocus("adjacency")); // Line style width 10?
				
		generateGraph(true, true, progressMonitor).configureGraphSeries(graphSeries);
    	org.icepear.echarts.Graph echartsGraph = new org.icepear.echarts.Graph()
    			.addSeries(graphSeries)
    			.setLegend();
    	
	    Engine engine = new Engine();
	    String chartJSON = engine.renderJsonOption(echartsGraph);
	    
		String chartHTML = Context
				.singleton("chart", chartJSON)
				.compose(Context.singleton("graphContainerId", GRAPH_CONTAINER_COUNTER.incrementAndGet()))
				.interpolateToString(GRAPH_TEMPLATE);
		addContent(graphAction, chartHTML);
	    
		graphAction.setDecorator(
				createHelpDecorator(
						"A fixed force-layout graph of package classifiers showing relationships between them", 
						null, 
						null, 
						null, 
						"""
						Hover mouse over nodes elements to display tooltips. 
						Double-click on nodes to navigate go documentation. 
						Drag to rearrange.
						""",  
						null));
		
		return graphAction;		
	}
		
	protected Action createCircularGraphActionWithDependenciesAndSubpackages(ProgressMonitor progressMonitor) {
		Action graphAction = AppFactory.eINSTANCE.createAction();
		graphAction.setText("Circular Graph");
		graphAction.setLocation("circular-layout-graph-with-dependencies-and-subpackages.html");
		
		GraphSeries graphSeries = new org.icepear.echarts.charts.graph.GraphSeries()
				.setSymbolSize(ICON_SIZE)
				.setDraggable(true)				
				.setLayout("circular")
				.setCircular(new GraphCircular().setRotateLabel(true))
                .setLabel(new SeriesLabel().setShow(true).setPosition("right"))
                .setLineStyle(new GraphEdgeLineStyle().setColor(SOURCE_KEY).setCurveness(0.3))
                .setRoam(true)
                .setEmphasis(new GraphEmphasis().setFocus("adjacency")); // Line style width 10?
				
		generateGraph(true, true, progressMonitor).configureGraphSeries(graphSeries);
    	org.icepear.echarts.Graph echartsGraph = new org.icepear.echarts.Graph()
    			.addSeries(graphSeries)
    			.setLegend();
    	
	    Engine engine = new Engine();
	    String chartJSON = engine.renderJsonOption(echartsGraph);
	    
		String chartHTML = Context
				.singleton("chart", chartJSON)
				.compose(Context.singleton("graphContainerId", GRAPH_CONTAINER_COUNTER.incrementAndGet()))
				.interpolateToString(GRAPH_TEMPLATE);
		addContent(graphAction, chartHTML);
	    
		graphAction.setDecorator(
				createHelpDecorator(
						"A circular layout graph of package classifiers showing relationships between them", 
						null, 
						null, 
						null, 
						"""
						Hover mouse over nodes elements to display tooltips. 
						Double-click on nodes to navigate go documentation. 
						Drag to rearrange.
						""",  
						null));
		
		return graphAction;		
	}
		
	protected Action createForceGraphActionWithDependenciesAndSubpackages(ProgressMonitor progressMonitor) {
		Action graphAction = AppFactory.eINSTANCE.createAction();
		graphAction.setText("Force Graph");
		graphAction.setLocation("force-layout-graph-with-dependencies-and-subpackages.html");
		
		GraphSeries graphSeries = new org.icepear.echarts.charts.graph.GraphSeries()
				.setSymbolSize(ICON_SIZE)
				.setDraggable(true)				
				.setLayout("force")
				.setForce(new GraphForce().setRepulsion(200).setGravity(0.1).setEdgeLength(200))
                .setLabel(new SeriesLabel().setShow(true).setPosition("right"))
                .setLineStyle(new GraphEdgeLineStyle().setColor(SOURCE_KEY).setCurveness(0))
                .setRoam(true)
//                .setScaleLimit(scaleLimit)
                .setEmphasis(new GraphEmphasis().setFocus("adjacency")); // Line style width 10?
		
		generateGraph(true, true, progressMonitor).configureGraphSeries(graphSeries);
    	org.icepear.echarts.Graph echartsGraph = new org.icepear.echarts.Graph()
                .setLegend()
                .addSeries(graphSeries);
    	
	    Engine engine = new Engine();
	    String chartJSON = engine.renderJsonOption(echartsGraph);
	    
		String chartHTML = Context
				.singleton("chart", chartJSON)
				.compose(Context.singleton("graphContainerId", GRAPH_CONTAINER_COUNTER.incrementAndGet()))
				.interpolateToString(GRAPH_TEMPLATE);
		addContent(graphAction, chartHTML);
	    
		graphAction.setDecorator(
				createHelpDecorator(
						"A live force-layout graph of package classifiers showing relationships between them", 
						null, 
						null, 
						null, 
						"""
						Hover mouse over nodes elements to display tooltips. 
						Double-click on nodes to navigate go documentation. 
						Drag to rearrange, nodes will not stay in place once released.
						""",  
						null));
		
		return graphAction;		
	}
		
	final static String GRAPH_3D_TEMPLATE = 
			"""
			<div id="${graphContainerId}" class="row" style="height:70vh;width:100%">
			</div>
			<script type="module">
				import { CSS2DRenderer, CSS2DObject } from 'three/addons/renderers/CSS2DRenderer.js';
				import SpriteText from "https://cdn.jsdelivr.net/npm/three-spritetext@1.10.0/dist/three-spritetext.mjs";
				
				${graph}
			</script>
			""";
	
	static final String ON_NODE_CLICK = 
			"""
		    (node, event) => {
		        if (node.lastClick) {
			        if (event.timeStamp - node.lastClick < 500) {
						if (Array.isArray(node.value) && node.value.length > 0) {
							if (node.value[0].link) {
								window.open(node.value[0].link, "_self");
							} else if (node.value[0].externalLink) {
								window.open(node.value[0].externalLink);
							}
						}		
						delete node.lastClick;
					} else {
						node.lastClick = event.timeStamp;
					}
				} else {
					node.lastClick = event.timeStamp;
				}
		    }			
			""";
	
	
	protected Action createForceGraph3DActionWithDependenciesAndSubpackagesDetailed(ProgressMonitor progressMonitor) {
		Action graphAction = AppFactory.eINSTANCE.createAction();
		graphAction.setText("Force Graph 3D Detailed");
		graphAction.setLocation("force-layout-graph-3d-with-dependencies-and-subpackages-detailed.html");
		
		ForceGraph3D forceGraph3D = generate3DGraph(true, true, progressMonitor);
		String forceGraphContainerId = "force-graph-" + GRAPH_CONTAINER_COUNTER.incrementAndGet();
		forceGraph3D
			.elementId(forceGraphContainerId)
			.nodeAutoColorBy("'group'")
			.nodeVal("'size'")
			.linkDirectionalArrowLength(2.5)
			.linkDirectionalArrowRelPos(1)
			.linkCurvature("'curvature'")
			.linkCurveRotation("'rotation'")	
			.linkAutoColorBy("'group'")
			.addExtraRederer("new CSS2DRenderer()")
			.onNodeClick(ON_NODE_CLICK)
			.nodeThreeObject(NODE_THREE_OBJECT)
			.nodeThreeObjectExtend(true)
			.linkThreeObject(LINK_THREE_OBJECT)
			.linkPositionUpdate(LINK_POSITION_UPDATE)
			.linkThreeObjectExtend(true)
			.onNodeDragEnd(ON_NODE_DRAG_END);
			    
		String graphHTML = Context
				.singleton("graph", forceGraph3D.produce(4))
				.compose(Context.singleton("graphContainerId", forceGraphContainerId))
				.interpolateToString(GRAPH_3D_TEMPLATE);
		addContent(graphAction, graphHTML);
	    
		graphAction.setDecorator(
				createHelpDecorator(
						"A live force-layout graph of package classifiers showing detailed relationships between them", 
						null, 
						null, 
						null, 
						"""
						Hover mouse over nodes elements to display tooltips. 
						Double-click on nodes to navigate go documentation. 
						Left click - rotate.
						Mouse wheel - zoom.
						Right click - pan.
						Drag to rearrange.
						""",  
						null));
		
		return graphAction;		
	}	
	
		
	protected Action createForceGraph3DActionWithDependenciesAndSubpackages(ProgressMonitor progressMonitor) {
		Action graphAction = AppFactory.eINSTANCE.createAction();
		graphAction.setText("Force Graph 3D");
		graphAction.setLocation("force-layout-graph-3d-with-dependencies-and-subpackages.html");
		
		Graph graph = generateGraph(true, true, progressMonitor);
		
		ForceGraph3D forceGraph3D = GraphUtil.asForceGraph3D(graph);
		String forceGraphContainerId = "force-graph" + GRAPH_CONTAINER_COUNTER.incrementAndGet();
		forceGraph3D
			.elementId(forceGraphContainerId)
			.nodeAutoColorBy("'group'")
			.nodeVal("'size'")
			.linkDirectionalArrowLength(2.5)
			.linkDirectionalArrowRelPos(1)
			.addExtraRederer("new CSS2DRenderer()")
			.linkCurvature("'curvature'")
			.linkCurveRotation("'rotation'")
			.onNodeClick(ON_NODE_CLICK)
			.nodeThreeObject(NODE_THREE_OBJECT)
			.nodeThreeObjectExtend(true)
			.onNodeDragEnd(ON_NODE_DRAG_END);
			    
		String graphHTML = Context
				.singleton("graph", forceGraph3D.produce(4))
				.compose(Context.singleton("graphContainerId", forceGraphContainerId))
				.interpolateToString(GRAPH_3D_TEMPLATE);
		addContent(graphAction, graphHTML);
	    
		graphAction.setDecorator(
				createHelpDecorator(
						"A live force-layout graph of package classifiers showing relationships between them", 
						null, 
						null, 
						null, 
						"""
						Hover mouse over nodes elements to display tooltips. 
						Double-click on nodes to navigate go documentation. 
						Left click - rotate.
						Mouse wheel - zoom.
						Right click - pan.
						Drag to rearrange.
						""",  
						null));
		
		return graphAction;		
	}
		
}