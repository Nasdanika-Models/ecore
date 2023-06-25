package org.nasdanika.models.ecore.graph.processors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EGenericType;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcorePackage;
import org.nasdanika.common.Context;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.emf.persistence.EObjectLoader;
import org.nasdanika.graph.emf.EObjectNode;
import org.nasdanika.graph.emf.EOperationConnection;
import org.nasdanika.graph.emf.EReferenceConnection;
import org.nasdanika.graph.processor.NodeProcessorConfig;
import org.nasdanika.graph.processor.OutgoingEndpoint;
import org.nasdanika.html.TagName;
import org.nasdanika.html.model.app.Action;
import org.nasdanika.html.model.app.AppFactory;
import org.nasdanika.html.model.app.Label;
import org.nasdanika.html.model.app.gen.DynamicTableBuilder;
import org.nasdanika.html.model.app.graph.Registry;
import org.nasdanika.html.model.app.graph.WidgetFactory;
import org.nasdanika.html.model.app.graph.emf.IncomingReferenceBuilder;
import org.nasdanika.html.model.app.graph.emf.OutgoingReferenceBuilder;
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
			NodeProcessorConfig<Object, WidgetFactory, WidgetFactory, Registry<URI>> config,
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
	protected String typeLink(EReferenceConnection connection, WidgetFactory widgetFactory, ProgressMonitor progressMonitor) {
		EObject tt = connection.getTarget().getTarget();
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
							ENamedElement ane = (ENamedElement) a.getKey().getTarget().getTarget();
							ENamedElement bne = (ENamedElement) b.getKey().getTarget().getTarget();
							return ane.getName().compareTo(bne.getName());
						}).collect(Collectors.toList()),  
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
							ENamedElement ane = (ENamedElement) a.getKey().getTarget().getTarget();
							ENamedElement bne = (ENamedElement) b.getKey().getTarget().getTarget();
							return ane.getName().compareTo(bne.getName());
						}).collect(Collectors.toList()),  
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
					.addStringColumnBuilder("parameters", true, false, "Parameters", endpoint -> endpoint.getValue().createWidgetString(new ReifiedTypeSelector(EcorePackage.Literals.EOPERATION__EPARAMETERS), progressMonitor))
					.addStringColumnBuilder("exceptions", true, false, "Exceptions", endpoint -> endpoint.getValue().createWidgetString(new ReifiedTypeSelector(EcorePackage.Literals.EOPERATION__EGENERIC_EXCEPTIONS), progressMonitor))
					.addStringColumnBuilder("type-parameters", true, false, "Type Parameters", endpoint -> endpoint.getValue().createWidgetString(new ReifiedTypeSelector(EcorePackage.Literals.EOPERATION__ETYPE_PARAMETERS), progressMonitor));		
				
				// TODO - overrides, not visible by default and not sortable
				
				
				
				List<Entry<EReferenceConnection, WidgetFactory>> sorted = new ArrayList<>();
				
				referenceOutgoingEndpoints
					.stream().sorted((a,b) -> {
						ENamedElement ane = (ENamedElement) a.getKey().getTarget().getTarget();
						ENamedElement bne = (ENamedElement) b.getKey().getTarget().getTarget();
						return ane.getName().compareTo(bne.getName());
					})
					.forEach(e -> {
						EOperation eop = (EOperation) e.getKey().getTarget().getTarget();
						Iterator<Entry<EReferenceConnection, WidgetFactory>> sit = sorted.iterator();
						boolean isOverridden = false;
						while (sit.hasNext()) {
							EOperation fop = (EOperation) sit.next().getKey().getTarget().getTarget();
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
			List<Entry<EReferenceConnection, WidgetFactory>> referenceIncomingEndpoints, 
			Collection<Label> labels,
			Map<EReferenceConnection, Collection<Label>> incomingLabels, 
			ProgressMonitor progressMonitor) {
		
		for (Label label : labels) {
			if (label instanceof Action) {
				record SubTypeRecord(EClass subType, String subTypeLinkStr, EGenericType genericSuperType, WidgetFactory genericSuperTypeWidgetFactory, boolean isDirect) implements Comparable<SubTypeRecord> {

					@Override
					public int compareTo(SubTypeRecord o) {
						return subType().getName().compareTo(o.subType().getName());
					}
					
				}

				// TODO - collect subtypes into a record/class which would then build columns?
				List<SubTypeRecord> subTypes = new ArrayList<>();
				
				for (Entry<EReferenceConnection, WidgetFactory> incomingEndpoint: referenceIncomingEndpoints) {
					EGenericType genericSuperType = (EGenericType) ((EObjectNode) incomingEndpoint.getKey().getSource()).getTarget();
					WidgetFactory genericSuperTypeWidgetFactory = incomingEndpoint.getValue();
					
					Selector<List<SubTypeRecord>> selector = (wf, base, pm) -> {
						List<SubTypeRecord> ret = new ArrayList<>();
						EGenericTypeNodeProcessor processor = (EGenericTypeNodeProcessor) wf;
						for (Entry<EClass, WidgetFactory> subtypeWidgetFactoryEntry: processor.getSubTypeWidgetFactories().entrySet()) {
							ret.add(new SubTypeRecord(
									subtypeWidgetFactoryEntry.getKey(), 
									subtypeWidgetFactoryEntry.getValue().createLinkString(base, pm), 
									genericSuperType, 
									genericSuperTypeWidgetFactory, 
									subtypeWidgetFactoryEntry.getKey().getESuperTypes().contains(EClassNodeProcessor.this.getTarget())));							
						}
						return ret;
					};										
					
					subTypes.addAll(genericSuperTypeWidgetFactory.createWidget(selector, progressMonitor));
				}
				
				Collections.sort(subTypes);				
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
		
	private List<FeatureWidgetFactory> featureWidgetFactories = new ArrayList<>();
	
	protected FeatureWidgetFactory getFeatureWidgetFactory(WidgetFactory widgetFactory, URI base, ProgressMonitor progressMonitor) {
		return (FeatureWidgetFactory) widgetFactory;
	}
	
	@OutgoingEndpoint("reference.name == 'eAllOperations'")
	public final void setEOperationEndpoint(EReferenceConnection connection, WidgetFactory eOperationWidgetFactory, ProgressMonitor progressMonitor) {
		FeatureWidgetFactory featureWidgetFactory = eOperationWidgetFactory.createWidget((Selector<FeatureWidgetFactory>) this::getFeatureWidgetFactory, progressMonitor);
		if (featureWidgetFactory.isLoadable()) {
			featureWidgetFactories.add(featureWidgetFactory);
		}
	}	
	
	@OutgoingEndpoint("reference.name == 'eAllStructuralFeatures'")
	public final void setEStructuralFeatureEndpoint(EReferenceConnection connection, WidgetFactory eStructuralFeatureWidgetFactory, ProgressMonitor progressMonitor) {
		FeatureWidgetFactory featureWidgetFactory = eStructuralFeatureWidgetFactory.createWidget((Selector<FeatureWidgetFactory>) this::getFeatureWidgetFactory, progressMonitor);
		if (featureWidgetFactory.isLoadable()) {
			featureWidgetFactories.add(featureWidgetFactory);
		}
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
		
		Action loadSpecificationAction = AppFactory.eINSTANCE.createAction();
		loadSpecificationAction.setText("Load specification");
		loadSpecificationAction.setLocation("load-specification.html");
		
//		
//		EModelElementDocumentation loadDoc = EmfUtil.getLoadDocumentation(eObject);
//		if (loadDoc != null) {
//			loadSpecificationAction.getContent().add(interpolatedMarkdown(loadDoc.documentation(), loadDoc.location(), progressMonitor));
//		}
//		
//		List<EStructuralFeature> sortedFeatures = eObject.getEAllStructuralFeatures().stream().filter(predicate.and(elementPredicate)).sorted(namedElementComparator).collect(Collectors.toList());
		
		DynamicTableBuilder<FeatureWidgetFactory> loadSpecificationTableBuilder = new DynamicTableBuilder<>();
		loadSpecificationTableBuilder.addStringColumnBuilder("key", true, true, "Key", featureWidgetFactory -> {
				String key = featureWidgetFactory.getLoadKey(getTarget());
				// TODO - link if there is a feature spec detail
				if (featureWidgetFactory.isDefaultFeature(getTarget())) {
					key = "<b>" + key + "</b>";
				}
				if (featureWidgetFactory.hasLoadSpecificationAction()) {
					// TODO - link to feature's load-specification.html
				}
				return key;
			});
		
		
//			.addStringColumnBuilder("type", true, true, "Type", attr -> {
//				EGenericType genericType = attr.getEGenericType(); 
//				if (genericType == null) {
//					return null;
//				}
//				StringBuilder sb = new StringBuilder();
//				genericType(genericType, eObject, sb::append, progressMonitor);
//				return sb.toString();
//			})
//			.addStringColumnBuilder("cardinality", true, false, "Cardinality", EModelElementActionSupplier::cardinality)
//			.addBooleanColumnBuilder("homogenous", true, false, "Homogenous", homogenousPredicate)
//			.addBooleanColumnBuilder("strict-containment", true, false, "Strict Containment", strictContainmentPredicate)
//			.addStringColumnBuilder("exclusive-with", true, false, "Exclusive With", sf -> {
//				Object[] exclusiveWith = exclusiveWithExtractor.apply(sf);
//				if (exclusiveWith.length == 0) {
//					return null;
//				}
//				Tag ul = TagName.ul.create();
//				for (Object exw: exclusiveWith) {
//					ul.content(TagName.li.create(exw));
//				}
//				return ul.toString();				
//			})
//			.addStringColumnBuilder("description", true, false, "Description", this::getEStructuralFeatureFirstLoadDocSentence);
//			// Other things not visible?
//		
		org.nasdanika.html.model.html.Tag loadSpecificationTable = loadSpecificationTableBuilder.build(
				featureWidgetFactories
					.stream()
					.sorted((a, b) -> a.getLoadKey(getTarget()).compareTo(b.getLoadKey(getTarget())))
					.collect(Collectors.toList()), 
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
//			boolean isHomogenous = homogenousPredicate.test(sf);
//			if (isHomogenous) {
//				ETypedElementActionSupplier.addRow(table, "Homogenous").add("true");									
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
	protected Label createAction(ProgressMonitor progressMonitor) {
		Action action = (Action) super.createAction(progressMonitor);
		Action loadSpecificationAction = createLoadSpecificationAction(action, progressMonitor);
		if (loadSpecificationAction != null) {
			action.getNavigation().add(loadSpecificationAction);
		}
		return action;
	}
		
//	private void generateLoadSpecification(
//			Action action, 
//			Comparator<ENamedElement> namedElementComparator,
//			ProgressMonitor progressMonitor) {
//		
//		// Load specification
//		if (!eObject.isAbstract() && "true".equals(NcoreUtil.getNasdanikaAnnotationDetail(eObject, EObjectLoader.IS_LOADABLE, "true"))) {
//			
//			Function<EStructuralFeature, String> keyExtractor = sf -> NcoreUtil.getNasdanikaAnnotationDetail(sf, EObjectLoader.LOAD_KEY, NcoreUtil.getFeatureKey(eObject, sf));
//			Predicate<EStructuralFeature> homogenousPredicate = sf -> "true".equals(NcoreUtil.getNasdanikaAnnotationDetail(sf, EObjectLoader.IS_HOMOGENOUS)) || NcoreUtil.getNasdanikaAnnotationDetail(sf, EObjectLoader.REFERENCE_TYPE) != null;
//			Predicate<EStructuralFeature> strictContainmentPredicate = homogenousPredicate.and(sf -> "true".equals(NcoreUtil.getNasdanikaAnnotationDetail(sf, EObjectLoader.IS_STRICT_CONTAINMENT)));
//			Function<EStructuralFeature, Object[]> exclusiveWithExtractor = sf -> EObjectLoader.getExclusiveWith(eObject, sf, EObjectLoader.LOAD_KEY_PROVIDER);
//			
//	}
	
	
		
}
