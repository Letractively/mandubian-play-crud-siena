## How to use the CRUD-Siena Play! framework ##
### First of all, you have to enable Siena support to your Play! project ###
Just call the install command for module siena
```
play install siena
```

> _You can find information about it directly [there](http://www.playframework.org/modules/siena-1.3/home)_

> #### YOU SHALL USE VERSION >=1.2 of the siena module because it integrates Siena 0.7.5 correcting an important issue concerning JSON (de)serialization of embedded objects ####

---


### Then you install CrudSiena module ###
_For Play! <1.1_, install CrudSiena <=1.01:
```
play install crudsiena-1.01
```

_For Play! >=1.1_, install CrudSiena >=1.1:
```
play install crudsiena-1.1
```



---


### You need to make your Siena entity inherit the class SienaSupport ###
_This is exactly the sister class of Play! default class JpaSupport_
> For example:
```
@Table("employees")
public class Employee extends SienaSupport {
        
        @Id(Generator.AUTO_INCREMENT)
        public Long id;
        
        @Column("first_name")
        @Max(100) @NotNull
        public String firstName;
        
        @Column("last_name")
        @Max(200) @NotNull
        public String lastName;
        
        @Column("contact_info")
        public Json contactInfo;
        
        @Column("hire_date")
        public Date hireDate;

        @Column("fire_date")
        @DateTime	// displays a timepicker (with date & time)
        @As(lang={"*"}, value={"yyyy-MM-dd hh:mm:ss"}) // REQUIRED to retrieve the time selected with JQueryUI Timepicker in Play!
        public Date fireDate;

        @Column("boss") 
        @Index("boss_index")
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
        
        public static Query<Employee> all() {
                return Model.all(Employee.class);
        }
        
        public String toString() {
                return firstName + " " + lastName;
        }

}
```

Please be aware of the possibility to manage:
  * _Json_ fields
  * _One to Many_ relation
  * _Many to One_ relation
  * _Embedded_ json-serialized objects : single object/list/map
  * _@Max_/_@Min are managed
  *_@DateTime_is managed and displays a timepicker (date+time) but you MUST add also the following annotation so that it gets the time also:
`@As(lang={"*"}, value={"yyyy-MM-dd hh:mm:ss"})`_

**With GAE, there are limitations to be aware of (at least until further evolutions):
  * _@Id_ annotated field corresponding to the primary key must be Long type
  * _@Id_ annotated field corresponding to the primary key must be called "id"**


For more information about Siena, go directly to [Siena site](http://www.sienaproject.com)



---


### You need to create a controller inheriting class CRUD for each Siena entity requiring CRUD support ###
_This is exactly the sister class of Play! default controller CRUD_

The name of the Controller class corresponding to a model class can be:
  * **ModelClassName** _(this option is valid on in crudsiena because I found it more intuitive)_
> For example:
```
public class Employee extends controllers.CRUD {    

}
```

  * **ModelClassName+'s'** (pluralized) _(but any char instead of 's' is OK also)_
> For example:
```
public class Employees extends controllers.CRUD {    

}
```

  * **@For(ModelClass)** annotation added to your controller
> For example:
```
@For(models.Employee.class)
public class Employees extends controllers.CRUD {    

}
```


---


### You need to enable routes to the CRUD module ###
At the beginning of your conf/routes file, add:
```
*      	/admin             						module:crudsiena
```