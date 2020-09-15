package org.terasology.gestalt.di.singleton;

import javax.inject.Singleton;

@Singleton
public class BasicSingleton {
    
    public String returnString(){
        return "It's works!";
    }
}
