/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.pmml.compiler.commons.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import org.junit.Test;
import org.kie.pmml.commons.model.tuples.KiePMMLNameValue;

import static com.github.javaparser.StaticJavaParser.parseClassOrInterfaceType;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.kie.pmml.compiler.commons.testutils.CodegenTestUtils.commonValidateCompilation;
import static org.kie.pmml.compiler.commons.utils.CommonCodegenUtils.LAMBDA_PARAMETER_NAME;
import static org.kie.pmml.compiler.commons.utils.CommonCodegenUtils.OPTIONAL_FILTERED_KIEPMMLNAMEVALUE_NAME;
import static org.kie.pmml.compiler.commons.utils.CommonCodegenUtils.PARAMETER_NAME_TEMPLATE;

public class CommonCodegenUtilsTest {

    @Test
    public void populateMethodDeclarations() {
        final List<MethodDeclaration> toAdd = IntStream.range(0, 5)
                .boxed()
                .map(index -> getMethodDeclaration("METHOD_" + index))
                .collect(Collectors.toList());
        final ClassOrInterfaceDeclaration toPopulate = new ClassOrInterfaceDeclaration();
        assertTrue(toPopulate.getMembers().isEmpty());
        CommonCodegenUtils.populateMethodDeclarations(toPopulate, toAdd);
        final NodeList<BodyDeclaration<?>> retrieved = toPopulate.getMembers();
        assertEquals(toAdd.size(), retrieved.size());
        assertTrue(toAdd.stream().anyMatch(methodDeclaration -> retrieved.stream()
                .anyMatch(bodyDeclaration -> bodyDeclaration.equals(methodDeclaration))));
    }

    @Test
    public void getFilteredKiePMMLNameValueExpression() {
        String kiePMMLNameValueListParam = "KIEPMMLNAMEVALUELISTPARAM";
        String fieldNameToRef = "FIELDNAMETOREF";
        ExpressionStmt retrieved = CommonCodegenUtils.getFilteredKiePMMLNameValueExpression(kiePMMLNameValueListParam, fieldNameToRef, true);
        assertNotNull(retrieved);
        String expected = String.format("%1$s<%2$s> %3$s = %4$s.stream().filter((%2$s %5$s) -> %6$s.equals(\"%7$s\", %5$s.getName())).findFirst();",
                                        Optional.class.getName(),
                                        KiePMMLNameValue.class.getName(),
                                        OPTIONAL_FILTERED_KIEPMMLNAMEVALUE_NAME,
                                        kiePMMLNameValueListParam,
                                        LAMBDA_PARAMETER_NAME,
                                        Objects.class.getName(),
                                        fieldNameToRef);
        String retrievedString = retrieved.toString();
        assertEquals(expected, retrievedString);
        BlockStmt body = new BlockStmt();
        body.addStatement(retrieved);
        Parameter listParameter = new Parameter(CommonCodegenUtils.getTypedClassOrInterfaceType(List.class.getName(), Collections.singletonList(KiePMMLNameValue.class.getName())), kiePMMLNameValueListParam);
        Parameter fieldRefParameter = new Parameter(parseClassOrInterfaceType(String.class.getName()), fieldNameToRef);
        commonValidateCompilation(body, Arrays.asList(listParameter, fieldRefParameter));
        //
        retrieved = CommonCodegenUtils.getFilteredKiePMMLNameValueExpression(kiePMMLNameValueListParam, fieldNameToRef, false);
        assertNotNull(retrieved);
        expected = String.format("%1$s<%2$s> %3$s = %4$s.stream().filter((%2$s %5$s) -> %6$s.equals(%7$s, %5$s.getName())).findFirst();",
                                        Optional.class.getName(),
                                        KiePMMLNameValue.class.getName(),
                                        OPTIONAL_FILTERED_KIEPMMLNAMEVALUE_NAME,
                                        kiePMMLNameValueListParam,
                                        LAMBDA_PARAMETER_NAME,
                                        Objects.class.getName(),
                                        fieldNameToRef);
        retrievedString = retrieved.toString();
        assertEquals(expected, retrievedString);
        body = new BlockStmt();
        body.addStatement(retrieved);
        listParameter = new Parameter(CommonCodegenUtils.getTypedClassOrInterfaceType(List.class.getName(), Collections.singletonList(KiePMMLNameValue.class.getName())), kiePMMLNameValueListParam);
        fieldRefParameter = new Parameter(parseClassOrInterfaceType(String.class.getName()), fieldNameToRef);
        commonValidateCompilation(body, Arrays.asList(listParameter, fieldRefParameter));
    }

