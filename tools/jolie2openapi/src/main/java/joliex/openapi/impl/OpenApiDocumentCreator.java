package joliex.openapi.impl;


import jolie.lang.NativeType;
import jolie.lang.parse.ast.InputPortInfo;
import jolie.lang.parse.ast.OperationDeclaration;

import jolie.lang.parse.ast.RequestResponseOperationDeclaration;
import jolie.lang.parse.ast.types.TypeDefinition;
import jolie.lang.parse.ast.types.TypeDefinitionLink;
import jolie.lang.parse.ast.types.TypeInlineDefinition;
import jolie.lang.parse.util.ProgramInspector;
import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import jolie.util.Range;
import joliex.openapi.impl.support.OperationRestDescriptor;
import joliex.openapi.impl.support.RestDescriptor;
import joliex.openapi.impl.support.Utils;

import java.util.*;


public class OpenApiDocumentCreator {

    String inputPort;
    String outputDirectory;
    String routerHost;
    ProgramInspector inspector;
    private LinkedHashMap<String, TypeDefinition> typeMap;
    private LinkedHashMap<String, TypeDefinition> faultMap;
    private LinkedHashMap<String, TypeDefinition> subTypeMap;

    private static final HashMap<String, String> OPENAPI_NATIVE_EQUIVALENT = new HashMap<>();
    private HashMap<String, Value > subTypes = new HashMap<>();

    public OpenApiDocumentCreator(ProgramInspector inspector, String inputPort, String outputDirectory,
                                  String routerHost) {

        this.inspector = inspector;
        this.outputDirectory = outputDirectory;
        this.inputPort = inputPort;
        this.routerHost = routerHost;

        OPENAPI_NATIVE_EQUIVALENT.put("int", "integer");
        OPENAPI_NATIVE_EQUIVALENT.put("bool", "boolean");
        OPENAPI_NATIVE_EQUIVALENT.put("double", "double");
        OPENAPI_NATIVE_EQUIVALENT.put("long", "long");
        OPENAPI_NATIVE_EQUIVALENT.put("string", "string");
    }

