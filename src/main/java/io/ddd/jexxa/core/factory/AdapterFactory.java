package io.ddd.jexxa.core.factory;


import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang.Validate;

/*
 * Rules for creating a driving/driven adapter:
 * 1. Public Default constructor available
 * 2. Public constructor with Properties as attribute
 * 3. Public static method with return type if the requested interface
 * 4. Public static method with return type if the requested interface and Properties as argument
 */
public class AdapterFactory
{
    private List<String> whiteListPackages = new ArrayList<>();

    public AdapterFactory whiteListPackage(String packageName)
    {
        whiteListPackages.add(packageName);
        return this;
    }

    public <T> T create(Class<T> interfaceType) {
        Validate.notNull(interfaceType);
        //TODO: validate Interface
        //TODO: Better method name e.g. instanceOf

        Class<?> implementation = getImplementationOf(interfaceType);

        //Apply 1. convention and try to use default constructor
        var instance = interfaceType.cast(ClassFactory.newInstanceOf(implementation));
        
        //Apply 2. convention and try to use a factory method 
        if (instance == null) {
            instance = interfaceType.cast(ClassFactory.createByFactoryMethod(implementation, interfaceType));
        }
        
        Validate.notNull(instance, "No suitable constructor found to create " + interfaceType.getName());
        
        return instance;
    }

    public <T> T create(Class<T> interfaceType, Properties properties) {
        Validate.notNull(interfaceType);
        //TODO: validate Interface
        //TODO: Better method name e.g. instance

        Class<?> implementation = getImplementationOf(interfaceType);

        Object[] args = new Object[1];
        args[0]= properties;


        //Apply 1. convention and try to use a constructor accepting properties 
        T instance = interfaceType.cast(ClassFactory.newInstanceOf(implementation, args));

        //Apply 2. convention and try to use a factory method accepting properties
        if (instance == null)
        {
            instance = interfaceType.cast(ClassFactory.createByFactoryMethod(implementation, interfaceType, properties));
        }

        //Try to use default constructor
        //Apply 3. convention and try to use default constructor
        if (instance == null) {
            instance = interfaceType.cast(ClassFactory.newInstanceOf(implementation));
        }

        //Apply 4. convention and try to use a factory method
        if (instance == null) {
            instance = interfaceType.cast(ClassFactory.createByFactoryMethod(implementation, interfaceType));
        }
        
        Validate.notNull(instance, "No suitable constructor found to create " + interfaceType.getName());

        return instance;
    }

    /*Most likely only for DrivingAdapter*/

    public <T> T createByType(Class<T> instanceType, Properties properties) {
        Validate.notNull(instanceType);

        Object[] args = new Object[1];
        args[0]= properties;

        //Apply 1. convention and try to use a constructor accepting properties
        T instance = instanceType.cast(ClassFactory.newInstanceOf(instanceType, args));


        //Apply 2. convention and try to use a factory method accepting properties
        if (instance == null)
        {
            instance = instanceType.cast(ClassFactory.createByFactoryMethod(instanceType, instanceType, properties));
        }

        //Apply 2. convention Try to use default constructor
        if (instance == null)
        {
            instance = ClassFactory.newInstanceOf(instanceType);
        }

        //Apply 4. convention Try to use default factory method 
        if (instance == null)
        {
            instance = ClassFactory.createByFactoryMethod(instanceType,instanceType);
        }

        Validate.notNull(instance, "No suitable constructor found to create " + instanceType.getName());

        return instance;
    }


    boolean validateAdaptersAvailable(List<Class <?> > adapterList)
    {
        var dependencyScanner = new DependencyScanner().
                whiteListPackages(whiteListPackages);
        
        return adapterList.
                stream().
                noneMatch(adapter -> validateImplementationOf(adapter, dependencyScanner) == null);
    }


    private <T> Class<?> getImplementationOf(Class<T> interfaceType) {
        var dependencyScanner = new DependencyScanner().whiteListPackages(whiteListPackages);

        var results = dependencyScanner.getClassesImplementing(interfaceType);

        Validate.notNull(results);
        Validate.notEmpty(results, "No implementation of " + interfaceType.getName() + " available");
        Validate.isTrue( results.size() == 1, "Multiple implementation of " + interfaceType.getName() + " available");

        return validateImplementationOf(interfaceType, dependencyScanner);
    }


    private <T> Class<?> validateImplementationOf(Class<T> interfaceType, DependencyScanner dependencyScanner) {
        var results = dependencyScanner.getClassesImplementing(interfaceType);

        if (results == null || results.isEmpty()) {
            return null;
        }

        return results.get(0);
    }

}