package io.micronaut.oraclecloud.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.stream.Collectors;

@CacheableTask
public abstract class GenerateClientFactories extends DefaultTask {

    private static final String TEMPLATE = """
        package {{package}};

        import io.micronaut.oraclecloud.clients.SdkClients;
        import io.micronaut.core.annotation.Internal;

        @Internal
        final class SdkProcessorDummy {

            @SdkClients(value = SdkClients.Kind.ASYNC, clientClasses = {{clientClassNames}})
            final static class SdkAsyncProcessorDummy {
            }

            @SdkClients(value = SdkClients.Kind.REACTOR, clientClasses = {{clientClassNames}})
            final static class SdkReactorProcessorDummy {
            }

            @SdkClients(value = SdkClients.Kind.RXJAVA2, clientClasses = {{clientClassNames}})
            final static class SdkRxProcessorDummy {
            }

        }
        """;

    @PathSensitive(PathSensitivity.NONE)
    @InputFiles
    public abstract ConfigurableFileCollection getSources();

    @Input
    public abstract Property<String> getPackageName();

    @Internal
    public abstract DirectoryProperty getBaseOutputDirectory();

    @OutputDirectory
    public Provider<Directory> getOutputDirectory() {
        return getBaseOutputDirectory();
    }

    @TaskAction
    public void generateClientFactories() {
        String clientNames = getSources().getAsFileTree()
            .matching((filterable) ->
                filterable.include("com/oracle/bmc/*/*Client.java")
            )
            .getFiles()
            .stream()
            .map(f -> '"' + getPackage(f) + "." + getClientName(f) + '"')
            .collect(Collectors.joining(", "));

        String pack = getPackageName().get();

        if (!clientNames.isEmpty()) {
            String a = "3";
            File outputFile = getOutputDirectory().get()
                .file(pack.replace('.', File.separatorChar) + "/SdkProcessorDummy.java")
                .getAsFile();

            String content = TEMPLATE.replace("{{package}}", pack)
                .replace("{{clientClassNames}}", "{" + clientNames + "}");
            writeToFile(outputFile, content);
        }
    }

    private void writeToFile(File file, String content) {
        file.getParentFile().mkdirs();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(content);
        } catch (IOException e) {
            getLogger().error("Failed to write to file", e);
        }
    }

    private String getClientName(File file) {
        String name = file.getName();
        if (name.endsWith(".java")) {
            name = name.substring(0, name.length() - ".java".length());
        }
        return name;
    }

    private String getPackage(File file) {
        String path = file.getParentFile().getAbsolutePath();
        return path.substring(path.indexOf("com/oracle/bmc")).replace(File.separatorChar, '.');
    }
}
