package org.nasdanika.models.ecore.graph.processors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EGenericType;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.ETypedElement;
import org.eclipse.emf.ecore.EcorePackage;
import org.icepear.echarts.charts.graph.GraphCircular;
import org.icepear.echarts.charts.graph.GraphEdgeLineStyle;
import org.icepear.echarts.charts.graph.GraphEmphasis;
import org.icepear.echarts.charts.graph.GraphForce;
import org.icepear.echarts.charts.graph.GraphSeries;
import org.icepear.echarts.components.series.SeriesLabel;
import org.icepear.echarts.render.Engine;
import org.nasdanika.common.Consumer;
import org.nasdanika.common.Context;
import org.nasdanika.common.DiagramGenerator;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.common.Supplier;
import org.nasdanika.diagram.plantuml.Link;
import org.nasdanika.diagram.plantuml.clazz.Aggregation;
import org.nasdanika.diagram.plantuml.clazz.Association;
import org.nasdanika.diagram.plantuml.clazz.Attribute;
import org.nasdanika.diagram.plantuml.clazz.ClassDiagram;
import org.nasdanika.diagram.plantuml.clazz.Composition;
import org.nasdanika.diagram.plantuml.clazz.DiagramElement;
import org.nasdanika.diagram.plantuml.clazz.Generalization;
import org.nasdanika.diagram.plantuml.clazz.Implementation;
import org.nasdanika.diagram.plantuml.clazz.Operation;
import org.nasdanika.diagram.plantuml.clazz.Relation;
import org.nasdanika.diagram.plantuml.clazz.SuperType;
import org.nasdanika.emf.EmfUtil.EModelElementDocumentation;
import org.nasdanika.emf.persistence.EObjectLoader;
import org.nasdanika.graph.emf.Connection;
import org.nasdanika.graph.emf.EOperationConnection;
import org.nasdanika.graph.emf.EReferenceConnection;
import org.nasdanika.graph.processor.IncomingEndpoint;
import org.nasdanika.graph.processor.NodeProcessorConfig;
import org.nasdanika.graph.processor.OutgoingEndpoint;
import org.nasdanika.html.TagName;
import org.nasdanika.html.model.app.Action;
import org.nasdanika.html.model.app.AppFactory;
import org.nasdanika.html.model.app.Label;
import org.nasdanika.html.model.app.gen.DynamicTableBuilder;
import org.nasdanika.html.model.app.graph.WidgetFactory;
import org.nasdanika.html.model.app.graph.emf.EObjectNodeProcessor;
import org.nasdanika.html.model.app.graph.emf.IncomingReferenceBuilder;
import org.nasdanika.html.model.app.graph.emf.OutgoingReferenceBuilder;
import org.nasdanika.models.echarts.graph.GraphFactory;
import org.nasdanika.models.echarts.graph.Item;
import org.nasdanika.models.ecore.graph.ReifiedTypeConnection;
import org.nasdanika.ncore.util.NcoreUtil;

public class EClassNodeProcessor extends EClassifierNodeProcessor<EClass> {
	
	public class ReifiedTypeSelector {
		
		private Object selector;
		
		ReifiedTypeSelector(Object selector) {
			this.selector = selector;
		}
		
		public Object getSelector() {
			return selector;
		}
		
		public WidgetFactory getReifiedTypeWidgetFactory(EGenericType eGenericType) {
			return reifiedTypesWidgetFactories.get(eGenericType);
		}
		
		public ReifiedTypeSelector createSelector(Object selector) {
			return new ReifiedTypeSelector(selector);
		}
		
	}

	public EClassNodeProcessor(
			NodeProcessorConfig<WidgetFactory, WidgetFactory> config,
			Context context,
			java.util.function.Function<ProgressMonitor, Action> prototypeProvider) {
		super(config, context, prototypeProvider);
	}
		
	@Override
	protected boolean isCallOutgoingReferenceLabelsSuppliers(EReference eReference) {
		if (eReference == EcorePackage.Literals.ECLASS__EATTRIBUTES) {
			return true;
		}
		if (eReference == EcorePackage.Literals.ECLASS__EREFERENCES) {
			return true;
		}
		if (eReference == EcorePackage.Literals.ECLASS__EOPERATIONS) {
			return true;
		}
		return super.isCallOutgoingReferenceLabelsSuppliers(eReference);
	}
	
	/**
	 * Effective generic type for structural features
	 */
	protected String typeLink(Connection connection, WidgetFactory widgetFactory, ProgressMonitor progressMonitor) {
		EObject tt = connection.getTarget().get();
		if (tt instanceof EStructuralFeature) {
			EStructuralFeature feature = (EStructuralFeature) tt;
			EGenericType featureType = getTarget().getFeatureType(feature);
			if (featureType != null) {
				String typeName = featureType.getERawType().getName(); 
				String typeNameComment = "<!-- " + typeName + "--> ";
				WidgetFactory genericTypeWidgetFactory = featureGenericTypesWidgetFactories.get(tt);
				if (genericTypeWidgetFactory != null) {
					String linkStr = genericTypeWidgetFactory.createLinkString(progressMonitor);
					if (linkStr != null) {
						return typeNameComment + linkStr;			
					}
				}
				return typeNameComment + typeName;
			}
		}
		return super.typeLink(connection, widgetFactory, progressMonitor);
	}
	
	private Map<EStructuralFeature, WidgetFactory> featureGenericTypesWidgetFactories = new HashMap<>();
	
	@OutgoingEndpoint("operation.name == 'getFeatureType'")
	public final void setFeatureTypeEndpoint(EOperationConnection connection, WidgetFactory genericTypeWidgetFactory) {
		featureGenericTypesWidgetFactories.put((EStructuralFeature) connection.getArguments().get(0), genericTypeWidgetFactory);
	}
	
	private Map<EGenericType,WidgetFactory> reifiedTypesWidgetFactories = new HashMap<>();
	
	@OutgoingEndpoint
	public final void setReifiedTypeEndpoint(ReifiedTypeConnection connection, WidgetFactory reifiedTypeWidgetFactory) {
		reifiedTypesWidgetFactories.put(connection.getGenericType(), reifiedTypeWidgetFactory);
	}
	
	private record SubTypeRecord(
			EClass subType,
			WidgetFactory subTypeWidgetFactory,
			String subTypeLinkStr, 
			EGenericType genericSuperType, 
			WidgetFactory genericSuperTypeWidgetFactory, 
			boolean isDirect) implements Comparable<SubTypeRecord> {

		@Override
		public int compareTo(SubTypeRecord o) {
			return subType().getName().compareTo(o.subType().getName());
		}
		
	}
	
	private List<SubTypeRecord> getSubTypes(ProgressMonitor progressMonitor) {
		List<SubTypeRecord> subTypes = new ArrayList<>();		
		
		for (Entry<EGenericType, WidgetFactory> rgt: classifierReferencingGenericTypes.entrySet()) { 			
			Selector<List<SubTypeRecord>> selector = (wf, base, pm) -> {
				List<SubTypeRecord> ret = new ArrayList<>();
				EGenericTypeNodeProcessor processor = (EGenericTypeNodeProcessor) wf;
				for (Entry<EClass, WidgetFactory> subtypeWidgetFactoryEntry: processor.getSubTypeWidgetFactories().entrySet()) {
					ret.add(new SubTypeRecord(
							subtypeWidgetFactoryEntry.getKey(),
							subtypeWidgetFactoryEntry.getValue(),
							subtypeWidgetFactoryEntry.getValue().createLinkString(base, pm), 
							rgt.getKey(), 
							rgt.getValue(), 
							subtypeWidgetFactoryEntry.getKey().getESuperTypes().contains(EClassNodeProcessor.this.getTarget())));							
				}
				return ret;
			};			
			subTypes.addAll(rgt.getValue().select(selector, progressMonitor));			
		}
		Collections.sort(subTypes);
		return subTypes;
	}
	