    @Test
    public void addMapPopulation() {
        final Map<String, MethodDeclaration> toAdd = IntStream.range(0, 5).boxed().collect(Collectors.toMap(index -> "KEY_" + index, index -> getMethodDeclaration("METHOD_" + index)));
        BlockStmt body = new BlockStmt();
        String mapName = "MAP_NAME";
        CommonCodegenUtils.addMapPopulation(toAdd, body, mapName);
        NodeList<Statement> statements = body.getStatements();
        assertEquals(toAdd.size(), statements.size());
        for (Statement statement : statements) {
            assertTrue(statement instanceof ExpressionStmt);
            ExpressionStmt expressionStmt = (ExpressionStmt) statement;
            com.github.javaparser.ast.expr.Expression expression = expressionStmt.getExpression();
            assertTrue(expression instanceof MethodCallExpr);
            MethodCallExpr methodCallExpr = (MethodCallExpr) expression;
            final NodeList<com.github.javaparser.ast.expr.Expression> arguments = methodCallExpr.getArguments();
            assertEquals(2, arguments.size());
            assertTrue(arguments.get(0) instanceof StringLiteralExpr);
            assertTrue(arguments.get(1) instanceof MethodReferenceExpr);
            MethodReferenceExpr methodReferenceExpr = (MethodReferenceExpr) arguments.get(1);
            assertTrue(methodReferenceExpr.getScope() instanceof ThisExpr);
            final com.github.javaparser.ast.expr.Expression scope = methodCallExpr.getScope().orElse(null);
            assertNotNull(scope);
            assertTrue(scope instanceof NameExpr);
            assertEquals(mapName, ((NameExpr) scope).getNameAsString());
        }
        for (Map.Entry<String, MethodDeclaration> entry : toAdd.entrySet()) {
            int matchingDeclarations = (int) statements.stream().filter(statement -> {
                ExpressionStmt expressionStmt = (ExpressionStmt) statement;
                com.github.javaparser.ast.expr.Expression expression = expressionStmt.getExpression();
                MethodCallExpr methodCallExpr = (MethodCallExpr) expression;
                final NodeList<com.github.javaparser.ast.expr.Expression> arguments = methodCallExpr.getArguments();
                if (!entry.getKey().equals(((StringLiteralExpr) arguments.get(0)).getValue())) {
                    return false;
                }
                MethodReferenceExpr methodReferenceExpr = (MethodReferenceExpr) arguments.get(1);
                return entry.getValue().getName().asString().equals(methodReferenceExpr.getIdentifier());
            }).count();
            assertEquals(1, matchingDeclarations);
        }
    }

    @Test
    public void getParamMethodDeclaration() {
        String methodName = "METHOD_NAME";
        final Map<String, ClassOrInterfaceType> parameterNameTypeMap = new HashMap<>();
        parameterNameTypeMap.put("stringParam", parseClassOrInterfaceType(String.class.getName()));
        parameterNameTypeMap.put("kiePMMLNameValueParam", parseClassOrInterfaceType(KiePMMLNameValue.class.getName()));
        parameterNameTypeMap.put("listParam", new ClassOrInterfaceType(null, new SimpleName(List.class.getName()), NodeList.nodeList(parseClassOrInterfaceType(KiePMMLNameValue.class.getName()))));
        MethodDeclaration retrieved = CommonCodegenUtils.getMethodDeclaration(methodName, parameterNameTypeMap);
        commonValidateMethodDeclaration(retrieved, methodName);
        commonValidateMethodDeclarationParams(retrieved, parameterNameTypeMap);
    }

    @Test
    public void getNoParamMethodDeclarationByString() {
        String methodName = "METHOD_NAME";
        MethodDeclaration retrieved = CommonCodegenUtils.getMethodDeclaration(methodName);
        commonValidateMethodDeclaration(retrieved, methodName);
    }

    @Test
    public void getReturnStmt() {
        String returnedVariable = "RETURNED_VARIABLE";
        ReturnStmt retrieved = CommonCodegenUtils.getReturnStmt(returnedVariable);
        String expected = String.format("return %s;", returnedVariable);
        assertEquals(expected, retrieved.toString());
    }

    @Test
    public void getTypedClassOrInterfaceType() {
        String className = "CLASS_NAME";
        List<String> typesName = Arrays.asList("TypeA", "TypeB");
        ClassOrInterfaceType retrieved = CommonCodegenUtils.getTypedClassOrInterfaceType(className, typesName);
        assertNotNull(retrieved);
        String expected = String.format("%1$s<%2$s,%3$s>", className, typesName.get(0), typesName.get(1));
        assertEquals(expected, retrieved.asString());
    }

    private void commonValidateMethodDeclaration(MethodDeclaration toValidate, String methodName) {
        assertNotNull(toValidate);
        assertEquals(methodName, toValidate.getName().asString());
    }

    private void commonValidateMethodDeclarationParams(MethodDeclaration toValidate, Map<String, ClassOrInterfaceType> parameterNameTypeMap) {
        final NodeList<Parameter> retrieved = toValidate.getParameters();
        assertNotNull(retrieved);
        assertEquals(parameterNameTypeMap.size(), retrieved.size());
        for (Parameter parameter : retrieved) {
            assertTrue(parameterNameTypeMap.containsKey(parameter.getNameAsString()));
            assertEquals(parameterNameTypeMap.get(parameter.getNameAsString()), parameter.getType());
        }
    }

    private MethodDeclaration getMethodDeclaration(String methodName) {
        return CommonCodegenUtils.getMethodDeclaration(methodName);
    }
}