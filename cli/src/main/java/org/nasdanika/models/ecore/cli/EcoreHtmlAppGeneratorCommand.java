package org.nasdanika.models.ecore.cli;

import java.util.Collection;
import java.util.function.Consumer;

import org.eclipse.emf.ecore.EObject;
import org.nasdanika.cli.Overrides;
import org.nasdanika.cli.ParentCommands;
import org.nasdanika.common.Context;
import org.nasdanika.common.Description;
import org.nasdanika.common.Diagnostic;
import org.nasdanika.common.EObjectSupplier;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.models.app.cli.HtmlAppGeneratorCommand;
import org.nasdanika.models.app.graph.emf.HtmlAppGenerator;
import org.nasdanika.models.ecore.graph.processors.EcoreHtmlAppGenerator;

import picocli.CommandLine.Command;

@Command(
		description = "Generates Ecore model documentation html app model",
		versionProvider = ModuleVersionProvider.class,		
		mixinStandardHelpOptions = true,
		name = "html-app")
@ParentCommands(EObjectSupplier.class)
@Overrides(HtmlAppGeneratorCommand.class)
@Description(icon = "https://docs.nasdanika.org/images/html-application.svg")
public class EcoreHtmlAppGeneratorCommand extends HtmlAppGeneratorCommand {
	
	@Override
	protected HtmlAppGenerator createHtmlAppGenerator(
			Collection<EObject> sources, 
			Context context,
			ProgressMonitor progressMonitor, 
			Consumer<Diagnostic> diagnosticConsumer) {
		
		return EcoreHtmlAppGenerator.loadEcoreHtmlAppGenerator(
				sources, 
				context, 
				createPrototypeProvider(progressMonitor), 
				createFactoryPredicate(progressMonitor),
				createEPackagePredicate(progressMonitor),
				diagnosticConsumer, 
				progressMonitor);
	}		
	
}
