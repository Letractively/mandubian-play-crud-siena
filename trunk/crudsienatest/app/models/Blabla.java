package models;

import models.crudsiena.SienaSupport;
import siena.Column;
import siena.Generator;
import siena.Id;
import siena.Model;
import siena.Query;
import siena.Table;

@Table("blabla")
public class Blabla extends SienaSupport {
	 @Id(Generator.AUTO_INCREMENT)
     public Long id;
     
     @Column("blabla")
     public String blabla;
     
     public static Query<Blabla> all() {
         return Model.all(Blabla.class);
	 }

}
