package net.singular.authenticator;

import net.singular.authenticator.testability.DependencyInjector;

import java.util.ArrayList;

public class SingularCodeUtils {
    public String getUsername(int id){
        AccountDb mAccountDb = DependencyInjector.getAccountDb();
        ArrayList<String> usernames = new ArrayList<String>();
        mAccountDb.getNames(usernames);
        return usernames.get(id);
    }
}