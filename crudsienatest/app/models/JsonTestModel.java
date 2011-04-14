package models;

import net.sf.oval.guard.Post;
import siena.Generator;
import siena.Id;
import siena.Model;
import siena.NotNull;
import siena.Query;
import siena.Table;

@Table("jsontest")
public class JsonTestModel extends Model {
	//id automatically generated
	@Id(Generator.AUTO_INCREMENT)
    public Long id;
	
	//The Category title
	@NotNull
	public String title;

    public static Query<JsonTestModel> all() {
    	return Model.all(JsonTestModel.class);
    }
    
    public static JsonTestModel findById(Long id) {
    	return JsonTestModel.all().filter("id", id).get();
    }
	
	public JsonTestModel(String title) {
		this.title = title;
	}


}
