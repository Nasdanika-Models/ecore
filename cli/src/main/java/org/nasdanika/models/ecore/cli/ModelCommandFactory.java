package org.nasdanika.models.ecore.cli;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;

import org.nasdanika.capability.CapabilityProvider;
import org.nasdanika.cli.SubCommandCapabilityFactory;
import org.nasdanika.common.ProgressMonitor;

import picocli.CommandLine;

public class ModelCommandFactory extends SubCommandCapabilityFactory<ModelCommand> {

	@Override
	protected CompletionStage<ModelCommand> createCommand(
			List<CommandLine> parentPath, 
			BiFunction<Object, ProgressMonitor, CompletionStage<Iterable<CapabilityProvider<Object>>>> resolver,
			ProgressMonitor progressMonitor) {
		if (parentPath != null && parentPath.size() > 1) {
			Object userObj = parentPath.get(parentPath.size() - 1).getCommandSpec().userObject();
			if (userObj instanceof org.nasdanika.html.model.app.gen.cli.AppCommand) {
				return CompletableFuture.completedStage(new ModelCommand());
			}
		}
		return null;
	}

	@Override
	protected Class<ModelCommand> getCommandType() {
		return ModelCommand.class;
	}

}
