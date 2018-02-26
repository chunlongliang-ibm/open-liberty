/*******************************************************************************
 * Copyright (c) 2010, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package suite.r80.base.ejb31misc.jcdi.ejb;

/**
 * Common remote interface for beans that verify CDI BeanManager
 * injection and lookups.
 **/
public interface BeanManagerRemote {
    /**
     * Verifies that the BeanManager was properly injected per the
     * conifguration of the bean, and that the BeanManager may be
     * looked up.
     **/
    public void verifyBeanMangerInjectionAndLookup();
}
