package models.crudsiena;

import play.data.binding.TypeBinder;
import java.lang.annotation.Annotation;
import java.util.List;

import com.google.appengine.api.datastore.Blob;

import play.data.Upload;
import play.data.binding.Binder;
import play.db.Model;
import play.exceptions.UnexpectedException;
import play.mvc.Http.Request;
import play.mvc.Scope.Params;

public class GaeByteArrayBinder implements TypeBinder<byte[]> {

	@SuppressWarnings("unchecked")
    public Object bind(String name, Annotation[] annotations, String value, Class actualClass) {
        List<Upload> uploads = (List<Upload>) Request.current().args.get("__UPLOADS");
        for(Upload upload : uploads) {
            if(upload.getFieldName().equals(value)) {
            	byte[] data = upload.asBytes();
            	
            	if(data.length > 0)
            		return data;
            }
        }
        return Binder.MISSING;
    }
}
