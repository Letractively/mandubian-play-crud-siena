package controllers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import com.google.appengine.api.datastore.Blob;

import models.crudsiena.SienaSupport;

import play.Logger;
import play.Play;
import play.data.binding.BeanWrapper;
import play.data.validation.Required;
import play.data.validation.Password;
import play.data.validation.Validation;
import play.exceptions.TemplateNotFoundException;
import play.i18n.Messages;
import play.modules.crudsiena.SienaUtils;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.Router;
import siena.Column;
import siena.DateTime;
import siena.Filter;
import siena.Id;
import siena.Json;
import siena.Max;
import siena.Model;
import siena.Query;
import siena.embed.Embedded;
import siena.embed.EmbeddedList;

public class CRUD extends Controller {

    @Before
    static void addType() {
        ObjectType type = ObjectType.get(getControllerClass());
        renderArgs.put("type", type);
    }

    public static void index() {
        try {
            render();
        } catch (TemplateNotFoundException e) {
            render("CRUD/index.html");
        }
    }

    public static void list(int page, String filterField, String filterValue, String orderBy, String order) {
        ObjectType type = ObjectType.get(getControllerClass());
        notFoundIfNull(type);
        if (page < 1) {
            page = 1;
        }
        List<? extends SienaSupport> objects = type.findPage(page, filterField, filterValue, orderBy, order);
        Long count = type.count(filterField, filterValue);
        Long totalCount = type.count(null, null);
        try {
            render(type, objects, count, totalCount, page, orderBy, order);
        } catch (TemplateNotFoundException e) {
            render("CRUD/list.html", type, objects, count, totalCount, page, orderBy, order);
        }
    }

    public static void show(String id) {
        ObjectType type = ObjectType.get(getControllerClass());
        notFoundIfNull(type);
        SienaSupport object = type.findById(id);
        try {
            render(type, object);
        } catch (TemplateNotFoundException e) {
            render("CRUD/show.html", type, object);
        }
    }

    public static void addListElement(String id, String field) throws Exception {
        ObjectType type = ObjectType.get(getControllerClass());
        notFoundIfNull(type);
        SienaSupport object = type.findById(id);
                
        object.addListElement(field);
        
        validation.valid(object);
        if (validation.hasError(field)) {
            renderArgs.put("error", validation.error(field));
        }
        
        try {
            render(type, object, field);
        } catch (TemplateNotFoundException e) {
            render("CRUD/addListElement.html", id, type, object, field);
        }
    }

    public static void deleteListElement(String id, String field, Integer idx) throws Exception {
        ObjectType type = ObjectType.get(getControllerClass());
        notFoundIfNull(type);
        SienaSupport object = type.findById(id);
                
        object.deleteListElement(field, idx);

        validation.valid(object);
        if (validation.hasError(field)) {
            renderArgs.put("error", validation.error(field));
        }
        
        try {
            render(id, type, object, field);
        } catch (TemplateNotFoundException e) {
            render("CRUD/deleteListElement.html", id, type, object, field);
        }
    }
    
    public static void addMapElement(String id, String field, String mkey) throws Exception {
        ObjectType type = ObjectType.get(getControllerClass());
        notFoundIfNull(type);
        SienaSupport object = type.findById(id);
                
        object.addMapElement(field, mkey);

        validation.valid(object);
        if (validation.hasError(field)) {
            renderArgs.put("error", validation.error(field));
        }
        try {
            render(id, type, object, field);
        } catch (TemplateNotFoundException e) {
            render("CRUD/addMapElement.html", id, type, object, field);
        }
    }
    
    public static void deleteMapElement(String id, String field, String mkey) throws Exception {
        ObjectType type = ObjectType.get(getControllerClass());
        notFoundIfNull(type);
        SienaSupport object = type.findById(id);
                
        object.deleteMapElement(field, mkey);

        validation.valid(object);
        if (validation.hasError(field)) {
            renderArgs.put("error", validation.error(field));
        }
        try {
            render(id, type, object, field);
        } catch (TemplateNotFoundException e) {
            render("CRUD/deleteMapElement.html", id, type, object, field);
        }
    }
    
