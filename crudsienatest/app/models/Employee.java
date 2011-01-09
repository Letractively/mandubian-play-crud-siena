package models;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.google.appengine.api.datastore.Blob;

import play.data.binding.As;
import play.data.validation.MaxSize;
import play.data.validation.Password;
import play.modules.crudsiena.CrudUnique;

import models.crudsiena.SienaSupport;
import siena.Column;
import siena.DateTime;
import siena.Filter;
import siena.Generator;
import siena.Id;
import siena.Index;
import siena.Json;
import siena.Max;
import siena.Model;
import siena.NotNull;
import siena.Query;
import siena.Table;
import siena.embed.At;
import siena.embed.Embedded;
import siena.embed.EmbeddedList;
import siena.embed.EmbeddedMap;


@Table("employees")
public class Employee extends SienaSupport {
        
        @Id(Generator.AUTO_INCREMENT)
        public Long id;
        
        @Column("first_name")
        @Max(10) @NotNull
        @CrudUnique
        //@play.data.validation.MaxSize(10) @play.data.validation.Required
        public String firstName;
                
        @Column("last_name")
        @Max(200) @NotNull
        public String lastName;
        
        @Password
        @Column("pwd")
        public String pwd;
        
        @Column("contact_info")
        @NotNull
        public Json contactInfo;       
        
        @Column("hire_date")
        public Date hireDate;
        
        @Column("fire_date")
        @DateTime
        //@As(lang={"*"}, value={"yyyy-MM-dd hh:mm:ss"})
        public Date fireDate;
        
        @Column("boss") @Index("boss_index")
        public Employee boss;
        
        @Filter("boss")
        public Query<Employee> employees;
               
        @Embedded
        public Image profileImage;
        
        @Embedded
        public List<Image> otherImages;

        @Embedded
        public Map<String, Image> stillImages;
        
        @EmbeddedMap
        public class Image {
                public String filename;
                public String title;
                public int views;
        }
              
        @Embedded
        public List<UserBlabla> items;

        @Column("blob")
        @As(binder=models.crudsiena.GaeBlobBinder.class)
        public Blob blob;
        
        @EmbeddedList
        public class UserBlabla {
          @At(0) public String item;
          @At(1) public String item2;
        }
        
        public static Query<Employee> all() {
                return Model.all(Employee.class);
        }
        
        public String toString() {
        	return firstName + " " + lastName;
        }

}