    public Value ConvertDocument() throws FaultException {


        InputPortInfo[] inputPorts = inspector.getInputPorts();
        RestDescriptor restDescriptor = new RestDescriptor();
        restDescriptor.loadDescriptor("rest_template.json");
        Value errorValue = Value.create();
        Value documentValue = Value.create();
        for (InputPortInfo inputPortInfo : inputPorts) {
            if (inputPort.equals(inputPortInfo.id())) {
                if (!"sodep".equalsIgnoreCase(inputPortInfo.protocolId())) {
                    errorValue.getChildren("wrongProtocol").add(Value.create(inputPortInfo.protocolId()));
                }

                Map<String, OperationDeclaration> operationsMap = inputPortInfo.operationsMap();
                documentValue.getFirstChild("openapi").setValue("3.0.0");
                documentValue.getFirstChild("info").getFirstChild("description").setValue("");
                documentValue.getFirstChild("info").getFirstChild("version").setValue("1.0.0");
                documentValue.getFirstChild("info").getFirstChild("title").setValue("OpenApi Descriptor for " + inputPort);
                operationsMap.forEach((operationName, operationDeclaration) -> {
                    if (operationDeclaration instanceof RequestResponseOperationDeclaration) {
                        try {
                            OperationRestDescriptor operationDescriptor =
                                    restDescriptor.getOperationRestDescriptor(operationName);
                            TypeDefinition requestType =
                                    ((RequestResponseOperationDeclaration) operationDeclaration).requestType();
                            TypeDefinition responseType =
                                    ((RequestResponseOperationDeclaration) operationDeclaration).responseType();
                            documentValue.getFirstChild("paths").getFirstChild(operationDescriptor.path()).getFirstChild(operationDescriptor.method()).getFirstChild("description").setValue("");
                            documentValue.getFirstChild("paths").getFirstChild(operationDescriptor.path()).getFirstChild(operationDescriptor.method()).getFirstChild("operationId").setValue(operationName);

                            ValueVector inPathParametersVector = inPathValue(operationDescriptor, requestType);

                            inPathParametersVector.forEach(value -> {
                                documentValue.getFirstChild("paths").getFirstChild(operationDescriptor.path()).getFirstChild(operationDescriptor.method()).getFirstChild("parameters").getChildren("_").add(value);
                            });

                            ValueVector inQueryParametersVector = inQueryValue(operationDescriptor, requestType);

                            inQueryParametersVector.forEach(value -> {
                                documentValue.getFirstChild("paths").getFirstChild(operationDescriptor.path()).getFirstChild(operationDescriptor.method()).getFirstChild("parameters").getChildren("_").add(value);
                            });

                            ValueVector inHeaderParametersVector = inHeaderValue(operationDescriptor, requestType);

                            inHeaderParametersVector.forEach(value -> {
                                documentValue.getFirstChild("paths").getFirstChild(operationDescriptor.path()).getFirstChild(operationDescriptor.method()).getFirstChild("parameters").getChildren("_").add(value);
                            });


                            documentValue.getFirstChild("paths").getFirstChild(operationDescriptor.path())
                                    .getFirstChild(operationDescriptor.method())
                                    .getFirstChild("responses")
                                    .getFirstChild("200")
                                    .getFirstChild("content")
                                    .getFirstChild("application/json")
                                    .getFirstChild("schema")
                                    .getFirstChild("$ref")
                                    .setValue("#/components/schemas/" + responseType.id());

                            documentValue.getFirstChild("paths").getFirstChild(operationDescriptor.path())
                                    .getFirstChild(operationDescriptor.method())
                                    .getFirstChild("responses")
                                    .getFirstChild("200")
                                    .getFirstChild("description")
                                    .setValue("");

                            Value bodyType = bodyType(operationDescriptor, requestType);
                            if (bodyType.hasChildren("properties")) {

                                documentValue.getFirstChild("paths")
                                        .getFirstChild(operationDescriptor.path())
                                        .getFirstChild(operationDescriptor.method())
                                        .getFirstChild("requestBody")
                                        .getFirstChild("description")
                                        .setValue("");

                                documentValue.getFirstChild("paths")
                                        .getFirstChild(operationDescriptor.path())
                                        .getFirstChild(operationDescriptor.method())
                                        .getFirstChild("requestBody")
                                        .getFirstChild("required")
                                        .setValue(true);

                                documentValue.getFirstChild("paths")
                                        .getFirstChild(operationDescriptor.path())
                                        .getFirstChild(operationDescriptor.method())
                                        .getFirstChild("requestBody")
                                        .getFirstChild("content")
                                        .getFirstChild("application/json")
                                        .getFirstChild("schema")
                                        .getFirstChild("$ref")
                                        .setValue("#/components/schemas/" + requestType.id());

                                documentValue.getFirstChild("components").getFirstChild("schemas").getFirstChild(requestType.id()).deepCopy(bodyType);
                            }

                            Value responseTypeValue = responseType (responseType);
                            documentValue.getFirstChild("components").getFirstChild("schemas").getFirstChild(responseType.id()).deepCopy(responseTypeValue);

                            subTypes.forEach((s, value) -> {
                                        documentValue.getFirstChild("components").getFirstChild("schemas").getFirstChild(s).deepCopy(value);
                                    });

                            ((RequestResponseOperationDeclaration) operationDeclaration).faults().forEach((s, typeDefinition) ->{

                                         if (operationDescriptor.isInExceptions(s)) {
                                             documentValue.getFirstChild("paths").getFirstChild(operationDescriptor.path())
                                                     .getFirstChild(operationDescriptor.method())
                                                     .getFirstChild("responses")
                                                     .getFirstChild(operationDescriptor.getException(s))
                                                     .getFirstChild("content")
                                                     .getFirstChild("application/json")
                                                     .getFirstChild("schema")
                                                     .getFirstChild("type")
                                                     .setValue("object");


                                             documentValue.getFirstChild("paths").getFirstChild(operationDescriptor.path())
                                                     .getFirstChild(operationDescriptor.method())
                                                     .getFirstChild("responses")
                                                     .getFirstChild(operationDescriptor.getException(s))
                                                     .getFirstChild("description")
                                                     .setValue("Fault HTTP response for " + s );

                                             documentValue.getFirstChild("paths").getFirstChild(operationDescriptor.path())
                                                     .getFirstChild(operationDescriptor.method())
                                                     .getFirstChild("responses")
                                                     .getFirstChild(operationDescriptor.getException(s))
                                                     .getFirstChild("content")
                                                     .getFirstChild("application/json")
                                                     .getFirstChild("schema")
                                                     .getFirstChild("properties")
                                                     .getFirstChild("fault")
                                                     .getFirstChild("type")
                                                     .setValue("string");

                                             documentValue.getFirstChild("paths").getFirstChild(operationDescriptor.path())
                                                     .getFirstChild(operationDescriptor.method())
                                                     .getFirstChild("responses")
                                                     .getFirstChild(operationDescriptor.getException(s))
                                                     .getFirstChild("content")
                                                     .getFirstChild("application/json")
                                                     .getFirstChild("schema")
                                                     .getFirstChild("properties")
                                                     .getFirstChild("message")
                                                     .getFirstChild("type")
                                                     .setValue("string");
                                         }
                                   }
                                    );

                        } catch (FaultException e) {
                            if ("OperationNoPresent".equals(e.faultName())) {
                                errorValue.getChildren("operationNotMapped").add(Value.create(operationName));
                            } else if ("ComplexTypeInPathParameter".equals(e.faultName())) {
                                errorValue.getFirstChild(operationName).getChildren("complexTypeInPathParameter")
                                        .add(e.value());
                            } else if ("VoidTypeInPathParameter".equals(e.faultName())) {
                                errorValue.getFirstChild(operationName).getChildren("voidTypeInPathParameter")
                                        .add(e.value());
                            } else if ("LinkInPathDefinition".equals(e.faultName())) {
                                errorValue.getFirstChild(operationName).getChildren("linkTypeInPathParameter")
                                        .add(e.value());
                            } else if ("ComplexTypeInQueryParameter".equals(e.faultName())) {
                                errorValue.getFirstChild(operationName).getChildren("complexTypeInQueryParameter")
                                        .add(e.value());
                            } else if ("VoidTypeInQueryParameter".equals(e.faultName())) {
                                errorValue.getFirstChild(operationName).getChildren("voidTypeInQueryParameter")
                                        .add(e.value());
                            } else if ("LinkInQueryDefinition".equals(e.faultName())) {
                                errorValue.getFirstChild(operationName).getChildren("linkTypeInQueryParameter")
                                        .add(e.value());
                            }

                            errorValue.getChildren("operationNotMapped").add(Value.create(operationName));
                        }
                    }
                });
            } else {
                errorValue.getChildren("missingInputPort").add(Value.create(inputPort));
            }
        }
        if (errorValue.hasChildren()) {
            throw new FaultException("RestApiMappingError", errorValue);
        }

        return documentValue;
    }


