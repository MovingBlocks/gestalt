package org.terasology.gestalt.android.testbed.packageModuleB;

import org.terasology.test.api.ApiInterface;

public class PackageModuleBText implements ApiInterface {

    @Override
    public String apiMethod() {
        return "Greetings from Package Module B";
    }
}
