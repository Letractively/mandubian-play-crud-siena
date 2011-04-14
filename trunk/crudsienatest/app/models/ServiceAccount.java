package models;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import models.crudsiena.SienaSupport;
import siena.Column;
import siena.Id;
import siena.Index;
import siena.Model;
import siena.Query;
import siena.Table;

@Table("blabla")
public class ServiceAccount extends SienaSupport {
	@Id
    public Long id;

    @Column("owner") @Index("owner_index")
	public Employee owner;
	
    public static enum GeolocationService {
        FOURSQUARE(0xF059),
        LATITUDE (0x147),
        GEZENZI (0x932),
        FACEBOOK (0xFACE),
        TWITTER (0x71773);
       
        private long code;
       
        private GeolocationService(long code){
            this.code = code;
        }
        
        public long getCode(){
            return this.code;
        }
        
        private static final Map<Long, GeolocationService> lookup = new HashMap<Long, GeolocationService>();
        static {
            for (GeolocationService g : EnumSet.allOf(GeolocationService.class))
                lookup.put(g.getCode(), g);
        }
       
        public static GeolocationService get(long code){
            return lookup.get(code);
        }
    }

    public GeolocationService geoloc;
    
    public static Query<ServiceAccount> all() {
        return Model.all(ServiceAccount.class);
    }


}