    private ValueVector inPathValue(OperationRestDescriptor operationRestDescriptor, TypeDefinition typeDefinition)
            throws FaultException {
        ValueVector valueVector = ValueVector.create();
        if (!NativeType.isNativeTypeKeyword(typeDefinition.id())) {
            if (Utils.hasSubTypes(typeDefinition)) {
                Set<Map.Entry<String, TypeDefinition>> supportSet = Utils.subTypes(typeDefinition);
                Iterator i = supportSet.iterator();
                while (i.hasNext()) {
                    Map.Entry me = (Map.Entry) i.next();

                    if (operationRestDescriptor.isInPath(((TypeDefinition) me.getValue()).id())) {
                        if (((TypeDefinition) me.getValue()) instanceof TypeDefinitionLink) {
                            Value typeErrorValue = Value.create();
                            typeErrorValue.getFirstChild(((TypeDefinition) me.getValue()).id())
                                    .getFirstChild("message")
                                    .setValue("In path element need to be a native types not typeLink");
                            throw new FaultException("LinkInPathDefinition", typeErrorValue);
                        } else if (Utils.hasSubTypes((TypeDefinition) me.getValue())) {
                            Value typeErrorValue = Value.create();
                            typeErrorValue.getFirstChild(((TypeDefinition) me.getValue()).id())
                                    .getFirstChild("message")
                                    .setValue("In path element need to be a native types not complexType");
                            throw new FaultException("LinkInPathDefinition", typeErrorValue);
                        }
                        Value value = Value.create();
                        value.getFirstChild("in").setValue("path");
                        value.getFirstChild("name").setValue(((TypeDefinition) me.getValue()).id());

                        String typeName = Utils.nativeType((TypeDefinition) me.getValue()).id();
                        value.getFirstChild("schema").getFirstChild("type").setValue(OPENAPI_NATIVE_EQUIVALENT.get(typeName));
                        if (((TypeDefinition) me.getValue()).cardinality().min() == 1) {
                            value.getFirstChild("required").setValue(true);
                        } else {
                            value.getFirstChild("required").setValue(false);
                        }

                        valueVector.add(value);

                    }
                }
            }
        }
        return valueVector;
    }


