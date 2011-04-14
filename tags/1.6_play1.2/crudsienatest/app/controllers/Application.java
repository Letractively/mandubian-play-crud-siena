package controllers;

import java.lang.reflect.Method;
import java.util.List;

import models.Employee;
import models.JsonTestModel;
import models.Question;
import models.ServiceAccount;
import models.ServiceAccount.GeolocationService;
import play.Logger;
import play.exceptions.UnexpectedException;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.results.Result;
import siena.PersistenceManager;

import com.google.appengine.api.datastore.Blob;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Application extends Controller {

    public static void index() {
    	Employee emp = new Employee();
    	emp.firstName = "alpha";
    	emp.lastName = "beta";
    	emp.blob = new Blob(new byte[] { 0x00, 0x01 });
    	
    	emp.insert();
    	try {
    		Logger.info("blob:%d %d", emp.blob.getBytes()[0], emp.blob.getBytes()[1]);
    	}catch(Exception ex){}
        render();
    }

    
    public static void provision() {
    	for(int i=0; i<100; i++){
    		Question q = new Question(
    				"question", 
    				"answer", 
    				"slug", 
    				100L, 
    				30L, 
    				1L);
    		
    		q.insert();
    	}
    	render();
    	
    }

    public static void getOrder(Long containerId) {
    	List<Question> questions = models.Question.all().order("-qID").fetch();
    	render(questions);
    	
    }

    public static void getServices(Long id, GeolocationService type) {
    	Employee emp = Employee.all().filter("id", id).get();
    	
        List<ServiceAccount> services = emp.services.filter("geoloc", type).fetch();
        render(services);
    }
    
	public static void createJsonTest(String title) {
		validation.required(title);
		if(validation.hasErrors()){
			badRequest(); //"Not all required fields were present."
    	}
		
		JsonTestModel p = new JsonTestModel(title);
		p.insert();
		throw new MyRenderJson(p, PersistenceManager.class);
	}

	
	public static class MyRenderJson extends Result {

	    String json;

	    public MyRenderJson(Object o, Class<?>... classesToIgnore) {
	        
	        MyExclusionStrategy[] strats = new MyExclusionStrategy[classesToIgnore.length];
	        for(int i=0; i<classesToIgnore.length;i++) {
	        	strats[i] = new MyExclusionStrategy(classesToIgnore[i]);
	        }
	        Gson gson = new GsonBuilder()
	        	.setExclusionStrategies(strats)
	        	.create();
	        json = gson.toJson(o);
	    }
	   
	  
	    public MyRenderJson(String jsonString) {
	        json = jsonString;
	    }

	    public void apply(Request request, Response response) {
	        try {
	            setContentTypeIfNotSet(response, "application/json; charset=utf-8");
	            response.out.write(json.getBytes("utf-8"));
	        } catch (Exception e) {
	            throw new UnexpectedException(e);
	        }
	    }
	   
	    //
	   
	    static Method getMethod(Class<?> clazz, String name) {
	        for(Method m : clazz.getDeclaredMethods()) {
	            if(m.getName().equals(name)) {
	                return m;
	            }
	        }
	        return null;
	    }

		public class MyExclusionStrategy implements ExclusionStrategy {
		    private final Class<?> typeToSkip;

		    private MyExclusionStrategy(Class<?> typeToSkip) {
		      this.typeToSkip = typeToSkip;
		    }

		    public boolean shouldSkipClass(Class<?> clazz) {
		      return (clazz == typeToSkip);
		    }

		    public boolean shouldSkipField(FieldAttributes f) {
		      return false;
		    }
		  }

	} 
}