	// === Attributes ===
		
	//	TODO getEIDAttribute()
	
	/**
	 * Returns attributes action, creates if necessary. Matches by location.
	 * @param parent
	 * @return
	 */
	protected Action getAttributesAction(Action parent) {
		return parent.getNavigation()
			.stream()
			.filter(e -> e instanceof Action && "attributes.html".equals(((Action) e).getLocation()))
			.findFirst()
			.map(Action.class::cast)
			.orElseGet(() -> {
				Action attributesAction = AppFactory.eINSTANCE.createAction();
				attributesAction.setText("Attributes");
				attributesAction.setIcon("https://cdn.jsdelivr.net/gh/Nasdanika-Models/ecore@master/graph/web-resources/icons/EAttribute.gif");
				attributesAction.setLocation("attributes.html");
				parent.getNavigation().add(attributesAction);
				return attributesAction;
			});
	}

	@OutgoingReferenceBuilder(EcorePackage.ECLASS__EATTRIBUTES)
	public void buildEAttributesOutgoingReference(
			EReference eReference,
			List<Entry<EReferenceConnection, WidgetFactory>> referenceOutgoingEndpoints, 
			Collection<Label> labels,
			Map<EReferenceConnection, Collection<Label>> outgoingLabels, 
			ProgressMonitor progressMonitor) {
		
		// Own attributes 
		for (Label tLabel: labels) {
			if (tLabel instanceof Action) {
				Action attributesAction = getAttributesAction((Action) tLabel);
				EList<Action> tAnonymous = attributesAction.getAnonymous();
				for (Entry<EReferenceConnection, Collection<Label>> re: outgoingLabels.entrySet()) {
					for (Label childLabel: re.getValue()) {
						if (childLabel instanceof Action && !((Action) childLabel).getContent().isEmpty()) {
							tAnonymous.add((Action) childLabel);
						}
					}
				}
			}
		}
	}		

	@OutgoingReferenceBuilder(EcorePackage.ECLASS__EALL_ATTRIBUTES)
	public void buildEAllAttributesOutgoingReference(
			EReference eReference,
			List<Entry<EReferenceConnection, WidgetFactory>> referenceOutgoingEndpoints, 
			Collection<Label> labels,
			Map<EReferenceConnection, Collection<Label>> outgoingLabels, 
			ProgressMonitor progressMonitor) {

		// A page with a dynamic attributes table and links to attribute pages for attributes with documentation.
		for (Label label: labels) {
			if (label instanceof Action) {										
				DynamicTableBuilder<Entry<EReferenceConnection, WidgetFactory>> attributesTableBuilder = new DynamicTableBuilder<>("nsd-ecore-doc-table");
				attributesTableBuilder.setProperty("transitive-label", "Inherited");
				buildStructuralFeatureColumns(attributesTableBuilder, progressMonitor);
				
				org.nasdanika.html.model.html.Tag attributesTable = attributesTableBuilder.build(
						referenceOutgoingEndpoints.stream().sorted((a,b) -> {
							ENamedElement ane = (ENamedElement) a.getKey().getTarget().get();
							ENamedElement bne = (ENamedElement) b.getKey().getTarget().get();
							return ane.getName().compareTo(bne.getName());
						}).toList(),  
						"eclass-attributes", 
						"attributes-table", 
						progressMonitor);
				getAttributesAction((Action) label).getContent().add(attributesTable);
			}
		}
	}
	
	// === References ===
		
	//	getEAllContainments()
	
	// TODO - all incoming references
	
	/**
	 * Returns references action, creates if necessary. Matches by location.
	 * @param parent
	 * @return
	 */
	protected Action getReferencesAction(Action parent) {
		return parent.getNavigation()
			.stream()
			.filter(e -> e instanceof Action && "references.html".equals(((Action) e).getLocation()))
			.findFirst()
			.map(Action.class::cast)
			.orElseGet(() -> {
				Action referencesAction = AppFactory.eINSTANCE.createAction();
				referencesAction.setText("References");
				referencesAction.setIcon("https://cdn.jsdelivr.net/gh/Nasdanika-Models/ecore@master/graph/web-resources/icons/EReference.gif");
				referencesAction.setLocation("references.html");
				parent.getNavigation().add(referencesAction);
				return referencesAction;
			});
	}

	@OutgoingReferenceBuilder(EcorePackage.ECLASS__EREFERENCES)
	public void buildReferencesOutgoingReference(
			EReference eReference,
			List<Entry<EReferenceConnection, WidgetFactory>> referenceOutgoingEndpoints, 
			Collection<Label> labels,
			Map<EReferenceConnection, Collection<Label>> outgoingLabels, 
			ProgressMonitor progressMonitor) {
			// Own references 
		for (Label tLabel: labels) {
			if (tLabel instanceof Action) {
				Action referencesAction = getReferencesAction((Action) tLabel);
				EList<Action> tAnonymous = referencesAction.getAnonymous();
				for (Entry<EReferenceConnection, Collection<Label>> re: outgoingLabels.entrySet()) {
					for (Label childLabel: re.getValue()) {
						if (childLabel instanceof Action && !((Action) childLabel).getContent().isEmpty()) {
							tAnonymous.add((Action) childLabel);
						}
					}
				}
			}
		}
	}		
	
	@OutgoingReferenceBuilder(EcorePackage.ECLASS__EALL_REFERENCES)
	public void buildEAllReferencesOutgoingReference(
			EReference eReference,
			List<Entry<EReferenceConnection, WidgetFactory>> referenceOutgoingEndpoints, 
			Collection<Label> labels,
			Map<EReferenceConnection, Collection<Label>> outgoingLabels, 
			ProgressMonitor progressMonitor) {
		
		// A page with a dynamic references table and links to reference pages for references with documentation. 
		for (Label label: labels) {
			if (label instanceof Action) {										
				DynamicTableBuilder<Entry<EReferenceConnection, WidgetFactory>> referencesTableBuilder = new DynamicTableBuilder<>("nsd-ecore-doc-table");
				referencesTableBuilder.setProperty("transitive-label", "Inherited");
				buildStructuralFeatureColumns(referencesTableBuilder, progressMonitor);
// TODO										
//					getEKeys()
//					getEOpposite()
//					getEReferenceType()
//					isContainer()
//					isContainment()
//					isResolveProxies()
				
				org.nasdanika.html.model.html.Tag referencesTable = referencesTableBuilder.build(
						referenceOutgoingEndpoints.stream().sorted((a,b) -> {
							ENamedElement ane = (ENamedElement) a.getKey().getTarget().get();
							ENamedElement bne = (ENamedElement) b.getKey().getTarget().get();
							return ane.getName().compareTo(bne.getName());
						}).toList(),  
						"eclass-references", 
						"references-table", 
						progressMonitor);
				getReferencesAction((Action) label).getContent().add(referencesTable);
			}
		}
	}
	
	// === Operations ===
	
