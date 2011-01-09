package controllers;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;

import com.google.appengine.api.datastore.Blob;

import models.Employee;
import play.Logger;
import play.mvc.*;

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

}