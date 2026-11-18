package com.example.tmdt.ui.common;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/** Detached values for comparing app data, never Android views, listeners or SDK internals. */
public final class RenderSnapshot {
    private RenderSnapshot(){}
    public static Object of(Object... values){return freeze(Arrays.asList(values),new IdentityHashMap<>());}
    private static Object freeze(Object value,IdentityHashMap<Object,Boolean> visiting){
        if(value==null||value instanceof String||value instanceof Number||value instanceof Boolean||value instanceof Character)return value;
        if(value instanceof Date)return ((Date)value).getTime();
        if(value instanceof Enum||value instanceof java.time.temporal.TemporalAccessor)return value.toString();
        if(value instanceof com.google.firebase.Timestamp)return value;
        if(visiting.put(value,true)!=null)return "cycle";
        try{
            if(value instanceof Map){Map<Object,Object> out=new LinkedHashMap<>();
                for(Map.Entry<?,?> item:((Map<?,?>)value).entrySet())out.put(item.getKey(),freeze(item.getValue(),visiting));return out;}
            if(value instanceof Set){Set<Object> out=new HashSet<>();for(Object item:(Set<?>)value)out.add(freeze(item,visiting));return out;}
            if(value instanceof Iterable){List<Object> out=new ArrayList<>();for(Object item:(Iterable<?>)value)out.add(freeze(item,visiting));return out;}
            if(value.getClass().isArray()){List<Object> out=new ArrayList<>();for(int i=0;i<Array.getLength(value);i++)out.add(freeze(Array.get(value,i),visiting));return out;}
            String type=value.getClass().getName();
            if(!type.startsWith("com.example.tmdt.")||type.startsWith("com.example.tmdt.ui."))return null;
            Map<String,Object> out=new LinkedHashMap<>();out.put("type",type);
            for(Field field:value.getClass().getFields()){
                if(Modifier.isStatic(field.getModifiers())||field.isSynthetic()||field.getName().equals("changes")
                        ||field.getName().equals("destination")||field.getName().equals("revenueAnimated"))continue;
                try{out.put(field.getName(),freeze(field.get(value),visiting));}
                catch(IllegalAccessException ignored){throw new IllegalStateException("Cannot snapshot "+field.getName(),ignored);}
            }
            return out;
        }finally{visiting.remove(value);}
    }
}