	/**
	 * Returns operations action, creates if necessary. Matches by location.
	 * @param parent
	 * @return
	 */
	protected Action getOperationsAction(Action parent) {
		return parent.getNavigation()
			.stream()
			.filter(e -> e instanceof Action && "operations.html".equals(((Action) e).getLocation()))
			.findFirst()
			.map(Action.class::cast)
			.orElseGet(() -> {
				Action operationsAction = AppFactory.eINSTANCE.createAction();
				operationsAction.setText("Operations");
				operationsAction.setIcon("https://cdn.jsdelivr.net/gh/Nasdanika-Models/ecore@master/graph/web-resources/icons/EOperation.gif");
				operationsAction.setLocation("operations.html");
				parent.getNavigation().add(operationsAction);
				return operationsAction;
			});
	}
	

	@OutgoingReferenceBuilder(EcorePackage.ECLASS__EOPERATIONS)
	public void buildOperationsOutgoingReference(
			EReference eReference,
			List<Entry<EReferenceConnection, WidgetFactory>> referenceOutgoingEndpoints, 
			Collection<Label> labels,
			Map<EReferenceConnection, Collection<Label>> outgoingLabels, 
			ProgressMonitor progressMonitor) {
			// Own references 
		for (Label tLabel: labels) {
			if (tLabel instanceof Action) {
				Action operationsAction = getOperationsAction((Action) tLabel);
				EList<Action> tAnonymous = operationsAction.getAnonymous();
				for (Entry<EReferenceConnection, Collection<Label>> re: outgoingLabels.entrySet()) {
					for (Label childLabel: re.getValue()) {
						if (childLabel instanceof Action && !((Action) childLabel).getContent().isEmpty()) {
							tAnonymous.add((Action) childLabel);
						}
					}
				}
			}
		}
	}		

	@OutgoingReferenceBuilder(EcorePackage.ECLASS__EALL_OPERATIONS)
	public void buildEAllOperationsOutgoingReference(
			EReference eReference,
			List<Entry<EReferenceConnection, WidgetFactory>> referenceOutgoingEndpoints, 
			Collection<Label> labels,
			Map<EReferenceConnection, Collection<Label>> outgoingLabels, 
			ProgressMonitor progressMonitor) {
		// A page with a dynamic operations table and links to operations pages for operations with documentation. 
		for (Label label: labels) {
			if (label instanceof Action) {					
				DynamicTableBuilder<Entry<EReferenceConnection, WidgetFactory>> operationsTableBuilder = new DynamicTableBuilder<>("nsd-ecore-doc-table");
				operationsTableBuilder.setProperty("transitive-label", "Inherited");
				buildTypedElementColumns(operationsTableBuilder, progressMonitor);					
				operationsTableBuilder
					.addStringColumnBuilder("declaring-class", true, true, "Declaring Class", endpoint -> declaringClassLink(endpoint.getKey(), endpoint.getValue(), progressMonitor))
					.addStringColumnBuilder("parameters", true, false, "Parameters", endpoint -> endpoint.getValue().selectString(new ReifiedTypeSelector(EcorePackage.Literals.EOPERATION__EPARAMETERS), progressMonitor))
					.addStringColumnBuilder("exceptions", true, false, "Exceptions", endpoint -> endpoint.getValue().selectString(new ReifiedTypeSelector(EcorePackage.Literals.EOPERATION__EGENERIC_EXCEPTIONS), progressMonitor))
					.addStringColumnBuilder("type-parameters", true, false, "Type Parameters", endpoint -> endpoint.getValue().selectString(new ReifiedTypeSelector(EcorePackage.Literals.EOPERATION__ETYPE_PARAMETERS), progressMonitor));		
				
				// TODO - overrides, not visible by default and not sortable
				
				
				
				List<Entry<EReferenceConnection, WidgetFactory>> sorted = new ArrayList<>();
				
				referenceOutgoingEndpoints
					.stream().sorted((a,b) -> {
						ENamedElement ane = (ENamedElement) a.getKey().getTarget().get();
						ENamedElement bne = (ENamedElement) b.getKey().getTarget().get();
						return ane.getName().compareTo(bne.getName());
					})
					.forEach(e -> {
						EOperation eop = (EOperation) e.getKey().getTarget().get();
						Iterator<Entry<EReferenceConnection, WidgetFactory>> sit = sorted.iterator();
						boolean isOverridden = false;
						while (sit.hasNext()) {
							EOperation fop = (EOperation) sit.next().getKey().getTarget().get();
							if (eop.isOverrideOf(fop)) {
								sit.remove();
							} else if (fop.isOverrideOf(eop)) {
								isOverridden = true;
							}							
						}
						if (!isOverridden) {
							sorted.add(e);
						}
					});
				
				org.nasdanika.html.model.html.Tag operationsTable = operationsTableBuilder.build(
						sorted,  
						"eclass-operations", 
						"operations-table", 
						progressMonitor);
				getOperationsAction((Action) label).getContent().add(operationsTable);
			}
		}			
	}
		
	// === Inheritance ===
	
	/**
	 * Returns inheritance action (subtypes and supertypes), creates if necessary. Matches by location.
	 * @param parent
	 * @return
	 */
	protected Action getInheritanceAction(Action parent) {
		return parent.getNavigation()
			.stream()
			.filter(e -> e instanceof Action && "inheritance.html".equals(((Action) e).getLocation()))
			.findFirst()
			.map(Action.class::cast)
			.orElseGet(() -> {
				Action inheritanceAction = AppFactory.eINSTANCE.createAction();
				inheritanceAction.setText("Inheritance");
				inheritanceAction.setIcon("https://cdn.jsdelivr.net/gh/Nasdanika-Models/ecore@master/graph/web-resources/icons/EGenericSuperType.gif");
				inheritanceAction.setLocation("inheritance.html");
				parent.getNavigation().add(inheritanceAction);
				return inheritanceAction;
			});
	}
	
	@OutgoingReferenceBuilder(EcorePackage.ECLASS__EALL_GENERIC_SUPER_TYPES)
	public void buildEAllGenericSuperTypesOutgoingReference(
			EReference eReference,
			List<Entry<EReferenceConnection, WidgetFactory>> referenceOutgoingEndpoints, 
			Collection<Label> labels,
			Map<EReferenceConnection, Collection<Label>> outgoingLabels, 
			ProgressMonitor progressMonitor) {

		for (Label label: labels) {
			if (label instanceof Action) {					
				DynamicTableBuilder<Entry<EReferenceConnection, WidgetFactory>> superTypesTableBuilder = new DynamicTableBuilder<>("nsd-ecore-doc-table");
				superTypesTableBuilder.setProperty("transitive-label", "All");
				superTypesTableBuilder
					.addStringColumnBuilder("name", true, false, "Name", endpoint -> targetNameLink(endpoint.getKey(), endpoint.getValue(), progressMonitor))   
					.addStringColumnBuilder("description", true, false, "Description", endpoint -> description(endpoint.getKey(), endpoint.getValue(), progressMonitor));
				
				org.nasdanika.html.model.html.Tag superTypesTable = superTypesTableBuilder.build(
						referenceOutgoingEndpoints,  
						"eclass-supertypes", 
						"supertypes-table", 
						progressMonitor);
				
				Action superTypesSection = AppFactory.eINSTANCE.createAction();
				superTypesSection.setText("Supertypes");
				superTypesSection.getContent().add(superTypesTable);
									
				getInheritanceAction((Action) label).getSections().add(superTypesSection);
			}
		}			
	}
			
