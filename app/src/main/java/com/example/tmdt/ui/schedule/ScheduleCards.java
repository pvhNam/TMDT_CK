package com.example.tmdt.ui.schedule;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/** Retains cards across duplicate snapshots; content must be a detached value snapshot. */
final class ScheduleCards<V> {
    private static final class Item<V> {
        final Object content;
        final V view;
        Item(Object content,V view){this.content=content;this.view=view;}
    }
    private final Map<String,Item<V>> items=new HashMap<>();
    V get(String id,Object content,Supplier<V> create){
        Item<V> previous=items.get(id);
        if(previous!=null&&previous.content.equals(content))return previous.view;
        V view=create.get();items.put(id,new Item<>(content,view));return view;
    }
    void retain(Set<String> ids){items.keySet().retainAll(ids);}
    void clear(){items.clear();}
}
