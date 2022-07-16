package com.rrat.ogey.components;

import java.util.ArrayList;
import java.util.Objects;

public class CommandPermissions {

    private final ArrayList<String> allowed = new ArrayList<>();
    private final ArrayList<String> denied = new ArrayList<>();
    public boolean denyall;

    public String[] getAllowed(){
        return allowed.toArray(String[]::new);
    }

    public String[] getDenied() {
        return denied.toArray(String[]::new);
    }

    public void addAllowed(String arguments){
        allowed.add(arguments);
    }

    public void addDenied(String arguments){
        denied.add(arguments);
    }

    public void removePermission(String arguments){
        allowed.removeIf(s -> Objects.equals(s, arguments));
        denied.removeIf(s -> Objects.equals(s, arguments));
    }



}
