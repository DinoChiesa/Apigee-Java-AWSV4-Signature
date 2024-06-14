// Copyright (c) 2016 Apigee Corp, 2017-2024 Google LLC
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
import java.util.Map;
import org.testng.annotations.BeforeMethod;

public abstract class TestBase {

  static {
    java.security.Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
  }

  protected FakeMessage message;
  protected FakeMessageContext msgCtxt;
  protected FakeExecutionContext exeCtxt;

  @BeforeMethod()
  public void testSetup1() {
    message = new FakeMessage();
    msgCtxt = new FakeMessageContext(message);
    exeCtxt = new FakeExecutionContext();

    // msgCtxt =
    //     new MockUp<MessageContext>() {
    //       private Map<String, Object> variables;
    //
    //       public void $init() {
    //         getVariables();
    //       }
    //
    //       private Map<String, Object> getVariables() {
    //         if (variables == null) {
    //           variables = new HashMap<String, Object>();
    //         }
    //         return variables;
    //       }
    //
    //       @Mock()
    //       public Object getVariable(final String name) {
    //         return getVariables().get(name);
    //       }
    //
    //       @Mock()
    //       public boolean setVariable(final String name, final Object value) {
    //         if (isVerbose())
    //           System.out.printf(
    //               "setVariable(%s) <= %s\n", name, (value != null) ? value : "(null)");
    //         getVariables().put(name, value);
    //         return true;
    //       }
    //
    //       @Mock()
    //       public boolean removeVariable(final String name) {
    //         if (isVerbose()) System.out.printf("removeVariable(%s)\n", name);
    //         if (getVariables().containsKey(name)) {
    //           variables.remove(name);
    //         }
    //         return true;
    //       }
    //     }.getMockInstance();
    //
    // exeCtxt = new MockUp<ExecutionContext>() {}.getMockInstance();
    //
    // message =
    //     new MockUp<Message>() {
    //       private Map<String, Object> variables;
    //       private Map<String, Object> headers;
    //       private Map<String, Object> qparams;
    //       private String content;
    //
    //       public void $init() {
    //         getVariables();
    //       }
    //
    //       private Map<String, Object> getVariables() {
    //         if (variables == null) {
    //           variables = new HashMap<String, Object>();
    //         }
    //         return variables;
    //       }
    //
    //       private Map<String, Object> getHeaders() {
    //         if (headers == null) {
    //           headers = new HashMap<String, Object>();
    //         }
    //         return headers;
    //       }
    //
    //       private Map<String, Object> getQparams() {
    //         if (qparams == null) {
    //           qparams = new HashMap<String, Object>();
    //         }
    //         return qparams;
    //       }
    //
    //       @Mock()
    //       public String getContent() {
    //         return this.content;
    //       }
    //
    //       @Mock()
    //       public void setContent(String content) {
    //         this.content = content;
    //       }
    //
    //       @Mock()
    //       public Object getVariable(final String name) {
    //         return getVariables().get(name);
    //       }
    //
    //       @Mock()
    //       public boolean setVariable(final String name, final Object value) {
    //         getVariables().put(name, value);
    //         return true;
    //       }
    //
    //       @Mock()
    //       public boolean removeVariable(final String name) {
    //         if (getVariables().containsKey(name)) {
    //           variables.remove(name);
    //         }
    //         return true;
    //       }
    //
    //       @Mock()
    //       public String getHeader(final String name) {
    //         List<String> headerList = getHeaders(name);
    //         return (headerList != null) ? headerList.get(0) : null;
    //       }
    //
    //       @Mock()
    //       public List<String> getHeaders(final String name) {
    //         String lowerName = name.toLowerCase();
    //         if (getHeaders().containsKey(lowerName)) {
    //           @SuppressWarnings("unchecked")
    //           List<String> list = (List<String>) getHeaders().get(lowerName);
    //           return list;
    //         }
    //         return null;
    //       }
    //
    //       @Mock()
    //       public boolean setHeader(final String name, final Object value) {
    //         String lowerName = name.toLowerCase();
    //         if (isVerbose()) {
    //           System.out.printf(
    //               "setHeader(%s) <= %s\n", lowerName, (value != null) ? value : "(null)");
    //         }
    //         if (getHeaders().containsKey(lowerName)) {
    //           if (!lowerName.equals("host")) {
    //             @SuppressWarnings("unchecked")
    //             List<String> values = (List<String>) getHeaders().get(lowerName);
    //             values.add(value.toString());
    //           }
    //         } else {
    //           List<String> values = new ArrayList<String>();
    //           values.add(value.toString());
    //           getHeaders().put(lowerName, values);
    //         }
    //         return true;
    //       }
    //
    //       @Mock()
    //       public boolean removeHeader(final String name) {
    //         String lowerName = name.toLowerCase();
    //         if (isVerbose()) {
    //           System.out.printf("removeHeader(%s)\n", lowerName);
    //         }
    //         if (getHeaders().containsKey(lowerName)) {
    //           getHeaders().remove(lowerName);
    //         }
    //         return true;
    //       }
    //
    //       @Mock()
    //       public Set<String> getHeaderNames() {
    //         return getHeaders().entrySet().stream()
    //             .map(e -> e.getKey())
    //             .collect(Collectors.toSet());
    //       }
    //
    //       @Mock()
    //       public Set<String> getQueryParamNames() {
    //         return getQparams().entrySet().stream()
    //             .map(e -> e.getKey())
    //             .collect(Collectors.toSet());
    //       }
    //
    //       @Mock()
    //       public String getQueryParam(final String name) {
    //         List<String> paramList = getQueryParams(name);
    //         return (paramList != null) ? paramList.get(0) : null;
    //       }
    //
    //       @Mock()
    //       public boolean setQueryParam(final String name, final Object value) {
    //         if (isVerbose()) {
    //           System.out.printf(
    //               "setQueryParam(%s) <= %s\n", name, (value != null) ? value : "(null)");
    //         }
    //         if (getQparams().containsKey(name)) {
    //           @SuppressWarnings("unchecked")
    //           List<String> values = (List<String>) getQparams().get(name);
    //           values.add(value.toString());
    //         } else {
    //           List<String> values = new ArrayList<String>();
    //           values.add(value.toString());
    //           getQparams().put(name, values);
    //         }
    //         return true;
    //       }
    //
    //       @Mock()
    //       public List<String> getQueryParams(final String name) {
    //         if (getQparams().containsKey(name)) {
    //           @SuppressWarnings("unchecked")
    //           List<String> list = (List<String>) getQparams().get(name);
    //           return list;
    //         }
    //         return null;
    //       }
    //     }.getMockInstance();

    System.out.printf("=============================================\n");
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
