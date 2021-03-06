/*
 * Licensed to Jasig under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Jasig licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License.  You may obtain a
 * copy of the License at the following location:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jasig.cas.adaptors.x509.authentication.principal;

import java.io.FileInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Unit test for {@link X509CertificateCredentialsToSubjectPrinciplalResolver}.
 *
 * @author Marvin S. Addison
 * @version $Revision$ $Date$
 *
 */
@RunWith(Parameterized.class)
public class X509CertificateCredentialsToSubjectPrinciplalResolverTests {
    
    private X509Certificate certificate;
    private X509CertificateCredentialsToSubjectPrinciplalResolver resolver;
    private String expected;

    /**
     * Creates a new test instance with the given parameters.
     *
     * @param certPath
     * @param descriptor
     * @param expectedResult
     */
    public X509CertificateCredentialsToSubjectPrinciplalResolverTests(
        final String certPath,
        final String descriptor,
        final String expectedResult) {
       
        this.resolver = new X509CertificateCredentialsToSubjectPrinciplalResolver();
        this.resolver.setDescriptor(descriptor);
        try {
	        this.certificate = (X509Certificate) CertificateFactory.getInstance("X509").generateCertificate(
		            new FileInputStream(certPath));
        } catch (Exception e) {
            Assert.fail(String.format("Error parsing certificate %s: %s", certPath, e.getMessage()));
        }
        this.expected = expectedResult;
    }

    /**
     * Gets the unit test parameters.
     *
     * @return  Test parameter data.
     */
    @Parameters
    public static Collection<Object[]> getTestParameters()
    {
      final Collection<Object[]> params = new ArrayList<Object[]>();
      
      // Test case #1
      // Use CN for principal ID
      params.add(new Object[] {
          "target/test-classes/x509-ctop-resolver-hizzy.crt",
          "$CN",
          "Hizzogarthington I.S. Pleakinsense"
      });
      
      // Test case #2
      // Use email address for principal ID
      params.add(new Object[] {
          "target/test-classes/x509-ctop-resolver-hizzy.crt",
          "$EMAILADDRESS",
          "hizzy@vt.edu"
      });
      
      // Test case #2
      // Use combination of ou and cn for principal ID
      params.add(new Object[] {
          "target/test-classes/x509-ctop-resolver-hizzy.crt",
          "$OU $CN",
          "Middleware Hizzogarthington I.S. Pleakinsense"
      });

      // Test case #3
      // Use combination of serial number and cn for principal ID
      params.add(new Object[] {
          "target/test-classes/x509-ctop-resolver-gazzo.crt",
          "$CN:$SERIALNUMBER",
          "Gazzaloddi P. Wishwashington:271828183"
      });

      // Test case #4
      // Build principal ID from multivalued attributes
      params.add(new Object[] {
          "target/test-classes/x509-ctop-resolver-jacky.crt",
          "$UID@$DC.$DC",
          "jacky@vt.edu"
      });
      
      return params;
    }

    @Test
    public void testResolvePrincipalInternal() {
        Assert.assertEquals(this.expected, this.resolver.resolvePrincipalInternal(this.certificate));
    }

}