	/**
	 * Building generic sub-types table. Sub-type {@link EClass} *-all generic supertypes-> {@link EGenericType} -eClassifier-> Super-type.  
	 * @param referenceIncomingEndpoints
	 * @param labels
	 * @param incomingLabels
	 * @param progressMonitor
	 */
	@IncomingReferenceBuilder(
			nsURI = EcorePackage.eNS_URI, 
			classID = EcorePackage.EGENERIC_TYPE, 
			referenceID = EcorePackage.EGENERIC_TYPE__ECLASSIFIER)
	public void buildEGenericTypeClassifierIncomingReference(
			EReference eReference,
			List<Entry<EReferenceConnection, WidgetFactory>> referenceIncomingEndpoints, 
			Collection<Label> labels,
			Map<EReferenceConnection, Collection<Label>> incomingLabels, 
			ProgressMonitor progressMonitor) {
		
		for (Label label : labels) {
			if (label instanceof Action) {

				List<SubTypeRecord> subTypes = getSubTypes(progressMonitor);
				if (!subTypes.isEmpty()) {				
					DynamicTableBuilder<SubTypeRecord> subTypesTableBuilder = new DynamicTableBuilder<>("nsd-ecore-doc-table");
					subTypesTableBuilder.setProperty("transitive-label", "All");
					subTypesTableBuilder.addStringColumnBuilder("name", true, false, "Name", subTypeRecord -> {						
						return subTypeRecord.isDirect() ? TagName.b.create(subTypeRecord.subTypeLinkStr()).toString() : subTypeRecord.subTypeLinkStr();
					});

					// TODO Description from the generic type?
	
					org.nasdanika.html.model.html.Tag subTypesTable = subTypesTableBuilder.build(subTypes, "eclass-subtypes", "subtypes-table", progressMonitor);
	
					Action subTypesSection = AppFactory.eINSTANCE.createAction();
					subTypesSection.setText("Subtypes");
					subTypesSection.getContent().add(subTypesTable);
	
					getInheritanceAction((Action) label).getSections().add(subTypesSection);
				}
			}
		}
	}
	
	@Override
	protected void configureLabel(EObject eObject, Label label, ProgressMonitor progressMonitor) {
		super.configureLabel(eObject, label, progressMonitor);
		if (getTarget().isAbstract()) {
			label.setText("<i>" + label.getText() + "</i>");
		}
	}
		
	// --- Load specification ---
		
	private List<Map.Entry<EReferenceConnection,FeatureWidgetFactory>> featureWidgetFactories = Collections.synchronizedList(new ArrayList<>());
	
	protected FeatureWidgetFactory getFeatureWidgetFactory(WidgetFactory widgetFactory, URI base, ProgressMonitor progressMonitor) {
		return (FeatureWidgetFactory) widgetFactory;
	}
	
	@OutgoingEndpoint("reference.name == 'eAllOperations'")
	public final void setEOperationEndpoint(EReferenceConnection connection, WidgetFactory eOperationWidgetFactory, ProgressMonitor progressMonitor) {
		FeatureWidgetFactory featureWidgetFactory = eOperationWidgetFactory.select((Selector<FeatureWidgetFactory>) this::getFeatureWidgetFactory, progressMonitor);
		EOperation eOp = (EOperation) connection.getTarget().get();
		synchronized (featureWidgetFactories) {
			Iterator<Entry<EReferenceConnection, FeatureWidgetFactory>> it = featureWidgetFactories.iterator();
			while (it.hasNext()) {
				Entry<EReferenceConnection, FeatureWidgetFactory> next = it.next();
				EObject nextTarget = next.getKey().getTarget().get();
				if (nextTarget instanceof EOperation) {
					EOperation nextEOp = (EOperation) nextTarget;
					if (nextEOp.isOverrideOf(eOp)) {
						return;
					}
					if (eOp.isOverrideOf(nextEOp)) {
						it.remove();
					}
				}
			}
			if (featureWidgetFactory.isLoadable()) {
				featureWidgetFactories.add(Map.entry(connection, featureWidgetFactory));
			}
		}
	}	
	
	@OutgoingEndpoint("reference.name == 'eAllStructuralFeatures'")
	public final void setEStructuralFeatureEndpoint(EReferenceConnection connection, WidgetFactory eStructuralFeatureWidgetFactory, ProgressMonitor progressMonitor) {
		FeatureWidgetFactory featureWidgetFactory = eStructuralFeatureWidgetFactory.select((Selector<FeatureWidgetFactory>) this::getFeatureWidgetFactory, progressMonitor);
		if (featureWidgetFactory.isLoadable()) {
			featureWidgetFactories.add(Map.entry(connection, featureWidgetFactory));
		}
	}	
	
	protected Object parameterTypes(WidgetFactory widgetFactory, URI base, ProgressMonitor progressMonitor) {
		if (base == null) {
			base = uri;
		}
		EOperationNodeProcessor eOperationNodeProcessor = (EOperationNodeProcessor) widgetFactory;
		Map<EReferenceConnection, WidgetFactory> eParameterWidgetFactories = eOperationNodeProcessor.getEParameterWidgetFactories();
		ReifiedTypeSelector reifiedTypeSelector = new ReifiedTypeSelector(EcorePackage.Literals.EOPERATION__EPARAMETERS);
		if (eParameterWidgetFactories.size() == 1) {
			WidgetFactory pwf = eParameterWidgetFactories.values().iterator().next();
			return pwf.select(reifiedTypeSelector.createSelector(EcorePackage.Literals.ETYPED_ELEMENT__EGENERIC_TYPE), base, progressMonitor);
		}
		
		List<Object> ret = new ArrayList<>();
		ret.add("<ol>");
		for (WidgetFactory pwf: eParameterWidgetFactories.entrySet().stream().sorted((a,b) -> a.getKey().getIndex() - b.getKey().getIndex()).map(Map.Entry::getValue).toList()) {
			ret.add("<li>");
			ret.add(pwf.createLink(base, progressMonitor));
			ret.add(" : ");
			ret.add(pwf.select(reifiedTypeSelector.createSelector(EcorePackage.Literals.ETYPED_ELEMENT__EGENERIC_TYPE), base, progressMonitor));
			ret.add("</li>");
		}						
		ret.add("</ol>");
		return ret;
	}
	
