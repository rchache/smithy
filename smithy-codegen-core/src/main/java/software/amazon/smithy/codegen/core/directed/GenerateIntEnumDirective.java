/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core.directed;

import software.amazon.smithy.codegen.core.CodegenContext;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.shapes.IntEnumShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;

/**
 * Directive used to generate an intEnum.
 *
 * @param <C> CodegenContext type.
 * @param <S> Codegen settings type.
 * @see DirectedCodegen#generateIntEnumShape
 */
public final class GenerateIntEnumDirective<C extends CodegenContext<S, ?, ?>, S> extends ShapeDirective<Shape, C, S> {

    GenerateIntEnumDirective(C context, ServiceShape service, Shape shape) {
        super(context, service, validateShape(shape));
    }

    private static Shape validateShape(Shape shape) {
        if (shape.isIntEnumShape()) {
            return shape;
        }
        throw new IllegalArgumentException("GenerateIntEnum requires an IntEnum shape");
    }

    public IntEnumShape expectIntEnumShape() {
        return shape().asIntEnumShape()
                .orElseThrow(() -> new ExpectationNotMetException(
                        "Expected an IntEnum shape, but found " + shape(),
                        shape()));
    }
}
