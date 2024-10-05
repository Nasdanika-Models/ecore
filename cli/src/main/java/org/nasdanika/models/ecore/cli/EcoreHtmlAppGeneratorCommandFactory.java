package org.nasdanika.models.ecore.cli;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.nasdanika.cli.SubCommandCapabilityFactory;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.html.model.app.util.LabelSupplier;

import picocli.CommandLine;

public class EcoreHtmlAppGeneratorCommandFactory extends SubCommandCapabilityFactory<EcoreHtmlAppGeneratorCommand> {

	@Override
	protected Class<EcoreHtmlAppGeneratorCommand> getCommandType() {
		return EcoreHtmlAppGeneratorCommand.class;
	}
	
	@Override
	protected CompletionStage<EcoreHtmlAppGeneratorCommand> doCreateCommand(
			List<CommandLine> parentPath,
			Loader loader,
			ProgressMonitor progressMonitor) {
		
		// Do not bind to LabelSuppliers - would be an infinite loop
		if (!parentPath.isEmpty()) {
			CommandLine lastCommand = parentPath.get(parentPath.size() - 1);
			Object userObject = lastCommand.getCommandSpec().userObject();
			if (userObject instanceof LabelSupplier) {
				return null;
			}
		}
			
		return CompletableFuture.completedStage(new EcoreHtmlAppGeneratorCommand());
	}

}
