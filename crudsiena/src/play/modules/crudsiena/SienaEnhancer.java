package play.modules.crudsiena;

import java.util.HashMap;
import java.util.Map;


import javassist.CtClass;
import javassist.CtField;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.ArrayMemberValue;
import javassist.bytecode.annotation.IntegerMemberValue;
import javassist.bytecode.annotation.MemberValue;
import javassist.bytecode.annotation.StringMemberValue;
import play.Logger;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.classloading.enhancers.Enhancer;
import siena.Max;

public class SienaEnhancer extends Enhancer {

	@Override
	public void enhanceThisClass(ApplicationClass applicationClass)
			throws Exception {
        Logger.debug("CrudSiena: start to enhance class:" + applicationClass.name);
        // this method will be called after configuration finished

        final CtClass ctClass = makeClass(applicationClass);
        
        if (!ctClass.subtypeOf(classPool.get("models.crudsiena.SienaSupport"))) {
            return;
        }
        
        for (CtField cf: ctClass.getDeclaredFields()) {
            if (hasAnnotation(cf, siena.Max.class.getName())) {
            	// retrieves @siena.Max value
            	AnnotationsAttribute attr = getAnnotations(cf);
            	Annotation orig = attr.getAnnotation(Max.class.getName());
            	int val = ((IntegerMemberValue)orig.getMemberValue("value")).getValue();

            	// adds @play.data.validation.MaxSize value
            	Map<String, MemberValue> map = new HashMap<String, MemberValue>();
            	map.put("value", new IntegerMemberValue(attr.getConstPool(), val));

                createAnnotation(
            		attr, 
            		play.data.validation.MaxSize.class, map);
            }
            if (hasAnnotation(cf, siena.NotNull.class.getName())) {
            	// retrieves @siena.NotNull value
            	AnnotationsAttribute attr = getAnnotations(cf);
            	// adds @play.data.validation.Required value
            	createAnnotation(
            		attr, 
            		play.data.validation.Required.class);
            }
            if (hasAnnotation(cf, siena.DateTime.class.getName())) {
            	// retrieves @siena.DateTime value
            	AnnotationsAttribute attr = getAnnotations(cf);
            	
            	// adds @play.data.binding.As(lang={"*"}, value={"yyyy-MM-dd HH:mm:ss"})
            	Map<String, MemberValue> map = new HashMap<String, MemberValue>();
            	ConstPool cp = attr.getConstPool();
            	// creates lang array
            	MemberValue[] langArray = new MemberValue[1];
            	langArray[0] = new StringMemberValue("*", cp);
            	ArrayMemberValue langMember = new ArrayMemberValue(cp);
            	langMember.setValue(langArray);            	
                map.put("lang", langMember);
            	// creates value array
            	MemberValue[] valArray = new MemberValue[1];
            	valArray[0] = new StringMemberValue("yyyy-MM-dd HH:mm:ss", cp);
            	ArrayMemberValue valMember = new ArrayMemberValue(cp);
            	valMember.setValue(valArray);            	
                map.put("value", valMember);

                createAnnotation(
            		attr, 
            		play.data.binding.As.class, map);
            }
        }
        
        // Done.
        applicationClass.enhancedByteCode = ctClass.toBytecode();
        ctClass.defrost();
	}

}
