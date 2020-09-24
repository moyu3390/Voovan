package org.voovan.http.server.module.annontationRouter.swagger;

import org.voovan.http.message.HttpStatic;
import org.voovan.http.server.HttpRequest;
import org.voovan.http.server.HttpResponse;
import org.voovan.http.server.HttpSession;
import org.voovan.http.server.module.annontationRouter.annotation.*;
import org.voovan.http.server.module.annontationRouter.swagger.entity.*;
import org.voovan.http.server.module.annontationRouter.swagger.entity.Property;
import org.voovan.tools.TFile;
import org.voovan.tools.TObject;
import org.voovan.tools.TString;
import org.voovan.tools.json.JSON;
import org.voovan.tools.reflect.TReflect;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Class name
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class SwaggerApi {
    public static final List<SwaggerRouter> SWAGGER_ROUTERS = new ArrayList<SwaggerRouter>();

    public static Swagger buildSwagger() {
        Swagger swagger = new Swagger();

        swagger.getTags().addAll(parseAllTags());

        Map<String, Tag> tagsMap = new HashMap<String, Tag>();
        for(SwaggerRouter swaggerRouter : SWAGGER_ROUTERS) {
            String url = swaggerRouter.getPath();
            while(url.indexOf("/:") >0 ) {
                int startIndex = url.indexOf(":");
                url = url.substring(0, startIndex) + "{" + url.substring(startIndex + 1);
                int endIndex = url.indexOf("/", startIndex + 1);
                if(endIndex > 0) {
                    url = TString.insert(url, endIndex, "}");
                } else {
                    url = url + "}";
                }
            }
            String routeMethod = swaggerRouter.getRouteMethod();
            Router classAnnotation = swaggerRouter.getClassAnnotation();
            Class clazz = swaggerRouter.getClass();
            Router methodAnnotation = swaggerRouter.getMethodAnnotation();
            Method method = swaggerRouter.getMethod();

            if(methodAnnotation.hide()) {
                continue;
            }

            Path path = new Path(swaggerRouter.getMethod().getName(), methodAnnotation.summary(), methodAnnotation.description(),
                    new String[]{HttpStatic.APPLICATION_JSON_STRING}, new String[]{HttpStatic.APPLICATION_JSON_STRING},
                    methodAnnotation.deprecated());

            path.getTags().addAll(TObject.asList(methodAnnotation.tags()));
            for(Tag tag : parseTags(classAnnotation).values()) {
                path.getTags().add(tag.getName());
            }

            Annotation[][] paramAnnotationsArrary = method.getParameterAnnotations();
            Class[] paramTypes = method.getParameterTypes();
            int unNamedParamCount = 1;
            for (int i = 0; i < paramAnnotationsArrary.length; i++) {
                Annotation[] paramAnnotations = paramAnnotationsArrary[i];

                if (paramTypes[i] == HttpRequest.class ||
                        paramTypes[i] == HttpResponse.class ||
                        paramTypes[i] == HttpSession.class) {

                    continue;
                }

                if (paramAnnotations.length > 0) {
                    for (Annotation paramAnnotation : paramAnnotations) {
                        if (paramAnnotation instanceof Param) {
                            Parameter parameter = new Parameter();
                            parameter.setIn("path");
                            String[] types = getParamType(paramTypes[i]);
                            parameter.setType(types[0]);
                            parameter.setFormat(types[1]);
                            parameter.setName(((Param) paramAnnotation).value());
                            parameter.setDescription(((Param) paramAnnotation).description());
                            parameter.setRequired(((Param) paramAnnotation).isRequire());
                            parameter.setDefaultVal(((Param) paramAnnotation).defaultVal());
                            path.getParameters().add(parameter);
                        } else if (paramAnnotation instanceof Header) {
                            Parameter parameter = new Parameter();
                            parameter.setIn("header");
                            String[] types = getParamType(paramTypes[i]);
                            parameter.setType(types[0]);
                            parameter.setFormat(types[1]);
                            parameter.setName(((Header) paramAnnotation).value());
                            parameter.setDescription(((Header) paramAnnotation).description());
                            parameter.setRequired(((Header) paramAnnotation).isRequire());
                            parameter.setDefaultVal(((Header) paramAnnotation).defaultVal());
                            path.getParameters().add(parameter);
                        }  else if (paramAnnotation instanceof Cookie) {
                            Parameter parameter = new Parameter();
                            parameter.setIn("cookie");
                            String[] types = getParamType(paramTypes[i]);
                            parameter.setType(types[0]);
                            parameter.setFormat(types[1]);
                            parameter.setName(((Cookie) paramAnnotation).value());
                            parameter.setDescription(((Cookie) paramAnnotation).description());
                            parameter.setRequired(((Cookie) paramAnnotation).isRequire());
                            parameter.setDefaultVal(((Cookie) paramAnnotation).defaultVal());
                            path.getParameters().add(parameter);
                        }else if (paramAnnotation instanceof BodyParam) {
                            if(path.getParameters().size() == 0) {
                                Parameter parameter = new Parameter();
                                parameter.setIn("body");
                                parameter.setName("body");
                                path.getParameters().add(parameter);
                            }

                            Parameter parameter = path.getParameters().get(0);

                            Schema schema = parameter.getSchema();
                            String name = ((BodyParam) paramAnnotation).value();
                            String description = ((BodyParam) paramAnnotation).description();
                            String defaultVal = ((BodyParam) paramAnnotation).defaultVal();
                            boolean isRequire = ((BodyParam) paramAnnotation).isRequire();
                            createSchema(parameter.getSchema(), paramTypes[i], name, description, defaultVal, isRequire);
                        } else if(paramAnnotation instanceof Body) {
                            Parameter parameter = new Parameter();
                            parameter.setIn("body");
                            parameter.setName("body");
                            Schema schema = parameter.getSchema();
                            createSchema(parameter.getSchema(), paramTypes[i], null, null, null, true);
                            path.getParameters().add(parameter);
                        }
                    }
                } else {
                    Parameter parameter = new Parameter();
                    parameter.setIn("path");
                    String[] types = getParamType(paramTypes[i]);
                    parameter.setType(types[0]);
                    parameter.setFormat(types[1]);
                    parameter.setName("param" + unNamedParamCount);
                    parameter.setRequired(true);
                    path.getParameters().add(parameter);
                    unNamedParamCount++;
                }
            }


            swagger.getPaths().put(url, TObject.asMap(routeMethod.toLowerCase(), path));
        }

        try {
            TFile.writeFile(new File("/Users/helyho/swagger.json"), false, JSON.formatJson(JSON.removeNullNode(JSON.toJSON(swagger))).getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return swagger;
    }

    public static Collection<Tag> parseAllTags() {

        Map<String, Tag> tagsMap = new HashMap<String, Tag>();
        for(SwaggerRouter swaggerRouter : SWAGGER_ROUTERS) {
            tagsMap.putAll(parseTags(swaggerRouter.getClassAnnotation()));

        }
        return tagsMap.values();
    }

    public static Map<String, Tag> parseTags(Router router) {
        Map<String, Tag> tagsMap = new HashMap<String, Tag>();
        for(String tag : router.tags()) {
            tag = tag.trim();

            if (tag.length() > 0) {
                int descStart = tag.indexOf("[");
                int descEnd = tag.lastIndexOf("]");
                String name = descStart > 0 ? tag.substring(0, descStart) : tag;
                String description = descStart > 0 && descEnd > 0 && descEnd > descStart ? tag.substring(descStart + 1, descEnd) : "";
                tagsMap.put(name, new Tag(name, description));
            }
        }

        return tagsMap;
    }

    public static Schema createSchema(Schema schema, Class clazz, String name, String description, String defaultVal, Boolean required){
        if(schema == null) {
            schema = new Schema();
        }

        if(TReflect.isSystemType(clazz)) {
            if(name == null) {
                String[] types = getParamType(clazz);
                schema.setType(types[0]);
                schema.setFormat(types[1]);

            } else {
                String[] types = getParamType(clazz);
                schema.setType("object");
                Property property = new Property(types[0], types[1]);
                property.setDefaultVal(TString.isNullOrEmpty(defaultVal) ? null : defaultVal);
                property.setDescription(TString.isNullOrEmpty(description) ? null : description);
                schema.getProperties().put(name, property);
                if(!required) {
                    schema.getRequired().add(name);
                }
            }
        } else {
            for (Field field : TReflect.getFields(clazz)) {
                String[] types = getParamType(field.getType());
                Property property = new Property(types[0], types[1]);
                schema.getProperties().put(field.getName(), property);
            }
        }

        return schema;
    }

    public static String[] getParamType(Class clazz) {
        if(TReflect.getUnPackageType(clazz.getName()).equals("int")) {
            return new String[]{"integer", "int32"};
        } else if(TReflect.getUnPackageType(clazz.getName()).equals("long")) {
            return new String[]{"integer", "int64"};
        } else if(TReflect.getUnPackageType(clazz.getName()).equals("float")) {
            return new String[]{"number", "float"};
        } else if(TReflect.getUnPackageType(clazz.getName()).equals("String")) {
            return new String[]{"string", null};
        } else if(TReflect.getUnPackageType(clazz.getName()).equals("byte")) {
            return new String[]{"string", "byte"};
        } else if(TReflect.getUnPackageType(clazz.getName()).equals("boolean")) {
            return new String[]{"boolean", null};
        } else if(TReflect.getUnPackageType(clazz.getSimpleName()).equals("Date")) {
            return new String[]{"string", "date"};
        } else if(TReflect.getUnPackageType(clazz.getSimpleName()).equals("String")) {
            return new String[]{"string", null};
        } else if(TReflect.getUnPackageType(clazz.getSimpleName()).equals("BigDecimal")) {
            return new String[]{"number", null};
        } else if(clazz.isArray()) {
            return new String[]{"array", clazz.getComponentType().getName().toLowerCase()};
        } else if(clazz == List.class) {
            Class[] classes = TReflect.getGenericClass(clazz);
            return new String[]{"array", classes!=null ? classes[0].getName().toLowerCase() : null};
        } else if(clazz == Map.class) {
            return new String[]{"object", null};
        } else {
            return new String[]{null, null};
        }
    }
}