/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;

import java.nio.file.Paths;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.build.MockManifest;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.utils.CodeInterceptor;
import software.amazon.smithy.utils.CodeSection;
import software.amazon.smithy.utils.ListUtils;

public class WriterDelegatorTest {
    @Test
    public void createsSymbolsAndFilesForShapeWriters() {
        MockManifest mockManifest = new MockManifest();
        SymbolProvider provider = (shape) -> Symbol.builder()
                .namespace("com.foo", ".")
                .name("Baz")
                .definitionFile("com/foo/Baz.bam")
                .build();
        WriterDelegator<MySimpleWriter> delegator = new WriterDelegator<>(
                mockManifest,
                provider,
                (f, n) -> new MySimpleWriter(n));
        Shape shape = StringShape.builder().id("com.foo#Baz").build();
        delegator.useShapeWriter(shape, writer -> {});

        assertThat(delegator.getWriters(), hasKey(Paths.get("com/foo/Baz.bam").toString()));
    }

    @Test
    public void createsFilesForSymbolWriters() {
        MockManifest mockManifest = new MockManifest();
        SymbolProvider provider = (shape) -> null;
        WriterDelegator<MySimpleWriter> delegator = new WriterDelegator<>(
                mockManifest,
                provider,
                (f, n) -> new MySimpleWriter(n));
        Symbol symbol = Symbol.builder()
                .namespace("com.foo", ".")
                .name("Baz")
                .definitionFile("com/foo/Baz.bam")
                .build();
        delegator.useSymbolWriter(symbol, writer -> {});

        assertThat(delegator.getWriters(), hasKey(Paths.get("com/foo/Baz.bam").toString()));
    }

    @Test
    public void aggregatesDependencies() {
        MockManifest mockManifest = new MockManifest();
        SymbolProvider provider = (shape) -> null;
        WriterDelegator<MySimpleWriter> delegator = new WriterDelegator<>(
                mockManifest,
                provider,
                (f, n) -> new MySimpleWriter(n));
        SymbolDependency dependency = SymbolDependency.builder()
                .packageName("x")
                .version("123")
                .build();

        delegator.useFileWriter("foo/baz", writer -> {
            writer.addDependency(dependency);
        });

        assertThat(delegator.getDependencies(), contains(dependency));
    }

    @Test
    public void writesNewlineBetweenFiles() {
        MockManifest mockManifest = new MockManifest();
        SymbolProvider provider = (shape) -> null;
        WriterDelegator<MySimpleWriter> delegator = new WriterDelegator<>(
                mockManifest,
                provider,
                (f, n) -> new MySimpleWriter(n));

        delegator.useFileWriter("foo/baz", writer -> {
            writer.write(".");
        });

        delegator.useFileWriter("foo/baz", writer -> {
            writer.write(".");
        });

        assertThat(delegator.getWriters().get(Paths.get("foo/baz").toString()).toString(),
                equalTo(".\n\n.\n"));
    }

    @Test
    public void canDisableNewlineBetweenFiles() {
        MockManifest mockManifest = new MockManifest();
        SymbolProvider provider = (shape) -> null;
        WriterDelegator<MySimpleWriter> delegator = new WriterDelegator<>(
                mockManifest,
                provider,
                (f, n) -> new MySimpleWriter(n));
        delegator.setAutomaticSeparator("");

        delegator.useFileWriter("foo/baz", writer -> {
            writer.writeInline(".");
        });

        delegator.useFileWriter("foo/baz", writer -> {
            writer.writeInline(".");
        });

        assertThat(delegator.getWriters().get(Paths.get("foo/baz").toString()).toString(),
                equalTo("..\n"));
    }

    @Test
    public void flushesAllWriters() {
        MockManifest mockManifest = new MockManifest();
        SymbolProvider provider = (shape) -> Symbol.builder()
                .namespace("com.foo", ".")
                .name("Baz")
                .definitionFile("com/foo/Baz.bam")
                .build();
        WriterDelegator<MySimpleWriter> delegator = new WriterDelegator<>(
                mockManifest,
                provider,
                (f, n) -> new MySimpleWriter(n));
        Shape shape = StringShape.builder().id("com.foo#Baz").build();
        delegator.useShapeWriter(shape, writer -> {
            writer.write("Hi!");
        });

        delegator.flushWriters();

        assertThat(mockManifest.getFileString("com/foo/Baz.bam"), equalTo(Optional.of("Hi!\n")));
    }

    @Test
    public void registersInterceptors() {
        MockManifest mockManifest = new MockManifest();
        SymbolProvider provider = (shape) -> Symbol.builder()
                .namespace("com.foo", ".")
                .name("Baz")
                .definitionFile("com/foo/Baz.bam")
                .build();
        WriterDelegator<MySimpleWriter> delegator = new WriterDelegator<>(
                mockManifest,
                provider,
                (f, n) -> new MySimpleWriter(n));

        CodeInterceptor<CodeSection, MySimpleWriter> a = CodeInterceptor.forName("test", (w, s) -> {
            w.write("Yes");
        });
        delegator.setInterceptors(ListUtils.of(a));

        delegator.useFileWriter("/foo.txt", w -> {
            w.pushState("test").popState();
        });

        assertThat(delegator.getWriters().get(Paths.get("/foo.txt").toString()).toString(), equalTo("Yes\n"));
    }
}