    private ValueVector inQueryValue(OperationRestDescriptor operationRestDescriptor, TypeDefinition typeDefinition)
            throws FaultException {
        ValueVector valueVector = ValueVector.create();
        if (!NativeType.isNativeTypeKeyword(typeDefinition.id())) {
            if (Utils.hasSubTypes(typeDefinition)) {
                Set<Map.Entry<String, TypeDefinition>> supportSet = Utils.subTypes(typeDefinition);
                Iterator i = supportSet.iterator();
                while (i.hasNext()) {
                    Map.Entry me = (Map.Entry) i.next();

                    if (operationRestDescriptor.isInQuery(((TypeDefinition) me.getValue()).id())) {
                        if (((TypeDefinition) me.getValue()) instanceof TypeDefinitionLink) {
                            Value typeErrorValue = Value.create();
                            typeErrorValue.getFirstChild(((TypeDefinition) me.getValue()).id())
                                    .getFirstChild("message")
                                    .setValue("In query element need to be a native types not typeLink");
                            throw new FaultException("LinkInQueryDefinition", typeErrorValue);
                        } else if (Utils.hasSubTypes((TypeDefinition) me.getValue())) {
                            Value typeErrorValue = Value.create();
                            typeErrorValue.getFirstChild(((TypeDefinition) me.getValue()).id())
                                    .getFirstChild("message")
                                    .setValue("In path element need to be a native types not complexType");
                            throw new FaultException("LinkInQueryDefinition", typeErrorValue);
                        }
                        Value value = Value.create();
                        value.getFirstChild("in").setValue("query");
                        value.getFirstChild("name").setValue(((TypeDefinition) me.getValue()).id());

                        String typeName = Utils.nativeType((TypeDefinition) me.getValue()).id();
                        value.getFirstChild("schema").getFirstChild("type").setValue(OPENAPI_NATIVE_EQUIVALENT.get(typeName));
                        if (((TypeDefinition) me.getValue()).cardinality().min() == 1) {
                            value.getFirstChild("required").setValue(true);
                        } else {
                            value.getFirstChild("required").setValue(false);
                        }

                        valueVector.add(value);

                    }
                }
            }
        }
        return valueVector;
    }


