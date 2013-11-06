package org.jboss.test.hornetqinterop;

import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.NoInitialContextException;
import javax.naming.spi.InitialContextFactory;
import java.util.Hashtable;

/**
 * Initial context factory which returns {@code NamingContext} instances.
 * TODO
 *
 * @author <a href="mailto:istudens@redhat.com">Ivo Studensky</a>
 */
public class ModuleInitialContextFactory implements InitialContextFactory {
    public static final String MODULE_INITIAL_CONTEXT_FACTORY = "jboss.naming.module.factory.initial";
    public static final String MODULE_NAME = "jboss.naming.module.name";

    /**
     * Get an initial context instance.
     *
     * @param environment The naming environment
     * @return A naming context instance
     * @throws javax.naming.NamingException
     */
    @SuppressWarnings("unchecked")
    public Context getInitialContext(Hashtable<?, ?> environment) throws NamingException {
        final String moduleName = (String) environment.get(MODULE_NAME);
        final Module module;
        try {
            module = Module.getBootModuleLoader().loadModule(ModuleIdentifier.create("eap5-client"));
        } catch (Throwable e) {
            throw new NoInitialContextException("Could not find a module " + moduleName);
        }
        if (module == null)
            throw new NoInitialContextException("Could not find a module " + moduleName);

        final ClassLoader currentCL = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(module.getClassLoader());

            final String initialFactory = (String) environment.get(MODULE_INITIAL_CONTEXT_FACTORY);
            if (initialFactory == null)
                throw new NoInitialContextException("No initial context factory found under " + MODULE_INITIAL_CONTEXT_FACTORY + " environment property");

            final Hashtable<String, Object> envWithProperFactory = new Hashtable<String, Object>((Hashtable<String, Object>) environment);
            envWithProperFactory.put(Context.INITIAL_CONTEXT_FACTORY, initialFactory);

            return new InitialContext(envWithProperFactory);
        } finally {
            Thread.currentThread().setContextClassLoader(currentCL);
        }
    }

}