    public static void save(String id) throws Exception {
        ObjectType type = ObjectType.get(getControllerClass());
        notFoundIfNull(type);
        SienaSupport object = type.findById(id);
        
        object = object.edit("object", params);
        // Look if we need to deserialize
        for (ObjectField field : type.getFields()) {
            if (field.type.equals("serializedText") && params.get("object." + field.name) != null) {
                Field f = object.getClass().getDeclaredField(field.name);
                f.set(object, CRUD.collectionDeserializer(params.get("object." + field.name),(Class)((ParameterizedType) f.getGenericType()).getActualTypeArguments()[0]));
            }
        }

        validation.valid(object);
        if (validation.hasErrors()) {
            renderArgs.put("error", Messages.get("crud.hasErrors"));
            try {
                render(request.controller.replace(".", "/") + "/show.html", type, object);
            } catch (TemplateNotFoundException e) {
                render("CRUD/show.html", type, object);
            }
        }
        object.update();
        flash.success(Messages.get("crud.saved", type.modelName, object.getEntityId()));
        if (params.get("_save") != null) {
            redirect(request.controller + ".list");
        }
        redirect(request.controller + ".show", object.getEntityId());
    }

    public static void blank() {
        ObjectType type = ObjectType.get(getControllerClass());
        notFoundIfNull(type);
        try {
            render(type);
        } catch (TemplateNotFoundException e) {
            render("CRUD/blank.html", type);
        }
    }

    public static void create() throws Exception {
        ObjectType type = ObjectType.get(getControllerClass());
        notFoundIfNull(type);
        SienaSupport object = type.entityClass.newInstance();
        validation.valid(object.edit("object", params));
        if (validation.hasErrors()) {
            renderArgs.put("error", Messages.get("crud.hasErrors"));
            try {
                render(request.controller.replace(".", "/") + "/blank.html", type);
            } catch (TemplateNotFoundException e) {
                render("CRUD/blank.html", type);
            }
        }
        object.insert();
        flash.success(Messages.get("crud.created", type.modelName, object.getEntityId()));
        if (params.get("_save") != null) {
            redirect(request.controller + ".list");
        }
        if (params.get("_saveAndAddAnother") != null) {
            redirect(request.controller + ".blank");
        }
        redirect(request.controller + ".show", object.getEntityId());
    }

    public static void delete(String id) {
        ObjectType type = ObjectType.get(getControllerClass());
        notFoundIfNull(type);
        SienaSupport object = type.findById(id);
        try {
            object.delete();
        } catch (Exception e) {
            flash.error(Messages.get("crud.delete.error", type.modelName, object.getEntityId()));
            redirect(request.controller + ".show", object.getEntityId());
        }
        flash.success(Messages.get("crud.deleted", type.modelName, object.getEntityId()));
        redirect(request.controller + ".list");
    }

    // ~~~~~~~~~~~~~
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface For {

        Class value();
    }

    // ~~~~~~~~~~~~~
    static int getPageSize() {
        return Integer.parseInt(Play.configuration.getProperty("crud.pageSize", "30"));
    }

    public static class ObjectType implements Comparable<ObjectType> {

        public Class<? extends CRUD> controllerClass;
        public Class<? extends SienaSupport> entityClass;
        public String name;
        public String modelName;
        public String controllerName;

        public ObjectType(Class modelClass) {
            this.modelName = modelClass.getSimpleName();
            this.entityClass = modelClass;
        }

        public ObjectType(String modelClass) throws ClassNotFoundException {
            this(Play.classloader.loadClass(modelClass));
        }

        public static ObjectType forClass(String modelClass) throws ClassNotFoundException {
            return new ObjectType(modelClass);
        }

        public static ObjectType get(Class controllerClass) {
            Class entityClass = getEntityClassForController(controllerClass);
            if (entityClass == null || !SienaSupport.class.isAssignableFrom(entityClass)) {
                return null;
            }
            ObjectType type = new ObjectType(entityClass);
            type.name = controllerClass.getSimpleName();
            type.controllerName = controllerClass.getSimpleName().toLowerCase();
            type.controllerClass = controllerClass;
            return type;
        }

        public static Class getEntityClassForController(Class controllerClass) {
            if (controllerClass.isAnnotationPresent(For.class)) {
                return ((For) (controllerClass.getAnnotation(For.class))).value();
            }
            String name = controllerClass.getSimpleName();
            // Search a controller for the model class
            // first tries name pluralized "model" -> "models"
            String searchName = "models." + name.substring(0, name.length() - 1);
            try {
                return Play.classloader.loadClass(searchName);
            } catch (ClassNotFoundException e) {
            	// now tries "model" -> "model"
            	searchName = "models." + name;
            	try {
                    return Play.classloader.loadClass(searchName);
                } catch (ClassNotFoundException e2) {
                	return null;
                }
            }
        }

