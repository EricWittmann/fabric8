/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.cxf;

public class FabricFailOverFeature extends FabricLoadBalancerFeature {

    protected LoadBalanceStrategy getDefaultLoadBalanceStrategy() {
        // This strategy always return the first physical address from the locator
        return new FirstOneLoadBalanceStrategy();
    }
}
