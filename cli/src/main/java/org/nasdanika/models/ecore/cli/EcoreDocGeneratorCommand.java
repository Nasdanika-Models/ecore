package org.nasdanika.models.ecore.cli;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcorePackage;
import org.nasdanika.capability.CapabilityLoader;
import org.nasdanika.capability.CapabilityProvider;
import org.nasdanika.capability.ServiceCapabilityFactory;
import org.nasdanika.capability.ServiceCapabilityFactory.Requirement;
import org.nasdanika.capability.emf.EPackageResourceSetContributor;
import org.nasdanika.capability.emf.ResourceSetContributor;
import org.nasdanika.cli.CommandGroup;
import org.nasdanika.cli.ParentCommands;
import org.nasdanika.cli.ProgressMonitorMixIn;
import org.nasdanika.common.Context;
import org.nasdanika.common.Diagnostic;
import org.nasdanika.common.EObjectSupplier;
import org.nasdanika.common.ExecutionException;
import org.nasdanika.common.MutableContext;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.models.app.Action;
import org.nasdanika.models.app.Label;
import org.nasdanika.models.app.gen.AppSiteGenerator;
import org.nasdanika.models.ecore.graph.processors.EcoreHtmlAppGenerator;
import org.nasdanika.models.ecore.graph.processors.EcoreNodeProcessorFactory;
import org.nasdanika.ncore.NcorePackage;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

@Command(
		description = {
				"Generates html application model from an ecore model"
		},
		versionProvider = ModuleVersionProvider.class,		
		mixinStandardHelpOptions = true,
		name = "ecore-doc")
@ParentCommands(EObjectSupplier.class)
//@Description()
public class EcoreDocGeneratorCommand extends CommandGroup implements EObjectSupplier<EObject> {
		
	public EcoreDocGeneratorCommand() {
		super();
	}

	public EcoreDocGeneratorCommand(CapabilityLoader capabilityLoader) {
		super(capabilityLoader);
	}
		
	@ParentCommand
	EObjectSupplier<EObject> eObjectSupplier;
	
	@Option(
			names = "--diagram", 
			description = "Diagram file to generate")	
	private File diagramFile;
	
	@Option(
			names = "--layout-width", 
			description = {
				"Width for auto-layout of the generated diagram",
				"Defaults to ${DEFAULT-VALUE}"
			},
			defaultValue = "1400")
	private double layoutWidth;
	
	@Option(
			names = "--layout-height", 
			description = {
					"Height for auto-layout of the generated diagram",
					"Defaults to ${DEFAULT-VALUE}"
				},
			defaultValue = "800")	
	private double layoutHeight;	

	@Override
	public Collection<EObject> getEObjects(ProgressMonitor progressMonitor) {
		MutableContext context = Context.EMPTY_CONTEXT.fork();
		Consumer<Diagnostic> diagnosticConsumer = d -> d.dump(System.out, 0);
		List<Function<URI,Action>> actionProviders = new ArrayList<>();		
		EcoreGenProcessorsFactory ecoreGenProcessorFactory = new EcoreGenProcessorsFactory(
				diagramFile == null ? null : diagramFile.getAbsoluteFile().toPath(),
				layoutWidth,
				layoutHeight,
				context);	
		
		EcoreNodeProcessorFactory ecoreNodeProcessorFactory = new EcoreNodeProcessorFactory(
				context, 
				(uri, pm) -> {
					for (Function<URI, Action> ap: actionProviders) {
						Action prototype = ap.apply(uri);
						if (prototype != null) {
							return prototype;
						}
					}
					return null;
				},
				diagnosticConsumer,
				ecoreGenProcessorFactory);
		
		Predicate<ResourceSetContributor> contributorPredicate = contributor -> contributor instanceof EPackageResourceSetContributor;		
		Requirement<Predicate<ResourceSetContributor>, ResourceSetContributor> contributorRequirement = ServiceCapabilityFactory.createRequirement(
				ResourceSetContributor.class, 
				null,
				contributorPredicate);

		Map<EPackage, URI> references = new IdentityHashMap<EPackage, URI>();
		for (CapabilityProvider<Object> resourceSetContributorProvider: capabilityLoader.load(contributorRequirement, progressMonitor)) {
			resourceSetContributorProvider.getPublisher().filter(Objects::nonNull).collectList().block().forEach(contributor -> {
				if (contributor instanceof EPackageResourceSetContributor) {
					EPackageResourceSetContributor ePackageResourceSetContributor = (EPackageResourceSetContributor) contributor;
					URI docURI = ePackageResourceSetContributor.getDocumentationURI();
					if (docURI != null) {
						references.put(ePackageResourceSetContributor.getEPackage(), docURI);
					}
				}
			});
		}
				
		Function<? super EObject, URI> uriResolver = eObj -> {
			return references.get(eObj); // TODO	
		};
		
		
		Collection<EObject> ecoreObjects = eObjectSupplier.getEObjects(progressMonitor);
		EcoreHtmlAppGenerator eCoreHtmlAppGenerator = new EcoreHtmlAppGenerator(
				ecoreObjects, 
				references.keySet(), // Verify 
				uriResolver,
				ecoreNodeProcessorFactory);
		
		Map<EObject, Collection<Label>> labelMap = eCoreHtmlAppGenerator.generateHtmlAppModel(diagnosticConsumer, progressMonitor);
		return labelMap
				.values()
				.stream()
				.flatMap(Collection::stream)
				.map(EObject.class::cast)
				.toList();
	}
	
}
