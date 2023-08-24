package org.nasdanika.models.ecore.graph.processors;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EModelElement;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EcorePackage;
import org.nasdanika.common.Consumer;
import org.nasdanika.common.Context;
import org.nasdanika.common.DiagramGenerator;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.common.Supplier;
import org.nasdanika.diagram.plantuml.clazz.ClassDiagram;
import org.nasdanika.diagram.plantuml.clazz.DiagramElement;
import org.nasdanika.graph.emf.EReferenceConnection;
import org.nasdanika.graph.processor.NodeProcessorConfig;
import org.nasdanika.graph.processor.OutgoingEndpoint;
import org.nasdanika.html.model.app.Action;
import org.nasdanika.html.model.app.AppFactory;
import org.nasdanika.html.model.app.Label;
import org.nasdanika.html.model.app.gen.DynamicTableBuilder;
import org.nasdanika.html.model.app.graph.WidgetFactory;
import org.nasdanika.html.model.app.graph.emf.EObjectNodeProcessor;
import org.nasdanika.html.model.app.graph.emf.OutgoingReferenceBuilder;

public class EPackageNodeProcessor extends ENamedElementNodeProcessor<EPackage> {
	
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
				generateDiagramAction(labels, progressMonitor);				
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
		Action pAction = (Action) parent;
		return pAction.getNavigation()
			.stream()
			.filter(e -> e instanceof Action && "sub-packages.html".equals(((Action) e).getLocation()))
			.findFirst()
			.map(Action.class::cast)
			.orElseGet(() -> {
				Action subPackagesAction = AppFactory.eINSTANCE.createAction();
				subPackagesAction.setText("Sub-packages");
				subPackagesAction.setIcon("https://cdn.jsdelivr.net/gh/Nasdanika-Models/ecore@master/graph/web-resources/icons/EPackage.gif");
				subPackagesAction.setLocation("sub-packages.html");
				pAction.getNavigation().add(subPackagesAction);
				return subPackagesAction;
			});
	}
	
	@OutgoingReferenceBuilder(EcorePackage.EPACKAGE__ECLASSIFIERS)
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
				
				org.nasdanika.html.model.html.Tag classifiersTable = classifiersTableBuilder.build(
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
	
	@OutgoingReferenceBuilder(EcorePackage.EPACKAGE__ESUBPACKAGES)
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
				
				org.nasdanika.html.model.html.Tag subPackagesTable = subPackagesTableBuilder.build(
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
	
	protected void generateDiagramAction(Collection<Label> labels, ProgressMonitor progressMonitor) {
		for (Label label: labels) {
			if (label instanceof Action) {
				Action action = (Action) label;
				Action diagramAction = createDiagramAction(action, progressMonitor);
				if (diagramAction != null) {
					action.getNavigation().add(diagramAction);
				}
			}
		}
	}
	
	
	private Map<String, WidgetFactory> eClassifierWidgetFactories = Collections.synchronizedMap(new TreeMap<>());

	@OutgoingEndpoint("reference.name == 'eClassifiers'")
	public final void setEClassifierEndpoint(EReferenceConnection connection, WidgetFactory eClassifierWidgetFactory) {
		eClassifierWidgetFactories.put(((ENamedElement) connection.getTarget().get()).getName(), eClassifierWidgetFactory);
	}	
	
	/**
	 * Returns attributes action, creates if necessary. Matches by location.
	 * @param parent
	 * @return
	 */
	protected Action createDiagramAction(Action parent, ProgressMonitor progressMonitor) {
		DiagramGenerator diagramGenerator = context.get(DiagramGenerator.class);
		if (diagramGenerator == null || !diagramGenerator.isSupported(DiagramGenerator.UML_DIALECT)) {
			return null;
		}
		
		ClassDiagram classDiagram = new ClassDiagram();
		Map<EModelElement, CompletableFuture<DiagramElement>> diagramElementsMap = new HashMap<>();
		Function<EModelElement, CompletableFuture<DiagramElement>> diagramElementProvider = k -> diagramElementsMap.computeIfAbsent(k, kk -> new CompletableFuture<>());
		Function<EModelElement, CompletionStage<DiagramElement>> diagramElementCompletionStageProvider = k -> diagramElementProvider.apply(k);

		Selector<DiagramElement> eClassifierDiagramElementSelector = (widgetFactory, sBase, pm) -> {
			return ((EClassifierNodeProcessor<?>) widgetFactory).generateDiagramElement(sBase, diagramElementCompletionStageProvider, pm);
		};
		
		// Classifiers
		for (WidgetFactory cwf: eClassifierWidgetFactories.values()) {
			EClassifier eClassifier = (EClassifier) cwf.createWidget(EObjectNodeProcessor.TARGET_SELECTOR, progressMonitor); 
			CompletableFuture<DiagramElement> eccf = diagramElementProvider.apply(eClassifier);
			if (!eccf.isDone()) {
				DiagramElement ecde = cwf.createWidget(eClassifierDiagramElementSelector, uri, progressMonitor);
				classDiagram.getDiagramElements().add(ecde);
				eccf.complete(ecde);
			}
		}		
		
		String diagram = diagramGenerator.generateUmlDiagram(classDiagram.toString());
		
		Action diagramAction = AppFactory.eINSTANCE.createAction();
		diagramAction.setText("Diagram");
		diagramAction.setIcon("fas fa-project-diagram");
		diagramAction.setLocation("diagram.html");
		addContent(diagramAction, diagram); 		
		return diagramAction;
	}
	
	
}