    private ValueVector inHeaderValue(OperationRestDescriptor operationRestDescriptor, TypeDefinition typeDefinition)
            throws FaultException {
        ValueVector valueVector = ValueVector.create();
        if (!NativeType.isNativeTypeKeyword(typeDefinition.id())) {
            if (Utils.hasSubTypes(typeDefinition)) {
                Set<Map.Entry<String, TypeDefinition>> supportSet = Utils.subTypes(typeDefinition);
                Iterator i = supportSet.iterator();
                while (i.hasNext()) {
                    Map.Entry me = (Map.Entry) i.next();

                    if (operationRestDescriptor.isInHeader(((TypeDefinition) me.getValue()).id())) {
                        if (((TypeDefinition) me.getValue()) instanceof TypeDefinitionLink) {
                            Value typeErrorValue = Value.create();
                            typeErrorValue.getFirstChild(((TypeDefinition) me.getValue()).id())
                                    .getFirstChild("message")
                                    .setValue("In query element need to be a native types not typeLink");
                            throw new FaultException("LinkInQueryDefinition", typeErrorValue);
                        } else if (Utils.hasSubTypes((TypeDefinition) me.getValue())) {
                            Value typeErrorValue = Value.create();
                            typeErrorValue.getFirstChild(((TypeDefinition) me.getValue()).id())
                                    .getFirstChild("message")
                                    .setValue("In path element need to be a native types not complexType");
                            throw new FaultException("LinkInQueryDefinition", typeErrorValue);
                        }
                        Value value = Value.create();
                        value.getFirstChild("in").setValue("header");
                        value.getFirstChild("name").setValue(((TypeDefinition) me.getValue()).id());

                        String typeName = Utils.nativeType((TypeDefinition) me.getValue()).id();
                        value.getFirstChild("schema").getFirstChild("type").setValue(OPENAPI_NATIVE_EQUIVALENT.get(typeName));
                        if (((TypeDefinition) me.getValue()).cardinality().min() == 1) {
                            value.getFirstChild("required").setValue(true);
                        } else {
                            value.getFirstChild("required").setValue(false);
                        }

                        valueVector.add(value);

                    }
                }
            }
        }
        return valueVector;
    }


    private Value bodyType(OperationRestDescriptor operationRestDescriptor, TypeDefinition typeDefinition) {
        Value value = Value.create();
        if (Utils.hasSubTypes(typeDefinition)) {
            Set<Map.Entry<String, TypeDefinition>> supportSet = Utils.subTypes(typeDefinition);
            Iterator i = supportSet.iterator();

            value.getFirstChild("type").setValue("object");
            while (i.hasNext()) {
                Map.Entry me = (Map.Entry) i.next();
                TypeDefinition nodeTypeDefinition = (TypeDefinition) me.getValue();
                if ((!operationRestDescriptor.isInHeader((String) me.getKey()) &
                        (!operationRestDescriptor.isInPath((String) me.getKey())) &
                        (!operationRestDescriptor.isInQuery((String) me.getKey())))) {
                    if (!Utils.hasSubTypes(nodeTypeDefinition)) {
                        if (nodeTypeDefinition.cardinality().max() > 1) {
                            value.getFirstChild("properties")
                                    .getFirstChild(nodeTypeDefinition.id())
                                    .getFirstChild("type").setValue("array");
                            value.getFirstChild("properties")
                                    .getFirstChild(nodeTypeDefinition.id())
                                    .getFirstChild("items")
                                    .getFirstChild("minItems")
                                    .setValue(nodeTypeDefinition.cardinality().min());
                            value.getFirstChild("properties")
                                    .getFirstChild(nodeTypeDefinition.id())
                                    .getFirstChild("items")
                                    .getFirstChild("maxItems")
                                    .setValue(nodeTypeDefinition.cardinality().max());

                            value.getFirstChild("properties")
                                    .getFirstChild(nodeTypeDefinition.id())
                                    .getFirstChild("items")
                                    .getFirstChild("type")
                                    .setValue(OPENAPI_NATIVE_EQUIVALENT.get(Utils.nativeType(nodeTypeDefinition).id()));
                        } else {

                            value.getFirstChild("properties")
                                    .getFirstChild(nodeTypeDefinition.id())
                                    .getFirstChild("type").setValue(OPENAPI_NATIVE_EQUIVALENT.get(Utils.nativeType(nodeTypeDefinition).id()));

                        }
                    }
                }
            }
        }
        return value;
    }