        public Object getListAction() {
            return Router.reverse(controllerClass.getName() + ".list");
        }

        public Object getBlankAction() {
            return Router.reverse(controllerClass.getName() + ".blank");
        }

        // FILTER = filterField>=filterValue
        public Long count(String filterField, String filterValue) {
        	Query<? extends SienaSupport> query = Model.all(entityClass);
        	filterQueryWithSearch(query, filterField, filterValue);
        	
        	return Long.valueOf(query.count());
        }

        public List<? extends SienaSupport> findPage(int page, String filterField, String filterValue, String orderBy, String order) {
            int pageLength = getPageSize();
            Query<? extends SienaSupport> query = Model.all(entityClass);
        	filterQueryWithSearch(query, filterField, filterValue);
        	
            if (orderBy == null) {
            	if(order == null || "ASC".equals(order.toUpperCase()))
            		orderBy = SienaUtils.findKeyName(entityClass);
            	else if("DESC".equals(order.toUpperCase()))
            		orderBy = "-" + SienaUtils.findKeyName(entityClass);
            }
            else if(order != null && "DESC".equals(order.toUpperCase())){
            		orderBy = "-"+orderBy;
            }
            
            return query.order(orderBy).fetch(pageLength, (page - 1) * pageLength);
        }
        
        // only searches on one field and with operator >=
        public Query<? extends SienaSupport> filterQueryWithSearch(
        		Query<? extends SienaSupport> query, String filterField, String filterValue) {            
            if(
            	filterValue!=null && !"".equals(filterValue)
            	&&
            	filterField!=null && !"".equals(filterField)
            ){
            	for (ObjectField field : getFields()) {
	                if (filterField.toLowerCase().equals(field.name.toLowerCase())) 
	                {
	                   query.filter(field.name+">=", filterValue);
	                }
	            }
            }
            return query;
        }

        public SienaSupport findById(Object id) {
        	Query<? extends SienaSupport> query = Model.all(entityClass);
        	try {
        		query.filter(SienaUtils.findKeyName(entityClass), play.data.binding.Binder.directBind(id + "", SienaUtils.findKeyType(entityClass)));
	        } catch (Exception e) {
	            throw new RuntimeException("Something bad with id type ?", e);
	        }
	        
	        return query.get();
        }

        public List<ObjectField> getFields() {
            List fields = new ArrayList();
            for (Field f : entityClass.getDeclaredFields()) {
                if (Modifier.isTransient(f.getModifiers()) || Modifier.isFinal(f.getModifiers())) {
                    continue;
                }
                ObjectField of = new ObjectField(f);
                if (of.type != null) {
                    fields.add(of);
                }
            }
            return fields;
        }

        public ObjectField getField(String name) {
            for (ObjectField field : getFields()) {
                if (field.name.equals(name)) {
                    return field;
                }
            }
            return null;
        }

        public int compareTo(ObjectType other) {
            return modelName.compareTo(other.modelName);
        }
    }

    public static class ObjectField {
	
        public String type = "unknown";
        public String name;
        public String relation;
        public String owner;
        public boolean multiple;
        public String multipleType;
        public boolean searchable;
        public Object[] choices;
        public boolean required;
        public String serializedValue;

