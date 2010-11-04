package models.crudsiena;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import play.Logger;
import play.Play;
import play.data.binding.BeanWrapper;
import play.data.binding.Binder;
import play.data.validation.Validation;
import play.exceptions.UnexpectedException;
import play.mvc.Scope.Params;
import siena.DateTime;
import siena.Filter;
import siena.Id;
import siena.Json;
import siena.Model;
import siena.Query;
import siena.embed.Embedded;

import com.google.gson.JsonParseException;

/**
 * All entity classes requirement CRUD Siena support should 
 * inherit from SienaSupport Class
 * 
 * @author mandubian
 *
 * 
 */
public abstract class SienaSupport 
	extends Model implements Serializable {
	private static final long serialVersionUID = 7939730889902489690L;
	
	public static <T extends SienaSupport> T create(Class type, String name, Map<String, String[]> params) {
        try {
            Object model = type.newInstance();
            return (T) edit(model, name, params);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
	
	public <T extends SienaSupport> T edit(String name, Params params) {
	    return (T) edit(this, name, params.all());
	}
	
	public static <T extends SienaSupport> T edit(Object o, String name, Map<String, String[]> params) {
		try {
			BeanWrapper bw = new BeanWrapper(o.getClass());
			// Start with relations
			Set<Field> fields = new HashSet<Field>();
			Class clazz = o.getClass();
			while (!clazz.equals(SienaSupport.class)) {
				Collections.addAll(fields, clazz.getDeclaredFields());
				clazz = clazz.getSuperclass();
			}
			for (Field field : fields) {
				boolean isEntity = false;
				boolean isJson = false;
				boolean isEmbedded = false;
				String relation = null;
				boolean multiple = false;
				String owner = null;
				String embedType = null;
				Class embedClass = null;

				// ONE TO MANY association
				// entity = type inherits SienaSupport
				if(SienaSupport.class.isAssignableFrom(field.getType())) {
					isEntity = true;
					relation = field.getType().getName();
				}

				// MANY TO ONE association
				// type QUERY<T> + annotation @Filter 
				else if(Query.class.isAssignableFrom(field.getType())){
					isEntity = true;
					multiple = true;
					Class fieldType = 
						(Class) ((ParameterizedType) 
								field.getGenericType()).getActualTypeArguments()[0];
					relation = fieldType.getName();
					owner = field.getAnnotation(Filter.class).value();
					// by default, takes the type of the parent entity in lower case
					if(owner == null || "".equals(owner)){
						owner = o.getClass().getName().toLowerCase();
					}
				}
				else if(Json.class.isAssignableFrom(field.getType())){
					isJson = true;
				}
				else if(field.isAnnotationPresent(Embedded.class)){
					isEmbedded = true;
					
					if(List.class.isAssignableFrom(field.getType())){
						multiple = true;
	            	}
					else if(Map.class.isAssignableFrom(field.getType())){
						multiple = true;
	            	}
	            	else {
	            		multiple = false;
	            	}
				}

				if (isEntity) {
					// builds entity list for many to one
					if (multiple) {
						//Collection l = new ArrayList();

						String[] ids = params.get(name + "." + field.getName() + "@id");
						if(ids == null) {
							ids = params.get(name + "." + field.getName() + ".id");
						}

						if (ids != null) {
							params.remove(name + "." + field.getName() + ".id");
							params.remove(name + "." + field.getName() + "@id");
							for (String _id : ids) {
								if (_id.equals("")) {
									continue;
								}
								Class relClass = Play.classloader.loadClass(relation);
								Object res = 
									Model.all(relClass)
										.filter("id", Binder.directBind(_id, findKeyType(relClass)))
										.get();
								if(res!=null){
									// sets the object to the owner field into the relation entity
									relClass.getField(owner).set(res, o);
								}
									
								else Validation.addError(name+"."+field.getName(), "validation.notFound", _id);
							}
							// can't set arraylist to Query<T>
							// bw.set(field.getName(), o, l);
						}
					}
					// builds simple entity for simple association
					else {
						String[] ids = params.get(name + "." + field.getName() + "@id");
						if(ids == null) {
							ids = params.get(name + "." + field.getName() + ".id");
						}
						if (ids != null && ids.length > 0 && !ids[0].equals("")) {
							params.remove(name + "." + field.getName() + ".id");
							params.remove(name + "." + field.getName() + "@id");

							Class relClass = Play.classloader.loadClass(relation);
							Object res = 
								Model.all(relClass)
									.filter("id", Binder.directBind(ids[0], findKeyType(relClass)))
									.get();
							if(res!=null)
								bw.set(field.getName(), o, res);
							else Validation.addError(name+"."+field.getName(), "validation.notFound", ids[0]);

						} else if(ids != null && ids.length > 0 && ids[0].equals("")) {
							bw.set(field.getName(), o , null);
							params.remove(name + "." + field.getName() + ".id");
							params.remove(name + "." + field.getName() + "@id");
						}
					}	                	
				}
				else if(isJson){
					String[] jsonStr = params.get(name + "." + field.getName());
					if (jsonStr != null && jsonStr.length > 0 && !jsonStr[0].equals("")) {
						try {
							com.google.gson.JsonParser parser = new com.google.gson.JsonParser();
							parser.parse(jsonStr[0]);
							
							params.remove(name + "." + field.getName());
							Json json = Json.loads(jsonStr[0]);
							if(json!=null)
								bw.set(field.getName(), o, json);
							else Validation.addError(name+"."+field.getName(), "validation.notParsable");
						}catch(JsonParseException ex){
							ex.printStackTrace();
							Logger.error("json parser exception:%s", 
									ex.getCause()!=null?ex.getCause().getMessage(): ex.getMessage());
							Validation.addError(
									name+"."+field.getName(), 
									"validation.notParsable", 
									ex.getCause()!=null?ex.getCause().getMessage(): ex.getMessage());
						}
						catch(IllegalArgumentException ex){
							ex.printStackTrace();
							Logger.error("json parser exception:%s", 
									ex.getCause()!=null?ex.getCause().getMessage(): ex.getMessage());
							Validation.addError(
									name+"."+field.getName(), 
									"validation.notParsable", 
									ex.getCause()!=null?ex.getCause().getMessage(): ex.getMessage());
						}
					}
				}	
			}
			// Then bind
			// all composites objects (simple entity, list and maps) are managed
			// by this function
			// v1.0.x code
			// bw.bind(name, o.getClass(), params, "", o);

			// v1.1 compliant
			bw.bind(name, (Type)o.getClass(), params, "", o, o.getClass().getAnnotations());
			
			return (T) o;
		} catch (Exception e) {
			throw new UnexpectedException(e);
		}
	}

	// validates and inserts the entity
    public boolean validateAndSave() {
        if(Validation.current().valid(this).ok) {
            this.insert();
            return true;
        }
        return false;
    } 
    
    // More utils
    public static Object findKey(Object entity) {
        try {
            Class c = entity.getClass();
            while (!c.equals(Object.class)) {
                for (Field field : c.getDeclaredFields()) {
                    if (field.isAnnotationPresent(Id.class)) {
                        field.setAccessible(true);
                        return field.get(entity);
                    }
                }
                c = c.getSuperclass();
            }
        } catch (Exception e) {
            throw new UnexpectedException("Error while determining the object @Id for an object of type " + entity.getClass());
        }
        return null;
    }    
    
 	public static Class findKeyType(Class c) {
        try {
            while (!c.equals(Object.class)) {
                for (Field field : c.getDeclaredFields()) {
                    if (field.isAnnotationPresent(Id.class)) {
                        field.setAccessible(true);
                        return field.getType();
                    }
                }
                c = c.getSuperclass();
            }
        } catch (Exception e) {
            throw new UnexpectedException("Error while determining the object @Id for an object of type " + c);
        }
        return null;
    }
 	
    private transient Object key;

    public Object getEntityId() {
        if (key == null) {
            key = findKey(this);
        }
        return key;
    }
    
    public <T extends SienaSupport> T addListElement(String name) {
	    return (T) addListElement(this, name);
	}
    
    public static <T extends SienaSupport> T addListElement(Object o, String fieldName) {
    	try {
    		Class clazz = o.getClass();
			BeanWrapper bw = new BeanWrapper(o.getClass());
			Field field = clazz.getField(fieldName);
			
			if(List.class.isAssignableFrom(field.getType())){
				List l = (List)field.get(o);
				if(l == null)
					l = new ArrayList();
				
				Class embedClass = 
					(Class) ((ParameterizedType) 
							field.getGenericType()).getActualTypeArguments()[0];
				BeanWrapper embedbw = new BeanWrapper(embedClass);
				Object embedObj = embedClass.newInstance();
				
				l.add(embedObj);
				
				Logger.debug(embedObj.toString());
				
				bw.set(field.getName(), o, l);			
			}
			else Validation.addError(
					clazz.getName() + "."+field.getName(), 
					"validation.fieldList.badType", fieldName);			
			
			((SienaSupport)o).update();
			return (T) o;
    	} catch (Exception e) {
			throw new UnexpectedException(e);
		}
	}
    
    public <T extends SienaSupport> T deleteListElement(String name, int idx) {
	    return (T) deleteListElement(this, name, idx);
	}
    
    public static <T extends SienaSupport> T deleteListElement(Object o, String fieldName, int idx) {
    	try {
    		Class clazz = o.getClass();
			BeanWrapper bw = new BeanWrapper(o.getClass());
			Field field = clazz.getField(fieldName);
			
			if(List.class.isAssignableFrom(field.getType())){
				List l = (List)field.get(o);
				if(l == null)
					Validation.addError(
							clazz.getName() + "."+field.getName(), 
							"validation.fieldList.empty", fieldName);
				else {
					if(idx < 0 || idx > l.size()-1)
						Validation.addError(
							clazz.getName() + "."+field.getName(), 
							"validation.fieldList.indexOutOfBound", fieldName);
					else l.remove(idx);
				}
			}
			else Validation.addError(
					clazz.getName() + "."+field.getName(), 
					"validation.fieldList.badType", fieldName);

			((SienaSupport)o).update();
			
    		return (T) o;
    	} catch (Exception e) {
			throw new UnexpectedException(e);
		}
    }
    
    public <T extends SienaSupport> T addMapElement(String fieldName, String key) {
	    return (T) addMapElement(this, fieldName, key);
	}
    
    public static <T extends SienaSupport> T addMapElement(
    		Object o, String fieldName, String key) 
    {
    	try {
    		Class clazz = o.getClass();
			BeanWrapper bw = new BeanWrapper(o.getClass());
			Field field = clazz.getField(fieldName);
			
			if(Map.class.isAssignableFrom(field.getType())){
				Map l = (Map)field.get(o);
								
				Class embedKeyClass = 
					(Class) ((ParameterizedType) 
							field.getGenericType()).getActualTypeArguments()[0];
				
				Class embedClass = 
					(Class) ((ParameterizedType) 
						field.getGenericType()).getActualTypeArguments()[1];
					
				if(l == null){
					l = new HashMap();
				}

				Object embedObj = embedClass.newInstance();
				Object embedKey = Binder.directBind(key, embedKeyClass);
				
				if(l.get(embedKey) != null){
					Logger.debug("element with key %s already existing", embedKey);
					Validation.addError(
							fieldName, 
							"validation.fieldMap.alreadyExists", embedKey.toString());	
				}
				else {
					l.put(embedKey, embedObj);
					Logger.debug("map added {%s:%s}", embedKey, embedObj);
				}		
				
				bw.set(field.getName(), o, l);			
			}
			else Validation.addError(
					clazz.getName() + "."+field.getName(), 
					"validation.fieldMap.badType", fieldName);			
			
			((SienaSupport)o).update();
			return (T) o;
    	} catch (Exception e) {
			throw new UnexpectedException(e);
		}
	}
    
    public <T extends SienaSupport> T deleteMapElement(String fieldName, String key) {
	    return (T) deleteMapElement(this, fieldName, key);
	}
    
    public static <T extends SienaSupport> T deleteMapElement(
    		Object o, String fieldName, String key) 
    {
    	try {
    		Class clazz = o.getClass();
			BeanWrapper bw = new BeanWrapper(o.getClass());
			Field field = clazz.getField(fieldName);
			
			if(Map.class.isAssignableFrom(field.getType())){
				Map l = (Map)field.get(o);
				if(l == null)
					Validation.addError(
							clazz.getName() + "."+field.getName(), 
							"validation.fieldMap.empty", fieldName);
				else {
					Class embedKeyClass = 
						(Class) ((ParameterizedType) 
								field.getGenericType()).getActualTypeArguments()[0];
					BeanWrapper keybw = new BeanWrapper(embedKeyClass);
					try {
						Object embedKey = Binder.directBind(key, embedKeyClass);
						l.remove(embedKey);
					}catch(Exception ex){
						Validation.addError(
							clazz.getName() + "."+field.getName() + "." + key, 
							"validation.fieldMap.keyBadFormat", fieldName, key);
					}					
				}
			}
			else Validation.addError(
					clazz.getName() + "."+field.getName(), 
					"validation.fieldMap.badType", fieldName);

			((SienaSupport)o).update();
			
    		return (T) o;
    	} catch (Exception e) {
			throw new UnexpectedException(e);
		}
    }
}
