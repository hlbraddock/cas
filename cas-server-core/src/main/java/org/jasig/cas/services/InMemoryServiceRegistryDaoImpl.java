/**
 * Licensed to Jasig under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Jasig licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.jasig.cas.services;

import java.util.ArrayList;
import java.util.List;

import org.jasig.cas.server.session.RegisteredService;
import org.jasig.cas.util.DefaultLongNumericGenerator;
import org.jasig.cas.server.util.LongNumericGenerator;

import javax.validation.constraints.NotNull;

/**
 * Default In Memory Service Registry Dao for test/demonstration purposes.
 * 
 * @author Scott Battaglia
 * @version $Revision: 1.1 $ $Date: 2005/08/19 18:27:17 $
 * @since 3.1
 *
 */
public final class InMemoryServiceRegistryDaoImpl implements ServiceRegistryDao {
    
    @NotNull
    private List<RegisteredService> registeredServices = new ArrayList<RegisteredService>();
    
    private LongNumericGenerator generator = new DefaultLongNumericGenerator();

    public boolean delete(RegisteredService registeredService) {
        return this.registeredServices.remove(registeredService);
    }

    public RegisteredService findServiceById(final long id) {
        for (final RegisteredService r : this.registeredServices) {
            if (r.getId() == id) {
                return r;
            }
        }
        
        return null;
    }

    public List<RegisteredService> load() {
        return this.registeredServices;
    }

    public RegisteredService save(final RegisteredService registeredService) {
        if (registeredService.getId() == -1) {
            ((RegisteredServiceImpl) registeredService).setId(this.generator.getNextLong());
        }

        this.registeredServices.remove(registeredService);
        this.registeredServices.add(registeredService);
        
        return registeredService;
    }

    public void setRegisteredServices(final List<RegisteredService> registeredServices) {
        this.registeredServices = registeredServices;
    }
}