    private Value responseType(TypeDefinition typeDefinition ) {
        Value value = Value.create();

        if (Utils.hasSubTypes(typeDefinition)) {
            Set<Map.Entry<String, TypeDefinition>> supportSet = Utils.subTypes(typeDefinition);
            Iterator i = supportSet.iterator();

            while (i.hasNext()) {
                value.getFirstChild("type").setValue("object");
                Map.Entry me = (Map.Entry) i.next();
                TypeDefinition nodeTypeDefinition = (TypeDefinition) me.getValue();
                if (!Utils.hasSubTypes(nodeTypeDefinition)) {
                    if (nodeTypeDefinition.cardinality().max() > 1) {
                        value.getFirstChild("properties")
                                .getFirstChild(nodeTypeDefinition.id())
                                .getFirstChild("type").setValue("array");
                        value.getFirstChild("properties")
                                .getFirstChild(nodeTypeDefinition.id())
                                .getFirstChild("items")
                                .getFirstChild("minItems")
                                .setValue(nodeTypeDefinition.cardinality().min());
                        value.getFirstChild("properties")
                                .getFirstChild(nodeTypeDefinition.id())
                                .getFirstChild("items")
                                .getFirstChild("maxItems")
                                .setValue(nodeTypeDefinition.cardinality().max());

                        value.getFirstChild("properties")
                                .getFirstChild(nodeTypeDefinition.id())
                                .getFirstChild("items")
                                .getFirstChild("type")
                                .setValue(OPENAPI_NATIVE_EQUIVALENT.get(Utils.nativeType(nodeTypeDefinition).id()));
                    } else {

                        value.getFirstChild("properties")
                                .getFirstChild(nodeTypeDefinition.id())
                                .getFirstChild("type").setValue(OPENAPI_NATIVE_EQUIVALENT.get(Utils.nativeType(nodeTypeDefinition).id()));

                    }
                }else{
                    if (nodeTypeDefinition instanceof TypeDefinitionLink){
                        value = typeDefinitionLinkParse((TypeDefinitionLink)nodeTypeDefinition);
                    }
                    if (nodeTypeDefinition instanceof TypeInlineDefinition){
                        value = inLineTypeDefinitionLinkParse((TypeInlineDefinition)nodeTypeDefinition);
                    }
                }
            }

        }else{
            value.getFirstChild("type").setValue("object");
        }

      return value;
    }
    private Value typeDefinitionLinkParse (TypeDefinitionLink typeDefinitionLink){

            Value  value = Value.create();
            if ((typeDefinitionLink.cardinality().min() == 1 ) & (typeDefinitionLink.cardinality().max() == 1))
            {
                value.getFirstChild("type").setValue("object");
                value.getFirstChild("properties")
                        .getFirstChild(typeDefinitionLink.id()).getFirstChild("type").setValue("object");
                value.getFirstChild("properties")
                        .getFirstChild(typeDefinitionLink.id()).getFirstChild("$ref").setValue("#/components/schemas/" + (typeDefinitionLink.linkedTypeName()));
                subTypes.put( typeDefinitionLink.linkedTypeName(), responseType( typeDefinitionLink));
            }else if ((typeDefinitionLink.cardinality().min() == 0 ) & (typeDefinitionLink.cardinality().max() >= 1)){
                value.getFirstChild("type").setValue("object");
                value.getFirstChild("properties")
                        .getFirstChild(typeDefinitionLink.id())
                        .getFirstChild("type").setValue("array");
                value.getFirstChild("properties")
                        .getFirstChild(typeDefinitionLink.id())
                        .getFirstChild("items")
                        .getFirstChild("minItems")
                        .setValue(typeDefinitionLink.cardinality().min());
                value.getFirstChild("properties")
                        .getFirstChild(typeDefinitionLink.id())
                        .getFirstChild("items")
                        .getFirstChild("maxItems")
                        .setValue(typeDefinitionLink.cardinality().max());
                value.getFirstChild("properties")
                        .getFirstChild(typeDefinitionLink.id())
                        .getFirstChild("items")
                        .getFirstChild("type")
                        .setValue("object");
                value.getFirstChild("properties")
                        .getFirstChild(typeDefinitionLink.id())
                        .getFirstChild("items")
                        .getFirstChild("$ref")
                        .setValue("#/components/schemas/" + (typeDefinitionLink.linkedTypeName()));
                subTypes.put(typeDefinitionLink.linkedTypeName(), responseType(typeDefinitionLink));
            }
            return value;
    }


