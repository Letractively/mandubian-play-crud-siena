package models.crudsiena;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;

import play.data.Upload;
import play.data.binding.Binder;
import play.data.binding.Global;
import play.data.binding.TypeBinder;
import play.mvc.Http.Request;

import com.google.appengine.api.datastore.Blob;

@Global
public class GaeBlobBinder implements TypeBinder<Blob> {

    public Object bind(String name, Annotation[] annotations, String value, Class actualClass, Type genericType) throws Exception {
    	List<Upload> uploads = (List<Upload>)Request.current().args.get("__UPLOADS");
        for(Upload upload : uploads) {
            if(upload.getFieldName().equals(value)) {
            	byte[] data = upload.asBytes();
            	
            	if(data.length > 0)
            		return new Blob(data);
            }
        }
        return Binder.MISSING;
    }

}