        public ObjectField(Field field) {
            if (CharSequence.class.isAssignableFrom(field.getType())) {
                type = "text";
                searchable = true;
                if (field.isAnnotationPresent(Max.class)) {
                    int maxSize = field.getAnnotation(Max.class).value();
                    if (maxSize > 100) {
                        type = "longtext";
                    }
                }
            }
            else if (Number.class.isAssignableFrom(field.getType()) 
            		|| field.getType().equals(double.class) 
            		|| field.getType().equals(int.class) 
            		|| field.getType().equals(long.class)) {
                type = "number";
            }
            else if (Boolean.class.isAssignableFrom(field.getType()) 
            		|| field.getType().equals(boolean.class)) {
                type = "boolean";
            }
            else if (Date.class.isAssignableFrom(field.getType())) {
                type = "date";
                if (field.isAnnotationPresent(DateTime.class)) {
                	type = "datetime";
                }
            }          
            // type SienaSupport 
            else if (SienaSupport.class.isAssignableFrom(field.getType())) {
                if (field.isAnnotationPresent(Column.class)) {
                	type = "relation";
                	relation = field.getType().getName();
                }
            }  
            // type QUERY<T> + annotation @Filter 
            else if(Query.class.isAssignableFrom(field.getType())){
            	Class fieldType = (Class) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
            	if (field.isAnnotationPresent(Filter.class)) {
            		type = "relation";
            		relation = fieldType.getName();
            		multiple = true;
            		// this is the owner of the ManyToOne relation 
            		owner = field.getAnnotation(Filter.class).value();
            	}
            }
            else if (field.getType().isEnum()) {
                type = "enum";
                relation = field.getType().getSimpleName();
                choices = field.getType().getEnumConstants();
            }
            // Json field
            else if (Json.class.isAssignableFrom(field.getType())) {
            	type = "longtext";
            }
            // @Embedded field
            else if (field.isAnnotationPresent(Embedded.class)) {
            	type = "embedded";
            	if(List.class.isAssignableFrom(field.getType())){
            		multiple = true;
            		multipleType = "list";
            		Class fieldType = (Class) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
            		relation = fieldType.getName();
            	}
            	else if(Map.class.isAssignableFrom(field.getType())){
            		multiple = true;
            		multipleType = "map";
            		// gets T2 for map<T1,T2>
            		Class fieldType = (Class) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[1];
            		relation = fieldType.getName();
            	}
            	else {
            		multiple = false;
            		relation = field.getType().getName();
            	}
            }
            else if (byte[].class.isAssignableFrom(field.getType())) {
            	type = "binary";
            }
            else if (Blob.class.isAssignableFrom(field.getType())) {
            	type = "blob";
            }
            
            if (field.isAnnotationPresent(Id.class)) {
                type = null;
            }
            if (field.isAnnotationPresent(Required.class)) {
                required = true;
            }
            if (field.isAnnotationPresent(Password.class)) {
            	 type = "password";
            }
            
            name = field.getName();
        }

        public Object[] getChoices() {
            return choices;
        }
        
        public static List<ObjectField> getFields(Class clazz) {
            List fields = new ArrayList();
            for (Field f : clazz.getFields()) {
                if (Modifier.isTransient(f.getModifiers()) || Modifier.isFinal(f.getModifiers())) {
                    continue;
                }
                ObjectField of = new ObjectField(f);
                if (of.type != null) {
                    fields.add(of);
                }
            }
            return fields;
        }

        public static ObjectField getField(List<ObjectField> fields, String name) {
            for (ObjectField field : fields) {
                if (field.name.equals(name)) {
                    return field;
                }
            }
            return null;
        }
    }

	public static String collectionSerializer(Collection<?> coll) {
        StringBuffer sb = new StringBuffer();
        for (Object obj : coll) {
            sb.append("\"" + obj.toString() + "\",");
        }
        if (sb.length() > 2) {
            return sb.substring(0, sb.length() - 1);
        }
        return null;

    }

    public static String arraySerializer(Object[] coll) {
       return collectionSerializer(Arrays.asList(coll));
    }

    public static Collection<?> collectionDeserializer(String target, Class<?> type) {
        String[] targets = target.trim().split(",");
        Collection results;
        Logger.info("type [" + type + "]");
        if (List.class.isAssignableFrom(type)) {
            results = new ArrayList();
        } else {
            results = new TreeSet();
        }
        for (String targ : targets) {
            if (targ.length() > 1) {
                targ = targ.substring(1, targ.length() - 1);
            }
            if (type.isEnum()) {
                Object[] constants = type.getEnumConstants();
                for (Object c : constants) {
                    if  (c.toString().equals(targ)) {
                        results.add(c);
                    }
                }
            } else if (CharSequence.class.isAssignableFrom(type)) {
                results.add(targ);
            } else if (Integer.class.isAssignableFrom(type)) {
                results.add(Integer.valueOf(targ));
            } else if (Float.class.isAssignableFrom(type)) {
                results.add(Float.valueOf(targ));
            } else if (Boolean.class.isAssignableFrom(type)) {
                 results.add(Boolean.valueOf(targ));
            } else if (Double.class.isAssignableFrom(type)) {
                 results.add(Double.valueOf(targ));
            } else if (Long.class.isAssignableFrom(type)) {
                results.add(Long.valueOf(targ));
            }  else if (Byte.class.isAssignableFrom(type)) {
                 results.add(Byte.valueOf(targ));
            }
        }

        return results;

    }
}

