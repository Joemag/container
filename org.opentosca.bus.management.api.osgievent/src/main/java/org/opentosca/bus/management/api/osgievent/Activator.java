package org.opentosca.bus.management.api.osgievent;

import org.apache.camel.component.direct.DirectComponent;
import org.apache.camel.component.stream.StreamComponent;
import org.apache.camel.core.osgi.OsgiDefaultCamelContext;
import org.apache.camel.core.osgi.OsgiServiceRegistry;
import org.apache.camel.impl.DefaultCamelContext;
import org.opentosca.bus.management.api.osgievent.route.Route;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Activator of the OSGiEvent-Management Bus-API.<br>
 * <br>
 * <p>
 * Copyright 2013 IAAS University of Stuttgart <br>
 * <br>
 * <p>
 * The activator is needed to add and start the camel routes.
 *
 * @author Michael Zimmermann - zimmerml@studi.informatik.uni-stuttgart.de
 */
public class Activator implements BundleActivator {

  public static String apiID;

  static DefaultCamelContext camelContext;

  final private static Logger LOG = LoggerFactory.getLogger(Activator.class);

  @Override
  public void start(final BundleContext bundleContext) throws Exception {

    Activator.apiID = bundleContext.getBundle().getSymbolicName();

    final OsgiServiceRegistry reg = new OsgiServiceRegistry(bundleContext);
    Activator.camelContext = new OsgiDefaultCamelContext(bundleContext, reg);

    // This explicitly binds the direct component, which should fix the OSGI startup
    Activator.camelContext.addComponent("direct", new DirectComponent());
    Activator.camelContext.addComponent("stream", new StreamComponent());

    Activator.camelContext.addRoutes(new Route());
    Activator.camelContext.start();
    Activator.LOG.info("Management Bus-OSGI-Event API started!");

  }

  @Override
  public void stop(final BundleContext arg0) throws Exception {
    Activator.camelContext = null;
    Activator.LOG.info("Management Bus-OSGI-Event API stopped!");

  }
}
