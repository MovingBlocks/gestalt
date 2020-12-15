package org.terasology.context;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class AnnotationMetaUtil {

    private AnnotationMetaUtil() {
        // Util class. don't create.
    }
    public static Map<String,Object> of(Object ... values){
        if(values.length == 0){
            return Collections.emptyMap();
        }
        HashMap<String,Object> result = new HashMap<>();
        int i = 0;
        while(i < values.length - 1){
            String key = values[i++].toString();
            Object value = values[i++];
            result.put(key,value);
        }
        return result;
    }


}
