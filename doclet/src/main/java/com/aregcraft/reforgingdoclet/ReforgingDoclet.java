package com.aregcraft.reforgingdoclet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.sun.source.doctree.DocCommentTree;
import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public class ReforgingDoclet implements Doclet {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private Path destinationDirectory;

    @Override
    public void init(Locale locale, Reporter reporter) {
    }

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public Set<? extends Option> getSupportedOptions() {
        return Set.of(new Option() {
            @Override
            public int getArgumentCount() {
                return 1;
            }

            @Override
            public String getDescription() {
                return "";
            }

            @Override
            public Kind getKind() {
                return Kind.STANDARD;
            }

            @Override
            public List<String> getNames() {
                return List.of("-d");
            }

            @Override
            public String getParameters() {
                return "";
            }

            @Override
            public boolean process(String option, List<String> arguments) {
                destinationDirectory = Path.of(arguments.get(0));
                try {
                    Files.createDirectories(destinationDirectory);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return true;
            }
        }, new Option() {
            @Override
            public int getArgumentCount() {
                return 0;
            }

            @Override
            public String getDescription() {
                return "";
            }

            @Override
            public Kind getKind() {
                return Kind.STANDARD;
            }

            @Override
            public List<String> getNames() {
                return List.of("-notimestamp", "-quiet");
            }

            @Override
            public String getParameters() {
                return "";
            }

            @Override
            public boolean process(String option, List<String> arguments) {
                return true;
            }
        });
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    private String toCamelCase(String string) {
        return Character.toLowerCase(string.charAt(0)) + string.substring(1);
    }

    private String docCommentTreeToString(DocCommentTree docCommentTree) {
        var visitor = new ReforgingDocTreeVisitor();
        var strings = docCommentTree.getFullBody().stream()
                .map(it -> it.accept(visitor, null))
                .filter(Objects::nonNull).toList();
        return String.join("", strings).replaceAll("\n", "");
    }

    private String getElementSimpleName(Element element) {
        return element.getSimpleName().toString();
    }

    private String getTypeSimpleName(TypeMirror typeMirror) {
        var name = typeMirror.toString().split("\\.");
        return name[name.length - 1].toLowerCase();
    }

    @Override
    public boolean run(DocletEnvironment environment) {
        var docTrees = environment.getDocTrees();
        environment.getSpecifiedElements().stream()
                .filter(it -> {
                    var name = getElementSimpleName(it);
                    return name.contains("Ability") && !name.equals("Ability") || name.equals("Price");
                }).forEach(e -> {
                    var docCommentTree = docTrees.getDocCommentTree(e);
                    if (docCommentTree != null) {
                        var file = destinationDirectory.resolve(toCamelCase(getElementSimpleName(e)) + ".json");
                        if (Files.notExists(file)) {
                            try {
                                Files.createFile(file);
                            } catch (IOException exception) {
                                exception.printStackTrace();
                            }
                        }
                        try (var writer = Files.newBufferedWriter(file)) {
                            var obj = new JsonObject();
                            obj.addProperty("description", docCommentTreeToString(docCommentTree));
                            var properties = new JsonObject();
                            obj.add("properties", properties);
                            e.getEnclosedElements().stream()
                                    .filter(it -> it.getKind() == ElementKind.FIELD)
                                    .filter(it -> !it.getModifiers().contains(Modifier.FINAL))
                                    .map(it -> (VariableElement) it)
                                    .forEach(p -> {
                                        var property = new JsonObject();
                                        property.addProperty("type", getTypeSimpleName(p.asType()));
                                        var propertyDocCommentTree = docTrees.getDocCommentTree(p);
                                        if (propertyDocCommentTree == null) {
                                            return;
                                        }
                                        property.addProperty("description",
                                                docCommentTreeToString(propertyDocCommentTree));
                                        properties.add(getElementSimpleName(p), property);
                                    });
                            GSON.toJson(obj, writer);
                        } catch (IOException exception) {
                            exception.printStackTrace();
                        }
                    }
                });
        return true;
    }
}