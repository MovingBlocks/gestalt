package org.terasology.gestalt.android.testbed.packageModuleA;

import org.terasology.test.api.ApiInterface;

public class PackageModuleAText implements ApiInterface {

    @Override
    public String apiMethod() {
        return "Hello from Package Module A!";
    }
}
