package play.modules.crudsiena;

import java.lang.reflect.Field;

import play.exceptions.UnexpectedException;
import siena.Id;

public class SienaUtils {
    // More utils
    public static Object findKey(Object entity) {
        try {
            Class<?> c = entity.getClass();
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
    
 	public static Class<?> findKeyType(Class<?> c) {
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
 	
 	public static String findKeyName(Class<?> c) {
        try {
            while (!c.equals(Object.class)) {
                for (Field field : c.getDeclaredFields()) {
                    if (field.isAnnotationPresent(Id.class)) {
                        field.setAccessible(true);
                        return field.getName();
                    }
                }
                c = c.getSuperclass();
            }
        } catch (Exception e) {
            throw new UnexpectedException("Error while determining the object @Id for an object of type " + c);
        }
        return null;
    }

}