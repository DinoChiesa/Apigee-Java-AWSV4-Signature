// Copyright © 2016 Apigee Corp, 2017-2024 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package com.google.apigee.callouts;

import com.google.apigee.fakes.FakeExecutionContext;
import com.google.apigee.fakes.FakeMessage;
import com.google.apigee.fakes.FakeMessageContext;
import java.lang.reflect.Method;
import java.util.Map;
import org.testng.annotations.BeforeMethod;

public abstract class TestBase {

  static {
    java.security.Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
  }

  protected FakeMessage message;
  protected FakeMessageContext msgCtxt;
  protected FakeExecutionContext exeCtxt;

  @BeforeMethod
  public void beforeMethod(Method method) throws Exception {
    String methodName = method.getName();
    String className = method.getDeclaringClass().getName();
    System.out.printf("\n\n==================================================================\n");
    System.out.printf("TEST %s.%s()\n", className, methodName);

    message = new FakeMessage();
    msgCtxt = new FakeMessageContext(message);
    exeCtxt = new FakeExecutionContext();
  }

  protected void reportThings(Map<String, String> props) {
    String test = props.get("testname");
    System.out.println("test  : " + test);
    String cipher = msgCtxt.getVariable("crypto_cipher");
    System.out.println("cipher: " + cipher);
    String action = msgCtxt.getVariable("crypto_action");
    System.out.println("action: " + action);
    String output = msgCtxt.getVariable("crypto_output");
    System.out.println("output: " + output);
    String keyHex = msgCtxt.getVariable("crypto_key_b16");
    System.out.println("key   : " + keyHex);
    String ivHex = msgCtxt.getVariable("crypto_iv_b16");
    System.out.println("iv    : " + ivHex);
    String aadHex = msgCtxt.getVariable("crypto_aad_b16");
    System.out.println("aad   : " + aadHex);
    String saltHex = msgCtxt.getVariable("crypto_salt_b16");
    System.out.println("salt  : " + saltHex);
    // Assert.assertNotNull(ivHex);
    // Assert.assertNotNull(output);
  }

  protected String xform(String suffix) {
    return ((String) msgCtxt.getVariable("awsv4sig_" + suffix)).replaceAll("↵", "\n");
  }

  abstract boolean isVerbose();
}