	/**
	 * Returns attributes action, creates if necessary. Matches by location.
	 * @param parent
	 * @return
	 */
	protected Action createLoadSpecificationAction(Action parent, ProgressMonitor progressMonitor) {
		if (getTarget().isAbstract() || "false".equals(NcoreUtil.getNasdanikaAnnotationDetail(getTarget(), EObjectLoader.IS_LOADABLE, "true"))) {
			return null;
		}
		
		List<Map.Entry<EReferenceConnection,FeatureWidgetFactory>> fwf;		
		synchronized (featureWidgetFactories) {
			fwf = new ArrayList<>(featureWidgetFactories);
		}
		List<Entry<EReferenceConnection, FeatureWidgetFactory>> features = fwf
				.stream()
				.filter(e -> e.getValue().isLoadable())
				.sorted((a, b) -> a.getValue().getLoadKey(getTarget()).compareTo(b.getValue().getLoadKey(getTarget())))
				.toList();
		
		if (features.isEmpty()) {
			return null;
		}
		
		Action loadSpecificationAction = AppFactory.eINSTANCE.createAction();
		loadSpecificationAction.setText("Load specification");
		loadSpecificationAction.setLocation("load-specification.html");
				
		EModelElementDocumentation loadDoc = getLoadDocumentation();
		if (loadDoc != null) {
			loadSpecificationAction.getContent().add(interpolatedMarkdown(loadDoc.documentation(), loadDoc.location(), progressMonitor));
		}
		
//		
//		List<EStructuralFeature> sortedFeatures = eObject.getEAllStructuralFeatures().stream().filter(predicate.and(elementPredicate)).sorted(namedElementComparator).toList();
		
		DynamicTableBuilder<Map.Entry<EReferenceConnection,FeatureWidgetFactory>> loadSpecificationTableBuilder = new DynamicTableBuilder<>();
		loadSpecificationTableBuilder.addStringColumnBuilder("key", true, true, "Key", featureWidgetFactoryEntry -> {
				FeatureWidgetFactory featureWidgetFactory = featureWidgetFactoryEntry.getValue();
				String key = featureWidgetFactory.getLoadKey(getTarget());
				// TODO - link if there is a feature spec detail
				if (featureWidgetFactory.isDefaultFeature(getTarget())) {
					key = "<b>" + key + "</b>";
				}
				if (featureWidgetFactory.hasLoadSpecificationAction()) {
					URI loadSpecURI = featureWidgetFactory.getLoadSpecRef(uri);
					key = "<a href=\"" + loadSpecURI + "\">" + key + "</a>";
				}
				return key;
			});
		
		loadSpecificationTableBuilder.addStringColumnBuilder("type", true, true, "Type", featureWidgetFactoryEntry -> {
			EObject target = featureWidgetFactoryEntry.getKey().getTarget().get();
			if (target instanceof EOperation) {
				return featureWidgetFactoryEntry.getValue().selectString((Selector<Object>) this::parameterTypes, progressMonitor);
			}
			return typeLink(featureWidgetFactoryEntry.getKey(), featureWidgetFactoryEntry.getValue(), progressMonitor);
		});  

		loadSpecificationTableBuilder.addStringColumnBuilder("multiplicity", true, false, "Multiplicity", featureWidgetFactoryEntry -> ((ETypedElementNodeProcessor<?>) featureWidgetFactoryEntry.getValue()).getMultiplicity());
		loadSpecificationTableBuilder.addBooleanColumnBuilder("homogeneous", true, false, "Homogeneous", featureWidgetFactoryEntry -> ((ETypedElementNodeProcessor<?>) featureWidgetFactoryEntry.getValue()).isHomogeneous());
		loadSpecificationTableBuilder.addBooleanColumnBuilder("strict-containment", true, false, "Strict Containment", featureWidgetFactoryEntry -> ((ETypedElementNodeProcessor<?>) featureWidgetFactoryEntry.getValue()).isStrictContainment());
		loadSpecificationTableBuilder.addStringColumnBuilder("exclusive", true, true, "Exclusive With", featureWidgetFactoryEntry -> ((ETypedElementNodeProcessor<?>) featureWidgetFactoryEntry.getValue()).getExclusiveWith(getTarget()));
		loadSpecificationTableBuilder.addStringColumnBuilder("description", true, false, "Description", featureWidgetFactoryEntry -> ((ETypedElementNodeProcessor<?>) featureWidgetFactoryEntry.getValue()).getLoadDescription());
		
		org.nasdanika.html.model.html.Tag loadSpecificationTable = loadSpecificationTableBuilder.build(
				features, 
				getTarget().getEPackage().getNsURI().hashCode() + "-" + getTarget().getName() + "-load-specification", 
				"load-specification-table", 
				progressMonitor);
//		
//		for (EStructuralFeature sf: sortedFeatures) {
//			Action featureAction = AppFactory.eINSTANCE.createAction();
//			String key = keyExtractor.apply(sf);
//			featureAction.setText(key);
//			String sectionAnchor = "key-section-" + key;
//			
//			featureAction.setName(sectionAnchor);			
//			loadSpecificationAction.getSections().add(featureAction);
//
//			// Properties table
//			Table table = context.get(BootstrapFactory.class).table();
//			table.toHTMLElement().style().width("auto");
//			
//			genericType(sf.getEGenericType(), eObject, ETypedElementActionSupplier.addRow(table, "Type")::add, progressMonitor);
//			
//			boolean isDefaultFeature = EObjectLoader.isDefaultFeature(eObject, sf);
//			if (isDefaultFeature) {
//				ETypedElementActionSupplier.addRow(table, "Default").add("true");				
//			}
//			
//			boolean isHomogeneous = HomogeneousPredicate.test(sf);
//			if (isHomogeneous) {
//				ETypedElementActionSupplier.addRow(table, "Homogeneous").add("true");									
//			}
//			
//			boolean isStrictContainment = strictContainmentPredicate.test(sf);			
//			if (isStrictContainment) {
//				ETypedElementActionSupplier.addRow(table, "Strict containment").add("true");									
//			}
//			
//			Object[] exclusiveWith = exclusiveWithExtractor.apply(sf);
//			if (exclusiveWith.length != 0) {
//				Tag ul = TagName.ul.create();
//				for (Object exw: exclusiveWith) {
//					ul.content(TagName.li.create(exw));
//				}
//				ETypedElementActionSupplier.addRow(table, "Exclusive with").add(ul);				
//			}
//
//			addContent(featureAction, table.toString());
//			
//			EModelElementDocumentation featureLoadDoc = getFeatureLoadDoc(sf);
//			if (featureLoadDoc != null) {
//				featureAction.getContent().add(interpolatedMarkdown(context.interpolateToString(featureLoadDoc.documentation()), featureLoadDoc.location(), progressMonitor));
//			}
//		}	
//		
		loadSpecificationAction.getContent().add(loadSpecificationTable);
		
		return loadSpecificationAction;
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
				generateLoadSpecification(labels, progressMonitor);				
			}
		}; 
		return super.createLabelsSupplier().then(loadSpecificationConsumer.asFunction());
	}
	
	protected void generateLoadSpecification(Collection<Label> labels, ProgressMonitor progressMonitor) {
		for (Label label: labels) {
			if (label instanceof Action) {
				Action action = (Action) label;
				Action loadSpecificationAction = createLoadSpecificationAction(action, progressMonitor);
				if (loadSpecificationAction != null) {
					action.getNavigation().add(loadSpecificationAction);
				}
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
		
	private Map<Integer,WidgetFactory> eGenericSuperTypeWidgetFactories = Collections.synchronizedMap(new TreeMap<>());
	
	@OutgoingEndpoint("reference.name == 'eGenericSuperTypes'")
	public final void setEGenericSuperTypeEndpoint(EReferenceConnection connection, WidgetFactory eGenericSuperTypeWidgetFactory) {
		eGenericSuperTypeWidgetFactories.put(connection.getIndex(), eGenericSuperTypeWidgetFactory);
	}	
			
	private Map<String,WidgetFactory> eAttributeWidgetFactories = Collections.synchronizedMap(new TreeMap<>());
	
	@OutgoingEndpoint("reference.name == 'eAttributes'")
	public final void setEAttributeEndpoint(EReferenceConnection connection, WidgetFactory eAttributeWidgetFactory) {
		eAttributeWidgetFactories.put(((EAttribute) connection.getTarget().get()).getName(), eAttributeWidgetFactory);
	}	
	
	private Map<String,WidgetFactory> eReferenceWidgetFactories = Collections.synchronizedMap(new TreeMap<>());
	
	@OutgoingEndpoint("reference.name == 'eReferences'")
	public final void setEReferenceEndpoint(EReferenceConnection connection, WidgetFactory eReferenceWidgetFactory) {
		eReferenceWidgetFactories.put(((EReference) connection.getTarget().get()).getName(), eReferenceWidgetFactory);
	}	
	
	private Map<String,WidgetFactory> eOperationWidgetFactories = Collections.synchronizedMap(new TreeMap<>());
	
	@OutgoingEndpoint("reference.name == 'eOperations'")
	public final void setEOperationEndpoint(EReferenceConnection connection, WidgetFactory eOperationWidgetFactory) {
		eOperationWidgetFactories.put(((EOperation) connection.getTarget().get()).getName(), eOperationWidgetFactory);
	}
		
	/**
	 * Creates a 
	 * @param base
	 * @param diagramElementProvider
	 * @return
	 */
	@Override
	public org.nasdanika.diagram.plantuml.clazz.Type generateDiagramElement(
			URI base, 
			Function<EClassifier, CompletionStage<DiagramElement>> diagramElementProvider,
			ProgressMonitor progressMonitor) {		
		
		// Own definition
		org.nasdanika.diagram.plantuml.clazz.Type type;
		if (getTarget().isInterface()) {
			type = new org.nasdanika.diagram.plantuml.clazz.Interface(getTarget().getName());
		} else {
			type = new org.nasdanika.diagram.plantuml.clazz.Class(getTarget().getName());
			((org.nasdanika.diagram.plantuml.clazz.Class) type).setAbstract(getTarget().isAbstract());
		}
		
		CompletionStage<DiagramElement> thisTypeCompletedStage = CompletableFuture.completedStage(type);
		
		Function<EClassifier, CompletionStage<DiagramElement>> dep = ec -> {
			if (ec.getEPackage().getNsURI().equals(getTarget().getEPackage().getNsURI()) && ec.getName().equals(getTarget().getName())) {
				return thisTypeCompletedStage;
			}
			return diagramElementProvider.apply(ec);
		};
		
		// TODO - generic parameters - add to name < ... >
		
		Selector<SuperType> superTypeSelector = (widgetFactory, sBase, pm) -> {			
			List<Link> typeLink = ((EGenericTypeNodeProcessor) widgetFactory).generateDiagramLink(sBase, progressMonitor);
			SuperType superType = new SuperType();
			superType.getName().addAll(typeLink);						
			return superType;
		};
		
		for (WidgetFactory swf: eGenericSuperTypeWidgetFactories.values()) {
			SuperType superType = swf.select(superTypeSelector, base, progressMonitor);
			type.getSuperTypes().add(superType);			
			EGenericType sgt = (EGenericType) swf.select(EObjectNodeProcessor.TARGET_SELECTOR, base, progressMonitor);
			EClassifier st = sgt.getEClassifier();
			dep.apply(st).thenAccept(stde -> {
				type.getSuperTypes().remove(superType);
				
				boolean isSuperInterface = ((EClass) st).isInterface();				
				boolean isClass = !getTarget().isInterface();
				
				Relation superTypeRelation = isSuperInterface && isClass ? new Implementation(type, stde) : new Generalization(type, stde);
				// TODO - generic type parameters bindings if any
			});
		}
				
		Selector<Attribute> attributeSelector = (widgetFactory, sBase, pm) -> {
			return ((EStructuralFeatureNodeProcessor<?>) widgetFactory).generateMember(sBase, pm);
		};
		
		for (WidgetFactory awf: eAttributeWidgetFactories.values()) {
			Attribute attr = awf.select(attributeSelector, base, progressMonitor);
			type.getAttributes().add(attr);
		}
		
		for (WidgetFactory rwf: eReferenceWidgetFactories.values()) {
			Attribute ref = rwf.select(attributeSelector, base, progressMonitor);
			type.getReferences().add(ref);
			
			// TODO - group opposites into one, a way to select only one of two to render
			
			EReference eRef = (EReference) rwf.select(EObjectNodeProcessor.TARGET_SELECTOR, base, progressMonitor);
			EClass refType = eRef.getEReferenceType();
			dep.apply(refType).thenAccept(rtde -> {
				type.getReferences().remove(ref);
				
				enum OppositeResult {
					NO_OPPOSITE,
					GENERATE,
					NO_GENERATE
				}
				
				Selector<OppositeResult> oppositeResultSelector = (wf, sBase, pm) -> {
					if (wf instanceof EReferenceNodeProcessor) {
						WidgetFactory orwf = ((EReferenceNodeProcessor) wf).getOppositeReferenceWidgetFactory();
						if (orwf != null) {
							Selector<Boolean> shallGenerateSelector = (owf, osBase, opm) ->	((EObjectNodeProcessor<?>) owf).getId() > ((EObjectNodeProcessor<?>) wf).getId();
							return orwf.select(shallGenerateSelector, progressMonitor) ? OppositeResult.GENERATE : OppositeResult.NO_GENERATE;
						}
					}
					return OppositeResult.NO_OPPOSITE;	
				};
				
				Selector<String> multiplicitySelector = (wf, sBase, pm) -> ((ETypedElementNodeProcessor<?>) wf).getRelationMultiplicity();
				String targetMultiplicity = rwf.select(multiplicitySelector, base, progressMonitor);
				Object refLink = rwf.createLink(base, progressMonitor);
				
				OppositeResult oppositeResult = rwf.select(oppositeResultSelector, progressMonitor);
				if (oppositeResult != OppositeResult.NO_GENERATE) {
					Relation refRelation;
					if (eRef.isContainment()) {
						refRelation = new Composition(type, rtde, oppositeResult == OppositeResult.GENERATE);
					} else if (eRef.isMany()) {
						refRelation = new Aggregation(type, rtde, oppositeResult == OppositeResult.GENERATE);
					} else {
						refRelation = new Association(type, rtde, oppositeResult == OppositeResult.GENERATE);
					}
					
					if (oppositeResult == OppositeResult.NO_OPPOSITE) {
						refRelation.getName().add(new Link(eRef.getName()));
										
						if (refLink instanceof Label) {
							refRelation.setTooltip(((Label) refLink).getTooltip());
						}
						if (refLink instanceof org.nasdanika.html.model.app.Link) {
							refRelation.setLocation(((org.nasdanika.html.model.app.Link) refLink).getLocation());
						}				
						
						if (targetMultiplicity != null) {
							refRelation.setTargetDecoration(targetMultiplicity);
						}
					} else {
						// Target decoration
						Link targetLink = new Link(eRef.getName());

						if (refLink instanceof Label) {
							targetLink.setTooltip(((Label) refLink).getTooltip());
						}
						if (refLink instanceof org.nasdanika.html.model.app.Link) {
							targetLink.setLocation(((org.nasdanika.html.model.app.Link) refLink).getLocation());
						}				
						
						if (targetMultiplicity == null) {
							refRelation.setTargetDecoration(targetLink.toString());
						} else {
							refRelation.setTargetDecoration(targetLink + "[" + targetMultiplicity + "]");							
						}
																		
						// Source decoration
						Selector<WidgetFactory> oppositeSelector = (wf, sBase, pm) -> ((EReferenceNodeProcessor) wf).getOppositeReferenceWidgetFactory();
						WidgetFactory owf = rwf.select(oppositeSelector, progressMonitor);
						EReference eRefOpposite = (EReference) owf.select(EObjectNodeProcessor.TARGET_SELECTOR, base, progressMonitor);
						Object refOppositeLink = owf.createLink(base, progressMonitor);
						String sourceMultiplicity = owf.select(multiplicitySelector, base, progressMonitor);
						
						Link sourceLink = new Link(eRefOpposite.getName());

						if (refOppositeLink instanceof Label) {
							sourceLink.setTooltip(((Label) refOppositeLink).getTooltip());
						}
						if (refOppositeLink instanceof org.nasdanika.html.model.app.Link) {
							sourceLink.setLocation(((org.nasdanika.html.model.app.Link) refOppositeLink).getLocation());
						}				
						
						if (sourceMultiplicity == null) {
							refRelation.setSourceDecoration(sourceLink.toString());
						} else {
							refRelation.setSourceDecoration(sourceLink + "[" + sourceMultiplicity + "]");							
						}
					}
					
					// TODO - generic parameters if any
				}
			});

		}
		
		Selector<Operation> operationSelector = (widgetFactory, sBase, pm) -> {
			return ((EOperationNodeProcessor) widgetFactory).generateOperation(sBase, pm);
		};
				
		for (WidgetFactory owf: eOperationWidgetFactories.values()) {
			Operation operation = owf.select(operationSelector, base, progressMonitor);
			type.getOperations().add(operation);
		}
		
		Object link = createLink(base, progressMonitor);
		if (link instanceof Label) {
			type.setTooltip(((Label) link).getTooltip());
		}
		if (link instanceof org.nasdanika.html.model.app.Link) {
			type.setLocation(((org.nasdanika.html.model.app.Link) link).getLocation());
		}
		
		return type;
	}
	
	/**
	 * Returns attributes action, creates if necessary. Matches by location.
	 * @param parent
	 * @return
	 */
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
		
		org.nasdanika.diagram.plantuml.clazz.Type thisType = generateDiagramElement(uri, diagramElementCompletionStageProvider, progressMonitor);		
		thisType.setStyle("#DDDDDD");
		classDiagram.getDiagramElements().add(thisType);
		diagramElementProvider.apply(getTarget()).complete(thisType);
				
		// Related elements
		
		Selector<DiagramElement> genericTypeClassifierDiagramElementSelector = (widgetFactory, sBase, pm) -> {
			return ((EGenericTypeNodeProcessor) widgetFactory).generateEClassifierDiagramElement(sBase, diagramElementCompletionStageProvider, pm);
		};

		// Supertypes
		for (WidgetFactory swf: eGenericSuperTypeWidgetFactories.values()) {
			EGenericType sgt = (EGenericType) swf.select(EObjectNodeProcessor.TARGET_SELECTOR, progressMonitor); 
			EClassifier st = sgt.getEClassifier();
			CompletableFuture<DiagramElement> stcf = diagramElementProvider.apply(st);
			if (!stcf.isDone()) {
				DiagramElement stde = swf.select(genericTypeClassifierDiagramElementSelector, uri, progressMonitor);
				classDiagram.getDiagramElements().add(stde);
				stcf.complete(stde);
			}
		}	
		
		Selector<DiagramElement> subTypeDiagramElementSelector = (widgetFactory, sBase, pm) -> {
			return ((EClassifierNodeProcessor<?>) widgetFactory).generateDiagramElement(sBase, diagramElementCompletionStageProvider, pm);
		};		
		
		// Subtypes
		for (SubTypeRecord subTypeRecord: getSubTypes(progressMonitor)) {
			if (subTypeRecord.isDirect()) {
				EClassifier st = subTypeRecord.subType();
				CompletableFuture<DiagramElement> stcf = diagramElementProvider.apply(st);
				if (!stcf.isDone()) {
					DiagramElement stde = subTypeRecord.subTypeWidgetFactory().select(subTypeDiagramElementSelector, uri, progressMonitor);
					classDiagram.getDiagramElements().add(stde);
					stcf.complete(stde);
				}
			}
		}	
		
		Selector<DiagramElement> referenceTypeDiagramElementSelector = (widgetFactory, sBase, pm) -> {
			return ((ETypedElementNodeProcessor<?>) widgetFactory).generateTypeDiagramElement(sBase, diagramElementCompletionStageProvider, pm);
		};
		
		// References
		
		// Outgoing
		for (WidgetFactory rwf: eReferenceWidgetFactories.values()) {
			EReference eRef = (EReference) rwf.select(EObjectNodeProcessor.TARGET_SELECTOR, uri, progressMonitor); 
			EClass refClass = eRef.getEReferenceType();
			CompletableFuture<DiagramElement> rccf = diagramElementProvider.apply(refClass);
			if (!rccf.isDone()) {
				DiagramElement rtde = rwf.select(referenceTypeDiagramElementSelector, uri, progressMonitor);
				classDiagram.getDiagramElements().add(rtde);
				rccf.complete(rtde);
			}
		}		
		
		Selector<DiagramElement> declaringClassDiagramElementSelector = (widgetFactory, sBase, pm) -> {
			if (widgetFactory instanceof EStructuralFeatureNodeProcessor<?>) {
				widgetFactory = ((EStructuralFeatureNodeProcessor<?>) widgetFactory).getDeclaringClassWidgetFactory();
			}
			EClassNodeProcessor eClassNodeProcessor = (EClassNodeProcessor) widgetFactory.select(SELF_SELECTOR, sBase, pm);
			return eClassNodeProcessor.generateDiagramElement(sBase, diagramElementCompletionStageProvider, pm);
		};
		
		// Incoming		
		for (Entry<EGenericType, WidgetFactory> crgt: classifierReferencingGenericTypes.entrySet()) {
			EGenericTypeNodeProcessor genericTypeNodeProcessor = (EGenericTypeNodeProcessor) crgt.getValue().select(SELF_SELECTOR, progressMonitor);
			for (Entry<ETypedElement, WidgetFactory> typedElementWidgetFactoryEntry: genericTypeNodeProcessor.getTypedElementWidgetFactories().entrySet()) {
				ETypedElement typedElement = typedElementWidgetFactoryEntry.getKey();
				if (typedElement instanceof EReference) {
					EClass declaringClass = ((EReference) typedElement).getEContainingClass();
					CompletableFuture<DiagramElement> dccf = diagramElementProvider.apply(declaringClass);
					if (!dccf.isDone()) {
						DiagramElement dcde = typedElementWidgetFactoryEntry.getValue().select(declaringClassDiagramElementSelector, uri, progressMonitor);
						classDiagram.getDiagramElements().add(dcde);
						dccf.complete(dcde);
					}
				}
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
	
	@Override
	public Collection<EClassifierNodeProcessor<?>> getEClassifierNodeProcessors(int depth, ProgressMonitor progressMonitor) {
		Collection<EClassifierNodeProcessor<?>> ret = new HashSet<>();
		ret.add(this);
		if (depth > 0) {
			Selector<Collection<EClassifierNodeProcessor<?>>> eClassifierNodeProcessorSelector =  EClassifierNodeProcessorProvider.createEClassifierNodeProcessorSelector(depth - 1);
			for (WidgetFactory rtwf: reifiedTypesWidgetFactories.values()) {
				ret.addAll(rtwf.select(eClassifierNodeProcessorSelector, progressMonitor));
			}		
			
			// Supertypes
			for (WidgetFactory gswf: eGenericSuperTypeWidgetFactories.values()) {
				ret.addAll(gswf.select(eClassifierNodeProcessorSelector, progressMonitor));				
			}

			// Subtypes
			for (SubTypeRecord subtypeRecord: getSubTypes(progressMonitor)) {
				if (subtypeRecord.isDirect()) {
					ret.addAll(subtypeRecord.subTypeWidgetFactory.select(eClassifierNodeProcessorSelector, progressMonitor));
				}
			}					
			
			// Attributes
			for (WidgetFactory awf: eAttributeWidgetFactories.values()) {
				ret.addAll(awf.select(eClassifierNodeProcessorSelector, progressMonitor));				
			}			
			
			// References
			for (WidgetFactory rwf: eReferenceWidgetFactories.values()) {
				ret.addAll(rwf.select(eClassifierNodeProcessorSelector, progressMonitor));				
			}

			Selector<Collection<EClassifierNodeProcessor<?>>> declaringClassSelector = (wf, sBase, pm) -> {
				if (wf instanceof EStructuralFeatureNodeProcessor<?>) {
					wf = ((EStructuralFeatureNodeProcessor<?>) wf).getDeclaringClassWidgetFactory();
				}
				return wf.select(eClassifierNodeProcessorSelector, sBase, progressMonitor);
			};	
			
			for (Entry<EGenericType, WidgetFactory> crgt: classifierReferencingGenericTypes.entrySet()) {
				EGenericTypeNodeProcessor genericTypeNodeProcessor = (EGenericTypeNodeProcessor) crgt.getValue().select(SELF_SELECTOR, progressMonitor);
				for (Entry<ETypedElement, WidgetFactory> typedElementWidgetFactoryEntry: genericTypeNodeProcessor.getTypedElementWidgetFactories().entrySet()) {
					ETypedElement typedElement = typedElementWidgetFactoryEntry.getKey();
					if (typedElement instanceof EReference) {
						ret.addAll(typedElementWidgetFactoryEntry.getValue().select(declaringClassSelector, progressMonitor)); 				
					}
				}
			}
			
			// Operations
			for (WidgetFactory owf: eOperationWidgetFactories.values()) {
				ret.addAll(owf.select(eClassifierNodeProcessorSelector, progressMonitor));				
			}						
		}
		return ret;
	}
			
	protected Label createGraphsLabel(ProgressMonitor progressMonitor) {		
		Label graphAction = AppFactory.eINSTANCE.createAction();
		graphAction.setText("Graphs");
		graphAction.setIcon("https://img.icons8.com/external-dreamstale-lineal-dreamstale/16/external-diagram-seo-media-dreamstale-lineal-dreamstale.png");
		graphAction.getChildren().add(createDefaultGraphAction(progressMonitor));
		graphAction.getChildren().add(createCircularGraphAction(progressMonitor));
		graphAction.getChildren().add(createForceGraphAction(progressMonitor));
	    
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
	
	protected Action createDefaultGraphAction(ProgressMonitor progressMonitor) {
		Action graphAction = AppFactory.eINSTANCE.createAction();
		graphAction.setText("Default Graph");
		graphAction.setLocation("default-graph.html");
		
		GraphSeries graphSeries = new org.icepear.echarts.charts.graph.GraphSeries()
				.setSymbolSize(16)
				.setDraggable(true)				
				.setLayout("none")
	            .setLabel(new SeriesLabel().setShow(true).setPosition("right"))
	            .setLineStyle(new GraphEdgeLineStyle().setColor("source").setCurveness(0))
	            .setRoam(true)
	            .setEmphasis(new GraphEmphasis().setFocus("adjacency")); // Line style width 10?
				
		generateGraph(progressMonitor).configureGraphSeries(graphSeries);		
		org.icepear.echarts.Graph echartsGraph = new org.icepear.echarts.Graph().addSeries(graphSeries).setLegend();
		
	    Engine engine = new Engine();
	    String chartJSON = engine.renderJsonOption(echartsGraph);
	    
		String chartHTML = Context
				.singleton("chart", chartJSON)
				.compose(Context.singleton("graphContainerId", EPackageNodeProcessor.GRAPH_CONTAINER_COUNTER.incrementAndGet()))
				.interpolateToString(EPackageNodeProcessor.GRAPH_TEMPLATE);
		addContent(graphAction, chartHTML);
	    
		graphAction.setDecorator(
				createHelpDecorator(
						"A fixed force-layout graph of this classifier and related classifiers showing relationships between them", 
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
		
		org.nasdanika.models.echarts.graph.Graph graph = generateGraph(progressMonitor);
		
		GraphSeries graphSeries = new org.icepear.echarts.charts.graph.GraphSeries()
				.setSymbolSize(16)
				.setDraggable(true)				
				.setLayout("circular")
				.setCircular(new GraphCircular().setRotateLabel(true))
	            .setLabel(new SeriesLabel().setShow(true).setPosition("right"))
	            .setLineStyle(new GraphEdgeLineStyle().setColor("source").setCurveness(0.3))
	            .setRoam(true)
	            .setEmphasis(new GraphEmphasis().setFocus("adjacency")); // Line style width 10?
				
		graph.configureGraphSeries(graphSeries);		
		org.icepear.echarts.Graph echartsGraph = new org.icepear.echarts.Graph().addSeries(graphSeries).setLegend();
		
	    Engine engine = new Engine();
	    String chartJSON = engine.renderJsonOption(echartsGraph);
	    
		String chartHTML = Context
				.singleton("chart", chartJSON)
				.compose(Context.singleton("graphContainerId", EPackageNodeProcessor.GRAPH_CONTAINER_COUNTER.incrementAndGet()))
				.interpolateToString(EPackageNodeProcessor.GRAPH_TEMPLATE);
		addContent(graphAction, chartHTML);
	    
		graphAction.setDecorator(
				createHelpDecorator(
						"A circular layout graph of this classifier and related classifiers showing relationships between them", 
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

	protected org.nasdanika.models.echarts.graph.Graph generateGraph(ProgressMonitor progressMonitor) {
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
			return ((EClassifierNodeProcessor<?>) widgetFactory).generateEChartsGraphNode(sBase, nodeCompletionStageProvider, categoryProvider, progressMonitor);
		};
		
		for (WidgetFactory cwf: getEClassifierNodeProcessors(1, progressMonitor)) {
			EClassifier eClassifier = (EClassifier) cwf.select(EObjectNodeProcessor.TARGET_SELECTOR, progressMonitor); 
			CompletableFuture<org.nasdanika.models.echarts.graph.Node> eccf = nodeProvider.apply(eClassifier);
			if (!eccf.isDone()) {
				org.nasdanika.models.echarts.graph.Node ecn = cwf.select(eClassifierNodeSelector, uri, progressMonitor);
				graph.getNodes().add(ecn);
				eccf.complete(ecn);
			}
		}
		
		forceLayout(graph);
		return graph;
	}
		
	protected Action createForceGraphAction(ProgressMonitor progressMonitor) {
		Action graphAction = AppFactory.eINSTANCE.createAction();
		graphAction.setText("Force Graph");
		graphAction.setLocation("force-layout-graph.html");
		
		GraphSeries graphSeries = new org.icepear.echarts.charts.graph.GraphSeries()
				.setSymbolSize(16)
				.setDraggable(true)				
				.setLayout("force")
				.setForce(new GraphForce().setRepulsion(200).setGravity(0.1).setEdgeLength(200))
	            .setLabel(new SeriesLabel().setShow(true).setPosition("right"))
	            .setLineStyle(new GraphEdgeLineStyle().setColor("source").setCurveness(0))
	            .setRoam(true)
	            .setEmphasis(new GraphEmphasis().setFocus("adjacency")); // Line style width 10?
		
		
		generateGraph(progressMonitor).configureGraphSeries(graphSeries);
		
		org.icepear.echarts.Graph echartsGraph = new org.icepear.echarts.Graph()
				.setLegend()
				.addSeries(graphSeries);
		
	    Engine engine = new Engine();
	    String chartJSON = engine.renderJsonOption(echartsGraph);
	    
		String chartHTML = Context
				.singleton("chart", chartJSON)
				.compose(Context.singleton("graphContainerId", EPackageNodeProcessor.GRAPH_CONTAINER_COUNTER.incrementAndGet()))
				.interpolateToString(EPackageNodeProcessor.GRAPH_TEMPLATE);
		addContent(graphAction, chartHTML);
	    
		graphAction.setDecorator(
				createHelpDecorator(
						"A live force-layout graph of this classifier and related classifiers showing relationships between them", 
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

}
