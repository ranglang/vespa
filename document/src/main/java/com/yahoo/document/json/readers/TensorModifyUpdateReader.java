// Copyright 2019 Oath Inc. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.

package com.yahoo.document.json.readers;

import com.yahoo.document.Field;
import com.yahoo.document.TensorDataType;
import com.yahoo.document.datatypes.TensorFieldValue;
import com.yahoo.document.json.TokenBuffer;
import com.yahoo.document.update.TensorModifyUpdate;
import com.yahoo.tensor.Tensor;
import com.yahoo.tensor.TensorAddress;
import com.yahoo.tensor.TensorType;

import java.util.Iterator;

import static com.yahoo.document.json.readers.JsonParserHelpers.expectObjectStart;
import static com.yahoo.document.json.readers.TensorReader.TENSOR_CELLS;
import static com.yahoo.document.json.readers.TensorReader.readTensorCells;

/**
 * Class used to read a modify update for a tensor field.
 */
public class TensorModifyUpdateReader {

    public static final String UPDATE_MODIFY = "modify";
    private static final String MODIFY_OPERATION = "operation";
    private static final String MODIFY_REPLACE = "replace";
    private static final String MODIFY_ADD = "add";
    private static final String MODIFY_MULTIPLY = "multiply";

    public static TensorModifyUpdate createModifyUpdate(TokenBuffer buffer, Field field) {
        expectFieldIsOfTypeTensor(field);
        expectTensorTypeHasNoneIndexedUnboundDimensions(field);
        expectObjectStart(buffer.currentToken());

        ModifyUpdateResult result = createModifyUpdateResult(buffer, field);
        expectOperationSpecified(result.operation, field.getName());
        expectTensorSpecified(result.tensor, field.getName());

        return new TensorModifyUpdate(result.operation, result.tensor);
    }

    private static void expectFieldIsOfTypeTensor(Field field) {
        if (!(field.getDataType() instanceof TensorDataType)) {
            throw new IllegalArgumentException("A modify update can only be applied to tensor fields. " +
                    "Field '" + field.getName() + "' is of type '" + field.getDataType().getName() + "'");
        }
    }

    private static void expectTensorTypeHasNoneIndexedUnboundDimensions(Field field) {
        TensorType tensorType = ((TensorDataType)field.getDataType()).getTensorType();
        if (tensorType.dimensions().stream()
                .anyMatch(dim -> dim.type().equals(TensorType.Dimension.Type.indexedUnbound))) {
            throw new IllegalArgumentException("A modify update cannot be applied to tensor types with indexed unbound dimensions. "
                    + "Field '" + field.getName() + "' has unsupported tensor type '" + tensorType + "'");
        }
    }

    private static void expectOperationSpecified(TensorModifyUpdate.Operation operation, String fieldName) {
        if (operation == null) {
            throw new IllegalArgumentException("Modify update for field '" + fieldName + "' does not contain an operation");
        }
    }

    private static void expectTensorSpecified(TensorFieldValue tensor, String fieldName) {
        if (tensor == null) {
            throw new IllegalArgumentException("Modify update for field '" + fieldName + "' does not contain tensor cells");
        }
    }

    private static class ModifyUpdateResult {
        TensorModifyUpdate.Operation operation = null;
        TensorFieldValue tensor = null;
    }

    private static ModifyUpdateResult createModifyUpdateResult(TokenBuffer buffer, Field field) {
        ModifyUpdateResult result = new ModifyUpdateResult();
        buffer.next();
        int localNesting = buffer.nesting();
        while (localNesting <= buffer.nesting()) {
            switch (buffer.currentName()) {
                case MODIFY_OPERATION:
                    result.operation = createOperation(buffer, field.getName());
                    break;
                case TENSOR_CELLS:
                    result.tensor = createTensor(buffer, field);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown JSON string '" + buffer.currentName() + "' in modify update for field '" + field.getName() + "'");
            }
            buffer.next();
        }
        return result;
    }

    private static TensorModifyUpdate.Operation createOperation(TokenBuffer buffer, String fieldName) {
        switch (buffer.currentText()) {
            case MODIFY_REPLACE:
                return TensorModifyUpdate.Operation.REPLACE;
            case MODIFY_ADD:
                return TensorModifyUpdate.Operation.ADD;
            case MODIFY_MULTIPLY:
                return TensorModifyUpdate.Operation.MULTIPLY;
            default:
                throw new IllegalArgumentException("Unknown operation '" + buffer.currentText() + "' in modify update for field '" + fieldName + "'");
        }
    }

    private static TensorFieldValue createTensor(TokenBuffer buffer, Field field) {
        TensorDataType tensorDataType = (TensorDataType)field.getDataType();
        TensorType originalType = tensorDataType.getTensorType();
        TensorType convertedType = TensorModifyUpdate.convertDimensionsToMapped(originalType);

        Tensor.Builder tensorBuilder = Tensor.Builder.of(convertedType);
        readTensorCells(buffer, tensorBuilder);
        Tensor tensor = tensorBuilder.build();

        validateBounds(tensor, originalType);

        return new TensorFieldValue(tensor);
    }

    /** Only validate if original type has indexed bound dimensions */
    static void validateBounds(Tensor convertedTensor, TensorType originalType) {
        if (originalType.dimensions().stream().noneMatch(d -> d instanceof TensorType.IndexedBoundDimension)) {
            return;
        }
        for (Iterator<Tensor.Cell> iter = convertedTensor.cellIterator(); iter.hasNext(); ) {
            Tensor.Cell cell = iter.next();
            TensorAddress address = cell.getKey();
            for (int i = 0; i < address.size(); ++i) {
                TensorType.Dimension dim = originalType.dimensions().get(i);
                if (dim instanceof TensorType.IndexedBoundDimension) {
                    long label = address.numericLabel(i);
                    long bound = dim.size().get();  // size is non-optional for indexed bound
                    if (label >= bound) {
                        throw new IndexOutOfBoundsException("Dimension '" + originalType.dimensions().get(i).name() +
                                "' has label '" + label + "' but type is " + originalType.toString());
                    }
                }
            }
        }
    }

}