    private Value inLineTypeDefinitionLinkParse (TypeInlineDefinition typeDefinitionLink){

        Value  value = Value.create();

        TypeInlineDefinition supportInLineTypeDefinition = null ;
        if (typeDefinitionLink.hasSubType("_")){
            if (typeDefinitionLink.getSubType("_") instanceof TypeInlineDefinition ){
                supportInLineTypeDefinition = (TypeInlineDefinition)typeDefinitionLink.getSubType("_");
            }
        }else{
            supportInLineTypeDefinition = typeDefinitionLink;
        }
        if ((supportInLineTypeDefinition.cardinality().min() == 1 ) & (supportInLineTypeDefinition.cardinality().max() == 1))
        {
            value.getFirstChild("type").setValue("object");
            value.getFirstChild("properties")
                    .getFirstChild(typeDefinitionLink.id()).getFirstChild("type").setValue("object");
            value.getFirstChild("properties")
                    .getFirstChild(typeDefinitionLink.id()).getFirstChild("type").setValue("#/components/schemas/linkedType" + (typeDefinitionLink.id()));
            subTypes.put( typeDefinitionLink.id(), responseType(supportInLineTypeDefinition));
        }else if ((supportInLineTypeDefinition.cardinality().min() == 0 ) & (supportInLineTypeDefinition.cardinality().max() >= 1)){
            value.getFirstChild("type").setValue("object");
            value.getFirstChild("properties")
                    .getFirstChild(typeDefinitionLink.id())
                    .getFirstChild("type").setValue("array");
            value.getFirstChild("properties")
                    .getFirstChild(typeDefinitionLink.id())
                    .getFirstChild("items")
                    .getFirstChild("minItems")
                    .setValue(supportInLineTypeDefinition.cardinality().min());
            value.getFirstChild("properties")
                    .getFirstChild(typeDefinitionLink.id())
                    .getFirstChild("items")
                    .getFirstChild("maxItems")
                    .setValue(supportInLineTypeDefinition.cardinality().max());
            value.getFirstChild("properties")
                    .getFirstChild(typeDefinitionLink.id())
                    .getFirstChild("items")
                    .getFirstChild("type")
                    .setValue("object");
            value.getFirstChild("properties")
                    .getFirstChild(typeDefinitionLink.id())
                    .getFirstChild("items")
                    .getFirstChild("$ref")
                    .setValue("#/components/schemas/" + ("linkedType"+ typeDefinitionLink.id()));
            subTypes.put("linkedType"+typeDefinitionLink.id(), responseType(supportInLineTypeDefinition));
        }
        return value;
    }

}
