package models;

import models.crudsiena.SienaSupport;
import siena.Column;
import siena.Generator;
import siena.Id;
import siena.Model;
import siena.Query;
import siena.Table;
import siena.Text;

@Table("blabla")
public class Question extends SienaSupport {
	@Id
    public Long qID;
    
    public String question;

    @Text
    public String answer;

    public String slug_anchor;

    public Boolean publish = false;

    @Column("user_id")
    public Long userID;

    @Column("container_id")
    public Long containerID;

    @Column("version_id")
    public Long versionID;

    public Question(String name, String answer, String slug, Long userID, Long containerID, Long versionID) {
        this.question = name;
        this.answer = answer;
        this.slug_anchor = play.templates.JavaExtensions.slugify(slug);
        this.containerID = containerID;
        this.userID = userID;
        this.versionID = versionID;
    }
    
    public static Query<Question> all() {
        return Model.all(Question.class);
    }

    public static Question byId(Long id) {
        return all().filter(("qID"), id).get();    
    }

}
